package com.qiuchris;

public abstract class CourseTask {
    private String userId;
    private String subjectCode;
    private String courseNumber;
    private String sectionNumber;
    private String year;
    private String session;

    protected String seatType = "";

    public CourseTask(String userId, String subjectCode, String courseNumber,
                      String sectionNumber, String year, String session) {
        this.userId = userId;
        this.subjectCode = subjectCode;
        this.courseNumber = courseNumber;
        this.sectionNumber = sectionNumber;
        this.year = year;
        this.session = session;
    }

    public abstract boolean checkAvailability();

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

    public String toKey() {
        return userId + ";" + subjectCode + ";" + courseNumber + ";" +
                sectionNumber + ";" + year + ";" + session + ";" + seatType;
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
