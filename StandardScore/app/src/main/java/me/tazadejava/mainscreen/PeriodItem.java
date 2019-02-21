package me.tazadejava.mainscreen;

import me.tazadejava.gradeupdates.ClassPeriod;

/**
 * Created by Darren on 6/13/2017.
 */
public class PeriodItem {

    public ClassPeriod classLink;
    private String className, teacherName, termGrade, semesterGrade;

    public PeriodItem(ClassPeriod classLink) {
        this.classLink = classLink;
        this.className = "P" + classLink.getPeriod() + ": " + classLink.getClassName();
        this.teacherName = classLink.getTeacher();
        this.termGrade = classLink.getLetterGrade() + (classLink.getPercentage() == -1 ? "" : " (" + classLink.getPercentage() + "%)");
        this.semesterGrade = (classLink.getUpdates() > 0 ? "!!! (" + classLink.getUpdates() + ")" : "");
    }

    public String getClassName() {
        return className;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getTermGrade() {
        return termGrade;
    }

    public String getSemesterGrade() {
        return semesterGrade;
    }
}
