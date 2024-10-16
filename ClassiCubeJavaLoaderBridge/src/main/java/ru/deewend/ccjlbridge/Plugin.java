package ru.deewend.ccjlbridge;

@SuppressWarnings("RedundantThrows")
public interface Plugin {
    default void preInit() throws Exception {}

    // ClassiCube plugin lifecycle
    default void init() throws Exception {}
    default void free() throws Exception {}
    default void reset() throws Exception {}
    default void onNewMap() throws Exception {}
    default void onNewMapLoaded() throws Exception {}
}
