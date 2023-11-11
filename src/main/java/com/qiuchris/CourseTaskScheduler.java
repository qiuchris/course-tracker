package com.qiuchris;

import net.dv8tion.jda.internal.utils.JDALogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CourseTaskScheduler {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<String>> userIdTasks = new ConcurrentHashMap<>();
    private Bot bot;

    public CourseTaskScheduler(Bot bot) {
        this.bot = bot;
    }

    public void addTask(CourseTask ct, long initialDelay, long delay, TimeUnit unit, boolean saveToFile) {
        String id = paramsToTask(userId, subjectCode, courseNumber, sectionNumber, session);
        if (scheduledTasks.containsKey(id)) {
            cancelTask(userId, subjectCode, courseNumber, sectionNumber, session);
        }

        Runnable r = () ->
                bot.checkCourse(userId, subjectCode, courseNumber, sectionNumber, session);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(r, initialDelay, delay, unit);
        scheduledTasks.put(id, future);

        List<String> userTasks = userIdTasks.computeIfAbsent(userId, k -> new ArrayList<>());
        userTasks.add(id);

        JDALogger.getLog("Bot").info("Added " + id + " to maps");
        if (saveToFile)
            saveTaskToFile(id, delay, unit);
    }

    public void cancelTask(String userId, String subjectCode, String courseNumber, String sectionNumber,
                           String session) {
        String id = paramsToTask(userId, subjectCode, courseNumber, sectionNumber, session);
        if (scheduledTasks.containsKey(id)) {
            ScheduledFuture<?> future = scheduledTasks.get(id);
            future.cancel(true);
            scheduledTasks.remove(id);

            List<String> userTasks = userIdTasks.get(userId);
            if (userTasks != null) {
                userIdTasks.get(userId).remove(id);
            }

            JDALogger.getLog("Bot").info("Removed " + id + " from maps");
            deleteTaskFromFile(id);
        }
    }

    private void saveTaskToFile(String id, long delay, TimeUnit unit) {
        String task = id + " " + delay + " " + unit + "\n";
        try {
            Files.write(Path.of("tasks.txt"), task.getBytes(), StandardOpenOption.APPEND);
            JDALogger.getLog("Bot").info("Added " + id + " to file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteTaskFromFile(String id) {
        try {
            List<String> lines = Files.readAllLines(Paths.get("tasks.txt"))
                    .stream()
                    .filter(line -> !line.startsWith(id))
                    .collect(Collectors.toList());
            Files.write(Paths.get("tasks.txt"), lines);
            JDALogger.getLog("Bot").info("Removed " + id + " from file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadTasksFromFile() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("tasks.txt"));
            int delayShift = 1;
            for (String line : lines) {
                String[] parts = line.split(" ", 3);
                String id = parts[0];
                long delay = Long.parseLong(parts[1]);
                TimeUnit unit = TimeUnit.valueOf(parts[2]);
                String[] params = id.split(";", 5);
                addTask(params[0], params[1], params[2], params[3], params[4], delayShift + ThreadLocalRandom.current().nextInt(10), delay, unit, false
                );
                delayShift += 5;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getUserIdTasks(String userId) {
        return userIdTasks.get(userId);
    }

    public String paramsToTask(String userId, String subjectCode, String courseNumber, String sectionNumber,
                               String session) {
        return userId + ";" + subjectCode + ";" + courseNumber + ";" + sectionNumber + ";" + session;
    }

    public String idToCourse(String id) {
        String[] a = id.split(";", 5);
        return a[1] + " " + a[2] + " " + a[3];
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
