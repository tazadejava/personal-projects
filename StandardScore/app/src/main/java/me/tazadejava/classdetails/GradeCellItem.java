package me.tazadejava.classdetails;

import me.tazadejava.gradeupdates.ClassPeriod;
import me.tazadejava.gradeupdates.GradesManager;

/**
 * Created by Darren on 6/8/2017.
 */
public abstract class GradeCellItem {

    protected String boldedText;
    protected String letterGrade;
    protected double percentageGrade;
    protected String pointsReceived, pointsTotal;

    protected ClassPeriod term;

    public GradeCellItem(String gradedName, String letterGrade, double percentageGrade, String pointsReceived, String pointsTotal, ClassPeriod term) {
        this.boldedText = gradedName;
        this.letterGrade = letterGrade;
        this.percentageGrade = percentageGrade;
        this.pointsReceived = pointsReceived;
        this.pointsTotal = pointsTotal;
        this.term = term;
    }

    public GradeCellItem(String save, ClassPeriod term) {
        this.term = term;
    }

    public abstract String save();

    public String getBoldedText() {
        return boldedText;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public double getPercentageGrade() {
        return percentageGrade;
    }

    public String getPointsReceived() {
        return pointsReceived;
    }

    public String getPointsTotal() {
        return pointsTotal;
    }

    public ClassPeriod getPeriod() {return term;}

    public void setBoldedText(String name) {
        boldedText = name;
    }

    public void setPercentageGrade(double percentageGrade) {
        this.percentageGrade = percentageGrade;
        letterGrade = GradesManager.getLetterGrade(percentageGrade);
    }

    public void setOutOf(String received, String total) {
        pointsReceived = received;
        pointsTotal = total;
    }

    public boolean isValidCell() {
        if(percentageGrade != -1 || (!pointsReceived.equals("*") && pointsTotal.equals("0"))) {
            if(!(this instanceof GradedItem) || (!boldedText.startsWith("There are no "))) {
                return true;
            }
        }

        return false;
    }
}
