package ru.deewend.ccjlbridge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Log {
    public static final byte LOG_LEVEL_INFO = 0;
    public static final byte LOG_LEVEL_WARN = 1;
    public static final byte LOG_LEVEL_SEVERE = 2;

    private static final DateFormat FORMAT = new SimpleDateFormat(ClassiCubeJavaLoaderBridge.LOG_FORMAT);

    private Log() {
    }

    public static String f(String fmt, Object... args) {
        return String.format(fmt, args);
    }

    public static void i(String message) {
        l(LOG_LEVEL_INFO, message, null);
    }

    public static void w(String message) {
        l(LOG_LEVEL_WARN, message, null);
    }

    public static void w(String message, Throwable t) {
        l(LOG_LEVEL_WARN, message, t);
    }

    public static void s(String message) {
        l(LOG_LEVEL_SEVERE, message, null);
    }

    public static void s(String message, Throwable t) {
        l(LOG_LEVEL_SEVERE, message, t);
    }

    public static void l(byte logLevel, String message) {
        l(logLevel, message, null);
    }

    public static void l(byte logLevel, String message, Throwable t) {
        String prefix;
        switch (logLevel) {
            case LOG_LEVEL_INFO:
                prefix = "[INFO] ";
                break;
            case LOG_LEVEL_WARN:
                prefix = "[WARN] ";
                break;
            case LOG_LEVEL_SEVERE:
                prefix = "[SEVERE] ";
                break;
            default:
                throw new IllegalArgumentException("Unknown logLevel: " + logLevel);
        }
        PrintStream stream = (logLevel == LOG_LEVEL_INFO ? System.out : System.err);
        if (!ClassiCubeJavaLoaderBridge.SHOULD_SAVE_LOGS_ON_DISK) {
            synchronized (Log.class) {
                print(stream, prefix, message, t);
            }

            return;
        }
        try (FileOutputStream fileStream = getLogFileOutputStream()) {
            PrintStream filePrintStream = new PrintStream(fileStream);

            synchronized (Log.class) {
                print(stream, prefix, message, t);
                print(filePrintStream, prefix, message, t);
            }
        } catch (IOException e) {
            System.err.println("Failed to print a log entry");
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    private static FileOutputStream getLogFileOutputStream() throws IOException {
        File logsDir = new File("./javalogs/");
        if (!logsDir.isDirectory()) {
            if (!logsDir.mkdir()) {
                throw new IOException("Could not create the ./logs/ directory");
            }
        }
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        String nameFormat = ClassiCubeJavaLoaderBridge.LOG_FILENAME_FORMAT;
        nameFormat = nameFormat.replace("dd", adjust(dayOfMonth, 2));
        nameFormat = nameFormat.replace("MM", adjust(month, 2));
        nameFormat = nameFormat.replace("yyyy", adjust(year, 4));

        File currentLogFile = new File(logsDir, nameFormat);
        //noinspection ResultOfMethodCallIgnored
        currentLogFile.createNewFile();

        return new FileOutputStream(currentLogFile, true);
    }

    private static String adjust(int dateElement, int count) {
        return dateElementToString(String.valueOf(dateElement), count, '0');
    }

    public static String dateElementToString(
            String element, int minLength, char padding
    ) {
        int length = element.length();
        if (length >= minLength) {
            return element;
        }
        int delta = minLength - length;
        char[] chars = new char[minLength];
        for (int i = 0; i < chars.length; i++) {
            if (i < delta) {
                chars[i] = padding;
            } else {
                chars[i] = element.charAt(i - delta);
            }
        }

        return String.valueOf(chars);
    }

    private static void print(
            PrintStream stream, String prefix, String message, Throwable t
    ) {
        stream.println(FORMAT.format(new Date()) + prefix + message);
        if (t != null) {
            t.printStackTrace(stream);
        }
    }
}
