package com.qiuchris;

import org.jsoup.nodes.Document;

public class GeneralCourseTask extends CourseTask {
    public GeneralCourseTask(String userId, String subjectCode, String courseNumber,
                             String sectionNumber, String year, String session) {
        super(userId, subjectCode, courseNumber, sectionNumber, year, session);
        seatType = SeatType.GENERAL;
    }

    @Override
    public boolean isSeatAvailable(Document d) {
        return Integer.parseInt(d.select("table > tbody > tr:nth-of-type(3) > td:nth-of-type(2) > strong")
                .get(0).text()) > 0;
    }
}
