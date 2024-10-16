# ClassiCubeJavaLoader
An attempt to bring the support of my favorite language into my favorite cuboid game.
## Building
Notes:
- If built in x64 Release mode, for unknown reasons the plugin causes ClassiCube to stop responding during terrain generation, at least on my machine;
- Compatibility with x86 platform has not been tested at all.

1. Clone ClassiCube repository, import the project into Visual Studio and build the solution;
2. Right click on top "Solution 'ClassiCube' (1 of 1 project)" line, click Add -> New Project... -> select **Dynamic-Link Library (C++)**;
3. Assuming you have created the new library project under 'ClassiCube' solution, delete all automatically generated source and header files. **Make sure you have selected Debug configuration and x64 platform**;
4. Navigate to Configuration Properties -> C/C++ -> All Options -> Precompiled Header -> select **Not Using Precompiled Headers**;
5. Navigate to Configuration Properties -> Linker -> Input -> Additional Dependencies -> specify the path to `src/x64/Debug/ClassiCube.lib` file;
6. Navigate to Configuration Properties -> C/C++ -> General -> Additional Include Directories -> specify the path to `%JAVA_HOME%/include` and `%JAVA_HOME%/include/win32` directories;
7. Import `ClassiCubeJavaLoader.c` file into `Source Files`;
8. Select Build -> Build Solution, you should find the binary under `src/x64/Debug/` folder;
9. Navigate to `ClassiCubeJavaLoaderBridge` directory, run `gradlew.bat build` command, the jarfile should be appear in `ClassiCubeJavaLoaderBridge/build/libs` folder (TODO document ability to write custom Java bridge by implementing `ccjl.Interface` class and changing `JLMOD` macro in `ClassiCubeJavaLoader.c`);
10. Navigate to `ClassiCubeJavaLoaderBridgePlugin` directory, perform the same actions;
11. Make sure your gamefolder is designed in the following way:<br><br>
    ClassiCube/<br>
    ├─ javaplugins/<br>
    │  ├─ ClassiCubeJavaLoaderBridgePlugin.jar<br>
    ├─ plugins/<br>
    │  ├─ ClassiCubeJavaLoader.dll<br>
    ├─ ClassiCube.exe<br>
    ├─ ClassiCubeJavaLoaderBridge.jar<br>

<br>12. Launch ClassiCube and have fun!

## What is supported for now
1. All 51 events, unfortunately without any parameters for those events which provide them;
2. Partial re-implementation of `Chat.h`;
3. Almost full implementation SchedulerTask API with limitations of `interval` parameter (TODO document them and the reasons, it is a quite funny thing).

## Implementing a plugin
TODO document, for now please check the example plugin located in `ClassiCubeJavaLoaderBridgePlugin` folder. Note the `plugin.properties` file under `resources` directory. Aside from the fields shown there, the loader also supports `dependsOn` parameter which you can use for enumerating plugin identifiers your work depends on (they will have a higher loading priority). Example: `dependsOn=Plugin1, Plugin2, ExamplePlugin`

## Some other notes
Use this project on your own risk, it may contain security issues. If you find one, please make sure to report it privately. One of the options could be my email: deewenddev(at)gmail(dot)com.
