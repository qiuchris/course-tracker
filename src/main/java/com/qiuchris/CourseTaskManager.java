package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CourseTaskManager {
    private CourseTaskScheduler ts;
    private CourseValidator cv;
    private Logger log = JDALogger.getLog("CourseTaskManager");

    public CourseTaskManager(JDA jda) {
        this.ts = new CourseTaskScheduler(jda);
        this.cv = new CourseValidator();

        try {
            if (new File(Bot.TASKS_PATH).createNewFile())
                log.info("Created " + Bot.TASKS_PATH);
        } catch (IOException e) {
            log.info("Unable to create " + Bot.TASKS_PATH);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean addCourseTask(String course, String session, String seatType, String userId) throws CourseAvailableException {
        if (getUserIdTasks(userId) != null && getUserIdTasks(userId).size() >= 5) {
            log.info("User " + userId + " has too many tasks");
            return false;
        }
        if (!(course.matches("^[A-Za-z]{2,4} \\d{3}[A-Za-z]? [A-Za-z0-9]{3}$") && cv.validCourse(course))) {
            log.info("Invalid course: " + course);
            throw new IllegalArgumentException();
        }
        String[] params = course.split(" ", 3);
        CourseTask ct;
        if (seatType.equals(SeatType.RESTRICTED.toString())) {
            ct = new RestrictedCourseTask(userId, params[0], params[1], params[2], session.substring(0, 4),
                    session.substring(4));
        } else if (seatType.equals(SeatType.GENERAL.toString())){
            ct = new GeneralCourseTask(userId, params[0], params[1], params[2], session.substring(0, 4),
                            session.substring(4));
        } else {
            ct = new CourseTask(userId, params[0], params[1], params[2], session.substring(0, 4),
                            session.substring(4));
        }
        if (ct.checkAvailability()) {
            throw new CourseAvailableException();
        }
        ts.addTask(ct, ThreadLocalRandom.current().nextInt(10) + 3,
                Bot.DEFAULT_TIME, TimeUnit.SECONDS, true);
        return true;
    }

    public void removeCourseTask(String course, String session, String seatType, String userId) {
        if (!(course.matches("^[A-Za-z]{2,4} \\d{3}[A-Za-z]? [A-Za-z0-9]{3}$") && cv.validCourse(course))) {
            log.info("Invalid course: " + course);
            throw new IllegalArgumentException();
        }
        String[] params = course.split(" ", 3);
        if (seatType.equals(SeatType.RESTRICTED.toString())) {
            ts.cancelTask(new RestrictedCourseTask(userId, params[0], params[1], params[2],
                    session.substring(0, 4), session.substring(4)));
        } else if (seatType.equals(SeatType.GENERAL.toString())) {
            ts.cancelTask(new GeneralCourseTask(userId, params[0], params[1], params[2],
                    session.substring(0, 4), session.substring(4)));
        } else {
            ts.cancelTask(new CourseTask(userId, params[0], params[1], params[2],
                    session.substring(0, 4), session.substring(4)));
        }
    }

    public void addValidCourse(String course) {
        cv.addCourse(course);
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
