package com.qiuchris;

import net.dv8tion.jda.internal.utils.JDALogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.SocketTimeoutException;

public class RestrictedCourseTask extends CourseTask {
    public RestrictedCourseTask(String userId, String subjectCode, String courseNumber,
                                String sectionNumber, String year, String session) {
        super(userId, subjectCode, courseNumber, sectionNumber, year, session);
        seatType = "Restricted";
    }

    @Override
    public boolean checkAvailability() {
        String url = "https://courses.students.ubc.ca/cs/courseschedule?sesscd=" + this.getSession() +
                "&pname=subjarea&tname=subj-section&course=" + this.getCourseNumber() +
                "&sessyr=" + this.getYear() + "&section=" + this.getSectionNumber() + "&dept=" + this.getSubjectCode();
        try {
            JDALogger.getLog("Bot").info("Checking SSC at url: " + url);
            Document d = Jsoup.connect(url).timeout(3000).get();
            if (Integer.parseInt(d.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > strong")
                    .get(0).text()) > 0) {
                return true;
            } else {
                return false;
            }
        } catch (SocketTimeoutException e) {
            JDALogger.getLog("Bot").error("SocketTimeoutException checking url: " + url);
        } catch (Exception e) {
            JDALogger.getLog("Bot").error("Failed to check SSC at url: " + url);
        }
        return false;
    }
}
