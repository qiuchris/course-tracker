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

    @Override
    public String toString() {
        return subjectCode + " " + courseNumber + " " + sectionNumber + " " + session;
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
        return session.equals(that.session);
    }

    public String toKey() {
        return userId + ";" + subjectCode + ";" + courseNumber + ";" + sectionNumber + ";" + session;
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

    public String getSession() {
        return session;
    }
}
