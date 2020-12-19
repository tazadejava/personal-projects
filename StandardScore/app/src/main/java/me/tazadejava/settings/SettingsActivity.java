package me.tazadejava.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

import me.tazadejava.mainscreen.PeriodListActivity;
import me.tazadejava.standardscore.R;

public class SettingsActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_ENABLE_SERVICE = "pref_enable_service";
    public static final String PREF_ENABLE_DATA_USAGE = "pref_enable_data_usage";
    public static final String PREF_ENABLE_ALL_NOTIFICATIONS = "pref_enable_all_notifications";
    public static final String PREF_ENABLE_UPDATE_NOTIFICATIONS = "pref_enable_update_notifications";
    public static final String PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS = "pref_enable_updated_grade_notifications";

    public static final String PREF_VIEW_UPDATE_HISTORY = "pref_view_update_history";
    public static final String PREF_ENABLE_CRASH_LOG_TOAST = "pref_enable_crash_log_toast";
    public static final String PREF_VIEW_CRASH_LOGS = "pref_view_crash_logs";
    public static final String PREF_SEND_FEEDBACK = "pref_send_feedback";
    public static final String PREF_PRIVACY_POLICY = "pref_privacy_policy";
    public static final String PREF_LOG_OUT = "pref_log_out";

    private SettingsFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        fragment = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case PREF_ENABLE_ALL_NOTIFICATIONS:
                if(sharedPreferences.getBoolean(key, true)) {
                    sharedPreferences.edit().putBoolean(PREF_ENABLE_UPDATE_NOTIFICATIONS, true).apply();
                    sharedPreferences.edit().putBoolean(PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS, true).apply();
                    ((SwitchPreference) fragment.findPreference(PREF_ENABLE_UPDATE_NOTIFICATIONS)).setChecked(true);
                    ((SwitchPreference) fragment.findPreference(PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS)).setChecked(true);

                    fragment.getPreferenceScreen().findPreference(PREF_ENABLE_UPDATE_NOTIFICATIONS).setEnabled(true);
                    fragment.getPreferenceScreen().findPreference(PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS).setEnabled(true);
                } else {
                    sharedPreferences.edit().putBoolean(PREF_ENABLE_UPDATE_NOTIFICATIONS, false).apply();
                    sharedPreferences.edit().putBoolean(PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS, false).apply();((SwitchPreference) fragment.findPreference(PREF_ENABLE_UPDATE_NOTIFICATIONS)).setChecked(false);
                    ((SwitchPreference) fragment.findPreference(PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS)).setChecked(false);


                    fragment.getPreferenceScreen().findPreference(PREF_ENABLE_UPDATE_NOTIFICATIONS).setEnabled(false);
                    fragment.getPreferenceScreen().findPreference(PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS).setEnabled(false);
                }
                break;
            case PREF_ENABLE_SERVICE:
                if(sharedPreferences.getBoolean(key, true)) {
                    PeriodListActivity.startBackgroundServices(this);

                    if(!Settings.canDrawOverlays(this)) {
                        androidx.appcompat.app.AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Grant permission?");
                        builder.setMessage("The app needs the overlay permission to update grades in the background.");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                myIntent.setData(Uri.parse("package:" + getPackageName()));
                                startActivityForResult(myIntent, 111);
                            }
                        });
                        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sharedPreferences.edit().putBoolean(SettingsActivity.PREF_ENABLE_SERVICE, false).apply();
                                ((SwitchPreference) fragment.getPreferenceScreen().findPreference(PREF_ENABLE_SERVICE)).setChecked(false);
                            }
                        });
                        builder.create().show();
                    }
                } else {
                    PeriodListActivity.cancelBackgroundService(this);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case 111:
                if(!Settings.canDrawOverlays(this)) {
                    PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit().putBoolean(SettingsActivity.PREF_ENABLE_SERVICE, false).apply();
                    ((SwitchPreference) fragment.getPreferenceScreen().findPreference(PREF_ENABLE_SERVICE)).setChecked(false);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
