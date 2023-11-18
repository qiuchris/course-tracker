package com.qiuchris;

import net.dv8tion.jda.api.JDA;
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
    private JDA jda;

    public CourseTaskScheduler(JDA jda) {
        this.jda = jda;
    }

    public void addTask(CourseTask ct, long initialDelay, long delay, TimeUnit unit, boolean saveToFile) {
        String key = ct.toKey();
        if (scheduledTasks.containsKey(key)) {
            cancelTask(ct);
        }

        Runnable r = () -> {if (ct.checkAvailability()) {
            ct.sendAvailableMessage(jda);
            cancelTask(ct);
        }};
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
                if (params[6].equals("Restricted")) {
                    addTask(new RestrictedCourseTask(params[0], params[1], params[2], params[3], params[4], params[5]),
                            delayShift + ThreadLocalRandom.current().nextInt(10),
                            delay, unit, false);
                } else {
                    addTask(new GeneralCourseTask(params[0], params[1], params[2], params[3], params[4], params[5]),
                            delayShift + ThreadLocalRandom.current().nextInt(10),
                            delay, unit, false);
                }
                delayShift += 5;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<CourseTask> getUserIdTasks(String userId) {
        return userIdTasks.get(userId);
    }

    public int getNumTasks() {
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
