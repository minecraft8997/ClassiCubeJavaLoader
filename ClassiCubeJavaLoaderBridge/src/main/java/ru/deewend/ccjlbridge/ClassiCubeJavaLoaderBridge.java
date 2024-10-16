package ru.deewend.ccjlbridge;

import ccjl.Interface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClassiCubeJavaLoaderBridge {
    public static boolean DEBUG = false;
    public static final int VERSION_CODE = 1;

    public static final String LOG_FORMAT = "[HH:mm:ss dd.MM.yyyy] ";
    public static final String LOG_FILENAME_FORMAT = "dd-MM-yyyy-logs.txt";
    public static final boolean SHOULD_SAVE_LOGS_ON_DISK = true;

    private static ClassiCubeJavaLoaderBridge instance;

    private final List<String> pendingChatMessages = new ArrayList<>();

    private ClassiCubeJavaLoaderBridge() {
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(Interface.getPendingScheduledTaskIDs()));
        System.out.println(Arrays.toString(Interface.getPendingScheduledTaskIntervals()));
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
        PluginManager.getInstance().loadPlugins();
        int loaded = PluginManager.getInstance().getPluginCount();
        ClassiCubeJavaLoaderBridge.getInstance()
                .addChatMessage("Initialized " + loaded + " Java plugin" + (loaded != 1 ? "s" : ""));

        return true;
    }

    public void addChatMessage(String message) {
        Objects.requireNonNull(message);

        synchronized (pendingChatMessages) {
            pendingChatMessages.add(message);
        }
    }

    public String[] getPendingChatMessages() {
        return getPendingChatMessages(true);
    }

    public String[] getPendingChatMessages(boolean reset) {
        String[] messages;
        synchronized (pendingChatMessages) {
            messages = pendingChatMessages.toArray(new String[0]);

            if (reset) pendingChatMessages.clear();
        }

        return messages;
    }
}
