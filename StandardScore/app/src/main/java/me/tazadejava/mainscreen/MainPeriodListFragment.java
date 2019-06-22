package me.tazadejava.mainscreen;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.tazadejava.gradeupdates.ClassPeriod;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

public class MainPeriodListFragment extends Fragment {

    private RecyclerView periodList;
    private ClassPeriod.GradeTerm term;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_class_details, container, false);

        term = ClassPeriod.GradeTerm.valueOf(getArguments().getString("term"));
        periodList = parentView.findViewById(R.id.gradeCellsList);

        periodList.setMotionEventSplittingEnabled(false);

        initializePeriodList();

        return parentView;
    }

    private void initializePeriodList() {
        periodList.setAdapter(new PeriodListAdapter(getActivity(), ((StandardScoreApplication) getActivity().getApplication()).getGradesManager().getTermPeriodList(term)));
        ((PeriodListActivity) getActivity()).setPeriodListView(periodList);
        periodList.setLayoutManager(new LinearLayoutManager(getActivity()));
    }
}
