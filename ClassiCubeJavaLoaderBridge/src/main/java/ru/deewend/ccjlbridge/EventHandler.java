package ru.deewend.ccjlbridge;

public abstract class EventHandler<T extends Event> {
    public abstract void handleEvent(T event);
}
