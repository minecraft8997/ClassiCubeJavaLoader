package ru.deewend.ccjlbridge.util;

import ru.deewend.ccjlbridge.Log;

import java.io.File;

public class Helper {
    private Helper() {
    }

    public static File[] listPlugins() {
        File pluginsDir = new File("./javaplugins/");
        if (!pluginsDir.isDirectory()) {
            if (!pluginsDir.mkdir()) {
                Log.w("Failed to create javaplugins directory");

                return null;
            }
            Log.i("Created javaplugins directory");
        }

        return pluginsDir.listFiles(file -> file.getName().endsWith(".jar"));
    }
}
