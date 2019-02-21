package me.tazadejava.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import me.tazadejava.gradeupdates.GradesManager;
import me.tazadejava.gradeupdates.UpdateHistoryPoint;
import me.tazadejava.standardscore.R;

/**
 * Created by Darren on 6/13/2017.
 */
public class UpdateHistoryAdapter extends RecyclerView.Adapter<UpdateHistoryAdapter.ViewHolder> {

    private List<DateTime> updateDates;
    private LinkedHashMap<DateTime, List<UpdateHistoryPoint>> updateHistory;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView historyDate, historyDetails;

        public ViewHolder(View itemView) {
            super(itemView);

            historyDate = itemView.findViewById(R.id.historyDate);
            historyDetails = itemView.findViewById(R.id.historyDetails);
        }
    }

    public UpdateHistoryAdapter(LinkedHashMap<DateTime, List<UpdateHistoryPoint>> updateHistory) {
        this.updateHistory = new LinkedHashMap<>(updateHistory);

        updateDates = new ArrayList<>(updateHistory.keySet());
        Collections.reverse(updateDates);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View scoresView = inflater.inflate(R.layout.grade_history_item, parent, false);

        return new ViewHolder(scoresView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.historyDate.setText(updateDates.get(position).toString(DateTimeFormat.shortDateTime()));

        List<UpdateHistoryPoint> points = updateHistory.get(updateDates.get(position));

        if(points.isEmpty()) {
            holder.historyDetails.setText("");
            holder.historyDetails.setVisibility(View.GONE);
        } else {
            holder.historyDetails.setVisibility(View.VISIBLE);
            SpannableStringBuilder details = new SpannableStringBuilder();

            for(UpdateHistoryPoint point : points) {
                SpannableStringBuilder message;

                if(point.className.equals(GradesManager.NONEXISTENT_TERMS_PLACEHOLDER)) {
                    message = new SpannableStringBuilder("Checked for updates in nonexistent terms");
                } else {
                    if (point.isUngraded) {
                        message = new SpannableStringBuilder("Added assignment " + point.assignmentName + " in " + point.className);
                        message.setSpan(new StyleSpan(Typeface.BOLD), 17, 17 + point.assignmentName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        if (point.pointsReceived.equals("0") && point.pointsTotal.equals("0")) {
                            message = new SpannableStringBuilder("Updated " + point.assignmentName + " in " + point.className);
                        } else {
                            message = new SpannableStringBuilder("Updated " + point.assignmentName + " in " + point.className + " (" + point.pointsReceived + " out of " + point.pointsTotal + ")");
                        }
                        message.setSpan(new StyleSpan(Typeface.BOLD), 8, 8 + point.assignmentName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                details.append(message);

                if(point != points.get(points.size() - 1)) {
                    details.append("\n");
                }
            }

            holder.historyDetails.setText(details);
        }
    }

    @Override
    public int getItemCount() {
        return updateDates.size();
    }
}
