package me.tazadejava.schedule;

import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.List;

import me.tazadejava.standardscore.R;

public class ScheduleViewActivity extends AppCompatActivity {

    private TextView lastUpdateText, downloadingScheduleText;
    private RecyclerView recyclerView;

    private WebInterface scheduleInterface;
    private ScheduleManager manager;

    private Handler updatingScheduleTextLoop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_view);

        recyclerView = findViewById(R.id.scheduleList);
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        lastUpdateText = findViewById(R.id.lastUpdateText);
        downloadingScheduleText = findViewById(R.id.downloadingScheduleText);

        manager = new ScheduleManager(this);

        scheduleInterface = new WebInterface(this);

        if(manager.hasNeverUpdated()) {
            scheduleInterface.updateSchedule();
            lastUpdateText.setText("Updating now");
        } else {
            updateList(manager.getPeriods(), false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.schedule_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_refresh:
                scheduleInterface.updateSchedule();
                lastUpdateText.setText("Updating now");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(scheduleInterface.isUpdating()) {
            scheduleInterface.clearReferences();
        }
    }

    public void updateList(List<SchedulePeriod> periods, boolean updateManagerData) {
        recyclerView.setAdapter(new ScheduleAdapter(periods));

        if(updateManagerData) {
            manager.updateData(periods);
        }

        lastUpdateText.setText(manager.getLastUpdateText());
    }

    public void beginUpdateLoop() {
        updatingScheduleTextLoop = new Handler();
        updatingScheduleTextLoop.postDelayed(new Runnable() {

            boolean firstFrame = true;

            @Override
            public void run() {
                if(!scheduleInterface.isUpdating()) {
                    return;
                }

                if(firstFrame) {
                    downloadingScheduleText.setText("Downloading schedule . . ");
                } else {
                    downloadingScheduleText.setText("Downloading schedule. . .");
                }

                firstFrame = !firstFrame;

                updatingScheduleTextLoop.postDelayed(this, 1000L);
            }
        }, 1000L);
    }
}
