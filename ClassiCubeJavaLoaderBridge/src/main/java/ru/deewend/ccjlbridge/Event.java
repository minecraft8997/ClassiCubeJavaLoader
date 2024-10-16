package ru.deewend.ccjlbridge;

public abstract class Event {
    private final boolean cancellable;

    private boolean cancelled;

    public Event(boolean cancellable) {
        this.cancellable = cancellable;
    }

    public final boolean isCancellable() {
        return cancellable;
    }

    public final boolean isCancelled() {
        return cancelled;
    }

    public final void setCancelled() {
        if (!cancellable) {
            throw new IllegalStateException("This event cannot be cancelled");
        }

        cancelled = true;
    }
}
