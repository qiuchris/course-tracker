package com.qiuchris;

import org.jsoup.nodes.Document;

public class RestrictedCourseTask extends CourseTask {
    public RestrictedCourseTask(String userId, String subjectCode, String courseNumber,
                                String sectionNumber, String year, String session) {
        super(userId, subjectCode, courseNumber, sectionNumber, year, session);
        seatType = SeatType.RESTRICTED;
    }

    @Override
    public boolean isSeatAvailable(Document d) {
        return Integer.parseInt(d.select("table > tbody > tr:nth-of-type(4) > td:nth-of-type(2) > strong")
                .get(0).text()) > 0;
    }
}
