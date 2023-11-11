package com.qiuchris;

public class CourseTask {
    private String userId;
    private String subjectCode;
    private String courseNumber;
    private String sectionNumber;
    private String session;

    public CourseTask(String userId, String subjectCode, String courseNumber,
                      String sectionNumber, String session) {
        this.userId = userId;
        this.subjectCode = subjectCode;
        this.courseNumber = courseNumber;
        this.sectionNumber = sectionNumber;
        this.session = session;
    }


}
