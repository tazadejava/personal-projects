package me.tazadejava.classdetails;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import me.tazadejava.gradeupdates.ClassPeriod;
import me.tazadejava.gradeupdates.GradesManager;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

public class TheoreticalAssignmentGradeDetailsFragment extends Fragment {

    private GradedItem item;
    private String lastOutOfReceived, lastPercentage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_cell_details, container, false);

        final ClassPeriod period = ((StandardScoreApplication) getActivity().getApplication()).getGradesManager().getCurrentPeriodByIndexAndTerm(getArguments().getInt("classPeriod"), ClassPeriod.GradeTerm.valueOf(getArguments().getString("classTerm")));

        lastOutOfReceived = getArguments().getString("lastOutOfReceived", "-");
        lastPercentage = getArguments().getString("lastPercentage", "-");

        item = (GradedItem) period.getGradedItems().get(getArguments().getInt("gradeCellItem"));

        TextView cellName = parentView.findViewById(R.id.cellName);
        cellName.setText(item.getGradedName());
        TextView cellOutOf = parentView.findViewById(R.id.cellOutOf);
        if(item.isNoCount() || item.isMissing()) {
            if(item.isNoCount() && item.isMissing()) {
                cellOutOf.setText(item.getPointsReceived() + " out of " + item.getPointsTotal() + " (MISSING, NO COUNT)");
            } else {
                if(item.isNoCount()) {
                    cellOutOf.setText(item.getPointsReceived() + " out of " + item.getPointsTotal() + " (NO COUNT)");
                } else {
                    cellOutOf.setText(item.getPointsReceived() + " out of " + item.getPointsTotal() + " (MISSING)");
                }
            }
        } else {
            cellOutOf.setText(item.getPointsReceived() + " out of " + item.getPointsTotal());
        }

        TextView comment = parentView.findViewById(R.id.cellSpecialNote);
        if(!item.getComment().isEmpty()) {
            if(!item.getAbsentNote().isEmpty()) {
                comment.setText("COMMENT: " + item.getComment() + "; ABSENT NOTE: " + item.getAbsentNote());
            } else {
                comment.setText("COMMENT: " + item.getComment());
            }
        } else if(!item.getAbsentNote().isEmpty()) {
            comment.setText("ABSENT NOTE: " + item.getAbsentNote());
        }

        final TextView ifTermGrade = parentView.findViewById(R.id.ifTermGrade);
        final TextView ifOutOfScore = parentView.findViewById(R.id.ifOutOfScore);

        final String termName = period.getTerm().NAME.toLowerCase();

        if(item.getPointsReceived().equals("*")) {
            ifTermGrade.setText(Html.fromHtml("To get <b>-%</b> for " + termName + ", <br>it's necessary to get at least: <b>- out of " + item.getPointsTotal() + "</b> on this assignment."));
            ifOutOfScore.setText(Html.fromHtml("If you get <b>- out of " + item.getPointsTotal() + "</b>, <br>the " + termName + " grade will be: <b>-%</b>."));
        } else {
            ifTermGrade.setText(Html.fromHtml("To have gotten <b>-%</b> for " + termName + ", <br>it was necessary to get at least: <b>- out of " + item.getPointsTotal() + "</b> on this assignment."));
            ifOutOfScore.setText(Html.fromHtml("If you had gotten <b>- out of " + item.getPointsTotal() + "</b>, <br>the " + termName + " grade would be: <b>-%</b>."));
        }

        final TheoreticalGradeCalculator calculator = new TheoreticalGradeCalculator(period, item.getHeader());

        ifOutOfScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Set a theoretical grade score (out of " + item.getPointsTotal() + ")");

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setText("" + (lastOutOfReceived.equals("-") ? item.getPointsTotal() : lastOutOfReceived));
                input.selectAll();
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getText().toString().isEmpty()) {
                            return;
                        }
                        lastOutOfReceived = input.getText().toString();
                        double inputtedScore = Double.parseDouble(lastOutOfReceived);

                        String outOfPercentage = Math.round(inputtedScore / Integer.parseInt(item.getPointsTotal()) * 10000d) / 100d + "%";
                        if(outOfPercentage.startsWith("Infinity")) {
                            outOfPercentage = "XC";
                        }

                        double resultingPercentage = calculator.calculateResultingTermPercentage(item, Double.parseDouble(lastOutOfReceived), Double.parseDouble(item.getPointsTotal()));
                        resultingPercentage = Math.round(resultingPercentage * 100d) / 100d;
                        if(item.getPointsReceived().equals("*")) {
                            ifOutOfScore.setText(Html.fromHtml("If you get <b>" + lastOutOfReceived + " out of " + item.getPointsTotal() + " (" + outOfPercentage + ")</b>, <br>the " + termName + " grade will be: <b>" + GradesManager.getLetterGrade(resultingPercentage) + " (" + resultingPercentage + "%)</b>."));
                        } else {
                            ifOutOfScore.setText(Html.fromHtml("If you had gotten <b>" + lastOutOfReceived + " out of " + item.getPointsTotal() + " (" + outOfPercentage + ")</b>, <br>the " + termName + " grade would be: <b>" + GradesManager.getLetterGrade(resultingPercentage) + " (" + resultingPercentage + "%)</b>."));
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
            }
        });

        ifTermGrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Set a theoretical percentage");

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setText("" + (lastPercentage.equals("-") ? "90" : lastPercentage));
                input.selectAll();
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getText().toString().isEmpty()) {
                            return;
                        }
                        lastPercentage = input.getText().toString();

                        double inputtedPercentage = Double.parseDouble(input.getText().toString());
                        int pointsTotal = Integer.parseInt(item.getPointsTotal());

                        double requiredPoints = calculator.calculateRequiredPointsReceived(item, inputtedPercentage, pointsTotal);

                        String outOfPercentage;
                        if(pointsTotal == 0) {
                            outOfPercentage = "XC";
                        } else {
                            outOfPercentage = (Math.round(requiredPoints / pointsTotal * 10000d) / 100d) + "%";
                        }

                        if(item.getPointsReceived().equals("*")) {
                            if (requiredPoints > pointsTotal && pointsTotal > 0) {
                                ifTermGrade.setText(Html.fromHtml("To get <b>" + input.getText() + "%</b> for " + termName + ", <br>it's not possible. Sorry! You need <b>" + requiredPoints + " out of " + pointsTotal + " (" + outOfPercentage + ")."));
                            } else if (requiredPoints <= 0) {
                                ifTermGrade.setText(Html.fromHtml("To get <b>" + input.getText() + "%</b> for " + termName + ", <br>you don't have to do anything (any grade on this assignment will reach the threshold)!"));
                            } else {
                                ifTermGrade.setText(Html.fromHtml("To get <b>" + input.getText() + "%</b> for " + termName + ", <br>it's necessary to get at least: <b>" + requiredPoints + " out of " + item.getPointsTotal() + " (" + outOfPercentage + ")</b> on this assignment."));
                            }
                        } else {
                            if (requiredPoints > pointsTotal && pointsTotal > 0) {
                                ifTermGrade.setText(Html.fromHtml("To have gotten <b>" + input.getText() + "%</b> for " + termName + ", <br>it wasn't possible. Sorry! You had needed <b>" + String.format("%.2f", requiredPoints) + " out of " + pointsTotal + " (" + outOfPercentage + ")."));
                            } else if (requiredPoints <= 0) {
                                ifTermGrade.setText(Html.fromHtml("To have gotten <b>" + input.getText() + "%</b> for " + termName + ", <br>you didn't have to do anything (any grade on this assignment would reach the threshold)!"));
                            } else {
                                ifTermGrade.setText(Html.fromHtml("To have gotten <b>" + input.getText() + "%</b> for " + termName + ", <br>it was necessary to get at least: <b>" + String.format("%.2f", requiredPoints) + " out of " + item.getPointsTotal() + " (" + outOfPercentage + ")</b> on this assignment."));
                            }
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
            }
        });

        return parentView;
    }

    public Bundle getSaveState() {
        Bundle bundle = new Bundle();

        bundle.putString("lastOutOfReceived", lastOutOfReceived);
        bundle.putString("lastPercentage", lastPercentage);

        bundle.putAll(getArguments());

        return bundle;
    }
}
