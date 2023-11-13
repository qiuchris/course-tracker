package com.qiuchris;

import net.dv8tion.jda.internal.utils.JDALogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CourseTaskScheduler {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<CourseTask>> userIdTasks = new ConcurrentHashMap<>();
    private Bot bot;

    public CourseTaskScheduler(Bot bot) {
        this.bot = bot;
    }

    public void addTask(CourseTask ct, long initialDelay, long delay, TimeUnit unit, boolean saveToFile) {
        String key = ct.toKey();
        if (scheduledTasks.containsKey(key)) {
            cancelTask(ct);
        }

        Runnable r = () -> bot.checkCourse(ct);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(r, initialDelay, delay, unit);
        scheduledTasks.put(key, future);

        List<CourseTask> userTasks = userIdTasks.computeIfAbsent(ct.getUserId(), k -> new ArrayList<>());
        userTasks.add(ct);

        JDALogger.getLog("Bot").info("Added " + key + " to maps");
        if (saveToFile)
            saveTaskToFile(ct, delay, unit);
    }

    public void cancelTask(CourseTask ct) {
        String key = ct.toKey();
        if (scheduledTasks.containsKey(key)) {
            ScheduledFuture<?> future = scheduledTasks.get(key);
            future.cancel(true);
            scheduledTasks.remove(key);

            List<CourseTask> userTasks = userIdTasks.get(ct.getUserId());
            if (userTasks != null) {
                userTasks.remove(ct);
            }

            JDALogger.getLog("Bot").info("Removed " + key + " from maps");
            deleteTaskFromFile(ct);
        }
    }

    private void saveTaskToFile(CourseTask ct, long delay, TimeUnit unit) {
        String task = ct.toKey() + " " + delay + " " + unit + "\n";
        try {
            Files.write(Path.of("data/tasks.txt"), task.getBytes(), StandardOpenOption.APPEND);
            JDALogger.getLog("Bot").info("Added " + ct.toKey() + " to file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteTaskFromFile(CourseTask ct) {
        try {
            List<String> lines = Files.readAllLines(Path.of("data/tasks.txt"))
                    .stream()
                    .filter(line -> !line.startsWith(ct.toKey()))
                    .collect(Collectors.toList());
            Files.write(Path.of("data/tasks.txt"), lines);
            JDALogger.getLog("Bot").info("Removed " + ct.toKey() + " from file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadTasksFromFile() {
        try {
            List<String> lines = Files.readAllLines(Path.of("data/tasks.txt"));
            int delayShift = 1;
            for (String line : lines) {
                String[] parts = line.split(" ", 3);
                String id = parts[0];
                long delay = Long.parseLong(parts[1]);
                TimeUnit unit = TimeUnit.valueOf(parts[2]);
                String[] params = id.split(";", 7);
                addTask(new CourseTask(params[0], params[1], params[2], params[3], params[4], params[5], params[6]),
                        delayShift + ThreadLocalRandom.current().nextInt(10),
                        delay, unit, false);
                delayShift += 5;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<CourseTask> getUserIdTasks(String userId) {
        return userIdTasks.get(userId);
    }

    public int numTasks() {
        return scheduledTasks.size();
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
