package me.tazadejava.gradeupdates;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.tazadejava.classdetails.AdjustedHeaderItem;
import me.tazadejava.classdetails.GradeCellItem;
import me.tazadejava.classdetails.GradedItem;
import me.tazadejava.classdetails.HeaderItem;
import me.tazadejava.classdetails.PercentageDate;

/**
 * Created by Darren on 6/8/2017.
 */
public class ClassPeriod {

    public enum GradeTerm {
        S1("SEMESTER 1", 0, 2), S2("SEMESTER 2", 0, 5), T1("TERM 1", 1, 0), T2("TERM 2", 2, 1), T3("TERM 3", 3, 3), T4("TERM 4", 4, 4);

        public final String NAME;
        public final int STR, COLUMN;

        GradeTerm(String name, int str, int column) {
            NAME = name;
            STR = str;
            COLUMN = column;
        }
    }

    private String id;

    private List<GradeCellItem> gradedItems;
    private HashMap<String, PercentageDate> percentageDates;

    private String className, teacher;
    private int period;
    private GradeTerm term;
    private String letterGrade;
    private double percentage;

    private String[] comments;

    private DateTime lastTermUpdate;
    private LocalDate termBeginDate, termEndDate;

    private int updates;

    public ClassPeriod(String id, String className, String teacher, int period, GradeTerm term, String[] termBeginDate, String[] termEndDate, String letterGrade, double percentage, GradesManager gradesManager) {
        this.id = id;
        this.className = className;
        this.teacher = teacher;
        this.period = period;
        this.term = term;
        this.letterGrade = letterGrade;
        this.percentage = percentage;

        this.termBeginDate = new LocalDate(Integer.valueOf(termBeginDate[2]), Integer.valueOf(termBeginDate[0]), Integer.valueOf(termBeginDate[1]));
        this.termEndDate = new LocalDate(Integer.valueOf(termEndDate[2]), Integer.valueOf(termEndDate[0]), Integer.valueOf(termEndDate[1]));

        gradedItems = new ArrayList<>();
        lastTermUpdate = DateTime.now();
        updates = 0;

        ClassPeriod oldTerm = gradesManager.getClassTermInstance(id);
        if(oldTerm != null) {
            percentageDates = oldTerm.percentageDates;
        } else {
            percentageDates = new HashMap<>();
            percentageDates.put(LocalDate.now().toString(), new PercentageDate(this));
        }
    }

    public ClassPeriod(String save) {
        String[] split = save.split(";");
        id = split[0];
        className = split[1];
        teacher = split[2];
        period = Integer.valueOf(split[3]);
        term = GradeTerm.valueOf(split[4]);
        letterGrade = String.valueOf(split[5]);
        percentage = Double.valueOf(split[6]);
        lastTermUpdate = DateTime.parse(split[7]);

        gradedItems = new ArrayList<>();
        if(split.length > 8 && !split[8].isEmpty()) {
            String[] gradeCellSplit = split[8].split("§");
            for (String cell : gradeCellSplit) {
                if(cell.charAt(0) == 'H') {
                    gradedItems.add(new HeaderItem(cell, this));
                } else {
                    gradedItems.add(new GradedItem(cell, this));
                }
            }
        }

        if(split.length > 9) {
            if(split[9].equals("true") || split[9].equals("false")) {
                split[9] = "0";
            }

            updates = Integer.valueOf(split[9]);
        }

        if(split.length > 10) {
            termBeginDate = LocalDate.parse(split[10]);
        }

        if(split.length > 11) {
            termEndDate = LocalDate.parse(split[11]);
        }

        percentageDates = new HashMap<>();
        if(split.length > 12 && !split[12].isEmpty()) {
            String[] percentageSplit = split[12].split("§");
            for (String item : percentageSplit) {
                PercentageDate pDate = new PercentageDate(item);
                percentageDates.put(pDate.getDate().toString(), pDate);
            }
        } else {
            percentageDates.put(LocalDate.now().toString(), new PercentageDate(this));
        }

        if(split.length > 13 && !split[13].isEmpty()) {
            comments = split[13].split("§");
        }
    }

    public String save() {
        String allGradeCellItems = "";
        for(GradeCellItem item : gradedItems) {
            allGradeCellItems += item.save() + "§";
        }
        if(!allGradeCellItems.isEmpty()) allGradeCellItems = allGradeCellItems.substring(0, allGradeCellItems.length() - 1);

        String allPercentageDates = "";
        for(PercentageDate item : percentageDates.values()) {
            allPercentageDates += item.save() + "§";
        }
        if(!allPercentageDates.isEmpty()) allPercentageDates = allPercentageDates.substring(0, allPercentageDates.length() - 1);

        String commentSplit = "";
        if(comments != null) {
            for (String comment : comments) {
                commentSplit += comment + "§";
            }
            if (!commentSplit.isEmpty()) commentSplit = commentSplit.substring(0, commentSplit.length() - 1);
        }

        return id + ";" + className + ";" + teacher + ";" + period + ";" + term + ";" + letterGrade + ";" + percentage + ";" + lastTermUpdate.toString() + ";" + allGradeCellItems + ";" + updates + (termBeginDate != null ? (";" + termBeginDate.toString() + ";" + termEndDate.toString()) : "") + ";" + allPercentageDates + ";" + commentSplit;
    }

