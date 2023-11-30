package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.utils.JDALogger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CourseTaskManager {
    private JDA jda;
    private CourseTaskScheduler ts = new CourseTaskScheduler(jda);
    private CourseValidator cv = new CourseValidator();

    public CourseTaskManager(JDA jda) {
        this.jda = jda;

        try {
            if (new File(Bot.TASKS_PATH).createNewFile())
                JDALogger.getLog("Bot").info("Created " + Bot.TASKS_PATH);
        } catch (IOException e) {
            JDALogger.getLog("Bot").info("Unable to create " + Bot.TASKS_PATH);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void addCourseTask(String course, String session, String seatType, String userId) {
        if (!course.matches("^[A-Za-z]{2,4} \\d{3}[A-Za-z]? [A-Za-z0-9]{3}$")) {
            JDALogger.getLog("Bot").info("Invalid course: " + course);
            throw new IllegalArgumentException();
        }
        if (!cv.validCourse(course)) {
            JDALogger.getLog("Bot").info("Invalid course: " + course);
            throw new IllegalArgumentException();
        }
        String[] params = course.split(" ", 3);

        if (seatType.equals("Restricted"))
            ts.addTask(new RestrictedCourseTask(userId, params[0], params[1], params[2], session.substring(0, 4),
                            session.substring(4)), ThreadLocalRandom.current().nextInt(10) + 3,
                    Bot.DEFAULT_TIME, TimeUnit.SECONDS, true);
        else if (seatType.equals("General")){
            ts.addTask(new GeneralCourseTask(userId, params[0], params[1], params[2], session.substring(0, 4),
                            session.substring(4)), ThreadLocalRandom.current().nextInt(10) + 3,
                    Bot.DEFAULT_TIME, TimeUnit.SECONDS, true);
        } else {
            ts.addTask(new CourseTask(userId, params[0], params[1], params[2], session.substring(0, 4),
                            session.substring(4)), ThreadLocalRandom.current().nextInt(10) + 3,
                    Bot.DEFAULT_TIME, TimeUnit.SECONDS, true);
        }

    }

    public void removeCourseTask(String course, String session, String seatType, String userId) {
        if (!course.matches("^[A-Za-z]{2,4} \\d{3}[A-Za-z]? [A-Za-z0-9]{3}$")) {
            JDALogger.getLog("Bot").info("Invalid course: " + course);
            throw new IllegalArgumentException();
        }
        if (!cv.validCourse(course)) {
            JDALogger.getLog("Bot").info("Invalid course: " + course);
            throw new IllegalArgumentException();
        }
        String[] params = course.split(" ", 3);
        if (seatType.equals("Restricted")) {
            ts.cancelTask(new RestrictedCourseTask(userId, params[0], params[1], params[2],
                    session.substring(0, 4), session.substring(4)));
        } else if (seatType.equals("General")) {
            ts.cancelTask(new GeneralCourseTask(userId, params[0], params[1], params[2],
                    session.substring(0, 4), session.substring(4)));
        } else {
            ts.cancelTask(new CourseTask(userId, params[0], params[1], params[2],
                    session.substring(0, 4), session.substring(4)));
        }
    }

    public void updateValidator() {
        new Thread(cv::updateCourses).start();
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
}
