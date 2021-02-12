package me.tazadejava.gradeupdates;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import me.tazadejava.classdetails.ClassDetailsActivity;
import me.tazadejava.classdetails.GradeCellItem;
import me.tazadejava.classdetails.GradedItem;
import me.tazadejava.classdetails.HeaderItem;
import me.tazadejava.classdetails.PercentageDate;
import me.tazadejava.intro.LoginActivity;
import me.tazadejava.intro.StandardScoreApplication;
import me.tazadejava.mainscreen.PeriodItem;
import me.tazadejava.mainscreen.PeriodListActivity;
import me.tazadejava.settings.SettingsActivity;
import me.tazadejava.standardscore.R;

/**
 * Created by Darren on 6/14/2017.
 */
public class GradesManager {

    public enum UpdateReason {
        JOB_SCHEDULER, IN_APP_TIME_ELAPSED, MANUAL
    }

    public static final String NONEXISTENT_TERMS_PLACEHOLDER = "NONEXISTENT_TERMS_PLACEHOLDER";

    public HashMap<String, List<ClassPeriod>> allClassPeriods = new HashMap<>();

    String lastDialogID;
    private String currentViewYear;

    private Context lastContext;
    private int retryTimeout = -1;

    HashMap<ClassPeriod.GradeTerm, Set<ClassPeriod>> updatedClassPeriods = new HashMap<>();
    private HashMap<ClassPeriod.GradeTerm, Set<ClassPeriod>> outdatedClassPeriods = new HashMap<>();
    public ClassPeriod.GradeTerm currentUpdateTerm;

    private boolean isGradeSearchComplete, areGradesUpdating;

    private ValueCallback<String> valueCallback;
    private String filterGrades, scrapeGrades;

    public ClassPeriod.GradeTerm newestTerm;
    private List<ClassPeriod.GradeTerm> lastUpdatedTerms;
    private UpdateReason lastUpdateReason;

    private int currentRow, currentColumn;
    private int iteratedRows, waitingForNewPageTimeout;
    private long lastUpdateTime;

    private WebView web, newWeb;

    public LinkedHashMap<DateTime, List<UpdateHistoryPoint>> updateHistory = new LinkedHashMap<>();

    private DateTime currentUpdateNow;

    private HashMap<String, HashMap<ClassPeriod.GradeTerm, HashMap<String, PercentageDate>>> averageTermGrades;
    private AverageTermGradeHistoryManager averageTermGradeManager;

    public GradesManager(Context context) {
        lastContext = context;
        averageTermGradeManager = new AverageTermGradeHistoryManager();

        loadJavascriptFiles(context);
        loadData(context);

        if (LoginActivity.isUserLoggedIn()) {
            calculateNewestTerm();

            //automatically update after a given amount of time
            if (!isSummer() && !(context instanceof UpdatingService)) {
                if (getCurrentClassPeriods().isEmpty() || isTimeToUpdate(context) || getLastUpdateMinutesAgo(newestTerm) == -1) {
                    Toast.makeText(context, getLastUpdateText(newestTerm), Toast.LENGTH_SHORT).show();
                    if (newestTerm == null) {
                        updateGrades(context, UpdateReason.IN_APP_TIME_ELAPSED);
                    } else {
                        updateGrades(context, UpdateReason.IN_APP_TIME_ELAPSED, getRecommendedUpdateTerms());
                    }
                }
            }
        }
    }

