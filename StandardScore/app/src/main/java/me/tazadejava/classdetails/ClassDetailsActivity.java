package me.tazadejava.classdetails;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import me.tazadejava.gradeupdates.ClassPeriod;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.standardscore.R;

public class ClassDetailsActivity extends AppCompatActivity {

    private int previousUpdatesCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_details);

        ClassPeriod period = ((StandardScoreApplication) getApplication()).getGradesManager().getCurrentPeriodByIndexAndTerm(getIntent().getExtras().getInt("classPeriod"), ClassPeriod.GradeTerm.valueOf(getIntent().getExtras().getString("classTerm")));

        previousUpdatesCount = period.getUpdates();

        RelativeLayout overheadTextLayout = findViewById(R.id.overheadTextLayout);
        overheadTextLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Fragment mainFragment = getSupportFragmentManager().findFragmentByTag("mainFragment");
                if(mainFragment != null && mainFragment.isVisible()) {
                    ClassGraphFragment fragment = new ClassGraphFragment();
                    fragment.setArguments(getIntent().getExtras());
                    getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fragment_slide_from_right, R.anim.fragment_slide_to_left, R.anim.fragment_slide_from_left, R.anim.fragment_slide_to_right).replace(R.id.classActivityFrameLayout, fragment).addToBackStack(null).commit();
                    return true;
                }
                return false;
            }
        });

        TextView className = findViewById(R.id.className);
        className.setText("P" + period.getPeriod() + ": " + period.getClassName());
        TextView teacherName = findViewById(R.id.teacherName);
        teacherName.setText(period.getTeacher());
        TextView termGrade = findViewById(R.id.termGrade);
        termGrade.setText(period.getLetterGrade() + " (" + period.getPercentage() + "%)");

        if(getIntent().hasExtra("isClassDetails")) {
            if(getIntent().getBooleanExtra("isClassDetails", true)) {
                ClassDetailsFragment fragment = new ClassDetailsFragment();
                fragment.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction().add(R.id.classActivityFrameLayout, fragment, "mainFragment").commit();
            } else if(getIntent().getBooleanExtra("isTheoreticalAssignment", false)) {
                TheoreticalAssignmentGradeDetailsFragment fragment = new TheoreticalAssignmentGradeDetailsFragment();

                fragment.setArguments(getIntent().getExtras());

                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fragment_slide_from_right, R.anim.fragment_slide_to_left, R.anim.fragment_slide_from_left, R.anim.fragment_slide_to_right).replace(R.id.classActivityFrameLayout, fragment, "theoreticalAssignmentFragment").commit();
            } else {
                TheoreticalHeaderGradeDetailsFragment fragment = new TheoreticalHeaderGradeDetailsFragment();

                fragment.setArguments(getIntent().getExtras());

                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fragment_slide_from_right, R.anim.fragment_slide_to_left, R.anim.fragment_slide_from_left, R.anim.fragment_slide_to_right).replace(R.id.classActivityFrameLayout, fragment, "theoreticalHeaderFragment").commit();
            }
        } else {
            ClassDetailsFragment fragment = new ClassDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.classActivityFrameLayout, fragment, "mainFragment").commit();
        }
    }

    public boolean shouldKickOnUpdate() {
        Fragment theoreticalAssignmentFragment = getSupportFragmentManager().findFragmentByTag("theoreticalAssignmentFragment");
        Fragment theoreticalHeaderFragment = getSupportFragmentManager().findFragmentByTag("theoreticalHeaderFragment");

        boolean isTheoreticalAssignment = theoreticalAssignmentFragment != null && theoreticalAssignmentFragment.isVisible();
        boolean isTheoreticalHeader = theoreticalHeaderFragment != null && theoreticalHeaderFragment.isVisible();

        ClassPeriod period = ((StandardScoreApplication) getApplication()).getGradesManager().getCurrentClassPeriods().get(getIntent().getIntExtra("classPeriod", 0));

        return (isTheoreticalAssignment || isTheoreticalHeader) && period.getUpdates() != 0;
    }

    public Bundle getSaveState() {
        Bundle saveState = new Bundle();

        Fragment classDetailsFragment = getSupportFragmentManager().findFragmentByTag("mainFragment");
        Fragment theoreticalAssignmentFragment = getSupportFragmentManager().findFragmentByTag("theoreticalAssignmentFragment");
        Fragment theoreticalHeaderFragment = getSupportFragmentManager().findFragmentByTag("theoreticalHeaderFragment");

        boolean isClassDetails = classDetailsFragment != null && classDetailsFragment.isVisible();
        saveState.putBoolean("isClassDetails", isClassDetails);
        boolean isTheoreticalAssignment = theoreticalAssignmentFragment != null && theoreticalAssignmentFragment.isVisible();
        saveState.putBoolean("isTheoreticalAssignment", isTheoreticalAssignment);
        boolean isTheoreticalHeader = theoreticalHeaderFragment != null && theoreticalHeaderFragment.isVisible();
        saveState.putBoolean("isTheoreticalHeader", isTheoreticalHeader);

        if(isClassDetails) {
            saveState.putParcelable("recyclerViewPosition", ((ClassDetailsFragment) classDetailsFragment).getSaveState());
        }
        if(isTheoreticalAssignment) {
            saveState.putAll(((TheoreticalAssignmentGradeDetailsFragment) theoreticalAssignmentFragment).getSaveState());
        }
        if(isTheoreticalHeader) {
            saveState.putAll(((TheoreticalHeaderGradeDetailsFragment) theoreticalHeaderFragment).getSaveState());
        }

        return saveState;
    }

    @Override
    public void onPause() {
        super.onPause();

        if(previousUpdatesCount != 0) {
            previousUpdatesCount = 0;
            ((StandardScoreApplication) getApplication()).getGradesManager().saveData(this);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setEnabled(getSupportFragmentManager().findFragmentById(R.id.classActivityFrameLayout) instanceof ClassDetailsFragment);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_hide_headers:
                ClassDetailsFragment fragment = (ClassDetailsFragment) getSupportFragmentManager().findFragmentById(R.id.classActivityFrameLayout);
                fragment.toggleInactiveHeaders();
                if(item.getTitle().toString().startsWith("Hide")) {
                    item.setTitle("Show Inactive Headers");
                } else {
                    item.setTitle("Hide Inactive Headers");
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.grades_list_toolbar, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);

        if(previousUpdatesCount != 0) {
            ((StandardScoreApplication) getApplication()).getGradesManager().saveData(this);
        }
    }
}
