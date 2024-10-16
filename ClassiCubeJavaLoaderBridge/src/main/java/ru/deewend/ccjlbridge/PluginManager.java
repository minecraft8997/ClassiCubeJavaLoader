package ru.deewend.ccjlbridge;

import ru.deewend.ccjlbridge.util.Helper;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginManager {
    private static final PluginManager INSTANCE = new PluginManager();
    private static final int MAX_LOAD_ATTEMPTS = 512;

    private final Map<String, Plugin> pluginMap = new HashMap<>();

    private PluginManager() {
    }

    public static PluginManager getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("BusyWait")
    void loadPlugins() {
        File[] files = Helper.listPlugins();
        if (files == null || files.length == 0) return;

        URL[] urls = new URL[files.length];

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                urls[i] = file.toURI().toURL();
            } catch (MalformedURLException e) {
                Log.s("Failed " +
                        "to construct a plugin file URL (" + file.getName() + ")", e);
                Log.s("The game will be terminated");

                System.exit(-1);
            }
        }
        Log.i("Located Java plugins: " + urls.length);

        try {
            URLClassLoader child = new URLClassLoader(urls, getClass().getClassLoader());

            int attempt = 0;
            Set<File> loadedFiles = new HashSet<>();
            Set<String> knownIdentifiers = new HashSet<>();
            boolean fullyLoaded;
            while (true) {
                fullyLoaded = true;
                for (File file : files) {
                    if (loadedFiles.contains(file)) continue;
                    loadedFiles.add(file);

                    String loadingMessage;
                    if (attempt == 0) {
                        loadingMessage = "Loading " + file.getName();
                    } else {
                        loadingMessage = "Trying to load " + file.getName() + " again";
                    }
                    Log.i(loadingMessage);

                    try (ZipFile zipFile = new ZipFile(file)) {
                        ZipEntry entry = zipFile.getEntry("plugin.properties");
                        if (entry == null) {
                            Log.w("Could not find \"/plugin.properties\" file " +
                                    "in " + file.getName() + ". The jarfile will just be " +
                                    "stored in classpath");

                            continue;
                        }
                        Properties properties = new Properties();
                        try (InputStream stream = zipFile.getInputStream(entry)) {
                            properties.load(stream);
                        }

                        String name = properties.getProperty("name");
                        String version = properties.getProperty("version");
                        String compatibleWithUnparsed =
                                properties.getProperty("compatibleWith");
                        String author = properties.getProperty("author");
                        String mainClass = properties.getProperty("mainClass");
                        String dependsOnUnparsed = properties.getProperty("dependsOn");

                        if (name == null || version == null ||
                                compatibleWithUnparsed == null ||
                                author == null || mainClass == null
                        ) {
                            Log.w("\"/plugin.properties\" file in " + file
                                    .getName() + " is missing one or multiple required " +
                                    "fields. The jarfile will just be stored in classpath");

                            continue;
                        }
                        String[] compatibleWith = compatibleWithUnparsed.split(", ");
                        boolean foundCurrentVersion = false;
                        boolean allSupportedVersionsAreHigher = true;
                        for (String versionUnparsed : compatibleWith) {
                            int supportedVersion = Integer.parseInt(versionUnparsed);
                            if (supportedVersion <= ClassiCubeJavaLoaderBridge.VERSION_CODE) {
                                allSupportedVersionsAreHigher = false;
                            }
                            if (supportedVersion == ClassiCubeJavaLoaderBridge.VERSION_CODE) {
                                foundCurrentVersion = true;

                                break;
                            }
                        }
                        if (!foundCurrentVersion) {
                            Log.w("Could not find current version code " +
                                    "(" + ClassiCubeJavaLoaderBridge.VERSION_CODE + ") in the " +
                                    "compatible version list of the plugin located " +
                                    "at \"" + file.getName() + "\". The plugin might " +
                                    "not run well inside this game instance");
                        }
                        if (allSupportedVersionsAreHigher) {
                            Log.w("Could not find any version code in " +
                                    "the compatible version list of the plugin " +
                                    "located at \"" + file.getName() + "\" that is " +
                                    "lower than the current one (" +
                                    ClassiCubeJavaLoaderBridge.VERSION_CODE + "). It is very " +
                                    "unlikely that the plugin will perform OK " +
                                    "inside the current game setup");
                        }

                        if (attempt == 0) {
                            knownIdentifiers.add(name);
                            loadedFiles.remove(file);
                            // we don't have to set fullyLoaded = false; at attempt=0

                            continue;
                        }

                        if (dependsOnUnparsed != null) {
                            String[] dependsOn = dependsOnUnparsed.split(", ");

                            boolean dependenciesOk = true;
                            for (String dependency : dependsOn) {
                                if (!knownIdentifiers.contains(dependency)) {
                                    throw new IllegalStateException("Could not " +
                                            "find dependency \"" + dependency + "\"");
                                }
                                if (getPlugin(dependency) == null) {
                                    dependenciesOk = false;

                                    break;
                                }
                            }
                            if (!dependenciesOk) {
                                Log.i("Required dependencies are not loaded, skipping loading the plugin for now");
                                loadedFiles.remove(file);
                                fullyLoaded = false;

                                continue;
                            }
                        }
                        if (pluginMap.containsKey(name)) {
                            throw new RuntimeException("Detected " +
                                    "two plugins having exactly the same identifiers");
                        }

                        Log.i(Log.f("Initializing %s v%s (from %s) by %s%s",
                                name, version, file.getName(), author, System.lineSeparator()));

                        Class<?> clazz = Class.forName(mainClass, true, child);
                        Object pluginInstanceObj = clazz.newInstance();
                        if (!(pluginInstanceObj instanceof Plugin)) {
                            throw new RuntimeException(mainClass + " from " + file
                                    .getName() + " does not implement Plugin interface");
                        }
                        Plugin plugin = (Plugin) pluginInstanceObj;
                        try {
                            plugin.init();
                        } catch (Throwable t) {
                            Log.s("Failed to pre-initialize the plugin \"" + name + "\":", t);
                            Log.s("The game will be terminated");

                            System.exit(-1);
                        }

                        pluginMap.put(name, plugin);
                    }
                }
                if (attempt >= 1 && fullyLoaded) break;
                if (attempt >= 2) Thread.sleep(100);

                attempt++;
                if (attempt >= MAX_LOAD_ATTEMPTS) {
                    throw new RuntimeException("Too many plugin pre-initializing " +
                            "attempts. Either there are plugins that depend on each other, or " +
                            "MAX_LOAD_ATTEMPTS=" + MAX_LOAD_ATTEMPTS + " is not sufficient for current setup " +
                            "(unlikely)");
                }
            }
        } catch (Exception e) {
            Log.s("A severe issue occurred while loading plugins:", e);
            Log.s("The game will be terminated");

            System.exit(-1);
        }
    }

    public Plugin getPlugin(String name) {
        return pluginMap.get(name);
    }

    public int getPluginCount() {
        return pluginMap.size();
    }

    @SuppressWarnings("SameParameterValue")
    void call(String methodStr) {
        for (Map.Entry<String, Plugin> entry : pluginMap.entrySet()) {
            try {
                Plugin plugin = entry.getValue();
                Method method = Plugin.class.getDeclaredMethod(methodStr);
                method.setAccessible(true);
                method.invoke(plugin);
            } catch (Throwable t) {
                Log.s("An issue occurred while calling the " + methodStr + "() method of plugin \"" +
                        entry.getKey() + "\":", t);
                Log.s("The game will be terminated");

                System.exit(-1);
            }
        }
    }
}
