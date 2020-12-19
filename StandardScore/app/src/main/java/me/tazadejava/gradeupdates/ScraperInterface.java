package me.tazadejava.gradeupdates;

import android.webkit.JavascriptInterface;

import java.util.Set;

import me.tazadejava.classdetails.GradedItem;
import me.tazadejava.classdetails.HeaderItem;

/**
 * Created by Darren on 6/8/2017.
 */
public class ScraperInterface {

    private GradesManager manager;

    public ScraperInterface(GradesManager manager) {
        this.manager = manager;
    }

    @JavascriptInterface
    public void registerClassTerm(String id, String className, String period, String teacher, String term, String termDates, String letterGrade, String percentage, String comments) {
        String letterGradeStr = "-";
        if(!letterGrade.isEmpty()) {
            letterGradeStr = "" + letterGrade.charAt(0);
            if(letterGrade.length() > 1 && (letterGrade.charAt(1) == '+' || letterGrade.charAt(1) == '-')) {
                letterGradeStr += "" + letterGrade.charAt(1);
            }
        }

        String termBeginSplit = termDates.split(" - ")[0].replace("(", "");
        String termEndSplit = termDates.split(" - ")[1].replace(")", "");

        double percentageNumber = -1.0d;
        try {
            percentageNumber = Double.valueOf(percentage);
        } catch (NumberFormatException ex) {}

        int periodNum = 0;
        for(int i = period.length() - 2; i >= 0; i--) {
            if(Character.isDigit(period.charAt(i))) {
                periodNum = Character.getNumericValue(period.charAt(i));
                break;
            }
        }

        ClassPeriod classPeriod = new ClassPeriod(id, className, teacher,
                periodNum,
                ClassPeriod.GradeTerm.valueOf(term.split(" ")[0]), termBeginSplit.split("/"), termEndSplit.split("/"), letterGradeStr,
                percentageNumber, manager);

        if(!comments.isEmpty()) {
            classPeriod.setComments(comments.split(";"));
        }

        manager.updatedClassPeriods.get(classPeriod.getTerm()).add(classPeriod);
    }

    @JavascriptInterface
    public void addGradedItem(String id, String gradeDate, String gradedName, String pointsOutOf, String percentageGrade, String letterGrade, String comment, boolean isMissing, boolean isNoCount, String absentNote) {
        ClassPeriod period = getPeriodInstance(id);

        String[] pointsSplit;
        if(pointsOutOf.isEmpty()) {
            pointsSplit = new String[] {"0", "0"};
        } else {
            pointsSplit = pointsOutOf.split(" out of ");
        }

        String letterGradeStr = "-";
        if(!letterGrade.isEmpty() && !letterGrade.equals(" ")) {
            letterGradeStr = "" + letterGrade.charAt(0);
            if(letterGrade.length() > 1 && (letterGrade.charAt(1) == '+' || letterGrade.charAt(1) == '-')) {
                letterGradeStr += "" + letterGrade.charAt(1);
            }
        }

        period.getGradedItems().add(new GradedItem(gradeDate, gradedName, letterGradeStr, !percentageGrade.contains(".") ? -1.0 : Double.valueOf(percentageGrade),
                pointsSplit[0], pointsSplit[1], period, comment, isMissing, isNoCount, absentNote));
    }

    @JavascriptInterface
    public void addGradeHeader(String id, String sectionName, String weight, String pointsOutOf, String percentageGrade, String letterGrade) {
        ClassPeriod period = getPeriodInstance(id);

        String[] pointsSplit = pointsOutOf.split(" out of ");
        if(pointsOutOf.equals(" ")) {
            pointsSplit = new String[] {"0", "0"};
        }

        String letterGradeStr = "-";
        if(!letterGrade.isEmpty() && !letterGrade.equals(" ")) {
            letterGradeStr = "" + letterGrade.charAt(0);
            if(letterGrade.length() > 1 && (letterGrade.charAt(1) == '+' || letterGrade.charAt(1) == '-')) {
                letterGradeStr += "" + letterGrade.charAt(1);
            }
        }

        String[] weightSplit = weight.split(" ");
        double scaledWeight = Double.valueOf(weightSplit[weightSplit.length - 1].replaceAll("[%)]", ""));
        double regularWeight;
        if(weight.contains("adjusted to")) {
            if(weightSplit[2].equals("at")) {
                regularWeight = Double.valueOf(weightSplit[3].replaceAll("[%),]", ""));
            } else {
                regularWeight = Double.valueOf(weightSplit[2].replaceAll("[%),]", ""));
            }
        } else {
            regularWeight = scaledWeight;
        }

        period.getGradedItems().add(new HeaderItem(scaledWeight, regularWeight, sectionName.split("weighted")[0], letterGradeStr, !percentageGrade.contains(".") ? -1.0 : Double.valueOf(percentageGrade),
                pointsSplit[0], pointsSplit[1], period));
    }

    private ClassPeriod getPeriodInstance(String ID) {
        for(Set<ClassPeriod> periods : manager.updatedClassPeriods.values()) {
            for(ClassPeriod period : periods) {
                if(period.getID().equals(ID)) {
                    return period;
                }
            }
        }

        return null;
    }

    @JavascriptInterface
    public void setLastDialog(String id) {
        manager.lastDialogID = id;
    }
}
