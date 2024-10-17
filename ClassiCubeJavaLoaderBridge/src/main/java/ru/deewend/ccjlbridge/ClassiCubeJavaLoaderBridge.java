package ru.deewend.ccjlbridge;

import ru.deewend.ccjlbridge.gameapi.Chat;

public class ClassiCubeJavaLoaderBridge {
    public static boolean DEBUG = false;
    public static final int VERSION_CODE = 4;

    public static final String LOG_FORMAT = "[HH:mm:ss dd.MM.yyyy] ";
    public static final String LOG_FILENAME_FORMAT = "dd-MM-yyyy-logs.txt";
    public static final boolean SHOULD_SAVE_LOGS_ON_DISK = true;

    private static ClassiCubeJavaLoaderBridge instance;

    private ClassiCubeJavaLoaderBridge() {
    }

    public static boolean start() {
        return (new ClassiCubeJavaLoaderBridge()).run();
    }

    public static ClassiCubeJavaLoaderBridge getInstance() {
        return instance;
    }

    public boolean run() {
        synchronized (this) {
            if (instance != null) {
                Log.s("Found a sign of multiple ClassiCubeJavaLoaderBridge instances inside a single JVM");
                Log.s("The game will be terminated");

                System.exit(-1);
            }

            instance = this;
        }
        System.out.println();
        PluginManager.getInstance().loadPlugins();
        int loaded = PluginManager.getInstance().getPluginCount();
        Chat.add("Loaded " + loaded + " Java plugin" + (loaded != 1 ? "s" : ""));

        PluginManager.getInstance().call("init");

        return true;
    }
}
