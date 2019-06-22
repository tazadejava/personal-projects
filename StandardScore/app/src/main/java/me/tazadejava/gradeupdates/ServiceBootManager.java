package me.tazadejava.gradeupdates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import me.tazadejava.mainscreen.PeriodListActivity;
import me.tazadejava.settings.SettingsActivity;

public class ServiceBootManager extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            if (!pref.getBoolean(SettingsActivity.PREF_ENABLE_SERVICE, false)) {
                return;
            }

            PeriodListActivity.startBackgroundServices(context);
        }
    }
}
