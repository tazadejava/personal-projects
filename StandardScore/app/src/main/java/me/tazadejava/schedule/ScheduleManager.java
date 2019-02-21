package me.tazadejava.schedule;

import android.content.Context;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.intro.LoginActivity;

public class ScheduleManager {

    private List<SchedulePeriod> periods;
    private DateTime lastUpdate;

    private String saveFilePath;

    public ScheduleManager(Context context) {
        saveFilePath = context.getFilesDir().getAbsolutePath();

        loadData();
    }

    private void saveData() {
        try {
            File saveFile = new File(saveFilePath + "/" + LoginActivity.currentUUID + "/schedule.txt");
            if(!saveFile.exists()) {
                saveFile.createNewFile();
            }

            FileWriter writer = new FileWriter(saveFile);

            if (lastUpdate == null) {
                writer.append("\n");
            } else {
                writer.append(lastUpdate.toString() + "\n");
            }

            for(SchedulePeriod period : periods) {
                writer.append(period.save() + "\n");
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        periods = new ArrayList<>();

        try {
            File saveFile = new File(saveFilePath + "/" + LoginActivity.currentUUID + "/schedule.txt");
            if(!saveFile.exists()) {
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(saveFile));

            String read = reader.readLine();

            if (read == null) {
                reader.close();
                return;
            }

            if(!read.isEmpty()) {
                lastUpdate = DateTime.parse(read);
            }

            while((read = reader.readLine()) != null) {
                periods.add(new SchedulePeriod(read));
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateData(List<SchedulePeriod> periods) {
        this.periods = periods;
        lastUpdate = DateTime.now();

        saveData();
    }

    public String getLastUpdateText() {
        if(lastUpdate == null) {
            return "Never updated";
        }

        int daysAgo = (int) ((DateTime.now().getMillis() - lastUpdate.getMillis()) / 1000 / 60 / 60 / 24);

        if(daysAgo == 0) {
            return "Last updated today";
        } else if(daysAgo == 1) {
            return "Last updated yesterday";
        } else {
            return "Last updated " + daysAgo + " days ago";
        }
    }

    public boolean hasNeverUpdated() {
        return lastUpdate == null;
    }

    public List<SchedulePeriod> getPeriods() {
        return periods;
    }
}
