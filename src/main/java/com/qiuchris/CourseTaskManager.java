package com.qiuchris;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CourseTaskManager {
    private CourseTaskScheduler ts;
    private HashSet<String> courseCodes;

    public CourseTaskManager(CourseTaskScheduler ts) {
        this.ts = ts;

        try {
            if (new File(Bot.COURSE_CODES_PATH).createNewFile())
                JDALogger.getLog("Bot").info("Created " + Bot.COURSE_CODES_PATH);
        } catch (IOException e) {
            JDALogger.getLog("Bot").info("Unable to create " + Bot.COURSE_CODES_PATH);
            throw new RuntimeException(e);
        }

        try {
            if (new File(Bot.TASKS_PATH).createNewFile())
                JDALogger.getLog("Bot").info("Created " + Bot.TASKS_PATH);
        } catch (IOException e) {
            JDALogger.getLog("Bot").info("Unable to create " + Bot.TASKS_PATH);
            throw new RuntimeException(e);
        }

        if (new File(Bot.COURSE_CODES_PATH).length() == 0) {
            updateCourseCodes();
        } else {
            loadCourseCodes();
        }
    }

    public void addCourseTask(CourseTask ct) {
        ts.addTask(ct, ThreadLocalRandom.current().nextInt(10), 60, TimeUnit.SECONDS, true);
    }

    public void removeCourseTask(CourseTask ct) {
        ts.cancelTask(ct);
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
            for (String s : Files.readAllLines(Paths.get(Bot.COURSE_CODES_PATH))) {
                courseCodes.add(s);
            }
            JDALogger.getLog("Bot").info("Loaded course codes");
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
