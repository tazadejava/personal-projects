package me.tazadejava.mainscreen;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.gradeupdates.UpdatingService;
import me.tazadejava.standardscore.R;

public class ServiceLogActivity extends AppCompatActivity {

    private List<String> logMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_log);

        UpdatingService.clearOldLogsIfAny(this);

        logMessages = new ArrayList<>();

        File logFile = new File(getFilesDir().getAbsolutePath() + "/servicelog.txt");
        if(logFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(logFile));

                String read;
                while ((read = reader.readLine()) != null) {
                    logMessages.add(0, read);
                }

                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        RecyclerView serviceLogList = findViewById(R.id.serviceLogList);
        serviceLogList.setLayoutManager(new LinearLayoutManager(this));
        serviceLogList.setAdapter(new ServiceLogAdapter());
    }

    public class ServiceLogAdapter extends RecyclerView.Adapter<ServiceLogAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView textView;

            public ViewHolder(View itemView) {
                super(itemView);

                textView = itemView.findViewById(R.id.serviceLogTextView);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.service_log_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(logMessages.get(position));

            if(position % 2 == 0) {
                holder.textView.setBackgroundColor(getResources().getColor(R.color.colorServiceLogMainColor, null));
            } else {
                holder.textView.setBackgroundColor(getResources().getColor(R.color.colorServiceLogOffColor, null));
            }
        }

        @Override
        public int getItemCount() {
            return logMessages.size();
        }
    }
}