    private void loadJavascriptFiles(Context context) {
        StringBuilder filterGrades = new StringBuilder();
        StringBuilder scrapeGrades = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("filterGrades.js"), StandardCharsets.UTF_8));

            String read = "";
            while((read = reader.readLine()) != null) {
                filterGrades.append(read);
            }

            reader.close();

            reader = new BufferedReader(new InputStreamReader(context.getAssets().open("scrapeGrades.js"), StandardCharsets.UTF_8));

            while((read = reader.readLine()) != null) {
                scrapeGrades.append(read);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        this.filterGrades = filterGrades.toString();
        this.scrapeGrades = scrapeGrades.toString();
    }

    private void loadData(Context context) {
        currentViewYear = getSchoolYear();

        String saveFilePath = context.getFilesDir().getAbsolutePath() + "/" + LoginActivity.currentUUID;

        if(context instanceof PeriodListActivity) {
            File yearFolders = new File(saveFilePath + "/grades/");
            if(yearFolders.exists()) {
                for (String schoolYearFolderString : yearFolders.list()) {
                    File schoolYearFolder = new File(saveFilePath + "/grades/" + schoolYearFolderString);
                    if (schoolYearFolder.isDirectory()) {
                        allClassPeriods.put(schoolYearFolderString, new ArrayList<ClassPeriod>());
                    }
                }
            }
        }

        File historyDataFile = new File(saveFilePath + "/history.txt");
        try {
            if(!historyDataFile.exists()) {
                historyDataFile.createNewFile();
            }

            BufferedReader reader = new BufferedReader(new FileReader(historyDataFile));

            String read;
            while((read = reader.readLine()) != null) {
                String[] split = read.split("~");

                DateTime time = DateTime.parse(split[0]);

                List<UpdateHistoryPoint> points = new ArrayList<>();
                for(int i = 1; i < split.length; i++) {
                    points.add(new UpdateHistoryPoint(split[i]));
                }

                updateHistory.put(time, points);
            }

            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadYearData(getSchoolYear(), saveFilePath);
    }

    private void loadYearData(String year, String saveFilePath) {
        if(allClassPeriods.containsKey(year)) {
            allClassPeriods.get(year).clear();
        } else {
            allClassPeriods.put(year, new ArrayList<ClassPeriod>());
        }

        try {
            BufferedReader reader;
            String read;

            File currentSchoolYearFolder = new File(saveFilePath + "/grades/" + year);
            if(!currentSchoolYearFolder.exists()) {
                currentSchoolYearFolder.mkdirs();
            }

            for(File file : getTermFiles(currentSchoolYearFolder.getAbsolutePath())) {
                if(file.exists()) {
                    reader = new BufferedReader(new FileReader(file));

                    while((read = reader.readLine()) != null) {
                        if(read.startsWith("AVG_DATA: ")) {
                            averageTermGradeManager.loadTerm(year, file.getName(), read);
                        } else {
                            ClassPeriod term = new ClassPeriod(read);
                            allClassPeriods.get(year).add(term);
                        }
                    }

                    reader.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveData(final Context context) {
        final Handler handler = new Handler(Looper.getMainLooper());

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String saveFilePath = context.getFilesDir().getAbsolutePath() + "/" + LoginActivity.currentUUID;
                File dataFolder = new File(saveFilePath + "/grades/" + getSchoolYear());
                if(!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                File[] termFiles = new File[6];
                ClassPeriod.GradeTerm[] terms = {ClassPeriod.GradeTerm.S1, ClassPeriod.GradeTerm.S2, ClassPeriod.GradeTerm.T1, ClassPeriod.GradeTerm.T2, ClassPeriod.GradeTerm.T3, ClassPeriod.GradeTerm.T4};

                try {
                    for(int s = 0; s < 2; s++) {
                        termFiles[s] = new File(dataFolder.getAbsolutePath() + "/semester" + (s + 1) + ".txt");
                        if(!termFiles[s].exists()) {
                            termFiles[s].createNewFile();
                        }
                    }

                    for(int t = 1; t <= 4; t++) {
                        termFiles[t + 1] = new File(dataFolder.getAbsolutePath() + "/term" + t + ".txt");
                        if(!termFiles[t + 1].exists()) {
                            termFiles[t + 1].createNewFile();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                UpdatingService.logMessage("Saving the update of " + getNewestClassPeriods().size() + " class terms!", lastContext);
                FileWriter writer;
                List<ClassPeriod.GradeTerm> termsList = Arrays.asList(ClassPeriod.GradeTerm.values());
                for(int i = 0; i < 6; i++) {
                    try {
                        File tempFile = new File(termFiles[i].getAbsolutePath().replace(".txt", "_temp.txt"));
                        writer = new FileWriter(tempFile);
                        for (ClassPeriod term : getNewestClassPeriods()) {
                            if (termsList.indexOf(term.getTerm()) == i) {
                                writer.append(term.save() + "\n");
                            }
                        }
                        averageTermGradeManager.saveData(terms[i], writer);
                        writer.close();

                        boolean overwritten = tempFile.renameTo(termFiles[i]);

                        if(!overwritten) {
                            UpdatingService.logMessage("The temp file overwriting process failed: " + termFiles[i].getName(), lastContext);
                        }
                    } catch(IOException ex) {
                        ex.printStackTrace();
                    }
                }

                try {
                    File historyDataTempFile = new File(saveFilePath + "/history_temp.txt");
                    File historyDataFile = new File(saveFilePath + "/history.txt");
                    writer = new FileWriter(historyDataTempFile, false);

                    for(Map.Entry<DateTime, List<UpdateHistoryPoint>> entry : updateHistory.entrySet()) {
                        if(entry.getKey().plusMonths(3).isBeforeNow()) {
                            continue;
                        }

                        StringBuilder historyPoints = new StringBuilder();
                        if(!entry.getValue().isEmpty()) {
                            for (UpdateHistoryPoint point : entry.getValue()) {
                                historyPoints.append(point.save() + "~");
                            }

                            historyPoints.deleteCharAt(historyPoints.length() - 1);
                        }

                        writer.append(entry.getKey().toString() + "~" + historyPoints.toString());
                        writer.append("\n");
                    }

                    writer.close();

                    boolean overwritten = historyDataTempFile.renameTo(historyDataFile);

                    if(!overwritten) {
                        UpdatingService.logMessage("The history temp file overwriting process failed", lastContext);
                    }
                } catch(IOException ex) {
                    ex.printStackTrace();
                }

                if(context instanceof UpdatingService) {
                    File stateIndicator = new File(saveFilePath + "/state.indicator");
                    if (!stateIndicator.exists()) {
                        try {
                            stateIndicator.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            clearReferences();
                        }
                    }, 3000);
                }
            }
        });
    }

    public boolean areGradesUpdating() {
        return areGradesUpdating;
    }

    public void updateGrades(final Context context, final UpdateReason updateReason, final ClassPeriod.GradeTerm... termsToUpdateInput) {
        if(areGradesUpdating) {
            UpdatingService.logMessage("GRADES ARE ALREADY UPDATING", context);
            return;
        }
        if(!isNetworkAvailable(context)) {
            abortUpdate();
            return;
        }
        if(getCurrentClassPeriods() == null) {
            abortUpdate();
            return;
        }

        lastContext = context;
        areGradesUpdating = true;
        lastUpdateReason = updateReason;
        lastUpdateTime = System.currentTimeMillis();
        currentUpdateNow = DateTime.now();

        final LinkedList<ClassPeriod.GradeTerm> termsToUpdateQueue = new LinkedList<>();

        if(termsToUpdateInput.length == 0) {
            termsToUpdateQueue.addAll(Arrays.asList(ClassPeriod.GradeTerm.values()));
        } else {
            termsToUpdateQueue.addAll(Arrays.asList(termsToUpdateInput));
        }
        Collections.sort(termsToUpdateQueue, new Comparator<ClassPeriod.GradeTerm>() {
            @Override
            public int compare(ClassPeriod.GradeTerm o1, ClassPeriod.GradeTerm o2) {
                return o2.compareTo(o1);
            }
        });
        lastUpdatedTerms = new ArrayList<>(termsToUpdateQueue);

        outdatedClassPeriods.clear();
        updatedClassPeriods.clear();

        if(!getCurrentClassPeriods().isEmpty()) {
            for(ClassPeriod period : getCurrentClassPeriods()) {
                if(termsToUpdateQueue.contains(period.getTerm())) {
                    if(!outdatedClassPeriods.containsKey(period.getTerm())) {
                        outdatedClassPeriods.put(period.getTerm(), new LinkedHashSet<ClassPeriod>());
                    }

                    outdatedClassPeriods.get(period.getTerm()).add(period);
                } else {
                    if(!updatedClassPeriods.containsKey(period.getTerm())) {
                        updatedClassPeriods.put(period.getTerm(), new LinkedHashSet<ClassPeriod>());
                    }

                    updatedClassPeriods.get(period.getTerm()).add(period);
                }
            }
        }

        for(ClassPeriod.GradeTerm term : termsToUpdateQueue) {
            if(!updatedClassPeriods.containsKey(term)) {
                updatedClassPeriods.put(term, new LinkedHashSet<ClassPeriod>());
            }
        }

        iteratedRows = 0;

        if(context instanceof PeriodListActivity) {
            ((PeriodListActivity) context).mainText.setText("Grades are being obtained...");
            ((PeriodListActivity) context).lastUpdateText.setText("");

            if(((PeriodListActivity) context).getMenu() != null) {
                ((PeriodListActivity) context).getMenu().getItem(2).setEnabled(false);
            }

            if(outdatedClassPeriods.isEmpty()) {
                ((PeriodListActivity) context).firstTimeHelperText.setVisibility(View.VISIBLE);
            }
        }

        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        final int type;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        web = new WebView(context);
        UpdatingService.logMessage("Created first webview to load page with.", lastContext);


        if(Settings.canDrawOverlays(context)) {
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 0;
            params.width = 0;
            params.height = 0;

            wm.addView(web, params);
        }

        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        WebSettings settings = web.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        final ScraperInterface scrapeInterface = new ScraperInterface(GradesManager.this);

        web.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onCreateWindow(WebView view, final boolean isDialog, boolean isUserGesture, Message resultMsg) {
                newWeb = new WebView(context);
                if(Settings.canDrawOverlays(context)) {
                    final WindowManager.LayoutParams params = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
                    params.gravity = Gravity.TOP | Gravity.START;
                    params.x = 0;
                    params.y = 0;
                    params.width = 0;
                    params.height = 0;

                    wm.addView(newWeb, params);
                }

                newWeb.getSettings().setJavaScriptEnabled(true);
                newWeb.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                newWeb.addJavascriptInterface(scrapeInterface, "scrape");
                newWeb.setWebChromeClient(new WebChromeClient());

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWeb);
                resultMsg.sendToTarget();

                newWeb.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageFinished(final WebView view, String url) {
                        super.onPageFinished(view, url);

                        if(view.getUrl() == null || view.getUrl().equalsIgnoreCase("about:blank")) {
                            return;
                        }

                        UpdatingService.logMessage("NEW URL PAGE FINISHED " + view.getUrl() + " AND " + isNetworkAvailable(context), context);
                        if(!isNetworkAvailable(context)) {
                            abortUpdate();
                            return;
                        }
                        if(view.getUrl().equals("https://www2.saas.wa-k12.net/scripts/cgiip.exe/WService=wlkwashs71/skyportexpired.w")) {
                            if (updateReason == UpdateReason.JOB_SCHEDULER) {
                                LoginActivity.deletePassword(lastContext);
                                sendMessageNotification("The background update failed", "Your password has expired and needs to be updated", context);
                            } else {
                                ActivityManager.RunningAppProcessInfo process = new ActivityManager.RunningAppProcessInfo();
                                ActivityManager.getMyMemoryState(process);
                                if(process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                                    Intent intent = new Intent(context, LoginActivity.class);
                                    intent.putExtra("changePassword", true);
                                    context.startActivity(intent);
                                }
                            }

                            abortUpdate();
                            return;
                        }

                        if(view.getUrl().contains("saas.wa-k12.net")) {
                            if(view.getUrl().contains("sfhome01.w")) {
                                waitingForNewPageTimeout = 2;
                                view.evaluateJavascript("javascript:(function(){" +
                                                "if(document.getElementsByClassName('sf_navMenuItem')[2] == null) {" +
                                                "return false; } else {" +
                                                "return true; }" +
                                                "})()"
                                        , new ValueCallback<String>() {
                                            @Override
                                            public void onReceiveValue(String value) {
                                                if (value.equals("true")) {
                                                    view.loadUrl("javascript:(function(){" +
                                                            "var l=document.getElementsByClassName('sf_navMenuItem');" +
                                                            "l[2].click();" +
                                                            "})()");
                                                }
                                            }
                                        });

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (waitingForNewPageTimeout == 2) {
                                            abortUpdate("Grades unavailable! Skyward down? (2)", context, updateReason, termsToUpdateInput);
                                        }
                                    }
                                }, 20000L);
                            } else if(view.getUrl().contains("sfgradebook001.w")) {
                                waitingForNewPageTimeout = 3;
                                currentRow = 0;
                                currentUpdateTerm = termsToUpdateQueue.poll();
                                currentColumn = currentUpdateTerm.COLUMN;
                                isGradeSearchComplete = false;

                                if(context instanceof PeriodListActivity) {
                                    ((PeriodListActivity) context).mainText.setText("Updating " + currentUpdateTerm.NAME.toLowerCase() + "...");
                                }

                                recursiveAccessGrade(view, termsToUpdateQueue);
                            }
                        }
                    }
                });

                return true;
            }
        });
        web.addJavascriptInterface(scrapeInterface, "scrape");
        waitingForNewPageTimeout = 0;
        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(final WebView view, String url) {
                super.onPageFinished(view, url);

                if(view.getUrl().equalsIgnoreCase("about:blank")) {
                    return;
                }

                UpdatingService.logMessage("PAGE FINISHED " + view.getUrl() + " AND " + isNetworkAvailable(context), context);
                if(!isNetworkAvailable(context)) {
                    abortUpdate();
                    return;
                }

                if(view.getUrl().contains("saas.wa-k12.net")) {
                    if(view.getUrl().contains("fwemnu01.w")) {

                        waitingForNewPageTimeout = 1;
                        view.evaluateJavascript("javascript:(function(){" +
                                        "editInputs = document.getElementsByClassName('EditInput');" +
                                        "editInputs[0].value = '" + LoginActivity.getUsername() + "';" +
                                        "editInputs[1].value = '" + LoginActivity.getPassword() + "';" +
                                        "document.getElementById('bLogin').click();" +
                                        "})()"
                                , null);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(waitingForNewPageTimeout == 1) {
                                    if(!isNetworkAvailable(context)) {
                                        abortUpdate();
                                    }

                                    abortUpdate("Grades unavailable! Skyward down? (1)", context, updateReason, termsToUpdateInput);
                                }
                            }
                        }, 15000L);
                    }
                }
            }
        });

        web.loadUrl("https://www2.saas.wa-k12.net/scripts/cgiip.exe/WService=wlkwashs71/fwemnu01.w");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(waitingForNewPageTimeout == 0) {
                    if(!isNetworkAvailable(context)) {
                        abortUpdate();
                    }

                    abortUpdate("Grades unavailable! Skyward down? (0)", context, updateReason, termsToUpdateInput);
                }
            }
        }, 10000L);
    }

    public void abortUpdate() {
        abortUpdate("No network. Try again later!", null, null, null);
    }

    private void abortUpdate(String msg, final Context context, final UpdateReason updateReason, final ClassPeriod.GradeTerm[] updateTermsArray) {
        if(areGradesUpdating) {
            UpdatingService.logMessage("Update was aborted. " + msg + " + " + (context == null) + " " + retryTimeout, context);
            areGradesUpdating = false;

            deleteWebviewData();

            if(context != null && retryTimeout == -1) {
                retryTimeout++;
                UpdatingService.logMessage("Reattempting to update! " + msg, lastContext);
                updateGrades(context, updateReason, updateTermsArray);
            } else {
                if(updateReason == UpdateReason.JOB_SCHEDULER) {
                    clearReferences();
                }

                retryTimeout = -1;

                if(PeriodListActivity.isRunning && context instanceof PeriodListActivity) {
                    PeriodListActivity act = (PeriodListActivity) context;
                    act.mainText.setText("");
                    act.lastUpdateText.setText(msg);

                    if (act.dialogTermPercentages != null) {
                        act.lastOpenedDialog.dismiss();
                        act.openGradeRefreshDialog(null);
                    }
                }
            }
        }
    }

    private void deleteWebviewData() {
        WindowManager wm = (WindowManager) lastContext.getSystemService(Context.WINDOW_SERVICE);

        if(web != null) {
            web.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {

                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    web.destroy();
                    web = null;
                }
            });

            if(Settings.canDrawOverlays(lastContext)) {
                wm.removeView(web);
            }
            web.clearCache(true);
            web.loadUrl("about:blank");
            web.stopLoading();
        }

        if(newWeb != null) {
            newWeb.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {

                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    newWeb.destroy();
                    newWeb = null;
                }
            });

            if(Settings.canDrawOverlays(lastContext)) {
                wm.removeView(newWeb);
            }
            newWeb.clearCache(true);
            newWeb.loadUrl("about:blank");
            newWeb.stopLoading();
        }

        if(lastContext instanceof PeriodListActivity) {
            ((PeriodListActivity) lastContext).getMenu().getItem(2).setEnabled(true);
        }
    }

    private void updateRowColumns(Queue<ClassPeriod.GradeTerm> updateTerms) {
        currentRow++;
        iteratedRows++;

        if (currentRow > 13) {
            //new column, means the term should be compared with the last if the context is not the service
            if(lastContext instanceof PeriodListActivity) {
                //if it is a first time update, do not send individual notifications
                if(!outdatedClassPeriods.isEmpty() || lastUpdatedTerms.size() != 6) {
                    compareUpdatedVersionWithOld(currentUpdateTerm);
                }

                allClassPeriods.put(currentViewYear, getFormattedUpdatingClassPeriods());

                PeriodListActivity context = (PeriodListActivity) lastContext;
                if(context.currentViewTerm == currentUpdateTerm) {
                    context.updateViews(currentUpdateTerm);
                }

                if(!PeriodListActivity.isRunning && context.currentViewTerm == currentUpdateTerm) {
                    ActivityManager.RunningAppProcessInfo process = new ActivityManager.RunningAppProcessInfo();
                    ActivityManager.getMyMemoryState(process);
                    if(process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        if(((StandardScoreApplication) context.getApplication()).getCurrentActivity() instanceof ClassDetailsActivity) {
                            ClassDetailsActivity act = (ClassDetailsActivity) ((StandardScoreApplication) context.getApplication()).getCurrentActivity();

                            Intent intent = new Intent(context, ClassDetailsActivity.class);
                            intent.putExtras(act.getIntent());
                            if(!act.shouldKickOnUpdate()) {
                                intent.putExtras(act.getSaveState());
                            }
                            act.finish();
                            act.startActivity(intent);
                        }
                    }
                }
            }

            if(updateTerms.isEmpty()) {
                isGradeSearchComplete = true;
            } else {
                currentRow = 0;
                currentUpdateTerm = updateTerms.poll();
                currentColumn = currentUpdateTerm.COLUMN;

                if(lastContext instanceof PeriodListActivity) {
                    ((PeriodListActivity) lastContext).mainText.setText("Updating " + currentUpdateTerm.NAME.toLowerCase() + "...");
                }
            }
        }
    }

    private void recursiveAccessGrade(final WebView view, final Queue<ClassPeriod.GradeTerm> updateTerms) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isNetworkAvailable(view.getContext())) {
                    abortUpdate();
                    return;
                }

                //NOTES ON FILTERGRADES.JS:
                //this is inefficient but will be revised in a future update
                //first, find all the rows that exist
                //then, iterate on that specific row
                view.evaluateJavascript(filterGrades.replace("SEARCH_ROW", String.valueOf(currentRow)).replace("SEARCH_COLUMN", String.valueOf(currentColumn)),
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                //first clause is if row was not found; second clause is if we iterated all possible rows
                                if(!value.equals("-1") && !(currentRow != 0 && value.equals("0"))) {
                                    UpdatingService.logMessage("Scraping grades out of row " + currentRow + " column " + currentColumn, lastContext);
                                    scrapeGradeData(view, scrapeGrades, updateTerms);

                                    //update percentage if in current view
                                    if(PeriodListActivity.isRunning && view.getContext() instanceof PeriodListActivity && (isTermUpdating(((PeriodListActivity) view.getContext()).currentViewTerm) || getLastUpdateMinutesAgo(newestTerm) == -1)) {
                                        ((PeriodListActivity) view.getContext()).lastUpdateText.setText(getUpdatePercentage() + "%");

                                        if(lastContext instanceof PeriodListActivity) {
                                            ((PeriodListActivity) lastContext).updateDialogTermTextViews();
                                        }
                                    }
                                } else {
                                    UpdatingService.logMessage("Search did not find clickable term row/col (" + currentRow + ", " + currentColumn + "); grade search complete? " + isGradeSearchComplete, lastContext);
                                    updateRowColumns(updateTerms);

                                    if(isGradeSearchComplete) {
                                        completeGradeSearch();
                                    } else {
                                        //update percentage if in current view
                                        if(PeriodListActivity.isRunning && view.getContext() instanceof PeriodListActivity && (isTermUpdating(((PeriodListActivity) view.getContext()).currentViewTerm) || getLastUpdateMinutesAgo(newestTerm) == -1)) {
                                            ((PeriodListActivity) view.getContext()).lastUpdateText.setText(getUpdatePercentage() + "%");

                                            if(lastContext instanceof PeriodListActivity) {
                                                ((PeriodListActivity) lastContext).updateDialogTermTextViews();
                                            }
                                        }

                                        recursiveAccessGrade(view, updateTerms);
                                    }
                                }
                            }
                        });
            }
        }, 100);
    }

    private void scrapeGradeData(final WebView view, final String scrapeGrades, final Queue<ClassPeriod.GradeTerm> updateTerms) {
        final Handler handler = new Handler();
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                if(!isNetworkAvailable(view.getContext())) {
                    abortUpdate();
                    return;
                }

                //scrape the data on the class
                view.evaluateJavascript("javascript:(function(){" +
                        "if(document.getElementsByClassName('shrinkMe').length == 0) {" +
                        "return 0;" +
                        "}" +
                        "var table=document.getElementsByClassName('shrinkMe')[0];" +
                        "if('" + lastDialogID + "' === (table.rows[0].childNodes[0].childNodes[0].childNodes[0].childNodes[0].childNodes[0].innerText + table.rows[0].childNodes[0].childNodes[2].childNodes[1].childNodes[0].rows[0].childNodes[0].childNodes[0].data + table.rows[0].childNodes[0].childNodes[2].childNodes[1].childNodes[0].rows[0].childNodes[0].childNodes[2].innerText)) {" +
                        "return -1;" +
                        "}" +
                        "return 1;" +
                        "})()", valueCallback);
            }
        };
        valueCallback = new ValueCallback<String>() {
            @Override
            public void onReceiveValue(final String value) {
                UpdatingService.logMessage("Received value " + value + " from valueCallback", lastContext);
                if(value.equals("0") || value.equals("-1")) {
                    handler.postDelayed(run, 500);
                } else {
//                    updateRowColumns(updateTerms);

                    final Runnable secondRun = new Runnable() {
                        @Override
                        public void run() {
                            view.evaluateJavascript(
                                    "javascript:(function(){" +
                                            "if(document.getElementById('showCommentInfo') != null) {" +
                                            "if(document.getElementById('dLog_showCommentInfo') != null && document.getElementById('dLog_showCommentInfo').style.display === 'block') {" +
                                            "return 1;" +
                                            "} else {" +
                                            "return 0;" +
                                            "}" +
                                            "} else {" +
                                            "return 1;" +
                                            "}" +
                                            "})()", valueCallback);
                        }
                    };
                    view.evaluateJavascript(
                            "javascript:(function(){" +
                                    "if(document.getElementById('showCommentInfo') != null) {" +
                                    "document.getElementById('showCommentInfo').click();" +
                                    "return 1;" +
                                    "}" +
                                    "return 0;" +
                                    "})()", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    if(value.equals("1")) {
                                        handler.postDelayed(secondRun, 750L);
                                    } else {
                                        secondRun.run();
                                    }
                                }
                            });

                    valueCallback = new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(final String value) {
                            if(value.equals("1")) {
                                view.evaluateJavascript(scrapeGrades, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(final String value) {
                                        updateRowColumns(updateTerms);

                                        if (isGradeSearchComplete) {
                                            completeGradeSearch();
                                        } else {
                                            final Runnable thirdRun = new Runnable() {
                                                @Override
                                                public void run() {
                                                    view.evaluateJavascript(
                                                            "javascript:(function(){" +
                                                                    "if(document.getElementById('showCommentInfo') != null && document.getElementById('dLog_showCommentInfo') != null && document.getElementById('dLog_showCommentInfo').style.display === 'block') {" +
                                                                    "return 0;" +
                                                                    "} else {" +
                                                                    "return 1;" +
                                                                    "}" +
                                                                    "return 1;" +
                                                                    "})()", valueCallback);
                                                }
                                            };
                                            view.evaluateJavascript(
                                                    "javascript:(function(){" +
                                                            "if(document.getElementById('showCommentInfo') != null && document.getElementById('dLog_showCommentInfo') != null && document.getElementById('dLog_showCommentInfo').style.display === 'block') {" +
                                                            "document.getElementById('dLog_showCommentInfo').childNodes[1].click();" +
                                                            "return 1;" +
                                                            "}" +
                                                            "return 0;" +
                                                            "})()", new ValueCallback<String>() {
                                                        @Override
                                                        public void onReceiveValue(String value) {
                                                            if(value.equals("1")) {
                                                                handler.postDelayed(thirdRun, 500);
                                                            } else {
                                                                thirdRun.run();
                                                            }
                                                        }
                                                    });

                                            valueCallback = new ValueCallback<String>() {
                                                @Override
                                                public void onReceiveValue(String value) {
                                                    if(value.equals("1")) {
                                                        recursiveAccessGrade(view, updateTerms);
                                                    } else {
                                                        handler.postDelayed(thirdRun, 500);
                                                    }
                                                }
                                            };
                                        }
                                    }
                                });
                            } else {
                                handler.postDelayed(secondRun, 500);
                            }
                        }
                    };
                }
            }
        };

        handler.postDelayed(run, 750L);
    }

    private List<ClassPeriod> getFormattedUpdatingClassPeriods() {
        List<ClassPeriod> fixed = new ArrayList<>();
        for(ClassPeriod.GradeTerm term : ClassPeriod.GradeTerm.values()) {
            if(!updatedClassPeriods.containsKey(term) || updatedClassPeriods.get(term).isEmpty()) {
                if(outdatedClassPeriods.containsKey(term)) {
                    fixed.addAll(outdatedClassPeriods.get(term));
                }
            } else {
                fixed.addAll(updatedClassPeriods.get(term));
            }
        }

        for(ClassPeriod term : fixed) {
            if(term.getTerm() == ClassPeriod.GradeTerm.S1 || term.getTerm() == ClassPeriod.GradeTerm.S2) {
                HeaderItem lastHeader = null;
                HeaderItem lastDeformedTermHeader = null;
                for(GradeCellItem item : term.getGradedItems()) {
                    if(item instanceof HeaderItem) {
                        if(item.getBoldedText().startsWith("T1 (") || item.getBoldedText().startsWith("T2 (") || item.getBoldedText().startsWith("T3 (") || item.getBoldedText().startsWith("T4 (")) {
                            if(lastHeader != null && lastHeader.getAdjustedSectionWeight() == 100) {
                                if(lastDeformedTermHeader == null) {
                                    lastDeformedTermHeader = (HeaderItem) item;
                                } else {
                                    lastHeader.setAdjustedSectionWeight(((HeaderItem) item).getAdjustedSectionWeight() + lastDeformedTermHeader.getAdjustedSectionWeight());
                                    lastHeader.setRegularSectionWeight(lastHeader.getAdjustedSectionWeight());
                                    double roundedPercentage = (item.getPercentageGrade() + lastDeformedTermHeader.getPercentageGrade()) / 2d;
                                    roundedPercentage = Math.round(roundedPercentage * 100d) / 100d;
                                    lastHeader.setPercentageGrade(roundedPercentage);
                                    double received = Double.parseDouble(item.getPointsReceived()) + Double.parseDouble(lastDeformedTermHeader.getPointsReceived());
                                    double total = Double.parseDouble(item.getPointsTotal()) + Double.parseDouble(lastDeformedTermHeader.getPointsTotal());
                                    lastHeader.setOutOf("" + (received % 1 == 0 ? ((int) received) : received), "" + (total % 1 == 0 ? ((int) total) : total));

                                    item.setBoldedText(item.getBoldedText().replace("(", ""));
                                    lastDeformedTermHeader.setBoldedText(lastDeformedTermHeader.getBoldedText().replace("(", ""));

                                    lastDeformedTermHeader = null;
                                }
                            }
                        } else {
                            lastHeader = (HeaderItem) item;
                        }
                    }
                }
            }
        }

        return fixed;
    }

    private void completeGradeSearch() {
        if(!areGradesUpdating) {
            return;
        }
        areGradesUpdating = false;

        UpdatingService.logMessage("Grade update is complete!", lastContext);
        UpdatingService.logMessage("Update took " + ((System.currentTimeMillis() - lastUpdateTime) / 1000d) + " seconds to complete!", lastContext);

        retryTimeout = -1;

        if(outdatedClassPeriods.isEmpty()) {
            if(lastUpdatedTerms.size() == 6) {
                if(!PeriodListActivity.isOnForeground) {
                    sendMessageNotification("Grades have been updated", "View grades", lastContext);
                }

                if (lastContext instanceof PeriodListActivity) {
                    ((PeriodListActivity) lastContext).firstTimeHelperText.setVisibility(View.GONE);
                }
            } else {
                StringBuilder updatedTerms = new StringBuilder();
                for (ClassPeriod.GradeTerm term : lastUpdatedTerms) {
                    updatedTerms.append(term.toString() + "-");
                }

                boolean updatedTermsExist = false;
                for(ClassPeriod.GradeTerm updatedTerm : lastUpdatedTerms) {
                    if(!updatedClassPeriods.get(updatedTerm).isEmpty()) {
                        updatedTermsExist = true;
                        break;
                    }
                }

                if(updatedTermsExist) {
                    if(lastContext instanceof UpdatingService) {
                        for(ClassPeriod.GradeTerm updatedTerm : lastUpdatedTerms) {
                            compareUpdatedVersionWithOld(updatedTerm);
                        }
                    }
                } else {
                    updateHistory.put(currentUpdateNow, new ArrayList<UpdateHistoryPoint>());
                    updateHistory.get(currentUpdateNow).add(new UpdateHistoryPoint(NONEXISTENT_TERMS_PLACEHOLDER, updatedTerms.substring(0, updatedTerms.length() - 1), false, "0", "0"));
                }
            }
        } else {
            if(lastContext instanceof UpdatingService) {
                for(ClassPeriod.GradeTerm updatedTerm : lastUpdatedTerms) {
                    compareUpdatedVersionWithOld(updatedTerm);
                }

                allClassPeriods.put(getSchoolYear(), getFormattedUpdatingClassPeriods());
            }
        }
        transferOldTaggedAssignments();

        calculateNewestTerm();
        if(lastContext instanceof PeriodListActivity) {
            final PeriodListActivity context = (PeriodListActivity) lastContext;
            if (context.currentViewTerm == null) {
                context.currentViewTerm = newestTerm;
            }

            context.updateViews(context.currentViewTerm);

            if(PeriodListActivity.isRunning) {
                if (context.dialogTermPercentages != null) {
                    context.lastOpenedDialog.dismiss();
                    context.openGradeRefreshDialog(null);
                }
            }
        }

        switch(lastUpdateReason) {
            case IN_APP_TIME_ELAPSED: case MANUAL:
                if(!PeriodListActivity.isOnForeground || !PeriodListActivity.isRunning) {
                    sendUpdateNotification(null, lastContext);
                }
                break;
            case JOB_SCHEDULER:
                sendUpdateNotification(null, lastContext);

                break;
        }

        saveData(lastContext);

        if(!(lastContext instanceof UpdatingService)) {
            deleteWebviewData();
        }
    }

    private void transferOldTaggedAssignments() {
        for(ClassPeriod.GradeTerm term : lastUpdatedTerms) {
            if(!outdatedClassPeriods.containsKey(term)) {
                continue;
            }

            for(ClassPeriod oldPeriod : outdatedClassPeriods.get(term)) {
                for(ClassPeriod newPeriod : updatedClassPeriods.get(term)) {
                    if (oldPeriod.getID().equals(newPeriod.getID())) {
                        for(GradeCellItem cellItem : oldPeriod.getGradedItems()) {
                            if(cellItem instanceof GradedItem) {
                                if(((GradedItem) cellItem).isTagged) {
                                    for(GradeCellItem newCellItem : newPeriod.getGradedItems()) {
                                        if(newCellItem.equals(cellItem)) {
                                            ((GradedItem) newCellItem).isTagged = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void compareUpdatedVersionWithOld(ClassPeriod.GradeTerm checkTerm) {
        Set<ClassPeriod.GradeTerm> changedTermGradeAverages = new HashSet<>();
        for(ClassPeriod newPeriod : updatedClassPeriods.get(checkTerm)) {
            if(!newPeriod.shouldUpdate()) {
                continue;
            }
            if(!lastUpdatedTerms.contains(newPeriod.getTerm())) {
                continue;
            }

            boolean foundTerm = false;
            if(outdatedClassPeriods.get(checkTerm) != null) {
                for (ClassPeriod previousPeriod : outdatedClassPeriods.get(checkTerm)) {
                    if (newPeriod.getID().equals(previousPeriod.getID())) {
                        foundTerm = true;
                        for (GradeCellItem newItem : newPeriod.getGradedItems()) {
                            if (newItem instanceof GradedItem) {
                                if (newItem.getPercentageGrade() != -1 || (!newItem.getPointsReceived().equals("*") && newItem.getPointsTotal().equals("0"))) {
                                    boolean foundAssignment = false;
                                    for (GradeCellItem oldItem : previousPeriod.getGradedItems()) {
                                        if (oldItem.equals(newItem)) {
                                            foundAssignment = true;
                                            if (oldItem.getPercentageGrade() != newItem.getPercentageGrade() || (newItem.getPointsTotal().equals("0") && !newItem.getPointsReceived().equals(oldItem.getPointsReceived()))) {
                                                changedTermGradeAverages.add(newItem.getPeriod().getTerm());
                                                appendUpdateHistory(currentUpdateNow, (GradedItem) newItem);
                                                newPeriod.incrementUpdate();

                                                if (newPeriod.getTerm().STR != 0) {
                                                    ((GradedItem) newItem).isTagged = true;
                                                    sendUpdateNotification((GradedItem) newItem, lastContext);
                                                }
                                            }
                                            break;
                                        }
                                    }

                                    if (!foundAssignment) {
                                        changedTermGradeAverages.add(newItem.getPeriod().getTerm());
                                        appendUpdateHistory(currentUpdateNow, (GradedItem) newItem);
                                        newPeriod.incrementUpdate();
                                        if (newPeriod.getTerm().STR != 0) {
                                            ((GradedItem) newItem).isTagged = true;
                                            sendUpdateNotification((GradedItem) newItem, lastContext);
                                        }
                                    }
                                } else {
                                    if (newItem.getPointsReceived().equals("*")) {
                                        boolean foundGradedAssignment = false;
                                        for (GradeCellItem oldItem : previousPeriod.getGradedItems()) {
                                            if (oldItem.equals(newItem)) {
                                                foundGradedAssignment = true;
                                                break;
                                            }
                                        }

                                        if (!foundGradedAssignment) {
                                            appendUpdateHistory(currentUpdateNow, (GradedItem) newItem);
                                        }
                                    }
                                }
                            }
                        }

                        if (!newPeriod.getTeacher().equals(previousPeriod.getTeacher())) {
                            sendMessageNotification("A teacher has been updated in " + newPeriod.getClassName(), previousPeriod.getTeacher() + " has been replaced", lastContext);
                        }
                        if (newPeriod.getComments() != null && !Arrays.equals(newPeriod.getComments(), previousPeriod.getComments())) {
                            sendCommentUpdateNotification(newPeriod, lastContext);
                        }
                        break;
                    }
                }
            }

            if(newPeriod.getTerm().STR > 0 && !foundTerm) {
                for(GradeCellItem newItem : newPeriod.getGradedItems()) {
                    if(!newItem.isValidCell()) {
                        continue;
                    }
                    if (newItem instanceof GradedItem) {
                        changedTermGradeAverages.add(newItem.getPeriod().getTerm());
                        appendUpdateHistory(currentUpdateNow, (GradedItem) newItem);
                        newPeriod.incrementUpdate();
                        ((GradedItem) newItem).isTagged = true;
                        sendUpdateNotification((GradedItem) newItem, lastContext);
                    }
                }
            }
        }

        if(!changedTermGradeAverages.isEmpty()) {
            for(ClassPeriod.GradeTerm term : changedTermGradeAverages) {
                List<ClassPeriod> termPeriods = new ArrayList<>();
                for(ClassPeriod period : getNewestClassPeriods()) {
                    if(period.getTerm() == term) {
                        termPeriods.add(period);
                    }
                }

                averageTermGradeManager.appendAverageGrade(termPeriods, term);
            }
        }

        if(!updateHistory.containsKey(currentUpdateNow)) {
            updateHistory.put(currentUpdateNow, new ArrayList<UpdateHistoryPoint>());
        }
    }

    private void appendUpdateHistory(DateTime time, GradedItem newItem) {
        if(!updateHistory.containsKey(time)) {
            updateHistory.put(time, new ArrayList<UpdateHistoryPoint>());
        }

        UpdateHistoryPoint appendPoint;
        if(newItem.getPointsReceived().equals("*")) {
            appendPoint = new UpdateHistoryPoint(newItem.getPeriod().getClassName(), newItem.getGradedName(), true, "0", "0");
        } else {
            appendPoint = new UpdateHistoryPoint(newItem.getPeriod().getClassName(), newItem.getGradedName(), false, newItem.getPointsReceived(), newItem.getPointsTotal());
        }

        if(!updateHistory.get(time).contains(appendPoint)) {
            updateHistory.get(time).add(appendPoint);
        }
    }

    public boolean isTermUpdating(ClassPeriod.GradeTerm term) {
        return lastUpdatedTerms != null && lastUpdatedTerms.contains(term) && areGradesUpdating;
    }

    public boolean doesGradeTermExist(ClassPeriod.GradeTerm term) {
        return getTerms(currentViewYear).contains(term);
    }

    public void sendUpdateNotification(GradedItem item, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if(!sharedPref.getBoolean(SettingsActivity.PREF_ENABLE_ALL_NOTIFICATIONS, true)) {
            return;
        }
        if(context instanceof PeriodListActivity) {
            if(PeriodListActivity.isOnForeground) {
                return;
            }
        }

        String CHANNEL_ID = "app_channel";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Main Channel", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        if(item == null) {
            if(!sharedPref.getBoolean(SettingsActivity.PREF_ENABLE_UPDATE_NOTIFICATIONS, false)) {
                return;
            }

            Intent intent = new Intent(context, PeriodListActivity.class);
            intent.putExtra("refresh", true);

            int requestID = (int) System.nanoTime();

            NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.icon).setContentTitle("Grades have been updated").setContentText("View grades").setDefaults(Notification.DEFAULT_SOUND).setAutoCancel(true);

            if(context instanceof UpdatingService) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT));
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT));
            }

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            manager.notify(0, nBuilder.build());
        } else {
            if(!sharedPref.getBoolean(SettingsActivity.PREF_ENABLE_UPDATED_GRADE_NOTIFICATIONS, true)) {
                return;
            }

            Intent intent = new Intent(context, PeriodListActivity.class);

            intent.putExtra("classPeriod", item.getPeriod().getPeriod());
            intent.putExtra("classTerm", item.getPeriod().getTerm().toString());
            intent.putExtra("assignmentName", item.getGradedName());
            intent.putExtra("assignmentDate", item.getBoldedText());

            int requestID = (int) System.nanoTime();

            NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.icon).setContentTitle("An assignment has been updated in " + item.getPeriod().getClassName()).setContentText("\"" + item.getGradedName() + "\" has been graded").setAutoCancel(true);
            if(context instanceof UpdatingService) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT));
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT));
            }

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            String notificationID = item.getPeriod().getTerm().COLUMN + "" + item.getPeriod().getPeriod() + "" + item.getPeriod().getGradedItems().indexOf(item) + "" + item.getPeriod().getGradedItems().size();

            manager.notify(Integer.parseInt(notificationID), nBuilder.build());
        }
    }

    public void sendCommentUpdateNotification(ClassPeriod period, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if(!sharedPref.getBoolean(SettingsActivity.PREF_ENABLE_ALL_NOTIFICATIONS, true)) {
            return;
        }
        if(context instanceof PeriodListActivity) {
            if(PeriodListActivity.isOnForeground) {
                return;
            }
        }

        String CHANNEL_ID = "app_channel";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Main Channel", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, PeriodListActivity.class);
        intent.putExtra("classPeriod", period.getPeriod());
        int requestID = (int) System.nanoTime();

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.icon).setContentTitle(period.getTeacher() + " has written a comment for " + period.getTerm().NAME).setContentText("Click to view comment").setAutoCancel(true);
        if(context instanceof UpdatingService) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT));
            nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify((int) (Math.random() * 100d) + 101, nBuilder.build());
    }

    private void sendMessageNotification(String title, String msg, Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if(!sharedPref.getBoolean(SettingsActivity.PREF_ENABLE_ALL_NOTIFICATIONS, true)) {
            return;
        }

        String CHANNEL_ID = "app_message_channel";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Message Channel", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, PeriodListActivity.class);
        int requestID = (int) System.nanoTime();
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.icon).setContentTitle(title).setContentText(msg).setDefaults(Notification.DEFAULT_SOUND).setAutoCancel(true).setContentText(msg);
        if(context instanceof UpdatingService) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT));
            nBuilder.setContentIntent(PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(0, nBuilder.build());
    }

    public ClassPeriod getCurrentPeriodByIndexAndTerm(int period, ClassPeriod.GradeTerm term) {
        for(ClassPeriod per : getCurrentClassPeriods()) {
            if(per.getPeriod() == period && per.getTerm() == term) {
                return per;
            }
        }

        return null;
    }

    public void clearReferences() {
        allClassPeriods.clear();
        outdatedClassPeriods.clear();
        updatedClassPeriods.clear();
        filterGrades = null;
        scrapeGrades = null;
        lastUpdatedTerms = null;

        deleteWebviewData();

        if(lastContext != null) {
            UpdatingService.logMessage("Cleared all references to save memory.", lastContext);

            if(lastContext instanceof UpdatingService) {
                ((UpdatingService) lastContext).stopForeground(true);
                ((UpdatingService) lastContext).stopSelf();
            }

            lastContext = null;
        }
    }

    private File[] getTermFiles(String dir) {
        File[] termFiles = new File[6];

        for(int t = 1; t < 5; t++) {
            termFiles[t - 1] = new File(dir + "/term" + t + ".txt");
        }
        for(int s = 0; s < 2; s++) {
            termFiles[4 + s] = new File(dir + "/semester" + (s + 1) + ".txt");
        }

        if(termFiles[0] == null) {
            return null;
        }

        return termFiles;
    }

    private List<ClassPeriod.GradeTerm> getTerms(String year) {
        if(!allClassPeriods.containsKey(year)) {
            return null;
        }

        List<ClassPeriod.GradeTerm> terms = new ArrayList<>();

        for(ClassPeriod term : allClassPeriods.get(year)) {
            if(!terms.contains(term.getTerm())) {
                terms.add(term.getTerm());
            }
        }

        return terms;
    }

    public int getLastUpdateMinutesAgo(ClassPeriod.GradeTerm gradeTerm) {
        for(ClassPeriod term : getCurrentClassPeriods()) {
            if(term.getTerm() == gradeTerm) {
                return (int) ((DateTime.now().getMillis() - term.getLastTermUpdate().getMillis()) / 1000 / 60);
            }
        }

        return -1;
    }

    public String getLastUpdateText(ClassPeriod.GradeTerm gradeTerm) {
        if(gradeTerm == null) {
            return "Last updated never";
        }

        int minAgo = getLastUpdateMinutesAgo(gradeTerm);

        if(minAgo == -1) {
            return "";
        } else if(minAgo < 120) {
            return "Last updated " + minAgo + " minute" + (minAgo == 1 ? "" : "s") + " ago";
        } else if(minAgo < 1440) {
            return "Last updated " + (minAgo / 60) + " hours ago";
        } else {
            int daysAgo = minAgo / 60 / 24;
            return "Last updated " + daysAgo + " day" + (daysAgo == 1 ? "" : "s") + " ago";
        }
    }

    public void calculateNewestTerm() {
        for(ClassPeriod term : getNewestClassPeriods()) {
            if((term.isCurrentTerm() && term.getTerm().STR > 0)) {
                newestTerm = term.getTerm();
                return;
            }

            if(newestTerm == null || term.getTerm().STR > newestTerm.STR) {
                newestTerm = term.getTerm();
            }
        }
    }

    public ClassPeriod.GradeTerm[] getRecommendedUpdateTerms() {
        Set<ClassPeriod.GradeTerm> terms = new HashSet<>();

        LocalDate[] termDates = new LocalDate[ClassPeriod.GradeTerm.values().length];
        boolean[] shouldUpdateTerms = new boolean[termDates.length];
        List<ClassPeriod.GradeTerm> termValues = Arrays.asList(ClassPeriod.GradeTerm.values());
        int existingTerms = 0;
        for(ClassPeriod period : getCurrentClassPeriods()) {
            int termDateIndex = termValues.indexOf(period.getTerm());
            if(termDates[termDateIndex] == null) {
                termDates[termDateIndex] = period.getTermEndDate();
                shouldUpdateTerms[termDateIndex] = period.shouldUpdate();
                existingTerms++;
            }
        }

        if(existingTerms == 0) {
            return ClassPeriod.GradeTerm.values();
        } else {
            LocalDate now = LocalDate.now();
            int activeTerms = 0;
            for(int i = 0; i < termDates.length; i++) {
                if(shouldUpdateTerms[i]) {
                    terms.add(termValues.get(i));
                    activeTerms++;
                }
            }

            if(activeTerms <= 2) {
                //after term 3 ends, add term 4 if it doesn't already exist
                if((shouldUpdateTerms[1] && termDates[4] != null && now.isAfter(termDates[4]))) {
                    terms.add(ClassPeriod.GradeTerm.T4);
                }
            }

            if(activeTerms == 2) {
                if (shouldUpdateTerms[4]) { //need to switch to next term
                    if (shouldUpdateTerms[0] && now.isAfter(termDates[0])) {
                        terms.add(ClassPeriod.GradeTerm.T2);
                    } else if (shouldUpdateTerms[1] && now.isAfter(termDates[1])) {
                        terms.add(ClassPeriod.GradeTerm.T3);
                        terms.add(ClassPeriod.GradeTerm.S2);
                    }
                }

                if (shouldUpdateTerms[5]) { //need to switch to next term
                    if (shouldUpdateTerms[2] && now.isAfter(termDates[2])) {
                        terms.add(ClassPeriod.GradeTerm.T4);
                    }
                }
            } else if(activeTerms == 0) {
                if(termDates[termValues.indexOf(ClassPeriod.GradeTerm.S1)] != null && !shouldUpdateTerms[termValues.indexOf(ClassPeriod.GradeTerm.S1)]) {
                    terms.add(ClassPeriod.GradeTerm.T3);
                    terms.add(ClassPeriod.GradeTerm.S2);
                }
            }
        }

        return terms.toArray(new ClassPeriod.GradeTerm[0]);
    }

    public List<PeriodItem> getTermPeriodList(ClassPeriod.GradeTerm gradeTerm) {
        List<PeriodItem> scores = new ArrayList<>();

        for(ClassPeriod term : allClassPeriods.get(currentViewYear)) {
            if(term.getTerm() == gradeTerm) {
                scores.add(new PeriodItem(term));
            }
        }

        return scores;
    }

    public boolean isTimeToUpdate(Context context) {
        return isTimeToUpdate(context, true);
    }

    public boolean isTimeToUpdate(Context context, boolean noNightUpdate) {
        if(!isNetworkAvailable(context)) {
            return false;
        }

        LocalTime lt = new LocalTime();

        if(!noNightUpdate || (lt.getHourOfDay() >= 6 && lt.getHourOfDay() <= 22)) {
            calculateNewestTerm();

            boolean newestTermShouldUpdate = true;
            for(ClassPeriod period : getNewestClassPeriods()) {
                if(period.getTerm() == newestTerm) {
                    if(!period.shouldUpdate()) {
                        newestTermShouldUpdate = false;
                        break;
                    }
                }
            }

            UpdatingService.logMessage("IS IN HOUR RANGE (" + lt.getHourOfDay() + "). NEWEST TERM IS " + newestTerm + " AND IT'S BEEN " + getLastUpdateMinutesAgo(newestTerm) + " (need >= 120 min.)", lastContext);
            if(newestTermShouldUpdate) {
                return getLastUpdateMinutesAgo(newestTerm) >= 120;
            } else {
                DateTime lastNonexistentUpdate = null;

                for(Map.Entry<DateTime, List<UpdateHistoryPoint>> entry : updateHistory.entrySet()) {
                    if(entry.getValue().size() == 1 && entry.getValue().get(0).className.equals(NONEXISTENT_TERMS_PLACEHOLDER)) {
                        if(lastNonexistentUpdate == null || lastNonexistentUpdate.isBefore(entry.getKey())) {
                            lastNonexistentUpdate = entry.getKey();
                        }
                    }
                }

                if(lastNonexistentUpdate == null) {
                    return true;
                } else {
                    return ((DateTime.now().getMillis() - lastNonexistentUpdate.getMillis()) / 1000 / 60) >= 120;
                }
            }
        } else {
            return false;
        }
    }

    public void loadSchoolYear(String year, PeriodListActivity activity) {
        if(year.contains("current")) {
            year = year.replace(" (current)", "");
        }
        if(year.equals(currentViewYear)) {
            return;
        }

        currentViewYear = year;
        if(allClassPeriods.get(year).isEmpty()) {
            loadYearData(year, activity.getFilesDir().getAbsolutePath() + "/" + LoginActivity.currentUUID);
        }

        if(activity != null) {
            if(getSchoolYear().equals(currentViewYear)) {
                calculateNewestTerm();
                activity.updateViews(newestTerm);
            } else {
                activity.updateViews(ClassPeriod.GradeTerm.T1);
            }
        }
    }

    public boolean isSummer() {
        ClassPeriod term4 = getClassTermInstance(ClassPeriod.GradeTerm.T4);

        if(term4 != null && term4.getTermEndDate() != null) {
            return new LocalDate().isAfter(term4.getTermEndDate().plusDays(1));
        }

        return false;
    }

    public ClassPeriod getClassTermInstance(ClassPeriod.GradeTerm term) {
        if(getSchoolYear().equals(currentViewYear)) {
            for(ClassPeriod classPeriod : getCurrentClassPeriods()) {
                if(classPeriod.getTerm() == term) {
                    return classPeriod;
                }
            }
        } else {
            for(ClassPeriod classPeriod : allClassPeriods.get(currentViewYear)) {
                if(classPeriod.getTerm() == term) {
                    return classPeriod;
                }
            }
        }

        return null;
    }

    public ClassPeriod[] getAllClassTermInstances(ClassPeriod.GradeTerm term) {
        List<ClassPeriod> instances = new ArrayList<>();
        for(ClassPeriod classPeriod : allClassPeriods.get(currentViewYear)) {
            if(classPeriod.getTerm() == term) {
                instances.add(classPeriod);
            }
        }

        return instances.toArray(new ClassPeriod[0]);
    }

    public int getUpdatePercentage() {
        if(iteratedRows == 0) {
            return 0;
        }

        return (int) Math.round((iteratedRows + 1d) / (14d * lastUpdatedTerms.size()) * 100d);
    }

    public int getTermUpdatePercentage(ClassPeriod.GradeTerm term) {
        if(!areGradesUpdating) {
            return -1;
        }
        int termIndex = lastUpdatedTerms.indexOf(term);
        if(termIndex == -1) {
            return -1;
        }

        int currentColumn = iteratedRows / 14;
        if(currentColumn < termIndex) {
            return 0;
        } else if(currentColumn > termIndex) {
            return 100;
        } else {
            if((iteratedRows + 1) % 14 == 1) {
                return 0;
            }
            if((iteratedRows + 1) % 14 == 0) {
                return 100;
            } else {
                return (int) Math.round(((iteratedRows + 1) % 14) / 14d * 100d);
            }
        }
    }

    public ClassPeriod getClassTermInstance(String id) {
        if(getSchoolYear().equals(currentViewYear)) {
            for(ClassPeriod classPeriod : getCurrentClassPeriods()) {
                if(classPeriod.getID().equals(id)) {
                    return classPeriod;
                }
            }
        } else {
            for(ClassPeriod classPeriod : allClassPeriods.get(currentViewYear)) {
                if(classPeriod.getID().equals(id)) {
                    return classPeriod;
                }
            }
        }

        return null;
    }

    public List<ClassPeriod> getNewestClassPeriods() {
        return allClassPeriods.get(getSchoolYear());
    }

    public List<ClassPeriod> getCurrentClassPeriods() {
        return allClassPeriods.get(currentViewYear);
    }

    public String getCurrentViewYear() {
        return currentViewYear;
    }

    public AverageTermGradeHistoryManager getAverageTermGradeManager() {
        return averageTermGradeManager;
    }

    public void exportDataToDownloads(final Activity act) {
        /*
        Store in a file to the user's choosing location
         */

        final File backupFolder = new File(act.getFilesDir().getAbsolutePath());

        try {
            //backup to a zip file
            final String fileName = "standard-score-backup-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "-").replace(".", "-") + ".zip";
            final ZipFile zipFile = new ZipFile(act.getFilesDir().getParentFile().getAbsolutePath() + "/" + fileName);
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA);
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
            parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
            parameters.setPassword("dj#901ejnc91n$ccmr...o!");

            zipFile.addFolder(backupFolder, parameters);

            //allow user to decide where to store the zip file
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent shareFile = new Intent(Intent.ACTION_SEND);

                    shareFile.setType("application/zip");
                    shareFile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(act, "me.tazadejava.standardscore.provider", zipFile.getFile()));
                    shareFile.putExtra(Intent.EXTRA_SUBJECT, fileName);
                    shareFile.putExtra(Intent.EXTRA_TEXT, fileName);

                    act.startActivityForResult(Intent.createChooser(shareFile, "Upload backup file"), 2001);
                }
            }, 500L);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    public void importDataFromDownloads(final Activity act) {
        androidx.appcompat.app.AlertDialog.Builder confirmSave = new androidx.appcompat.app.AlertDialog.Builder(act);

        confirmSave.setTitle("Restore your data from an external backup?");
        confirmSave.setMessage("You must select a valid .zip file.\nWARNING: All current data will be overridden.");

        confirmSave.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                intent.setType("application/zip");

                intent.addCategory(Intent.CATEGORY_OPENABLE);

                act.startActivityForResult(intent, 1001);
            }
        });

        confirmSave.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        confirmSave.show();
    }

    public static String getSchoolYear() {
        DateTime time = DateTime.now();

        int year = time.getYear();
        int month = time.getMonthOfYear();

        if(month <= 7) {
            return (year - 1) + "-" + year;
        } else {
            return year + "-" + (year + 1);
        }
    }

    public static String getLetterGrade(double grade) {
        if(grade >= 90) {
            return "A";
        } else if(grade >= 87 && grade < 90) {
            return "B+";
        } else if(grade >= 84 && grade < 87) {
            return "B";
        } else if(grade >= 80 && grade < 84) {
            return "B-";
        } else if(grade >= 77 && grade < 80) {
            return "C+";
        } else if(grade >= 74 && grade < 77) {
            return "C";
        } else if(grade >= 70 && grade < 74) {
            return "C-";
        } else if(grade >= 67 && grade < 70) {
            return "D+";
        } else if(grade >= 64 && grade < 67) {
            return "D";
        } else if(grade >= 60 && grade < 64) {
            return "D-";
        } else {
            return "F";
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        if(activeNetworkInfo != null) {
            if (context instanceof UpdatingService && !sharedPref.getBoolean(SettingsActivity.PREF_ENABLE_DATA_USAGE, false)) {
                return activeNetworkInfo.isConnected() && activeNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE;
            } else {
                return activeNetworkInfo.isConnected();
            }
        }

        return false;
    }
}
