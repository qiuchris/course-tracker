package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CourseTaskManager {
    private JDA jda;
    private CourseTaskScheduler ts;
    private HashSet<String> courseCodes;

    public CourseTaskManager(JDA jda) {
        this.jda = jda;
        this.ts = new CourseTaskScheduler(this);

        try {
            if (new File(Bot.COURSE_CODES_PATH).createNewFile())
                JDALogger.getLog("Bot").info("Created " + Bot.COURSE_CODES_PATH);
        } catch (IOException e) {
            JDALogger.getLog("Bot").info("Unable to create " + Bot.COURSE_CODES_PATH);
            e.printStackTrace();
            System.exit(1);
        }

        try {
            if (new File(Bot.TASKS_PATH).createNewFile())
                JDALogger.getLog("Bot").info("Created " + Bot.TASKS_PATH);
        } catch (IOException e) {
            JDALogger.getLog("Bot").info("Unable to create " + Bot.TASKS_PATH);
            e.printStackTrace();
            System.exit(1);
        }

        if (new File(Bot.COURSE_CODES_PATH).length() == 0) {
            updateCourseCodes();
        } else {
            loadCourseCodes();
        }
    }

    public void addCourseTask(String course, String session, String seatType, String userId) {
        if (!course.matches("^[A-Za-z]{2,4} \\d{3}[A-Za-z]? [A-Za-z0-9]{3}$")) {
            JDALogger.getLog("Bot").info("Invalid course: " + course);
            throw new IllegalArgumentException();
        }
        String[] params = course.split(" ", 3);
        if (!courseCodes.contains(params[0])) {
            JDALogger.getLog("Bot").info("Invalid course code: " + params[0]);
            throw new IllegalArgumentException();
        }
        if (seatType.equals("Restricted"))
            ts.addTask(new RestrictedCourseTask(userId, params[0], params[1], params[2], session.substring(0, 4),
                            session.substring(4)), ThreadLocalRandom.current().nextInt(10) + 3,
                    60, TimeUnit.SECONDS, true);
        else {
            ts.addTask(new GeneralCourseTask(userId, params[0], params[1], params[2], session.substring(0, 4),
                            session.substring(4)), ThreadLocalRandom.current().nextInt(10) + 3,
                    60, TimeUnit.SECONDS, true);
        }

    }

    public void removeCourseTask(String course, String session, String seatType, String userId) {
        if (!course.matches("^[A-Za-z]{2,4} \\d{3}[A-Za-z]? [A-Za-z0-9]{3}$")) {
            JDALogger.getLog("Bot").info("Invalid course: " + course);
            throw new IllegalArgumentException();
        }
        String[] params = course.split(" ", 3);
        if (!courseCodes.contains(params[0])) {
            JDALogger.getLog("Bot").info("Invalid course code: " + params[0]);
            throw new IllegalArgumentException();
        }
        if (seatType.equals("Restricted")) {
            ts.cancelTask(new GeneralCourseTask(userId, params[0], params[1], params[2],
                    session.substring(0, 4), session.substring(4)));
        } else {
            ts.cancelTask(new RestrictedCourseTask(userId, params[0], params[1], params[2],
                    session.substring(0, 4), session.substring(4)));
        }
    }

    public void sendAvailableMessage(CourseTask ct) {
        jda.getUserById(ct.getUserId()).openPrivateChannel().flatMap(channel ->
                channel.sendMessage("<@" + ct.getUserId() + "> A " + ct.getSeatType() + " seat for " +
                        ct.getSubjectCode() + " " + ct.getCourseNumber() + " " + ct.getSectionNumber() +
                        " is available. Register here: " + "https://courses.students.ubc.ca/cs/courseschedule?sesscd="
                        + ct.getSession() + "&pname=subjarea&tname=subj-section&course=" + ct.getCourseNumber() +
                        "&sessyr=" + ct.getYear() + "&section=" + ct.getSectionNumber() + "&dept="
                        + ct.getSubjectCode())).queue();
        JDALogger.getLog("Bot").info("Notifying " + ct.getUserId() + " for " + ct.getSubjectCode() + " " +
                ct.getCourseNumber() + " " + ct.getSectionNumber());
    }

    public List<CourseTask> getUserIdTasks(String userId) {
        return ts.getUserIdTasks(userId);
    }

    public int numTasks() {
        return ts.getNumTasks();
    }

    public void resumeTasks() {
        ts.loadTasksFromFile();
    }

    public void shutdown() {
        ts.shutdown();
    }

    public void saveCourseCodes() {
        try {
            Files.write(Path.of(Bot.COURSE_CODES_PATH), "".getBytes());
            for (String s : courseCodes) {
                Files.write(Path.of(Bot.COURSE_CODES_PATH), (s + "\n").getBytes(), StandardOpenOption.APPEND);
            }
            JDALogger.getLog("Bot").info("Saved course codes");
        } catch (IOException e) {
            JDALogger.getLog("Bot").error("IOException saving course codes");
        }
    }

    public void loadCourseCodes() {
        this.courseCodes = new HashSet<>();
        try {
            courseCodes.addAll(Files.readAllLines(Paths.get(Bot.COURSE_CODES_PATH)));
            JDALogger.getLog("Bot").info("Loaded course codes, size: " + courseCodes.size());
        } catch (IOException e) {
            JDALogger.getLog("Bot").error("IOException loading course codes");
        }
    }

    public void updateCourseCodes() {
        this.courseCodes = new HashSet<>();
        String url = "https://courses.students.ubc.ca/cs/courseschedule?pname=subjarea&tname=subj-all-departments";
        try {
            Document d = Jsoup.connect(url).timeout(3000).get();
            for (Element row : d.select("td:nth-of-type(1)")) {
                if (row.text().endsWith("*")) {
                    courseCodes.add(row.text().substring(0, row.text().length() - 2));
                } else {
                    courseCodes.add(row.text());
                }
            }
            saveCourseCodes();
            JDALogger.getLog("Bot").info("Updated course codes");
        } catch (SocketTimeoutException e) {
            JDALogger.getLog("Bot").error("SocketTimeoutException updating course codes at url: " + url);
        } catch (Exception e) {
            JDALogger.getLog("Bot").error("Failed to update course codes at url: " + url);
        }
    }
}
