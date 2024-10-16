package ru.deewend.ccjlbridge.gameapi;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ScheduledTask {
    private static class ScheduledTaskInfo {
        private final double interval;
        private final Runnable callback;
        private boolean pending = true;

        public ScheduledTaskInfo(double interval, Runnable callback) {
            this.interval = interval;
            this.callback = callback;
        }
    }

    public static final int MAX_TASK_COUNT = 1000;

    private static final Lock lock = new ReentrantLock(true);

    private static final Map<Integer, ScheduledTaskInfo> SCHEDULED_TASKS = new HashMap<>();

    private static int[] pendingScheduledTaskIDs;
    private static double[] pendingScheduledTaskIntervals;

    public static boolean add(double interval, Runnable callback) {
        Objects.requireNonNull(callback);

        long intervalSeconds = (long) interval;
        if (intervalSeconds < 0L || intervalSeconds >= 100000L) {
            Chat.add("&cInterval must be >= 0 sec. and must not exceed 10^5-1 sec.");
            Chat.add("&cThe second bound might be increased in future CCJL releases");

            return false;
        }
        long millis = ((long) Math.ceil(interval * 1000.0D)) % 1000L;
        double newInterval = intervalSeconds + (0.001D * millis);

        lock.lock();
        try {
            for (int id = 0; id < MAX_TASK_COUNT; id++) {
                if (!SCHEDULED_TASKS.containsKey(id)) {
                    SCHEDULED_TASKS.put(id, new ScheduledTaskInfo(newInterval, callback));

                    return true;
                }
            }
            Chat.add("&cCannot schedule more than " + MAX_TASK_COUNT + " tasks");

            return false;
        } finally {
            lock.unlock();
        }
    }

    // not meant to be called by plugins
    public static void invoke(int taskId) {
        lock.lock();
        try {
            ScheduledTaskInfo info = SCHEDULED_TASKS.get(taskId);
            if (info == null) {
                Chat.add("&cUnknown task id: " + taskId);

                return;
            }
            info.callback.run();
        } finally {
            lock.unlock();
        }
    }

    // thread unsafe
    public static void preparePendingInfo() {
        List<Integer> pendingScheduledTaskIDs = new ArrayList<>();
        List<Double> pendingScheduledTaskIntervals = new ArrayList<>();

        for (Map.Entry<Integer, ScheduledTaskInfo> entry : SCHEDULED_TASKS.entrySet()) {
            int id = entry.getKey();
            ScheduledTaskInfo info = entry.getValue();

            if (info.pending) {
                pendingScheduledTaskIDs.add(id);
                pendingScheduledTaskIntervals.add(info.interval);

                info.pending = false;
            }
        }
        int pendingCount = pendingScheduledTaskIDs.size();

        ScheduledTask.pendingScheduledTaskIDs = new int[pendingCount];
        for (int i = 0; i < pendingCount; i++) {
            ScheduledTask.pendingScheduledTaskIDs[i] = pendingScheduledTaskIDs.get(i);
        }
        ScheduledTask.pendingScheduledTaskIntervals = new double[pendingCount];
        for (int i = 0; i < pendingCount; i++) {
            ScheduledTask.pendingScheduledTaskIntervals[i] = pendingScheduledTaskIntervals.get(i);
        }
    }

    // thread unsafe
    public static int[] getPendingScheduledTaskIDs() {
        return pendingScheduledTaskIDs;
    }

    // thread unsafe
    public static double[] getPendingScheduledTaskIntervals() {
        return pendingScheduledTaskIntervals;
    }

    // thread unsafe
    public static void freePendingInfo() {
        pendingScheduledTaskIDs = null;
        pendingScheduledTaskIntervals = null;
    }

    public static Lock getLock() {
        return lock;
    }
}
