package me.tazadejava.classdetails;

import me.tazadejava.gradeupdates.ClassPeriod;

/**
 * Created by Darren on 6/8/2017.
 */
public class HeaderItem extends GradeCellItem {

    private double adjustedSectionWeight, regularSectionWeight;

    public HeaderItem(double adjustedSectionWeight, double regularSectionWeight, String gradedName, String letterGrade, double percentageGrade, String pointsReceived, String pointsTotal, ClassPeriod term) {
        super(gradedName, letterGrade, percentageGrade, pointsReceived, pointsTotal, term);

        this.adjustedSectionWeight = adjustedSectionWeight;
        this.regularSectionWeight = regularSectionWeight;
    }

    public HeaderItem(String save, ClassPeriod term) {
        super(save, term);

        String[] split = save.split("~");
        adjustedSectionWeight = Double.valueOf(split[1]);
        boldedText = split[2];
        letterGrade = split[3];
        percentageGrade = Double.valueOf(split[4]);
        pointsReceived = split[5];
        pointsTotal = split[6];
        if(split.length > 7) {
            regularSectionWeight = Double.valueOf(split[7]);
        }
    }

    @Override
    public String save() {
        return "H~" + adjustedSectionWeight + "~" + boldedText + "~" + letterGrade + "~" + percentageGrade + "~" + pointsReceived + "~" + pointsTotal + "~" + regularSectionWeight;
    }

    public double getAdjustedSectionWeight() {
        return adjustedSectionWeight;
    }

    public double getRegularSectionWeight() {
        return regularSectionWeight;
    }

    public void setAdjustedSectionWeight(Double weight) {
        adjustedSectionWeight = weight;
    }

    public void setRegularSectionWeight(Double weight) {
        regularSectionWeight = weight;
    }

    public boolean isSemesterTermSplitHeader() {
        if(getPeriod().getTerm().STR != 0) {
            return false;
        }

        return boldedText.equals("T1 ") || boldedText.equals("T2 ") || boldedText.equals("T3 ") || boldedText.equals("T4 ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HeaderItem that = (HeaderItem) o;
        return boldedText.equals(that.boldedText);
    }

    @Override
    public int hashCode() {
        return boldedText.hashCode();
    }
}
