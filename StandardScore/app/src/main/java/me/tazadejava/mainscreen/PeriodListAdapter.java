package me.tazadejava.mainscreen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import me.tazadejava.classdetails.ClassDetailsActivity;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

/**
 * Created by Darren on 6/13/2017.
 */
public class PeriodListAdapter extends RecyclerView.Adapter<PeriodListAdapter.ViewHolder> {

    private Activity activity;
    public List<PeriodItem> periodItems;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView termGrade, semesterGrade, className, teacherName;

        public ViewHolder(View itemView) {
            super(itemView);

            termGrade = (TextView) itemView.findViewById(R.id.termGrade);
            semesterGrade = (TextView) itemView.findViewById(R.id.semesterLetterGrade);
            className = (TextView) itemView.findViewById(R.id.className);
            teacherName = (TextView) itemView.findViewById(R.id.teacherName);
        }
    }

    public PeriodListAdapter(Activity activity, List<PeriodItem> periodItems) {
        this.activity = activity;
        this.periodItems = periodItems;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View scoresView = inflater.inflate(R.layout.period_item, parent, false);

        return new ViewHolder(scoresView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final PeriodItem item = periodItems.get(position);

        holder.termGrade.setText(item.getTermGrade());
        holder.semesterGrade.setText(item.getSemesterGrade());
        holder.className.setText(item.getClassName());
        holder.teacherName.setText(item.getTeacherName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPeriodDetails(item);
                holder.semesterGrade.setText("");
            }
        });

        if((position % 2) == 0) {
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    private void getPeriodDetails(PeriodItem item) {
        Intent goToClassDetails = new Intent(activity, ClassDetailsActivity.class);
        goToClassDetails.putExtra("classPeriod", item.classLink.getPeriod());
        goToClassDetails.putExtra("classTerm", item.classLink.getTerm().toString());
        activity.startActivity(goToClassDetails);
        activity.overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    @Override
    public int getItemCount() {
        return periodItems.size();
    }
}
