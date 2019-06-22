package me.tazadejava.mainscreen;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.LocalDate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tazadejava.classdetails.ClassDetailsActivity;
import me.tazadejava.classdetails.GradeCellItem;
import me.tazadejava.classdetails.GradedItem;
import me.tazadejava.gradeupdates.ClassPeriod;
import me.tazadejava.gradeupdates.GradesManager;
import me.tazadejava.gradeupdates.UpdatingService;
import me.tazadejava.intro.CustomExceptionHandler;
import me.tazadejava.intro.LoginActivity;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.schedule.ScheduleViewActivity;
import me.tazadejava.settings.SettingsActivity;
import me.tazadejava.standardscore.R;

public class PeriodListActivity extends AppCompatActivity {

    public static boolean isRunning, isOnForeground;

    public boolean isLoggedIn;
    private boolean hasCheckedLogin;

    public TextView mainText, lastUpdateText, termDates, firstTimeHelperText;
    private RecyclerView periodList;
    public ClassPeriod.GradeTerm currentViewTerm;

    private Fragment lastFragment;

    public TextView[] dialogTermPercentages;
    public Dialog lastOpenedDialog;
    private ColorStateList originalTextViewColor;

    private Menu menu;
    private SecretGestureListener secretGestureListener;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_period_list);

        if(getIntent().hasExtra("classPeriod")) {
            LoginActivity.loadFiles(this);

            if(((StandardScoreApplication) getApplication()).getGradesManager() != null) {
                ((StandardScoreApplication) getApplication()).deleteGradesManager();
            }
        }

        final TextView secretMessage = findViewById(R.id.secretMessage);
        termDates = findViewById(R.id.termDates);

        final RelativeLayout overheadLayout = findViewById(R.id.overheadTextLayout);

        overheadLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(getGradesManager().getAverageTermGradeManager().getAverageGrades(getGradesManager().getCurrentViewYear(), currentViewTerm).isEmpty()) {
                    return false;
                }

                TermGraphDetailsFragment fragment = new TermGraphDetailsFragment();
                Bundle bundle = new Bundle();
                bundle.putString("term", currentViewTerm.toString());
                fragment.setArguments(bundle);
                fragment.init(getGradesManager());
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fragment_slide_from_right, R.anim.fragment_slide_to_left, R.anim.fragment_slide_from_left, R.anim.fragment_slide_to_right).replace(R.id.mainActivityFragment, fragment).addToBackStack(null).commit();
                termDates.setText(getGradesManager().getCurrentViewYear());
                lastFragment = fragment;

                return true;
            }
        });

        final RelativeLayout mainLayout = findViewById(R.id.activity_main);
        final ColorStateList originalTextviewColor = termDates.getTextColors();

        secretGestureListener = new SecretGestureListener(this) {

            private long lastClassInvisibleTime = 0;
            private boolean canGesture;

            @Override
            public void onLongPressEvent() {
                if(!canGesture) {
                    canGesture = true;

                    final long invisibleTime = System.currentTimeMillis();
                    lastClassInvisibleTime = invisibleTime;
                    periodList.animate().alpha(0).setDuration(250).setInterpolator(new AccelerateInterpolator()).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            periodList.setVisibility(View.GONE);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if(invisibleTime == lastClassInvisibleTime) {
                                        periodList.setAlpha(1);
                                        periodList.setVisibility(View.VISIBLE);
                                        canGesture = false;
                                    }
                                }
                            }, 1000L);
                        }
                    }).start();
                }
            }

            @Override
            public void onGesture(Gesture gesture) {
                if(periodList == null) {
                    return;
                }
                if(!canGesture) {
                    return;
                }

                Vibrator vibrate = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if(gesture == Gesture.CLICK) {
                    vibrate.vibrate(30);
                } else {
                    vibrate.vibrate(20);
                }

                final long invisibleTime = System.currentTimeMillis();
                lastClassInvisibleTime = invisibleTime;
                periodList.animate().alpha(0).setDuration(250).setInterpolator(new AccelerateInterpolator()).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        periodList.setVisibility(View.GONE);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(invisibleTime == lastClassInvisibleTime) {
                                    periodList.setAlpha(1);
                                    periodList.setVisibility(View.VISIBLE);
                                    canGesture = false;
                                    termDates.setTextColor(originalTextviewColor);
                                }
                            }
                        }, 1000L);
                    }
                }).start();

                switch(gesture) {
                    case UP:
                        termDates.setTextColor(Color.BLUE);
                        break;
                    case RIGHT:
                        termDates.setTextColor(Color.MAGENTA);
                        break;
                    case DOWN:
                        termDates.setTextColor(Color.RED);
                        break;
                    case LEFT:
                        termDates.setTextColor(Color.GREEN);
                        break;
                    case CLICK:
                        termDates.setTextColor(Color.BLACK);
                        break;
                }
            }

            @Override
            public void onKonamiCodeSuccess() {
                double avg = 0d;
                int amount = 0;
                for(ClassPeriod classPeriod : getGradesManager().getCurrentClassPeriods()) {
                    if(classPeriod.getTerm() == currentViewTerm) {
                        avg += classPeriod.getPercentage();
                        amount++;
                    }
                }
                avg /= amount;
                avg = Math.round(avg * 100d) / 100d;

                String termName = currentViewTerm.NAME.toLowerCase();
                termName = termName.substring(0, 1).toUpperCase() + termName.substring(1);
                secretMessage.setText(termName + " grade average: " + avg + "%");

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        secretMessage.setText("");
                    }
                }, 5000);
            }
        };

        mainLayout.setOnTouchListener(secretGestureListener);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(CustomExceptionHandler.hasCrashLogs(this) && sharedPref.getBoolean(SettingsActivity.PREF_ENABLE_CRASH_LOG_TOAST, false)) {
            int crashLogs = CustomExceptionHandler.getCrashLogs(this).length;
            String text;
            if(crashLogs == 1) {
                text = "There is 1 pending crash log";
            } else {
                text = "There are " + crashLogs + " pending crash logs";
            }
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }

        mainText = findViewById(R.id.mainText);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        firstTimeHelperText = findViewById(R.id.firstTimeHelperText);

        if(((StandardScoreApplication) getApplication()).getGradesManager() == null) {
            ((StandardScoreApplication) getApplication()).initGradesManager(this);
        }

        init();

        if(getIntent().hasExtra("refresh")) {
            if(currentViewTerm == null) {
                if(getGradesManager().newestTerm == null) {
                    getGradesManager().calculateNewestTerm();
                }
                updateViews(getGradesManager().newestTerm);
            } else {
                updateViews(currentViewTerm);
            }
        }

        if(getIntent().hasExtra("classPeriod")) {
            ClassPeriod period = ((StandardScoreApplication) getApplication()).getGradesManager().getCurrentPeriodByIndexAndTerm(getIntent().getExtras().getInt("classPeriod"), ClassPeriod.GradeTerm.valueOf(getIntent().getExtras().getString("classTerm")));
            currentViewTerm = period.getTerm();

            Intent intent = new Intent(this, ClassDetailsActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
        }

        String saveFilePath = getFilesDir().getAbsolutePath() + "/" + LoginActivity.currentUUID;
        File stateIndicator = new File(saveFilePath + "/state.indicator");
        if(stateIndicator.exists()) {
            stateIndicator.delete();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(getIntent().hasExtra("refresh")) {
            if(currentViewTerm == null) {
                if(getGradesManager().newestTerm == null) {
                    getGradesManager().calculateNewestTerm();
                }
                updateViews(getGradesManager().newestTerm);
            } else {
                updateViews(currentViewTerm);
            }
        }

        if(getIntent().hasExtra("classPeriod")) {
            ClassPeriod period = ((StandardScoreApplication) getApplication()).getGradesManager().getCurrentPeriodByIndexAndTerm(getIntent().getExtras().getInt("classPeriod"), ClassPeriod.GradeTerm.valueOf(getIntent().getExtras().getString("classTerm")));
            currentViewTerm = period.getTerm();

            Intent newIntent = new Intent(this, ClassDetailsActivity.class);
            newIntent.putExtras(getIntent());
            startActivity(newIntent);
        }
    }

    private void init() {
        final Handler updateFront = new Handler();
        updateFront.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isRunning) {
                    return;
                }

                if(lastUpdateText.getText().toString().startsWith("Last updated ")) {
                    lastUpdateText.setText(getGradesManager().getLastUpdateText(currentViewTerm));
                }

                updateFront.postDelayed(this, 60000);
            }
        }, 60000);

        if(LoginActivity.getPassword() == null && !hasCheckedLogin) {
            hasCheckedLogin = true;
            LoginActivity.loadFiles(this);
        }

        if(LoginActivity.getPassword() != null) {
            isLoggedIn = true;
            findViewById(R.id.backToLogin).setVisibility(View.INVISIBLE);

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            if (!getGradesManager().isSummer() && pref.getBoolean(SettingsActivity.PREF_ENABLE_SERVICE, true)) {
                startBackgroundServices(this);
            }
        } else {
            mainText.setText("Click to view previous grades");
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean hasUpdates = false;
        for(ClassPeriod period : getGradesManager().allClassPeriods.get(GradesManager.getSchoolYear())) {
            if (period.getTerm() == currentViewTerm) {
                if(period.getUpdates() > 0) {
                    hasUpdates = true;
                    break;
                }
            }
        }

        menu.getItem(1).setEnabled(hasUpdates);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.period_list_toolbar, menu);
        this.menu = menu;

        if(getGradesManager() != null && getGradesManager().areGradesUpdating()) {
            menu.getItem(2).setEnabled(false);
        }

        return true;
    }

    public Menu getMenu() {
        return menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                return true;
            case R.id.action_clear_tags:
                for(ClassPeriod period : getGradesManager().allClassPeriods.get(GradesManager.getSchoolYear())) {
                    if(period.getTerm() == currentViewTerm) {
                        period.resetUpdates();

                        for (GradeCellItem gItem : period.getGradedItems()) {
                            if (gItem instanceof GradedItem) {
                                ((GradedItem) gItem).isTagged = false;
                            }
                        }
                    }
                }
                getGradesManager().saveData(this);

                Toast.makeText(this, "Cleared all update notifications for " + currentViewTerm.NAME, Toast.LENGTH_SHORT).show();
                updateViews(currentViewTerm);

                return true;
            case R.id.action_view_schedule:
                Intent schedule = new Intent(this, ScheduleViewActivity.class);
                startActivity(schedule);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        isRunning = true;

        if(currentViewTerm == null) {
            if(getGradesManager().newestTerm == null) {
                getGradesManager().calculateNewestTerm();
            }
            updateViews(getGradesManager().newestTerm);
        } else {
            updateViews(currentViewTerm);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        isRunning = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        isOnForeground = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        isOnForeground = true;

        if(currentViewTerm != null) {
            String saveFilePath = getFilesDir().getAbsolutePath() + "/" + LoginActivity.currentUUID;
            File stateIndicator = new File(saveFilePath + "/state.indicator");
            if(stateIndicator.exists()) {
                UpdatingService.logMessage("Found the state indicator... Reloading the view!", this);
                stateIndicator.delete();
                finish();
                startActivity(getIntent());
            } else {
                updateViews(currentViewTerm);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(getGradesManager() != null && getGradesManager().areGradesUpdating()) {
            getGradesManager().abortUpdate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case 0:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getGradesManager().exportDataToDownloads(this);
                }
                break;
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getGradesManager().importDataFromDownloads(this);
                }
                break;
        }
    }

    public static void startBackgroundServices(Context context) {
        Intent testService = new Intent(context, UpdatingService.class);
        PendingIntent pInt;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pInt = PendingIntent.getForegroundService(context, 1, testService, 0);
        } else {
            pInt = PendingIntent.getService(context, 1, testService, 0);
        }

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 16000, 30 * 60 * 1000, pInt);
    }

    public static void cancelBackgroundService(Context context) {
        context.stopService(new Intent(context, UpdatingService.class));

        Intent testService = new Intent(context, UpdatingService.class);
        PendingIntent pInt;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pInt = PendingIntent.getForegroundService(context, 1, testService, 0);
        } else {
            pInt = PendingIntent.getService(context, 1, testService, 0);
        }

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.cancel(pInt);
    }

    public boolean updateViews(ClassPeriod.GradeTerm term) {
        if(getGradesManager().getCurrentClassPeriods() == null || getGradesManager().getCurrentClassPeriods().isEmpty()) {
            if(periodList != null) {
                periodList.setAdapter(new PeriodListAdapter(this, new ArrayList<PeriodItem>()));
                periodList.invalidate();
                termDates.setText("");
                mainText.setText("");
                lastUpdateText.setText("");
            }
            return false;
        }
        if(!getGradesManager().doesGradeTermExist(term)) {
            if(term == currentViewTerm) {
                periodList.setAdapter(new PeriodListAdapter(this, new ArrayList<PeriodItem>()));
                periodList.invalidate();
                termDates.setText("");
                mainText.setText("");
                lastUpdateText.setText("");
            }
            return false;
        }

        if(term == null) {
            term = getGradesManager().newestTerm;
        }
        currentViewTerm = term;

        MainPeriodListFragment fragment = new MainPeriodListFragment();
        Bundle bundle = new Bundle();
        bundle.putString("term", term.toString());
        fragment.setArguments(bundle);
        if(getSupportFragmentManager().getFragments().isEmpty()) {
            getSupportFragmentManager().beginTransaction().add(R.id.mainActivityFragment, fragment).commitAllowingStateLoss();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.mainActivityFragment, fragment).commitAllowingStateLoss();
        }
        getSupportFragmentManager().executePendingTransactions();

        lastFragment = fragment;

        if(getGradesManager().isTermUpdating(term)) {
            if(getGradesManager().currentUpdateTerm == null) {
                mainText.setText("Grades are being obtained...");
            } else {
                mainText.setText("Updating " + getGradesManager().currentUpdateTerm.NAME.toLowerCase() + "...");
            }
            lastUpdateText.setText(getGradesManager().getUpdatePercentage() + "%");
        } else {
            mainText.setText(term.NAME);
            lastUpdateText.setText(getGradesManager().getLastUpdateText(term));
        }

        if(getGradesManager().isSummer()) {
            mainText.setTextColor(getResources().getColor(R.color.colorPeriodOverheadTextSummer, null));
            lastUpdateText.setText("");
        } else {
            mainText.setTextColor(getResources().getColor(R.color.colorPeriodOverheadTextMain, null));
        }

        ClassPeriod[] classPeriods = getGradesManager().getAllClassTermInstances(term);

        LocalDate beginDate, endDate;
        if(classPeriods.length > 1 && (!classPeriods[0].getTermBeginDate().equals(classPeriods[1].getTermBeginDate()) || !classPeriods[0].getTermEndDate().equals(classPeriods[1].getTermEndDate()))) {
            HashMap<LocalDate, Integer> beginDates = new HashMap<>(), endDates = new HashMap<>();
            for (ClassPeriod period : classPeriods) {
                if (beginDates.containsKey(period.getTermBeginDate())) {
                    beginDates.put(period.getTermBeginDate(), beginDates.get(period.getTermBeginDate()) + 1);
                } else {
                    beginDates.put(period.getTermBeginDate(), 1);
                }
                if (endDates.containsKey(period.getTermEndDate())) {
                    endDates.put(period.getTermEndDate(), endDates.get(period.getTermEndDate()) + 1);
                } else {
                    endDates.put(period.getTermEndDate(), 1);
                }
            }

            List<Map.Entry<LocalDate, Integer>> beginSorted = new ArrayList<>(beginDates.entrySet());
            Collections.sort(beginSorted, new Comparator<Map.Entry<LocalDate, Integer>>() {
                @Override
                public int compare(Map.Entry<LocalDate, Integer> o1, Map.Entry<LocalDate, Integer> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });
            List<Map.Entry<LocalDate, Integer>> endSorted = new ArrayList<>(endDates.entrySet());
            Collections.sort(endSorted, new Comparator<Map.Entry<LocalDate, Integer>>() {
                @Override
                public int compare(Map.Entry<LocalDate, Integer> o1, Map.Entry<LocalDate, Integer> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });

            beginDate = beginSorted.get(0).getKey();
            endDate = endSorted.get(0).getKey();
        } else {
            beginDate = classPeriods[0].getTermBeginDate();
            endDate = classPeriods[0].getTermEndDate();
        }

        if(beginDate != null) {
            termDates.setText("(" + beginDate.getMonthOfYear() + "/" + beginDate.getDayOfMonth() + " - " + endDate.getMonthOfYear() + "/" + endDate.getDayOfMonth() + ")");
        } else {
            termDates.setText("");
        }

        return true;
    }

    public void setPeriodListView(RecyclerView view) {
        periodList = view;
        periodList.setOverScrollMode(View.OVER_SCROLL_NEVER);

        ((PeriodListAdapter) periodList.getAdapter()).setSecretGestureListener(secretGestureListener);
    }

    public void openGradeRefreshDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final View dialog = getLayoutInflater().inflate(R.layout.term_selection_dialog, null);
        builder.setView(dialog);
        final Dialog dialogItem = builder.create();

        final ClassPeriod.GradeTerm[] terms = {ClassPeriod.GradeTerm.T1, ClassPeriod.GradeTerm.T2, ClassPeriod.GradeTerm.T3, ClassPeriod.GradeTerm.T4, ClassPeriod.GradeTerm.S1, ClassPeriod.GradeTerm.S2};
        final ImageView[] refreshTermImageViews = {dialog.findViewById(R.id.refreshButtonTerm1), dialog.findViewById(R.id.refreshButtonTerm2), dialog.findViewById(R.id.refreshButtonTerm3), dialog.findViewById(R.id.refreshButtonTerm4), dialog.findViewById(R.id.refreshButtonSemester1), dialog.findViewById(R.id.refreshButtonSemester2)};

        ImageView refreshAll = dialog.findViewById(R.id.allRefreshButton);
        refreshAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateViews(getGradesManager().newestTerm);
                for(ImageView refreshTermImage : refreshTermImageViews) {
                    if(refreshTermImage.getVisibility() == View.VISIBLE) {
                        refreshTermImage.animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500);
                    }
                }
                v.animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        dialogItem.dismiss();
                        getGradesManager().updateGrades(PeriodListActivity.this, GradesManager.UpdateReason.MANUAL, getGradesManager().getRecommendedUpdateTerms());
                    }
                });
            }
        });

        int ind = 0;
        for(ImageView refreshView : refreshTermImageViews) {
            if(ind >= 4) {
                break;
            }
            final int finalInd = ind;
            refreshView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateViews(terms[finalInd]);
                    v.animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            dialogItem.dismiss();
                            getGradesManager().updateGrades(PeriodListActivity.this, GradesManager.UpdateReason.MANUAL, terms[finalInd]);
                        }
                    });
                }
            });
            ind++;
        }

        ImageView refreshSemester1 = refreshTermImageViews[4];
        refreshSemester1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateViews(ClassPeriod.GradeTerm.S1);
                refreshTermImageViews[0].animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500);
                refreshTermImageViews[1].animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500);
                v.animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        dialogItem.dismiss();

                        ClassPeriod term = getGradesManager().getClassTermInstance(ClassPeriod.GradeTerm.T1);
                        ClassPeriod.GradeTerm[] terms;
                        if(term != null && term.shouldUpdate()) {
                            terms = new ClassPeriod.GradeTerm[] {ClassPeriod.GradeTerm.T1, ClassPeriod.GradeTerm.T2, ClassPeriod.GradeTerm.S1};
                        } else {
                            terms = new ClassPeriod.GradeTerm[] {ClassPeriod.GradeTerm.T2, ClassPeriod.GradeTerm.S1};
                        }
                        getGradesManager().updateGrades(PeriodListActivity.this, GradesManager.UpdateReason.MANUAL, terms);
                    }
                });
            }
        });
        ImageView refreshSemester2 = refreshTermImageViews[5];
        refreshSemester2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateViews(ClassPeriod.GradeTerm.S2);
                refreshTermImageViews[2].animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500);
                refreshTermImageViews[3].animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500);
                v.animate().rotationBy(-360).setInterpolator(new DecelerateInterpolator()).setDuration(500).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        dialogItem.dismiss();

                        ClassPeriod term = getGradesManager().getClassTermInstance(ClassPeriod.GradeTerm.T3);
                        ClassPeriod.GradeTerm[] terms;
                        if(term != null && term.shouldUpdate()) {
                            terms = new ClassPeriod.GradeTerm[] {ClassPeriod.GradeTerm.T3, ClassPeriod.GradeTerm.T4, ClassPeriod.GradeTerm.S2};
                        } else {
                            terms = new ClassPeriod.GradeTerm[] {ClassPeriod.GradeTerm.T4, ClassPeriod.GradeTerm.S2};
                        }
                        getGradesManager().updateGrades(PeriodListActivity.this, GradesManager.UpdateReason.MANUAL, terms);
                    }
                });
            }
        });

        if(!getGradesManager().getCurrentViewYear().equals(GradesManager.getSchoolYear()) || !isLoggedIn || getGradesManager().areGradesUpdating()) {
            refreshAll.setVisibility(View.INVISIBLE);
            refreshSemester1.setVisibility(View.INVISIBLE);
            refreshSemester2.setVisibility(View.INVISIBLE);

            for(ImageView refreshView : refreshTermImageViews) {
                refreshView.setVisibility(View.INVISIBLE);
            }
        }

        TextView[] dialogTermTextViews = new TextView[] {dialog.findViewById(R.id.term1), dialog.findViewById(R.id.term2), dialog.findViewById(R.id.term3), dialog.findViewById(R.id.term4), dialog.findViewById(R.id.semester1), dialog.findViewById(R.id.semester2)};
        RelativeLayout[] backgroundBoxTerms = new RelativeLayout[] {dialog.findViewById(R.id.dialogTerm1), dialog.findViewById(R.id.dialogTerm2), dialog.findViewById(R.id.dialogTerm3), dialog.findViewById(R.id.dialogTerm4), dialog.findViewById(R.id.dialogSemester1), dialog.findViewById(R.id.dialogSemester2)};
        dialogTermPercentages = new TextView[] {dialog.findViewById(R.id.term1Percentage), dialog.findViewById(R.id.term2Percentage), dialog.findViewById(R.id.term3Percentage), dialog.findViewById(R.id.term4Percentage), dialog.findViewById(R.id.semester1Percentage), dialog.findViewById(R.id.semester2Percentage)};
        originalTextViewColor = dialogTermTextViews[0].getTextColors();
        ind = 0;
        for(TextView termTextView : dialogTermTextViews) {
            int lastUpdate = getGradesManager().getLastUpdateMinutesAgo(terms[ind]);
            String name = terms[ind].NAME.substring(0, 1) + terms[ind].NAME.toLowerCase().substring(1, terms[ind].NAME.length());

            if(lastUpdate == -1 || !getGradesManager().getCurrentViewYear().equals(GradesManager.getSchoolYear())) {
                termTextView.setText(Html.fromHtml("<b>" + name + "</b>"));

                if (getGradesManager().getCurrentViewYear().equals(GradesManager.getSchoolYear()) && getGradesManager().isTermUpdating(terms[ind])) {
                    dialogTermPercentages[ind].setText(getGradesManager().getTermUpdatePercentage(terms[ind]) + "%");
                } else {
                    dialogTermPercentages[ind].setText("");
                }
            } else {
                if (getGradesManager().getCurrentViewYear().equals(GradesManager.getSchoolYear()) && getGradesManager().isTermUpdating(terms[ind])) {
                    termTextView.setText(Html.fromHtml("<b>" + name + "</b>"));
                    dialogTermPercentages[ind].setText(getGradesManager().getTermUpdatePercentage(terms[ind]) + "%");
                    refreshTermImageViews[ind].setVisibility(View.INVISIBLE);
                } else {
                    termTextView.setText(Html.fromHtml("<b>" + name + "</b>" + ((lastUpdate < 240) ? " (updated " + lastUpdate + " min. ago)" : "")));
                    dialogTermPercentages[ind].setText("");
                }
            }
            final int finalInd1 = ind;
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(updateViews(terms[finalInd1])) {
                        dialogItem.dismiss();
                    }
                }
            };
            backgroundBoxTerms[ind].setOnClickListener(clickListener);
            termTextView.setOnClickListener(clickListener);
            if(!getGradesManager().doesGradeTermExist(terms[ind])) {
                termTextView.setTextColor(Color.GRAY);
            }
            ClassPeriod termInstance = getGradesManager().getClassTermInstance(terms[ind]);
            if(termInstance != null && !termInstance.shouldUpdate()) {
                refreshTermImageViews[ind].setVisibility(View.INVISIBLE);
            }

            ind++;
        }

        final Spinner dropDownYear = dialog.findViewById(R.id.dropDownSchoolYearSelection);

        List<String> dropDownList = new ArrayList<>(getGradesManager().allClassPeriods.keySet());

        Collections.sort(dropDownList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        int currentPos = 0, count = 0;
        for(String year : dropDownList) {
            if (year.equals(getGradesManager().getCurrentViewYear())) {
                currentPos = count;
            }
            if(year.equals(GradesManager.getSchoolYear())) {
                dropDownList.set(count, year + " (current)");
            }
            count++;
        }

        dropDownYear.setAdapter(new ArrayAdapter<>(this, R.layout.school_year_spinner_item, dropDownList));
        dropDownYear.setSelection(currentPos, false);
        dropDownYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getGradesManager().loadSchoolYear(dropDownYear.getAdapter().getItem(position).toString(), PeriodListActivity.this);
                dialogItem.dismiss();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        dialogItem.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogTermPercentages = null;
                lastOpenedDialog = null;
            }
        });

        lastOpenedDialog = dialogItem;
        dialogItem.show();
    }

    public void updateDialogTermTextViews() {
        if(dialogTermPercentages == null) {
            return;
        }

        final ClassPeriod.GradeTerm[] terms = {ClassPeriod.GradeTerm.T1, ClassPeriod.GradeTerm.T2, ClassPeriod.GradeTerm.T3, ClassPeriod.GradeTerm.T4, ClassPeriod.GradeTerm.S1, ClassPeriod.GradeTerm.S2};

        int ind = 0;
        for(TextView termTextPercentage : dialogTermPercentages) {
            int lastUpdate = getGradesManager().getLastUpdateMinutesAgo(terms[ind]);

            if(lastUpdate == -1 || !getGradesManager().getCurrentViewYear().equals(GradesManager.getSchoolYear())) {
                if (getGradesManager().getCurrentViewYear().equals(GradesManager.getSchoolYear()) && getGradesManager().isTermUpdating(terms[ind])) {
                    termTextPercentage.setText(getGradesManager().getTermUpdatePercentage(terms[ind]) + "%");
                } else {
                    termTextPercentage.setText("");
                }
            } else {
                if (getGradesManager().isTermUpdating(terms[ind])) {
                    termTextPercentage.setText(getGradesManager().getTermUpdatePercentage(terms[ind]) + "%");
                }
            }

            ind++;
        }
    }

    public void backToLogin(View view) {
        Intent returnToLogin = new Intent(this, LoginActivity.class);
        startActivity(returnToLogin);
    }

    @Override
    public void onBackPressed() {
        if(lastFragment instanceof TermGraphDetailsFragment) {
            super.onBackPressed();
            lastFragment = null;

            updateViews(currentViewTerm);
        }
    }

    private GradesManager getGradesManager() {
        return ((StandardScoreApplication) getApplication()).getGradesManager();
    }
}
