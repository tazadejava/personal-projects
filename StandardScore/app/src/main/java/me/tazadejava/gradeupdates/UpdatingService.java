package me.tazadejava.gradeupdates;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.intro.CustomExceptionHandler;
import me.tazadejava.intro.LoginActivity;
import me.tazadejava.mainscreen.PeriodListActivity;

/**
 * Created by Darren on 6/18/2017.
 */
public class UpdatingService extends Service {

    private GradesManager manager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        logMessage("Updating service was created!", UpdatingService.this);

        openNotification();

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        openNotification();

        if(PeriodListActivity.isRunning) {
            logMessage("Not running service because activity is running!", this);
            stopForeground(true);
            stopSelf();
            return START_STICKY;
        }
        KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        if(!myKM.inKeyguardRestrictedInputMode()) {
            logMessage("Not running service because the phone is being used!", this);
            stopForeground(true);
            stopSelf();
            return START_STICKY;
        }
        if(!GradesManager.isNetworkAvailable(this)) {
            logMessage("Not running service because there is no internet available!", this);
            stopForeground(true);
            stopSelf();
            return START_STICKY;
        }


        LoginActivity.loadFiles(UpdatingService.this);
        if(!LoginActivity.isUserLoggedIn()) {
            logMessage("USER IS NOT LOGGED IN. DO NOT PROCEED!", UpdatingService.this);
            stopForeground(true);
            stopSelf();
            return START_STICKY;
        }
        manager = new GradesManager(UpdatingService.this);
        if(manager.isTimeToUpdate(UpdatingService.this)) {
            logMessage("Updating!!! The term to update: " + manager.newestTerm.NAME, UpdatingService.this);
            manager.updateGrades(UpdatingService.this, GradesManager.UpdateReason.JOB_SCHEDULER, manager.getRecommendedUpdateTerms());
        } else {
            manager.clearReferences();
            logMessage("Not time to update. Updating service is NOT running.", UpdatingService.this);

            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    public static void logMessage(String message, Context context) {
//        System.out.println(message);
        if(context == null) {
            return;
        }

        try {
            File logFile = new File(context.getFilesDir().getAbsolutePath() + "/servicelog.txt");
            if(!logFile.exists()) {
                logFile.createNewFile();
            }

            FileWriter writer = new FileWriter(logFile, true);

            DateTime now = DateTime.now();
            writer.append("[" + now.getMonthOfYear() + "/" + now.getDayOfMonth() + "/" + now.getYear() + " at " + now.getHourOfDay() + ":" + (now.getMinuteOfHour() < 10 ? "0" : "") + now.getMinuteOfHour() + "][" + context.getClass().getSimpleName() + "] " + message + "\n");

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clearOldLogsIfAny(Context context) {
        List<String> currentLog = new ArrayList<>();

        File logFile = new File(context.getFilesDir().getAbsolutePath() + "/servicelog.txt");
        if(logFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(logFile));

                String read;
                while ((read = reader.readLine()) != null) {
                    String[] datePortions = read.split(" ")[0].replace("[", "").split("/");
                    DateTime date = new DateTime(Integer.parseInt(datePortions[2]), Integer.parseInt(datePortions[0]), Integer.parseInt(datePortions[1]), 0, 0);

                    if(date.plusDays(30).isAfterNow()) {
                        currentLog.add(read);
                    }
                }

                reader.close();

                FileWriter writer = new FileWriter(logFile, false);

                for(String current : currentLog) {
                    writer.append(current + "\n");
                }

                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openNotification() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "updating_service_channel_ss";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Updating Service",
                    NotificationManager.IMPORTANCE_MIN);
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);

        if(manager != null && manager.areGradesUpdating()) {
            manager.abortUpdate();
        }
    }
}
