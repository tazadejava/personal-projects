package me.tazadejava.classdetails;

import org.joda.time.LocalDate;

import me.tazadejava.gradeupdates.ClassPeriod;

/**
 * Created by Darren on 10/8/2017.
 */
public class PercentageDate {

    private double percentage;
    private LocalDate date;

    public PercentageDate(ClassPeriod term) {
        this(term.getPercentage());
    }

    public PercentageDate(double percentage, LocalDate date) {
        this.percentage = percentage;
        this.date = date;
    }

    public PercentageDate(double percentage) {
        this(percentage, LocalDate.now());
    }

    public PercentageDate(String save) {
        String[] split = save.split("~");

        percentage = Double.valueOf(split[0]);
        date = LocalDate.parse(split[1]);
    }

    public String save() {
        return percentage + "~" + date.toString();
    }

    public double getPercentage() {
        return percentage;
    }

    public LocalDate getDate() {
        return date;
    }
}
