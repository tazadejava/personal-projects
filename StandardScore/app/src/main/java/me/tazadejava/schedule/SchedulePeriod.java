package me.tazadejava.schedule;

import me.tazadejava.gradeupdates.ClassPeriod;

public class SchedulePeriod {

    private String className, teacherName, roomName;
    private int period;
    private ClassPeriod.GradeTerm term;

    public SchedulePeriod(String className, String teacherName, String roomName, ClassPeriod.GradeTerm term, int period) {
        this.className = className;
        this.teacherName = teacherName;
        this.roomName = roomName;
        this.term = term;
        this.period = period;
    }

    public SchedulePeriod(String save) {
        String[] split = save.split("§");

        className = split[0];
        teacherName = split[1];
        roomName = split[2];

        period = Integer.parseInt(split[3]);
        term = ClassPeriod.GradeTerm.valueOf(split[4]);
    }

    public String save() {
        return className + "§" + teacherName + "§" + roomName + "§" + period + "§" + term;
    }

    public String getClassName() {
        return className;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getPeriod() {
        return period;
    }

    public ClassPeriod.GradeTerm getTerm() {
        return term;
    }
}
