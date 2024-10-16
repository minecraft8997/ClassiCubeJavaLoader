package ru.deewend.ccjlbridge.exampleplugin;

import ru.deewend.ccjlbridge.EventManager;
import ru.deewend.ccjlbridge.Plugin;
import ru.deewend.ccjlbridge.event.UserBlockChangedEvent;
import ru.deewend.ccjlbridge.event.WindowResizedEvent;
import ru.deewend.ccjlbridge.gameapi.Chat;
import ru.deewend.ccjlbridge.gameapi.ScheduledTask;

import java.util.concurrent.TimeUnit;

public class ExamplePlugin implements Plugin {
    private boolean initialResize = true;
    private long lastTimeResized;
    private boolean firstTimeLoaded = true;

    @Override
    public void init() {
        Chat.add("&dHello from ExamplePlugin <3_<3");

        ScheduledTask.add(60.0D, () -> Chat.add("&cDon't forget to do /banall"));

        EventManager.getInstance().registerEventHandler(UserBlockChangedEvent.class,
                (event -> Chat.add("You have just changed a block, haven't you?")));

        EventManager.getInstance().registerEventHandler(WindowResizedEvent.class, (event) -> {
            if (initialResize) {
                initialResize = false;

                return;
            }
            long delta;
            if (lastTimeResized != 0L) {
                delta = System.currentTimeMillis() - lastTimeResized;
            } else {
                delta = 0L;
            }
            String additionalMessage = null;
            if (delta != 0L) {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(delta);
                if (seconds == 0L) return; // to avoid spam

                additionalMessage = ", last time it was done " + TimeUnit.MILLISECONDS.toSeconds(delta) + " sec. ago";
            }
            Chat.add("You have resized the window" + (additionalMessage != null ? additionalMessage : ""));

            lastTimeResized = System.currentTimeMillis();
        });
    }

    @Override
    public void onNewMapLoaded() {
        if (firstTimeLoaded) {
            firstTimeLoaded = false;

            return;
        }
        Chat.add("Have fun!");
    }
}
