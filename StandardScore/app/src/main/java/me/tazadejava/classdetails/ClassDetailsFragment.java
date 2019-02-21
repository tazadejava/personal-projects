package me.tazadejava.classdetails;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.tazadejava.gradeupdates.ClassPeriod;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

public class ClassDetailsFragment extends Fragment {

    private boolean inactiveHeadersEnabled = true;

    private ClassPeriod period;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_class_details, container, false);

        period = ((StandardScoreApplication) getActivity().getApplication()).getGradesManager().getCurrentPeriodByIndexAndTerm(getArguments().getInt("classPeriod"), ClassPeriod.GradeTerm.valueOf(getArguments().getString("classTerm")));

        recyclerView = parentView.findViewById(R.id.gradeCellsList);
        recyclerView.setAdapter(new GradeCellAdapter((AppCompatActivity) getActivity(), period.getGradedItems()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerView.setMotionEventSplittingEnabled(false);

        Parcelable recyclerViewState = getArguments().getParcelable("recyclerViewPosition");
        if(recyclerViewState != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }

        if(getArguments().getString("assignmentName", null) != null) {
            int position = 0;

            String assignmentName = getArguments().getString("assignmentName");
            String assignmentBolded = getArguments().getString("assignmentDate");

            int counter = 0;
            for(GradeCellItem cellItem : period.getGradedItems()) {
                if(cellItem instanceof GradedItem) {
                    if(((GradedItem) cellItem).getGradedName().equals(assignmentName)) {
                        if(cellItem.getBoldedText().equals(assignmentBolded)) {
                            position = counter;
                            break;
                        }
                    }
                }

                counter++;
            }

            recyclerView.scrollToPosition(position);
        }

        TextView comments = parentView.findViewById(R.id.comments);
        if(period.getComments() != null) {
            comments.setVisibility(View.VISIBLE);
            comments.setText(Arrays.toString(period.getComments()).replace("[", "").replace("]", ""));
        }

        return parentView;
    }

    public void toggleInactiveHeaders() {
        if(inactiveHeadersEnabled) {
            List<GradeCellItem> modifiedGradedItems = new ArrayList<>(period.getGradedItems());

            boolean eraseHeader = false;
            for(int i = modifiedGradedItems.size() - 1; i >= 0; i--) {
                if(modifiedGradedItems.get(i) instanceof HeaderItem && eraseHeader) {
                    modifiedGradedItems.remove(i);
                    eraseHeader = false;
                } else {
                    if (modifiedGradedItems.get(i).getBoldedText().startsWith("There are no")) {
                        eraseHeader = true;
                        modifiedGradedItems.remove(i);
                    }
                }
            }

            recyclerView.setAdapter(new GradeCellAdapter((AppCompatActivity) getActivity(), modifiedGradedItems));
        } else {
            recyclerView.setAdapter(new GradeCellAdapter((AppCompatActivity) getActivity(), period.getGradedItems()));
        }
        inactiveHeadersEnabled = !inactiveHeadersEnabled;
    }

    public Parcelable getSaveState() {
        return recyclerView.getLayoutManager().onSaveInstanceState();
    }
}
