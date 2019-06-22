package me.tazadejava.schedule;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import me.tazadejava.gradeupdates.UpdatingService;
import me.tazadejava.intro.LoginActivity;

import static me.tazadejava.gradeupdates.GradesManager.isNetworkAvailable;

public class WebInterface {

    private final String scrapeSchedule;

    private boolean isUpdatingSchedule;

    private WebView web, newWeb;
    private int waitingForNewPageTimeout;

    private ScheduleViewActivity context;

    public WebInterface(ScheduleViewActivity context) {
        this.context = context;
        StringBuilder scrapeSchedule = new StringBuilder("");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("scrapeSchedule.js"), StandardCharsets.UTF_8));

            String read;
            while((read = reader.readLine()) != null) {
                scrapeSchedule.append(read);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.scrapeSchedule = scrapeSchedule.toString();
    }

    public void updateSchedule() {
        if(!isNetworkAvailable(context)) {
            return;
        }
        if (isUpdatingSchedule) {
            return;
        }

        context.beginUpdateLoop();

        isUpdatingSchedule = true;
        web = new WebView(context);

        if(Settings.canDrawOverlays(context)) {
            final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            int type;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                type = WindowManager.LayoutParams.TYPE_PHONE;
            }

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

        web.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onCreateWindow(WebView view, final boolean isDialog, boolean isUserGesture, Message resultMsg) {
                newWeb = new WebView(context);

                newWeb.getSettings().setJavaScriptEnabled(true);
                newWeb.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                newWeb.addJavascriptInterface(new ScheduleScraperInterface(context), "scrape");
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

                        if(!isNetworkAvailable(context)) {
                            clearReferences();
                            return;
                        }
                        if(view.getUrl().equals("https://www2.saas.wa-k12.net/scripts/cgiip.exe/WService=wlkwashs71/skyportexpired.w")) {
                            clearReferences();
                            Intent intent = new Intent(context, LoginActivity.class);
                            intent.putExtra("changePassword", true);
                            context.startActivity(intent);
                            return;
                        }

                        if(view.getUrl().contains("saas.wa-k12.net")) {
                            if(view.getUrl().contains("sfhome01.w")) {
                                waitingForNewPageTimeout = 2;
                                view.evaluateJavascript("javascript:(function(){" +
                                                "return document.getElementsByClassName('sf_navMenuItem')[5] != null;" +
                                                "})()"
                                        , new ValueCallback<String>() {
                                            @Override
                                            public void onReceiveValue(String value) {
                                                if (value.equals("true")) {
                                                    view.loadUrl("javascript:(function(){" +
                                                            "var l=document.getElementsByClassName('sf_navMenuItem');" +
                                                            "l[5].click();" +
                                                            "})()");
                                                }
                                            }
                                        });

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (waitingForNewPageTimeout == 2) {
                                            clearReferences();
                                        }
                                    }
                                }, 20000L);
                            } else if(view.getUrl().contains("sfschedule001.w")) {
                                waitingForNewPageTimeout = 3;

                                newWeb.evaluateJavascript(scrapeSchedule, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        clearReferences();
                                    }
                                });
                            }
                        }
                    }
                });

                return true;
            }
        });

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
                    clearReferences();
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
                                        clearReferences();
                                        return;
                                    }

                                    clearReferences();
                                }
                            }
                        }, 15000L);
                    }
                }
            }
        });

        web.loadUrl("https://www2.saas.wa-k12.net/scripts/cgiip.exe/WService=wlkwashs71/fwemnu01.w");
    }

    public boolean isUpdating() {
        return isUpdatingSchedule;
    }

    public void clearReferences() {
        if(web != null) {
            web.stopLoading();
            if(Settings.canDrawOverlays(context)) {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                wm.removeView(web);
            }

            web.post(new Runnable() {
                @Override
                public void run() {
                    web.destroy();
                    web = null;
                }
            });
        }
        if(newWeb != null) {
            newWeb.stopLoading();

            newWeb.post(new Runnable() {
                @Override
                public void run() {
                    newWeb.destroy();
                    newWeb = null;
                }
            });
        }
    }
}
