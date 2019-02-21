package me.tazadejava.classdetails;

public class AdjustedHeaderItem {

    HeaderItem header;

    String pointsReceived, pointsTotal;
    double weight, percentageGrade;

    public AdjustedHeaderItem(HeaderItem item, double weight) {
        header = item;
        pointsReceived = item.getPointsReceived();
        pointsTotal = item.getPointsTotal();
        this.weight = weight;
        percentageGrade = item.getPercentageGrade();
    }

    public AdjustedHeaderItem(String pointsReceived, String pointsTotal, double weight, double percentageGrade) {
        header = null;
        this.pointsReceived = pointsReceived;
        this.pointsTotal = pointsTotal;
        this.weight = weight;
        this.percentageGrade = percentageGrade;
    }
}
