package me.tazadejava.gradeupdates;

import java.util.Objects;

public class UpdateHistoryPoint {

    public String className, assignmentName;
    public String pointsReceived, pointsTotal;
    public boolean isUngraded;

    public UpdateHistoryPoint(String className, String assignmentName, boolean isUngraded, String pointsReceived, String pointsTotal) {
        this.className = className;
        this.assignmentName = assignmentName;
        this.isUngraded = isUngraded;

        this.pointsReceived = pointsReceived;
        this.pointsTotal = pointsTotal;
    }

    public UpdateHistoryPoint(String save) {
        String[] split = save.split(";");

        className = split[0];
        assignmentName = split[1];
        isUngraded = Boolean.parseBoolean(split[2]);

        if(split.length > 4) {
            pointsReceived = split[3];
            pointsTotal = split[4];
        } else {
            pointsReceived = "0";
            pointsTotal = "0";
        }
    }

    public String save() {
        return className + ";" + assignmentName + ";" + isUngraded + ";" + pointsReceived + ";" + pointsTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateHistoryPoint that = (UpdateHistoryPoint) o;
        return isUngraded == that.isUngraded &&
                Objects.equals(className, that.className) &&
                Objects.equals(assignmentName, that.assignmentName) &&
                Objects.equals(pointsReceived, that.pointsReceived) &&
                Objects.equals(pointsTotal, that.pointsTotal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, assignmentName, pointsReceived, pointsTotal, isUngraded);
    }
}
