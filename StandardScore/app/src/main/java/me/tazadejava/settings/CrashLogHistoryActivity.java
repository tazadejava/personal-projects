package me.tazadejava.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import me.tazadejava.intro.CustomExceptionHandler;
import me.tazadejava.mainscreen.PeriodListActivity;
import me.tazadejava.standardscore.R;

public class CrashLogHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_log_history);

        final File[] crashLogs = CustomExceptionHandler.getCrashLogs(this);
        final String[] crashLogTexts = new String[crashLogs.length + 1];
        crashLogTexts[0] = "Select a crash log...";
        for(int i = 0; i < crashLogs.length; i++) {
            crashLogTexts[i + 1] = crashLogs[i].getName();
        }

        TextView crashLogCount = findViewById(R.id.crashLogCount);
        String version = "";
        try {
             version = "v. " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " ";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if(crashLogs.length == 1) {
            crashLogCount.setText("There is 1 crash log (" + version + "send to developer!).");
        } else {
            crashLogCount.setText("There are " + crashLogs.length + " crash logs (" + version + "send to developer!).");
        }

        final TextView crashLogText = findViewById(R.id.crashLogText);
        crashLogText.setMovementMethod(new ScrollingMovementMethod());

        Spinner crashLogSelector = findViewById(R.id.crashLogSelector);
        crashLogSelector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, crashLogTexts));
        crashLogSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position > 0) {
                    StringBuilder crashLog = new StringBuilder();

                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(crashLogs[position - 1]));

                        String read;
                        while((read = reader.readLine()) != null) {
                            crashLog.append(read + "\n");
                        }

                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(crashLog.length() > 0) {
                        crashLogText.setText(crashLog.substring(0, crashLog.length() - 2));
                    }
                } else {
                    crashLogText.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button clearCrashLogs = findViewById(R.id.crashLogClearAllButton);

        clearCrashLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(File file : crashLogs) {
                    file.delete();
                }

                Intent goToMain = new Intent(CrashLogHistoryActivity.this, PeriodListActivity.class);
                startActivity(goToMain);
                Toast.makeText(CrashLogHistoryActivity.this, "Cleared all crash logs", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