    public void updateGradedItems(List<GradeCellItem> gradedItems) {
        this.gradedItems = gradedItems;
        lastTermUpdate = DateTime.now();
    }

    public void setComments(String[] comments) {
        this.comments = comments;
    }

    public List<GradeCellItem> getGradedItems() {
        return gradedItems;
    }

    public String getID() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public String getTeacher() {
        return teacher;
    }

    public int getPeriod() {
        return period;
    }

    public GradeTerm getTerm() {
        return term;
    }

    public LocalDate getTermBeginDate() {
        return termBeginDate;
    }

    public LocalDate getTermEndDate() {
        return termEndDate;
    }

    public String[] getComments() {
        return comments;
    }

    public boolean shouldUpdate() {
        LocalDate now = LocalDate.now();

        return now.isAfter(termBeginDate) && now.isBefore(termEndDate.plusDays(14));
    }

    public boolean isCurrentTerm() {
        LocalDate now = LocalDate.now();
        return now.isAfter(termBeginDate.minusDays(1)) && now.isBefore(termEndDate.plusDays(1));
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public double getPercentage() {
        return percentage;
    }

    public DateTime getLastTermUpdate() {
        return lastTermUpdate;
    }

    public List<HeaderItem> getHeaders() {
        List<HeaderItem> items = new ArrayList<>();
        for(GradeCellItem item : gradedItems) {
            if(item instanceof HeaderItem) {
                items.add((HeaderItem) item);
            }
        }

        return items;
    }

    public int getUpdates() {
        return updates;
    }

    public void resetUpdates() {
        updates = 0;
    }

    public void decrementUpdate() {
        updates--;

        if(updates < 0) {
            updates = 0;
        }
    }

    public void incrementUpdate() {
        if(getTerm().STR != 0) {
            updates++;
        }

        percentageDates.put(LocalDate.now().toString(), new PercentageDate(this));
    }

    public Collection<PercentageDate> getPercentageDates() {
        return percentageDates.values();
    }

    public List<AdjustedHeaderItem> getAdjustedHeaderItems(HeaderItem header) {
        List<AdjustedHeaderItem> headerItems = new ArrayList<>();

        int missingSections = 0;
        int hundredSections = 0;
        for(HeaderItem item : getHeaders()) {
            if(item.getAdjustedSectionWeight() == 0) {
                missingSections++;
            } else if(item.getAdjustedSectionWeight() == 100) {
                hundredSections++;
            }
        }

        if(header.getAdjustedSectionWeight() == 0) {
            switch(missingSections) {
                case 1:
                    for(HeaderItem item : getHeaders()) {
                        headerItems.add(new AdjustedHeaderItem(item, item.getRegularSectionWeight()));
                    }
                    break;
                default:
                    double newTotalPercentage = 0;
                    Set<HeaderItem> activeHeaders = new HashSet<>();
                    for(HeaderItem item : getHeaders()) {
                        if(item == header || item.getAdjustedSectionWeight() > 0) {
                            activeHeaders.add(item);
                            newTotalPercentage += item.getRegularSectionWeight();
                        }
                    }

                    for(HeaderItem item : activeHeaders) {
                        headerItems.add(new AdjustedHeaderItem(item, Math.round(item.getRegularSectionWeight() / newTotalPercentage * 1000d) / 10d));
                    }
                    break;
            }
        } else {
            if(hundredSections == getHeaders().size()) {
                double pointsReceived = 0, pointsTotal = 0;
                for(GradeCellItem item : getGradedItems()) {
                    if(item instanceof GradedItem) {
                        if(!item.getPointsReceived().equals("*")) {
                            pointsReceived += Double.valueOf(item.getPointsReceived());

                            if(!item.getPointsTotal().equals("*")) {
                                pointsTotal += Double.valueOf(item.getPointsTotal());
                            }
                        }
                    }
                }

                headerItems.add(new AdjustedHeaderItem(String.valueOf(pointsReceived), String.valueOf(pointsTotal), 100, pointsReceived / pointsTotal));
            } else {
                for (HeaderItem item : getHeaders()) {
                    headerItems.add(new AdjustedHeaderItem(item, item.getAdjustedSectionWeight()));
                }
            }
        }

        return headerItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassPeriod that = (ClassPeriod) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
