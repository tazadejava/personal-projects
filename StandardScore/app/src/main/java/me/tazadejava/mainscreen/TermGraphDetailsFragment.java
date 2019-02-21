package me.tazadejava.mainscreen;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import org.joda.time.LocalDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.tazadejava.classdetails.PercentageDate;
import me.tazadejava.gradeupdates.AverageTermGradeHistoryManager;
import me.tazadejava.gradeupdates.ClassPeriod;
import me.tazadejava.gradeupdates.GradesManager;
import me.tazadejava.standardscore.R;

import static android.content.Context.VIBRATOR_SERVICE;

public class TermGraphDetailsFragment extends Fragment {

    private AverageTermGradeHistoryManager averageTermGradeHistoryManager;
    private String year;

    public void init(GradesManager manager) {
        this.averageTermGradeHistoryManager = manager.getAverageTermGradeManager();
        year = manager.getCurrentViewYear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_class_graph, container, false);

        ClassPeriod.GradeTerm term = ClassPeriod.GradeTerm.valueOf(getArguments().getString("term"));
        setupGraph(term, (GraphView) parentView.findViewById(R.id.percentageGraph));

        return parentView;
    }

    private void setupGraph(ClassPeriod.GradeTerm term, GraphView graph) {

        LineGraphSeries<DataPoint> percentages = new LineGraphSeries<>();

        double minPercent = 100, maxPercent = 0;

        List<PercentageDate> sorted = new ArrayList<>(averageTermGradeHistoryManager.getAverageGrades(year, term));

        Collections.sort(sorted, new Comparator<PercentageDate>() {
            @Override
            public int compare(PercentageDate o1, PercentageDate o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });

        LocalDate minDate = LocalDate.now(), maxDate = LocalDate.now();
        for (PercentageDate point : sorted) {
            double percent = point.getPercentage();
            percentages.appendData(new DataPoint(point.getDate().toDate(), percent), true, sorted.size());

            if (percent < minPercent) {
                minPercent = percent;
            }
            if (percent > maxPercent) {
                maxPercent = percent;
            }

            if (point.getDate().isBefore(minDate)) {
                minDate = point.getDate();
            }
            if (point.getDate().isAfter(maxDate)) {
                maxDate = point.getDate();
            }
        }

        percentages.setDrawDataPoints(sorted.size() <= 1);
        percentages.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Vibrator v = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);

                double roundedDataPoint = dataPoint.getY();
                if (roundedDataPoint < 60) {
                    roundedDataPoint = 60;
                }
                if (roundedDataPoint > 100) {
                    roundedDataPoint = 100;
                }

                v.vibrate((int) (60d * (1d - ((roundedDataPoint - 60) / 40d))) + 30);
                Toast.makeText(getContext(), new SimpleDateFormat("MM/dd").format(dataPoint.getX()) + ": " + dataPoint.getY() + "% (" + GradesManager.getLetterGrade(dataPoint.getY()) + ")", Toast.LENGTH_SHORT).show();
            }
        });

        if (maxPercent <= 96) {
            maxPercent += 4;
        }
        if (minPercent >= 4) {
            minPercent -= 4;
        }

        maxPercent = Math.ceil(maxPercent);
        minPercent = Math.floor(minPercent);

        if((maxPercent - minPercent) % 4 != 0) {
            minPercent += ((maxPercent - minPercent) % 4);
        }

        if (64 >= minPercent && 64 <= maxPercent) {
            LineGraphSeries<DataPoint> lineD = new LineGraphSeries<>();
            lineD.setDrawDataPoints(false);
            lineD.setColor(Color.argb(120, 255, 50, 0));
            lineD.appendData(new DataPoint(sorted.get(0).getDate().toDate(), 64), false, 2);
            lineD.appendData(new DataPoint(sorted.get(sorted.size() - 1).getDate().toDate(), 64), false, 2);
            graph.addSeries(lineD);
        }

        if (74 >= minPercent && 74 <= maxPercent) {
            LineGraphSeries<DataPoint> lineC = new LineGraphSeries<>();
            lineC.setDrawDataPoints(false);
            lineC.setColor(Color.argb(120, 255, 140, 0));
            lineC.appendData(new DataPoint(sorted.get(0).getDate().toDate(), 74), false, 2);
            lineC.appendData(new DataPoint(sorted.get(sorted.size() - 1).getDate().toDate(), 74), false, 2);
            graph.addSeries(lineC);
        }

        if (84 >= minPercent && 84 <= maxPercent) {
            LineGraphSeries<DataPoint> lineB = new LineGraphSeries<>();
            lineB.setDrawDataPoints(false);
            lineB.setColor(Color.argb(120, 255, 210, 0));
            lineB.appendData(new DataPoint(sorted.get(0).getDate().toDate(), 84), false, 2);
            lineB.appendData(new DataPoint(sorted.get(sorted.size() - 1).getDate().toDate(), 84), false, 2);
            graph.addSeries(lineB);
        }

        if (90 >= minPercent && 90 <= maxPercent) {
            LineGraphSeries<DataPoint> lineA = new LineGraphSeries<>();
            lineA.setDrawDataPoints(false);
            lineA.setColor(Color.argb(120, 0, 255, 0));
            lineA.appendData(new DataPoint(sorted.get(0).getDate().toDate(), 90), false, 2);
            lineA.appendData(new DataPoint(sorted.get(sorted.size() - 1).getDate().toDate(), 90), false, 2);
            graph.addSeries(lineA);
        }

        graph.addSeries(percentages);

        graph.getViewport().setScrollable(false);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(sorted.get(0).getDate().toDate().getTime());
        graph.getViewport().setMaxX(sorted.get(sorted.size() - 1).getDate().toDate().getTime());
        graph.getViewport().setMinY((int) Math.floor(minPercent));
        graph.getViewport().setMaxY((int) Math.ceil(maxPercent));

        graph.getGridLabelRenderer().setPadding(48);
        graph.getGridLabelRenderer().setVerticalAxisTitle("Percentage (%)");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Date (month/day)");
        graph.getGridLabelRenderer().setHumanRounding(false);
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getContext(), new SimpleDateFormat("MM/dd")));
        graph.getGridLabelRenderer().setLabelsSpace(4);
        graph.getGridLabelRenderer().setNumHorizontalLabels(sorted.size() < 5 ? sorted.size() : 5);
    }
}
