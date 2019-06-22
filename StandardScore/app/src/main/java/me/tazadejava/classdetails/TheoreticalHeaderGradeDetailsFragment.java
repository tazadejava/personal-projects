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

public class TheoreticalHeaderGradeDetailsFragment extends Fragment {

    private String lastOutOfReceived, lastTotal, lastPercentage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_theoretical_grade_details, container, false);

        final ClassPeriod period = ((StandardScoreApplication) getActivity().getApplication()).getGradesManager().getCurrentPeriodByIndexAndTerm(getArguments().getInt("classPeriod"), ClassPeriod.GradeTerm.valueOf(getArguments().getString("classTerm")));

        lastOutOfReceived = getArguments().getString("lastOutOfReceived", "-");
        lastTotal = getArguments().getString("lastPercentage", "-");
        lastPercentage = getArguments().getString("lastPercentage", "-");

        final TextView ifOutOfScore = parentView.findViewById(R.id.ifOutOfScore);
        final TextView ifTermGrade = parentView.findViewById(R.id.ifTermGrade);

        final HeaderItem selectedHeaderItem = period.getHeaders().get(getArguments().getInt("headerIndex"));
        TextView cellName = parentView.findViewById(R.id.cellName);
        cellName.setText(selectedHeaderItem.getBoldedText());
        TextView cellWeight = parentView.findViewById(R.id.cellWeight);
        cellWeight.setText("Weighted at " + selectedHeaderItem.getAdjustedSectionWeight() + "%");
        TextView cellOutOf = parentView.findViewById(R.id.cellOutOf);
        cellOutOf.setText(selectedHeaderItem.getPointsReceived() + " out of " + selectedHeaderItem.getPointsTotal());

        final String termName = period.getTerm().NAME.toLowerCase();
        ifOutOfScore.setText(Html.fromHtml("If you get <b>- out of -</b>, <br>the " + termName + " grade will be: <b>-%</b>."));
        ifTermGrade.setText(Html.fromHtml("To get -% for " + termName + ",<br>it is necessary to get at least <b>-<br>out of -</b> on this assignment."));

        final TheoreticalGradeCalculator calculator = new TheoreticalGradeCalculator(period, selectedHeaderItem);

        ifOutOfScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Set a theoretical grade score to receive");

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setText("" + (lastOutOfReceived.equals("-") ? 100 : lastOutOfReceived));
                input.selectAll();
                builder.setView(input);

                builder.setPositiveButton("NEXT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getText().toString().isEmpty()) {
                            return;
                        }
                        lastOutOfReceived = input.getText().toString();

                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Set a theoretical total grade score");

                        final EditText input = new EditText(getContext());
                        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        input.setText("" + (lastTotal.equals("-") ? 100 : lastTotal));
                        input.selectAll();
                        builder.setView(input);

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(input.getText().toString().isEmpty()) {
                                    return;
                                }
                                lastTotal = input.getText().toString();
                                double inputtedScoreReceived = Double.parseDouble(lastOutOfReceived);
                                double inputtedScoreTotal = Double.parseDouble(lastTotal);

                                String outOfPercentage;
                                if(inputtedScoreTotal <= 0) {
                                    lastTotal = "0";
                                    outOfPercentage = "XC";
                                } else {
                                    outOfPercentage = (Math.round(inputtedScoreReceived / inputtedScoreTotal * 10000d) / 100d) + "%";
                                }

                                double resultingPercentage = calculator.calculateResultingTermPercentage(null, Double.parseDouble(lastOutOfReceived), Double.parseDouble(lastTotal));
                                resultingPercentage = Math.round(resultingPercentage * 100d) / 100d;

                                ifOutOfScore.setText(Html.fromHtml("If you get <b>" + lastOutOfReceived + " out of " + lastTotal + " (" + outOfPercentage + ")</b>, <br>the " + termName + " grade will be: <b>" + GradesManager.getLetterGrade(resultingPercentage) + " (" + resultingPercentage + "%)</b>."));
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        AlertDialog dialog2 = builder.create();
                        dialog2.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                        dialog2.show();
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

                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Set a theoretical total grade score");

                        final EditText input = new EditText(getContext());
                        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        input.setText("" + (lastTotal.equals("-") ? 100 : lastTotal));
                        input.selectAll();
                        builder.setView(input);

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(input.getText().toString().isEmpty()) {
                                    return;
                                }
                                lastTotal = input.getText().toString();

                                double inputtedPercentage = Double.parseDouble(lastPercentage);
                                int pointsTotal = Integer.parseInt(lastTotal);

                                double requiredPoints = calculator.calculateRequiredPointsReceived(null, inputtedPercentage, pointsTotal);

                                String outOfPercentage;
                                if(pointsTotal == 0) {
                                    outOfPercentage = "XC";
                                } else {
                                    outOfPercentage = (Math.round(requiredPoints / pointsTotal * 10000d) / 100d) + "%";
                                }

                                if (requiredPoints > pointsTotal && pointsTotal > 0) {
                                    ifTermGrade.setText(Html.fromHtml("To get <b>" + lastPercentage + "%</b> for " + termName + ", <br>it's not possible. Sorry! You need <b>" + requiredPoints + " out of " + pointsTotal + " (" + outOfPercentage + ")."));
                                } else if (requiredPoints <= 0) {
                                    ifTermGrade.setText(Html.fromHtml("To get <b>" + lastPercentage + "%</b> for " + termName + ", <br>you don't have to do anything (any grade on this assignment will reach the threshold)!"));
                                } else {
                                    ifTermGrade.setText(Html.fromHtml("To get <b>" + lastPercentage + "%</b> for " + termName + ", <br>it's necessary to get at least: <b>" + requiredPoints + " out of " + pointsTotal + " (" + outOfPercentage + ")</b> on this assignment."));
                                }
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        AlertDialog dialog2 = builder.create();
                        dialog2.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                        dialog2.show();
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
        bundle.putString("lastTotal", lastTotal);
        bundle.putString("lastPercentage", lastPercentage);

        bundle.putAll(getArguments());

        return bundle;
    }
}
