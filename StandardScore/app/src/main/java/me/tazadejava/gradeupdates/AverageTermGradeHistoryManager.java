package me.tazadejava.gradeupdates;

import org.joda.time.LocalDate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tazadejava.classdetails.PercentageDate;

public class AverageTermGradeHistoryManager {

    private HashMap<String, HashMap<ClassPeriod.GradeTerm, List<PercentageDate>>> averageGrades;

    public AverageTermGradeHistoryManager() {
        averageGrades = new HashMap<>();
    }

    public void loadTerm(String year, String termFileName, String data) {
        ClassPeriod.GradeTerm term;
        switch(termFileName.substring(0, termFileName.lastIndexOf("."))) {
            case "term1":
                term = ClassPeriod.GradeTerm.T1;
                break;
            case "term2":
                term = ClassPeriod.GradeTerm.T2;
                break;
            case "term3":
                term = ClassPeriod.GradeTerm.T3;
                break;
            case "term4":
                term = ClassPeriod.GradeTerm.T4;
                break;
            case "semester1":
                term = ClassPeriod.GradeTerm.S1;
                break;
            case "semester2":
                term = ClassPeriod.GradeTerm.S2;
                break;
            default:
                throw new IllegalArgumentException("Unknown loaded term file!");
        }

        data = data.replaceFirst("AVG_DATA: ", "");
        if(!averageGrades.containsKey(year)) {
            averageGrades.put(year, new HashMap<ClassPeriod.GradeTerm, List<PercentageDate>>());
        }

        averageGrades.get(year).put(term, new ArrayList<PercentageDate>());

        String[] dataSplit = data.split(";");
        for(String read : dataSplit) {
            averageGrades.get(year).get(term).add(new PercentageDate(read));
        }
    }

    public void saveData(ClassPeriod.GradeTerm term, FileWriter writer) throws IOException {
        if (averageGrades.isEmpty()) {
            return;
        }
        String year = GradesManager.getSchoolYear();
        if(!averageGrades.containsKey(year)) {
            return;
        }
        if(!averageGrades.get(year).containsKey(term) || averageGrades.get(year).get(term).isEmpty()) {
            return;
        }

        StringBuilder averageStr = new StringBuilder();
        for(PercentageDate percentageDate : averageGrades.get(year).get(term)) {
            averageStr.append(percentageDate.save() + ";");
        }

        writer.append("AVG_DATA: " + averageStr.substring(0, averageStr.length() - 1) + "\n");
    }

    public void appendAverageGrade(List<ClassPeriod> termPeriods, ClassPeriod.GradeTerm term) {
        String year = GradesManager.getSchoolYear();
        if(!averageGrades.containsKey(year)) {
            averageGrades.put(year, new HashMap<ClassPeriod.GradeTerm, List<PercentageDate>>());
        }
        if(!averageGrades.get(year).containsKey(term)) {
            averageGrades.get(year).put(term, new ArrayList<PercentageDate>());
        }

        double avg = 0d;
        int amount = 0;
        for(ClassPeriod classPeriod : termPeriods) {
            avg += classPeriod.getPercentage();
            amount++;
        }
        avg /= amount;
        avg = Math.round(avg * 100d) / 100d;

        averageGrades.get(year).get(term).add(new PercentageDate(avg));
    }

    public List<PercentageDate> getAverageGrades(String year, ClassPeriod.GradeTerm term) {
        if(!averageGrades.containsKey(year)) {
            return new ArrayList<>();
        }
        if(!averageGrades.get(year).containsKey(term)) {
            return new ArrayList<>();
        }

        return averageGrades.get(year).get(term);
    }
}
