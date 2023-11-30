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
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CourseValidator {
    private Set<String> courses;

    public CourseValidator() {
        try {
            if (new File(Bot.COURSES_PATH).createNewFile()) {
                JDALogger.getLog("Bot").info("Created " + Bot.COURSES_PATH);
            }
        } catch (IOException e) {
            JDALogger.getLog("Bot").info("Unable to create " + Bot.COURSES_PATH);
            e.printStackTrace();
            System.exit(1);
        }

        if (new File(Bot.COURSES_PATH).length() == 0) {
            new Thread(this::updateCourses).start();
        } else {
            loadCourses();
        }
    }

    public boolean validCourse(String course) {
        return courses.contains(course);
    }

    public void saveCourses() {
        try {
            Files.write(Path.of(Bot.COURSES_PATH), "".getBytes());
            for (String s : courses) {
                Files.write(Path.of(Bot.COURSES_PATH), (s + "\n").getBytes(), StandardOpenOption.APPEND);
            }
            JDALogger.getLog("Bot").info("Saved courses");
        } catch (IOException e) {
            JDALogger.getLog("Bot").error("IOException saving courses");
        }
    }

    public void loadCourses() {
        this.courses = Collections.synchronizedSet(new HashSet<>());
        try {
            courses.addAll(Files.readAllLines(Paths.get(Bot.COURSES_PATH)));
            JDALogger.getLog("Bot").info("Loaded courses, size: " + courses.size());
        } catch (IOException e) {
            JDALogger.getLog("Bot").error("IOException loading courses");
        }
    }

    public void updateCourses() {
        this.courses = Collections.synchronizedSet(new HashSet<>());
        String base_url = "https://courses.students.ubc.ca";
        String url = "https://courses.students.ubc.ca/cs/courseschedule?pname=subjarea&tname=subj-all-departments";
        JDALogger.getLog("CourseValidator").info("Updating courses...");
        try {
            Random r = new Random();
            Document d = Jsoup.connect(url).userAgent(Bot.USER_AGENT).timeout(10000).get();
            for (Element row1 : d.select("td:nth-of-type(1) > a")) {
                if (row1.hasAttr("href")) {
                    Thread.sleep(8000 + r.nextInt(5000));
                    Document e = Jsoup.connect(base_url + row1.attr("href"))
                            .userAgent(Bot.USER_AGENT).timeout(10000).get();
                    for (Element row2 : e.select("td:nth-of-type(1) > a")) {
                        if (row2.hasAttr("href")) {
                            Thread.sleep(8000 + r.nextInt(5000));
                            Document f = Jsoup.connect(base_url + row2.attr("href"))
                                    .userAgent(Bot.USER_AGENT).timeout(10000).get();
                            for (Element row3 : f.select("td:nth-of-type(2) > a")) {
                                courses.add(row3.text());
                            }
                        }
                    }
                }
            }
            JDALogger.getLog("CourseValidator").info("Saving courses...");
            saveCourses();
            JDALogger.getLog("CourseValidator").info("Finished updating courses.");
        } catch (SocketTimeoutException e) {
            JDALogger.getLog("CourseValidator").error("SocketTimeoutException updating courses at url: " + url);
        } catch (Exception e) {
            JDALogger.getLog("CourseValidator").error("Failed to update courses at url: " + url);
        }
    }
}
