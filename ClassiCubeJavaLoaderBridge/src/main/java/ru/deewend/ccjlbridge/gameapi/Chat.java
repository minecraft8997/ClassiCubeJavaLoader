package ru.deewend.ccjlbridge.gameapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Chat {
    private static final List<String> PENDING_CHAT_MESSAGES = new ArrayList<>();

    private Chat() {
    }

    public static void add(String message) {
        Objects.requireNonNull(message);

        synchronized (PENDING_CHAT_MESSAGES) {
            PENDING_CHAT_MESSAGES.add(message);
        }
    }

    // not meant to be called by plugins
    public static String[] getPendingChatMessages() {
        return getPendingChatMessages(true);
    }

    // not meant to be called by plugins, especially if reset is set to true
    public static String[] getPendingChatMessages(boolean reset) {
        String[] messages;
        synchronized (PENDING_CHAT_MESSAGES) {
            messages = PENDING_CHAT_MESSAGES.toArray(new String[0]);

            if (reset) PENDING_CHAT_MESSAGES.clear();
        }

        return messages;
    }
}
