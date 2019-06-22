package me.tazadejava.classdetails;

import java.util.List;

import me.tazadejava.gradeupdates.ClassPeriod;

public class TheoreticalGradeCalculator {

    private ClassPeriod period;
    private HeaderItem selectedHeaderItem;

    public TheoreticalGradeCalculator(ClassPeriod period, HeaderItem selectedHeaderItem) {
        this.period = period;
        this.selectedHeaderItem = selectedHeaderItem;
    }

    public double calculateResultingTermPercentage(GradedItem gradedItem, double pointsReceived, double pointsTotal) {
        double calculatedPercentage = 0d;
        List<AdjustedHeaderItem> headerItems = period.getAdjustedHeaderItems(selectedHeaderItem);
        for(AdjustedHeaderItem header : headerItems) {
            if(header.header != null && header.header.isSemesterTermSplitHeader()) {
                continue;
            }
            if(header.percentageGrade == -1 && header.weight == 0 && selectedHeaderItem != header.header) {
                continue;
            }

            if (header.header == null || selectedHeaderItem.getBoldedText().equals(header.header.getBoldedText())) {
                calculatedPercentage += getAddedSectionPercentage(header, gradedItem, pointsReceived, pointsTotal);
            } else {
                double headerGrade = header.weight * header.percentageGrade / 100d;
                if(headerGrade > header.weight) {
                    headerGrade = header.weight;
                }
                calculatedPercentage += headerGrade;
            }
        }

        return calculatedPercentage;
    }

    public double calculateRequiredPointsReceived(GradedItem gradedItem, double percentage, int pointsTotal) {
        double requiredScore = percentage;

        AdjustedHeaderItem adjustedHeader = null;
        List<AdjustedHeaderItem> headerItems = period.getAdjustedHeaderItems(selectedHeaderItem);
        if(headerItems.size() == 1) {
            adjustedHeader = headerItems.iterator().next();
        } else {
            for (AdjustedHeaderItem header : headerItems) {
                if(header.header != null && header.header.isSemesterTermSplitHeader()) {
                    continue;
                }
                if (selectedHeaderItem.getBoldedText().equals(header.header.getBoldedText())) {
                    adjustedHeader = header;
                } else {
                    double weight = header.weight * header.percentageGrade / 100d;

                    if (weight > header.weight) {
                        weight = header.weight;
                    }
                    requiredScore -= weight;
                }
            }
        }

        requiredScore = getAddedPointsForPercentage(adjustedHeader, gradedItem, pointsTotal, requiredScore);
        return requiredScore;
    }

    private double getAddedPointsForPercentage(AdjustedHeaderItem header, GradedItem gradedItem, double pointsTotal, double currentDownedPercentage) {
        double pointsRequired;

        if(gradedItem != null && !gradedItem.getPointsReceived().equals("*")) {
            pointsRequired = (currentDownedPercentage / header.weight * (Double.parseDouble(header.pointsTotal))) - Double.parseDouble(header.pointsReceived) + Double.parseDouble(gradedItem.getPointsReceived());
        } else {
            pointsRequired = (currentDownedPercentage / header.weight * (Double.parseDouble(header.pointsTotal) + pointsTotal)) - Double.parseDouble(header.pointsReceived);
        }

        pointsRequired = Math.round(pointsRequired * 10d) / 10d;

        return pointsRequired;
    }

    private double getAddedSectionPercentage(AdjustedHeaderItem header, GradedItem gradedItem, double addTop, double addBottom) {
        double receivedHeader = Double.valueOf(header.pointsReceived);
        double totalHeader = Double.valueOf(header.pointsTotal);

        double percentage;
        if(gradedItem != null && !gradedItem.getPointsReceived().equals("*")) {
            percentage = header.weight * (receivedHeader - Double.valueOf(gradedItem.getPointsReceived()) + addTop) / totalHeader;
        } else {
            percentage = header.weight * (receivedHeader + addTop) / (totalHeader + addBottom);
        }

        if(percentage > header.weight) {
            percentage = header.weight;
        }

        return percentage;
    }
}
