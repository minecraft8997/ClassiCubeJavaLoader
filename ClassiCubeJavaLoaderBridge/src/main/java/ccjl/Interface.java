package ccjl;

import ru.deewend.ccjlbridge.ClassiCubeJavaLoaderBridge;
import ru.deewend.ccjlbridge.EventManager;
import ru.deewend.ccjlbridge.gameapi.ScheduledTask;

// not meant to be called by plugins
public class Interface {
    private Interface() {
    }

    public static boolean start() {
        return ClassiCubeJavaLoaderBridge.start();
    }

    public static int[] getPendingScheduledTaskIDs() {
        ScheduledTask.getLock().lock();

        ScheduledTask.preparePendingInfo();

        return ScheduledTask.getPendingScheduledTaskIDs();
    }

    public static double getPendingScheduledTaskInterval(int taskId) {
        return ScheduledTask.getPendingScheduledTaskIntervals()[taskId];
    }

    public static Object freePendingInfo() {
        ScheduledTask.freePendingInfo();
        ScheduledTask.getLock().unlock();

        return null;
    }

    public static double[] getPendingScheduledTaskIntervals() {
        try {
            return ScheduledTask.getPendingScheduledTaskIntervals();
        } finally {
            ScheduledTask.freePendingInfo();

            ScheduledTask.getLock().unlock();
        }
    }

    public static String[] getPendingChatMessages() {
        return ClassiCubeJavaLoaderBridge.getInstance().getPendingChatMessages();
    }

    public static void fireEvent(int eventId) {
        EventManager.getInstance().fireRawEvent(eventId);
    }
}
