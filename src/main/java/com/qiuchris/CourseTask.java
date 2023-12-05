package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.SocketTimeoutException;

public class CourseTask {
    private final String userId;
    private final String subjectCode;
    private final String courseNumber;
    private final String sectionNumber;
    private final String year;
    private final String session;
    protected SeatType seatType = SeatType.ANY;

    public CourseTask(String userId, String subjectCode, String courseNumber,
                      String sectionNumber, String year, String session) {
        this.userId = userId;
        this.subjectCode = subjectCode;
        this.courseNumber = courseNumber;
        this.sectionNumber = sectionNumber;
        this.year = year;
        this.session = session;
    }

    public void sendAvailableMessage(JDA jda) {
        JDALogger.getLog("CourseTask").info("Notifying " + userId + " for " + this);
        try {
            jda.openPrivateChannelById(userId).flatMap(channel ->
                    channel.sendMessage("<@" + userId + "> A seat for " +
                            subjectCode + " " + courseNumber + " " + sectionNumber +
                            " is available. Register here: " + "https://courses.students.ubc.ca/cs/courseschedule?sesscd="
                            + session + "&pname=subjarea&tname=subj-section&course=" + courseNumber +
                            "&sessyr=" + year + "&section=" + sectionNumber + "&dept="
                            + subjectCode)).queue();
        } catch (ErrorResponseException e) {
            System.out.println("ErrorResponseException " + e.getErrorCode());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception sending message to " + userId);
        }
    }

    public boolean checkAvailability() {
        String url = "https://courses.students.ubc.ca/cs/courseschedule?sesscd=" + session +
                "&pname=subjarea&tname=subj-section&course=" + courseNumber +
                "&sessyr=" + year + "&section=" + sectionNumber + "&dept=" + subjectCode;
        try {
            JDALogger.getLog("Bot").info("Checking SSC for: " + this);
            Document d = Jsoup.connect(url).userAgent(Bot.USER_AGENT).timeout(3000).get();
            return isSeatAvailable(d);
        } catch (SocketTimeoutException e) {
            JDALogger.getLog("CourseTask").error("SocketTimeoutException checking url: " + url);
        } catch (Exception e) {
            JDALogger.getLog("CourseTask").error("Failed to check SSC at url: " + url);
        }
        return false;
    }

    public boolean isSeatAvailable(Document d) {
        return Integer.parseInt(d.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > strong")
                .get(0).text()) > 0;
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

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + subjectCode.hashCode();
        result = 31 * result + courseNumber.hashCode();
        result = 31 * result + sectionNumber.hashCode();
        result = 31 * result + year.hashCode();
        result = 31 * result + session.hashCode();
        result = 31 * result + seatType.hashCode();
        return result;
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

    public SeatType getSeatType() {
        return seatType;
    }
}
