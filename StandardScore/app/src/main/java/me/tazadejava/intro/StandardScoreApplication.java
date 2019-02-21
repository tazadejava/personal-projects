package me.tazadejava.intro;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import java.lang.ref.WeakReference;

import me.tazadejava.gradeupdates.GradesManager;

/**
 * Created by Darren on 11/6/2016.
 */
public class StandardScoreApplication extends Application {

    private static WeakReference<Context> context;

    private GradesManager gradesManager;
    private Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();

        context = new WeakReference<>(getApplicationContext());

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));

        ActivityLifecycleCallbacks callback = new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                currentActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        };
        registerActivityLifecycleCallbacks(callback);
    }

    public void initGradesManager(Context context) {
        gradesManager = new GradesManager(context);
    }

    public void deleteGradesManager() {
        gradesManager = null;
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public GradesManager getGradesManager() {
        return gradesManager;
    }

    public static int getColorId(int resourceID) {
        return context.get().getResources().getColor(resourceID, null);
    }
}
