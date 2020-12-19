package me.tazadejava.intro;

import android.content.Context;
import android.content.Intent;

import org.joda.time.LocalDateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import me.tazadejava.gradeupdates.UpdatingService;

/**
 * Created by Darren on 2/10/2018.
 */

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Context context;

    private boolean isService;
    private String path;

    public CustomExceptionHandler(Context context) {
        this.context = context;

        isService = context instanceof UpdatingService;
        path = context.getFilesDir().getAbsolutePath();
    }

    public static boolean hasCrashLogs(Context context) {
        File dataFolder = new File(context.getFilesDir().getAbsolutePath() + "/crashes/");
        return dataFolder.listFiles() != null && dataFolder.listFiles().length > 0;
    }

    public static File[] getCrashLogs(Context context) {
        return new File(context.getFilesDir().getAbsolutePath() + "/crashes/").listFiles();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        String stacktrace;

        Writer stringBuffer = new StringWriter();
        PrintWriter writer = new PrintWriter(stringBuffer);

        e.printStackTrace(writer);
        stacktrace = stringBuffer.toString();
        writer.close();

        saveCrashLog(stacktrace);

        if(getNumberOfRecentCrashes() < 2) {
            restartApp();
        }
    }

    private void restartApp() {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);

        System.exit(1);
    }

    private int getNumberOfRecentCrashes() {
        File dataFolder = new File(path + "/crashes/");
        if(dataFolder.exists()) {
            File[] crashes = dataFolder.listFiles();

            int recentCrashes = 0;

            LocalDateTime now = LocalDateTime.now();
            for(File crash : crashes) {
                LocalDateTime time = LocalDateTime.parse(crash.getName().replace(".txt", ""));

                if(time.plusSeconds(5).isAfter(now)) {
                    recentCrashes++;
                }
            }

            return recentCrashes;
        }

        return 0;
    }

    private void saveCrashLog(String log) {
        try {
            File dataFolder = new File(path + "/crashes/");
            if(!dataFolder.exists()) {
                dataFolder.mkdir();
            }
            File dataFile = new File(dataFolder.getAbsolutePath() + "/" + LocalDateTime.now().toString() + ".txt");

            FileWriter writer = new FileWriter(dataFile);

            writer.append(isService ? "FROM UPDATING SERVICE:" : "FROM MAIN ACTIVITY:");
            writer.append(log);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
