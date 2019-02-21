package me.tazadejava.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import me.tazadejava.gradeupdates.GradesManager;
import me.tazadejava.intro.CustomExceptionHandler;
import me.tazadejava.intro.LoginActivity;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if(!pref.getBoolean(SettingsActivity.PREF_ENABLE_ALL_NOTIFICATIONS, true)) {
            findPreference(SettingsActivity.PREF_ENABLE_UPDATE_NOTIFICATIONS).setEnabled(false);
            findPreference(SettingsActivity.PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS).setEnabled(false);
        }

        findPreference(SettingsActivity.PREF_VIEW_CRASH_LOGS).setEnabled(CustomExceptionHandler.hasCrashLogs(getContext()));

        findPreference(SettingsActivity.PREF_LOG_OUT).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showLogoutConfirmationDialog();
                return true;
            }
        });

        findPreference(SettingsActivity.PREF_SEND_FEEDBACK).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent sendEmail = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "tazadejava@gmail.com", null));
                startActivity(Intent.createChooser(sendEmail, "Send email..."));
                return true;
            }
        });

        findPreference(SettingsActivity.PREF_PRIVACY_POLICY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent viewPrivacyPolicy = new Intent(Intent.ACTION_VIEW);
                viewPrivacyPolicy.setData(Uri.parse("https://sites.google.com/view/standardscore/home"));
                startActivity(viewPrivacyPolicy);
                return true;
            }
        });

        findPreference(SettingsActivity.PREF_VIEW_UPDATE_HISTORY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent viewUpdateHistory = new Intent(getContext(), UpdateHistoryActivity.class);
                startActivity(viewUpdateHistory);
                return true;
            }
        });

        findPreference(SettingsActivity.PREF_VIEW_CRASH_LOGS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent viewCrashLogs = new Intent(getContext(), CrashLogHistoryActivity.class);
                startActivity(viewCrashLogs);
                return true;
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        GradesManager manager = ((StandardScoreApplication) getActivity().getApplication()).getGradesManager();
                        if(manager.areGradesUpdating()) {
                            manager.abortUpdate();
                        }

                        Intent loginActivity = new Intent(getContext(), LoginActivity.class);
                        loginActivity.putExtra("failedLogin", true);
                        loginActivity.putExtra("loggedOut", true);
                        startActivity(loginActivity);
                        break;
                }
            }
        };

        builder.setMessage("Log out? Your grades will be saved.").setPositiveButton("Yes", clickListener).setNegativeButton("No", clickListener).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener((SettingsActivity) getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener((SettingsActivity) getActivity());
    }
}
