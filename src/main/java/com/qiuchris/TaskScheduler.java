package com.qiuchris;

import net.dv8tion.jda.internal.utils.JDALogger;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TaskScheduler {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private Bot bot;

    public TaskScheduler(Bot bot) {
        this.bot = bot;
    }

    public void addTask(String userId, String subjectCode, String courseNumber, String sectionNumber,
                        String session, long delay, TimeUnit unit, boolean saveToFile) {

        String id = paramsToId(userId, subjectCode, courseNumber, sectionNumber, session);
        if (scheduledTasks.containsKey(id)) {
            cancelTask(userId, subjectCode, courseNumber, sectionNumber, session);
        }
        Runnable r = () ->
                bot.checkSSC(userId, subjectCode, courseNumber, sectionNumber, session);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(r,5, delay, unit);
        scheduledTasks.put(id, future);
        JDALogger.getLog("Bot").info("Added " + id + " to map");
        if (saveToFile)
            saveTaskToFile(id, delay, unit);
    }

    public void cancelTask(String userId, String subjectCode, String courseNumber, String sectionNumber,
                           String session) {
        String id = paramsToId(userId, subjectCode, courseNumber, sectionNumber, session);
        if (scheduledTasks.containsKey(id)) {
            ScheduledFuture<?> future = scheduledTasks.get(id);
            future.cancel(true);
            scheduledTasks.remove(id);
            JDALogger.getLog("Bot").info("Removed " + id + " from map");
            deleteTaskFromFile(id);
        }
    }

    private void saveTaskToFile(String id, long delay, TimeUnit unit) {
        StringBuilder taskLine = new StringBuilder();
        taskLine.append(id).append(" ").append(delay).append(" ").append(unit);
        taskLine.append("\n");

        try {
            Files.write(Path.of("tasks.txt"), taskLine.toString().getBytes(), StandardOpenOption.APPEND);
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
            for (String line : lines) {
                String[] parts = line.split(" ", 3);
                String id = parts[0];
                long delay = Long.parseLong(parts[1]);
                TimeUnit unit = TimeUnit.valueOf(parts[2]);
                String[] params = id.split(";", 5);
                addTask(params[0], params[1], params[2], params[3], params[4], delay, unit, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String paramsToId(String userId, String subjectCode, String courseNumber, String sectionNumber,
                             String session) {
        return userId + ";" + subjectCode + ";" + courseNumber + ";" + sectionNumber + ";" + session;
    }

    public int numTasks() {
        return scheduledTasks.size();
    }

    public void shutdown() {
        JDALogger.getLog("Bot").info("Server shutting down...");
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
