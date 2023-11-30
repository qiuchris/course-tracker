package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.SocketTimeoutException;

public class CourseTask {
    private String userId;
    private String subjectCode;
    private String courseNumber;
    private String sectionNumber;
    private String year;
    private String session;
    protected String seatType = "Any";

    public CourseTask(String userId, String subjectCode, String courseNumber,
                      String sectionNumber, String year, String session) {
        this.userId = userId;
        this.subjectCode = subjectCode;
        this.courseNumber = courseNumber;
        this.sectionNumber = sectionNumber;
        this.year = year;
        this.session = session;
    }

    public boolean isSeatAvailable(Document d) {
        return Integer.parseInt(d.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > strong")
                .get(0).text()) > 0;
    }

    public void sendAvailableMessage(JDA jda) {
        jda.getUserById(userId).openPrivateChannel().flatMap(channel ->
                channel.sendMessage("<@" + userId + "> A " + seatType + " seat for " +
                        subjectCode + " " + courseNumber + " " + sectionNumber +
                        " is available. Register here: " + "https://courses.students.ubc.ca/cs/courseschedule?sesscd="
                        + session + "&pname=subjarea&tname=subj-section&course=" + courseNumber +
                        "&sessyr=" + year + "&section=" + sectionNumber + "&dept="
                        + subjectCode)).queue();
        JDALogger.getLog("Bot").info("Notifying " + userId + " for " + subjectCode + " " +
                courseNumber + " " + sectionNumber);
    }

    public boolean checkAvailability() {
        String url = "https://courses.students.ubc.ca/cs/courseschedule?sesscd=" + this.getSession() +
                "&pname=subjarea&tname=subj-section&course=" + this.getCourseNumber() +
                "&sessyr=" + this.getYear() + "&section=" + this.getSectionNumber() + "&dept=" + this.getSubjectCode();
        try {
            JDALogger.getLog("Bot").info("Checking SSC for: " + this);
            Document d = Jsoup.connect(url).userAgent(Bot.USER_AGENT).timeout(3000).get();
            return isSeatAvailable(d);
        } catch (SocketTimeoutException e) {
            JDALogger.getLog("Bot").error("SocketTimeoutException checking url: " + url);
        } catch (Exception e) {
            JDALogger.getLog("Bot").error("Failed to check SSC at url: " + url);
        }
        return false;
    }

    public String toKey() {
        return userId + ";" + subjectCode + ";" + courseNumber + ";" +
                sectionNumber + ";" + year + ";" + session + ";" + seatType;
    }

    @Override
    public String toString() {
        return subjectCode + " " + courseNumber + " " + sectionNumber + " " + year + session + " " + seatType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseTask)) return false;

        CourseTask that = (CourseTask) o;

        if (!userId.equals(that.userId)) return false;
        if (!subjectCode.equals(that.subjectCode)) return false;
        if (!courseNumber.equals(that.courseNumber)) return false;
        if (!sectionNumber.equals(that.sectionNumber)) return false;
        if (!year.equals(that.year)) return false;
        if (!seatType.equals(that.seatType)) return false;
        return session.equals(that.session);
    }

    public String getUserId() {
        return userId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getCourseNumber() {
        return courseNumber;
    }

    public String getSectionNumber() {
        return sectionNumber;
    }

    public String getYear() {
        return year;
    }

    public String getSession() {
        return session;
    }

    public String getSeatType() {
        return seatType;
    }
}
