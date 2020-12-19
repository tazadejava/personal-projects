package me.tazadejava.classdetails;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

/**
 * Created by Darren on 6/13/2017.
 */
public class GradeCellAdapter extends RecyclerView.Adapter<GradeCellAdapter.ViewHolder> {

    private AppCompatActivity activity;
    private List<GradeCellItem> cellItems;
    private List<Integer> headerItemPositions;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView subSpecificText, gradeName, letterGrade, pointsEarned, specialNoteIndicator;

        public ViewHolder(View itemView) {
            super(itemView);

            subSpecificText = itemView.findViewById(R.id.subSpecificText);
            specialNoteIndicator = itemView.findViewById(R.id.specialNoteIndicator);
            gradeName = itemView.findViewById(R.id.gradeName);
            letterGrade = itemView.findViewById(R.id.letterGrade);
            pointsEarned = itemView.findViewById(R.id.pointsEarned);
        }
    }

    public GradeCellAdapter(AppCompatActivity activity, List<GradeCellItem> cellItems) {
        this.activity = activity;
        this.cellItems = cellItems;

        headerItemPositions = new ArrayList<>();
        int count = 0;
        for(GradeCellItem item : cellItems) {
            if(item instanceof HeaderItem) {
                headerItemPositions.add(count);
            }
            count++;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View scoresView = inflater.inflate(R.layout.grade_cell_item, parent, false);

        return new ViewHolder(scoresView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final GradeCellItem item = cellItems.get(position);

        if(item.getPercentageGrade() == -1) {
            holder.letterGrade.setText("");
        } else {
            holder.letterGrade.setText(item.getLetterGrade() + " (" + item.getPercentageGrade() + "%)");
        }
        if(item.getPointsReceived().equals("0") && item.getPointsTotal().equals("0")) {
            holder.pointsEarned.setText("");
        } else {
            holder.pointsEarned.setText(item.getPointsReceived() + " out of " + item.getPointsTotal());
        }

        holder.subSpecificText.setText(item.getBoldedText());
        if(item instanceof HeaderItem) {
            final HeaderItem hItem = (HeaderItem) item;
            if(((HeaderItem) item).getAdjustedSectionWeight() == -1) {
                holder.gradeName.setText("");
                holder.itemView.setBackgroundColor(StandardScoreApplication.getColorId(R.color.colorHeaderItemUnweighted));
            } else {
                holder.gradeName.setText("Weighted at " + ((HeaderItem) item).getAdjustedSectionWeight() + "%");
                if(((HeaderItem) item).isSemesterTermSplitHeader()) {
                    holder.itemView.setBackgroundColor(StandardScoreApplication.getColorId(R.color.colorHeaderItemSemesterTermSplit));
                } else {
                    holder.itemView.setBackgroundColor(StandardScoreApplication.getColorId(R.color.colorHeaderItem));
                }
            }

            holder.specialNoteIndicator.setText("");
            holder.gradeName.setTypeface(null, Typeface.BOLD);
            holder.letterGrade.setTypeface(null, Typeface.BOLD);

            holder.itemView.setOnTouchListener(new View.OnTouchListener() {

                private int lastTouchDown = -1;
                private int currentTouchDown = -1;

                @Override
                public boolean onTouch(final View v, MotionEvent event) {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_CANCEL:
                            lastTouchDown++;
                            break;
                        case MotionEvent.ACTION_UP:
                            if(currentTouchDown != -1) {
                                TheoreticalHeaderGradeDetailsFragment fragment = new TheoreticalHeaderGradeDetailsFragment();

                                Bundle extras = new Bundle();
                                extras.putInt("classPeriod", item.getPeriod().getPeriod());
                                extras.putString("classTerm", item.getPeriod().getTerm().toString());

                                switch (item.getBoldedText()) {
                                    case "T1 ": case "T3 ":
                                        extras.putInt("headerIndex", item.getPeriod().getHeaders().indexOf(item) - 2);
                                        break;
                                    case "T2 ": case "T4 ":
                                        extras.putInt("headerIndex", item.getPeriod().getHeaders().indexOf(item) - 1);
                                        break;
                                    default:
                                        extras.putInt("headerIndex", item.getPeriod().getHeaders().indexOf(item));
                                        break;
                                }
                                fragment.setArguments(extras);

                                activity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fragment_slide_from_right, R.anim.fragment_slide_to_left, R.anim.fragment_slide_from_left, R.anim.fragment_slide_to_right).replace(R.id.classActivityFrameLayout, fragment, "theoreticalHeaderFragment").addToBackStack(null).commit();
                            }

                            lastTouchDown++;
                            break;
                        case MotionEvent.ACTION_DOWN:
                            lastTouchDown++;
                            currentTouchDown = lastTouchDown;

                            new Handler().postDelayed(new Runnable() {
                            @Override
                                public void run() {
                                    if(currentTouchDown == lastTouchDown) {
                                        currentTouchDown = -1;
                                        Vibrator vibrate = (Vibrator) v.getContext().getSystemService(Context.VIBRATOR_SERVICE);
                                        vibrate.vibrate(30);
                                        if(holder.gradeName.getText().toString().contains("adjusted")) {
                                            holder.gradeName.setText("Weighted at " + ((HeaderItem) item).getAdjustedSectionWeight() + "%");
                                        } else {
                                            holder.gradeName.setText("Weighted at " + hItem.getAdjustedSectionWeight() + "%, adjusted from " + hItem.getRegularSectionWeight() + "%");
                                        }
                                    }
                                }
                            }, 300L);
                            return true;
                    }

                    return false;
                }
            });
        } else {
            GradedItem gItem = (GradedItem) item;
            holder.gradeName.setText(gItem.getGradedName());

            StringBuilder note = new StringBuilder();
            if(gItem.isMissing()) {
                note.append("! ");
            }
            if(gItem.isNoCount()) {
                note.append("X ");
            }
            if(!gItem.getComment().isEmpty()) {
                note.append("* ");
            }
            if(!gItem.getAbsentNote().isEmpty()) {
                note.append("* ");
            }
            if(note.length() == 0) {
                holder.specialNoteIndicator.setText("");
            } else {
                holder.specialNoteIndicator.setText(note.substring(0, note.length() - 1));
            }

            if(gItem.isTagged) {
                holder.itemView.setBackgroundColor(activity.getResources().getColor(R.color.colorUpdatedAssignment, null));
                gItem.isTagged = false;
                item.getPeriod().decrementUpdate();

                NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
                String notificationID = item.getPeriod().getTerm().COLUMN + "" + item.getPeriod().getPeriod() + "" + item.getPeriod().getGradedItems().indexOf(item) + "" + item.getPeriod().getGradedItems().size();
                manager.cancel(Integer.parseInt(notificationID));
            } else {
                holder.itemView.setBackgroundColor(getSubPositionColor(position));
            }
            holder.gradeName.setTypeface(null, Typeface.NORMAL);
            holder.letterGrade.setTypeface(null, Typeface.NORMAL);

            holder.itemView.setOnTouchListener(null);
            if(!gItem.getBoldedText().startsWith("There are no")) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TheoreticalAssignmentGradeDetailsFragment fragment = new TheoreticalAssignmentGradeDetailsFragment();

                        Bundle extras = new Bundle();

                        extras.putInt("classPeriod", item.getPeriod().getPeriod());
                        extras.putString("classTerm", item.getPeriod().getTerm().toString());

                        extras.putInt("gradeCellItem", item.getPeriod().getGradedItems().indexOf(item));
                        fragment.setArguments(extras);

                        activity.getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fragment_slide_from_right, R.anim.fragment_slide_to_left, R.anim.fragment_slide_from_left, R.anim.fragment_slide_to_right).replace(R.id.classActivityFrameLayout, fragment, "theoreticalAssignmentFragment").addToBackStack(null).commit();
                    }
                });
            } else {
                holder.itemView.setOnClickListener(null);
            }
        }
    }

    private int getSubPositionColor(int position) {
        int lastHeaderPosition = -1;
        for(Integer pos : headerItemPositions) {
            if(pos < position) {
                lastHeaderPosition = pos;
            } else {
                break;
            }
        }

        if((position - lastHeaderPosition) % 2 == 0) {
            return Color.WHITE;
        } else {
            return Color.LTGRAY;
        }
    }

    @Override
    public int getItemCount() {
        return cellItems.size();
    }
}
