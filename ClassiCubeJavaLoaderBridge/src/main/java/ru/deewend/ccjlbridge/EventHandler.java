package ru.deewend.ccjlbridge;

public interface EventHandler<T extends Event> {
    void handleEvent(T event);
}
