package me.tazadejava.schedule;

import android.webkit.JavascriptInterface;

import java.util.ArrayList;
import java.util.List;

import me.tazadejava.gradeupdates.ClassPeriod;

public class ScheduleScraperInterface {

    private List<SchedulePeriod> periods;

    private ScheduleViewActivity context;

    public ScheduleScraperInterface(ScheduleViewActivity context) {
        periods = new ArrayList<>();

        this.context = context;
    }

    @JavascriptInterface
    public void savePeriodTerm(String term, String period, String className, String teacherName, String roomName) {
        if(roomName.equals("Room GYM")) {
            roomName = "GYM";
        }

        ClassPeriod.GradeTerm gradeTerm = null;
        switch(term) {
            case "Term 1":
                gradeTerm = ClassPeriod.GradeTerm.T1;
                break;
            case "Term 2":
                gradeTerm = ClassPeriod.GradeTerm.T2;
                break;
            case "Term 3":
                gradeTerm = ClassPeriod.GradeTerm.T3;
                break;
            case "Term 4":
                gradeTerm = ClassPeriod.GradeTerm.T4;
                break;
        }
        periods.add(new SchedulePeriod(className, teacherName, roomName, gradeTerm, Integer.parseInt(period)));
    }

    @JavascriptInterface
    public void completeScheduleScrape() {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                context.updateList(periods, true);
            }
        });
    }
}
