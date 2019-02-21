package me.tazadejava.schedule;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import me.tazadejava.gradeupdates.ClassPeriod;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<SchedulePeriod> allPeriods, visiblePeriods;
    private boolean[] isTermVisible;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView roomName, className, teacherName;

        public ViewHolder(View itemView) {
            super(itemView);

            roomName = itemView.findViewById(R.id.termGrade);
            className = itemView.findViewById(R.id.className);
            teacherName = itemView.findViewById(R.id.teacherName);
        }
    }

    public ScheduleAdapter(List<SchedulePeriod> periods) {
        this.allPeriods = new ArrayList<>(periods);

        this.allPeriods.add(0, new SchedulePeriod(null, null, null, ClassPeriod.GradeTerm.T1, 0));
        this.allPeriods.add(0, new SchedulePeriod(null, null, null, ClassPeriod.GradeTerm.T2, 0));
        this.allPeriods.add(0, new SchedulePeriod(null, null, null, ClassPeriod.GradeTerm.T3, 0));
        this.allPeriods.add(0, new SchedulePeriod(null, null, null, ClassPeriod.GradeTerm.T4, 0));

        Collections.sort(this.allPeriods, new Comparator<SchedulePeriod>() {
            @Override
            public int compare(SchedulePeriod o1, SchedulePeriod o2) {
                return o1.getTerm().compareTo(o2.getTerm());
            }
        });

        visiblePeriods = new ArrayList<>();
        for(SchedulePeriod period : allPeriods) {
            if(period.getPeriod() == 0) {
                visiblePeriods.add(period);
            }
        }

        isTermVisible = new boolean[] {false, false, false, false};
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View scoresView = inflater.inflate(R.layout.period_item, parent, false);

        return new ViewHolder(scoresView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final SchedulePeriod item = visiblePeriods.get(position);

        if(item.getPeriod() == 0) {
            final int termIndex;
            switch(item.getTerm()) {
                case T1: default:
                    termIndex = 0;
                    break;
                case T2:
                    termIndex = 1;
                    break;
                case T3:
                    termIndex = 2;
                    break;
                case T4:
                    termIndex = 3;
                    break;
            }

            holder.className.setText(item.getTerm().NAME.toUpperCase());
            holder.teacherName.setText(isTermVisible[termIndex] ? "Click to collapse" : "Click to expand");
            holder.roomName.setText("");
            holder.itemView.setBackgroundColor(StandardScoreApplication.getColorId(R.color.colorHeaderItem));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if(isTermVisible[termIndex]) {
                        Iterator<SchedulePeriod> iterator = visiblePeriods.iterator();

                        while (iterator.hasNext()) {
                            SchedulePeriod period = iterator.next();
                            if (period == item) {
                                continue;
                            }

                            if (period.getTerm() == item.getTerm()) {
                                iterator.remove();
                            }
                        }
                    } else {
                        int itemIndex = visiblePeriods.indexOf(item);
                        int count = 1;
                        for(SchedulePeriod period : allPeriods) {
                            if(item != period && period.getTerm() == item.getTerm()) {
                                visiblePeriods.add(itemIndex + count, period);
                                count++;
                            }
                        }
                    }

                    isTermVisible[termIndex] = !isTermVisible[termIndex];
                    notifyDataSetChanged();
                }
            });
        } else {
            holder.className.setText("P" + item.getPeriod() + ": " + item.getClassName());
            holder.teacherName.setText(item.getTeacherName());
            holder.roomName.setText(item.getRoomName());

            if ((position % 2) == 0) {
                holder.itemView.setBackgroundColor(Color.LTGRAY);
            } else {
                holder.itemView.setBackgroundColor(Color.WHITE);
            }

            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return visiblePeriods.size();
    }
}
