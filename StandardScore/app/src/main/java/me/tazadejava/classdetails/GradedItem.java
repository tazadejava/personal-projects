package me.tazadejava.classdetails;

import java.util.Objects;

import me.tazadejava.gradeupdates.ClassPeriod;

/**
 * Created by Darren on 6/8/2017.
 */
public class GradedItem extends GradeCellItem {

    private String gradedName, comment, absentNote;
    private boolean isMissing, isNoCount;
    public boolean isTagged;

    public GradedItem(String gradeDate, String gradedName, String letterGrade, double percentageGrade, String pointsReceived, String pointsTotal, ClassPeriod term, String comment, boolean isMissing, boolean isNoCount, String absentNote) {
        super(gradeDate, letterGrade, percentageGrade, pointsReceived, pointsTotal, term);

        this.gradedName = gradedName;

        this.comment = comment.replaceAll("~", " ").replaceAll(";", " ");
        this.isMissing = isMissing;
        this.isNoCount = isNoCount;
        this.absentNote = absentNote.replaceAll("~", " ").replaceAll(";", " ");
    }

    public GradedItem(String save, ClassPeriod term) {
        super(save, term);

        String[] split = save.split("~");
        gradedName = split[1];
        boldedText = split[2];
        letterGrade = split[3];
        percentageGrade = Double.valueOf(split[4]);
        pointsReceived = split[5];
        pointsTotal = split[6];

        if(split.length > 7) {
            isTagged = Boolean.valueOf(split[7]);
        }
        if(split.length > 10) {
            comment = split[8];
            isMissing = Boolean.valueOf(split[9]);
            isNoCount = Boolean.valueOf(split[10]);
        } else {
            comment = "";
        }
        if(split.length > 11) {
            absentNote = split[11];
        } else {
            absentNote = "";
        }
    }

    @Override
    public String save() {
        return "G~" + gradedName + "~" + boldedText + "~" + letterGrade + "~" + percentageGrade + "~" + pointsReceived + "~" + pointsTotal + "~" + isTagged + "~" + comment
                + "~" + isMissing + "~" + isNoCount + "~" + absentNote;
    }

    public String getGradedName() {
        return gradedName;
    }

    public String getComment() {
        return comment;
    }

    public String getAbsentNote() {
        return absentNote;
    }

    public boolean isMissing() {
        return isMissing;
    }

    public boolean isNoCount() {
        return isNoCount;
    }

    public HeaderItem getHeader() {
        HeaderItem lastHeader = null;
        for(GradeCellItem item : term.getGradedItems()) {
            if(item instanceof HeaderItem && !((HeaderItem) item).isSemesterTermSplitHeader()) {
                lastHeader = (HeaderItem) item;
            }
            if(item == this) {
                break;
            }
        }

        return lastHeader;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GradedItem that = (GradedItem) o;
        return gradedName.equals(that.gradedName) && boldedText.equals(that.boldedText) && getHeader().equals(that.getHeader());
    }

    @Override
    public int hashCode() {
        return Objects.hash(gradedName, boldedText, getHeader());
    }
}
