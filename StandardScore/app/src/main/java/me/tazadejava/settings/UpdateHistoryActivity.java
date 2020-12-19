package me.tazadejava.settings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

public class UpdateHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_history_view);

        RecyclerView recyclerView = findViewById(R.id.updateHistoryView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new UpdateHistoryAdapter(((StandardScoreApplication) getApplication()).getGradesManager().updateHistory));
    }
}
