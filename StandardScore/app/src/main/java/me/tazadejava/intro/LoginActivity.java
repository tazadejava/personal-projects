package me.tazadejava.intro;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import me.tazadejava.mainscreen.PeriodListActivity;
import me.tazadejava.settings.SettingsActivity;
import me.tazadejava.standardscore.R;

public class LoginActivity extends AppCompatActivity {

    private static SecretKey key;
    public static String currentUUID;
    private static String username, password;

    private boolean hasLoggedIn = false, pageFinished = false;

    private long lastDialogTextClickTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.PREF_ENABLE_SERVICE, false) && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Grant permission?");
            builder.setMessage("The app needs the overlay permission to update grades in the background.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    myIntent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(myIntent, 111);
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    loadActivity();

                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putBoolean(SettingsActivity.PREF_ENABLE_SERVICE, false).apply();
                }
            });
            builder.create().show();
        } else {
            loadActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case 111:
                PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putBoolean(SettingsActivity.PREF_ENABLE_SERVICE, Settings.canDrawOverlays(this)).apply();

                loadActivity();
                break;
            case 1001: //restore from backup
                PeriodListActivity.restoreFromBackup(this, resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadActivity() {
        String customText = "";
        if(getIntent().hasExtra("failedLogin")) {
            deletePassword(this);
        }
        if(getIntent().hasExtra("loggedOut")) {
            ((StandardScoreApplication) getApplication()).deleteGradesManager();
        }

        if(getIntent().hasExtra("changePassword")) {
            deletePassword(this);
            customText = "Your password has expired. Please go to Skyward and change it, then return here.";
        }

        loadFiles(this);

        if(password == null) {
            openLoginDialog(username == null ? "" : username, customText);
        } else {
            hasLoggedIn = true;
            gotoMain();
        }
    }

    private void gotoMain() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent;
        if(!prefs.getBoolean(IntroTutorialActivity.COMPLETED_INTRODUCTION, false)) {
            intent = new Intent(this, IntroTutorialActivity.class);
        } else {
            intent = new Intent(this, PeriodListActivity.class);
        }

        startActivity(intent);
    }

    private void saveCredentials(String username, String password) {
        key = generateKey();
        LoginActivity.username = username;
        LoginActivity.password = password;

        currentUUID = null;

        //TODO: IF THE CURRENT.TXT EXISTS, THEN USE THAT UUID INSTEAD OF SEARCHING FOR ANOTHER ONE? MAYBE!?!?!??!

        List<File> potentialLogins = new ArrayList<>();

        for(File file : new File(getFilesDir().getAbsolutePath()).listFiles()) {
            if(file.isDirectory() && file.getName().split("-").length == 5) {
                potentialLogins.add(file);
            }
        }


        File uuidFile = new File(getFilesDir().getAbsolutePath() + "/current.txt");
//        if(uuidFile.exists() && potentialLogins.size() == 1) { //TODO: FIX THIS... IF THE USERNAME DOESN'T EXIST, HOW CAN IT TELL THAT IT IS THE SAME USER?!?!? THIS DOES NOT WORK BECAUSE MULTIPLE ACCOUNTS ARE NOW DISABLED...
//            currentUUID = potentialLogins.get(0).getName();
//        } else {
            for (File file : potentialLogins) {
                File loginInfoFile = new File(file.getAbsolutePath() + "/loginData.txt");

                try {
                    BufferedReader reader = new BufferedReader(new FileReader(loginInfoFile));

                    byte[] encoded = Base64.decode(reader.readLine(), Base64.DEFAULT);
                    SecretKey checkingKey = new SecretKeySpec(encoded, 0, encoded.length, "AES");

                    String read = reader.readLine();
                    read = read.substring(1, read.length() - 1);
                    String[] split = read.split(", ");
                    byte[] array = new byte[split.length];
                    int c = 0;
                    for (String str : split) {
                        array[c] = Byte.valueOf(str);
                        c++;
                    }

                    String checkingUsername = decrypt(array, checkingKey);

                    if (username.equals(checkingUsername)) {
                        currentUUID = file.getName();
                    }

                    reader.close();
                } catch (IOException | NoSuchPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }

                if (currentUUID != null) {
                    break;
                }
            }
//        }

        if(currentUUID == null) {
            currentUUID = UUID.randomUUID().toString();
        }

        try {
            File newUUIDFolder = new File(getFilesDir().getAbsolutePath() + "/" + currentUUID + "/");
            newUUIDFolder.mkdir();

            File dataFile = new File(getAccountPath(this) + "/loginData.txt");
            if(!dataFile.exists()) {
                dataFile.createNewFile();
            }

            FileWriter writer = new FileWriter(dataFile);

            writer.append(Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP) + "\n");
            writer.append(Arrays.toString(encrypt(username, key)) + "\n");
            writer.append(Arrays.toString(encrypt(password, key)) + "\n");

            writer.close();

            File currentFile = new File(getFilesDir().getAbsolutePath() + "/current.txt");
            if(!currentFile.exists()) {
                currentFile.createNewFile();
            }
            writer = new FileWriter(currentFile);
            writer.append(currentUUID);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadFiles(Context context) {
        try {
            File uuidFile = new File(context.getFilesDir().getAbsolutePath() + "/current.txt");
            if(!uuidFile.exists()) {
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(uuidFile));
            currentUUID = reader.readLine();
            reader.close();

            File dataFile = new File(getAccountPath(context) + "/loginData.txt");
            if(!dataFile.exists()) {
                return;
            }

            reader = new BufferedReader(new FileReader(dataFile));

            int count = 0;
            String read;
            while((read = reader.readLine()) != null) {
                switch(count) {
                    case 0:
                        byte[] encoded = Base64.decode(read, Base64.DEFAULT);
                        key = new SecretKeySpec(encoded, 0, encoded.length, "AES");
                        break;
                    case 1:case 2:
                        read = read.substring(1, read.length() - 1);
                        String[] split = read.split(", ");
                        byte[] array = new byte[split.length];
                        int c = 0;
                        for(String str : split) {
                            array[c] = Byte.valueOf(str);
                            c++;
                        }

                        if(count == 1) {
                            username = decrypt(array, key);
                        } else {
                            password = decrypt(array, key);
                        }
                        break;
                }
                count++;
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    public static boolean isUserLoggedIn() {
        return password != null;
    }

    public static void deletePassword(Context context) {
        try {
            File dataFile = new File(getAccountPath(context) + "/loginData.txt");
            if(!dataFile.exists()) {
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(dataFile));
            List<String> lines = new ArrayList<>();
            String read;
            while((read = reader.readLine()) != null) {
                lines.add(read);
            }
            reader.close();

            FileWriter writer = new FileWriter(dataFile, false);

            for(int i = 0; i < (lines.size() >= 2 ? 2 : lines.size()); i++) {
                writer.append(lines.get(i) + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        password = null;
    }

    public void openLoginDialog(String usernameText, String customText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.username_password_dialog, null);
        final TextView username = view.findViewById(R.id.username);

        final TextView usernameHelpButton = view.findViewById(R.id.usernameHelpButton);
        usernameHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(LoginActivity.this, "Typical username format:\nLAST_NAME  FIRST_NAME000\nVerify # of spaces is correct between first and last name", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                TextView view = toast.getView().findViewById(android.R.id.message);
                view.setGravity(Gravity.CENTER);
                toast.show();
            }
        });
        final TextView passwordHelpButton = view.findViewById(R.id.passwordHelpButton);
        passwordHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(LoginActivity.this, "Password may be an previously used password", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                TextView view = toast.getView().findViewById(android.R.id.message);
                view.setGravity(Gravity.CENTER);
                toast.show();
            }
        });

        TextView dialogText = view.findViewById(R.id.dialogText);

        dialogText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastDialogTextClickTime = System.currentTimeMillis();
            }
        });

        //restore from backup option
        dialogText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(System.currentTimeMillis() - lastDialogTextClickTime <= 2000) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                    intent.setType("application/zip");

                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    startActivityForResult(intent, 1001);
                    return true;
                }
                return false;
            }
        });

        username.setText(usernameText);
        final TextView custom = view.findViewById(R.id.customMessage);
        if(customText == null || customText.isEmpty()) {
            custom.setVisibility(View.GONE);
        } else {
            custom.setText(customText);
        }
        final TextView password = view.findViewById(R.id.password);
        password.setTypeface(Typeface.SANS_SERIF);

        builder.setView(view)
                .setPositiveButton("Sign In", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideKeyboard(view);
                        testLogin(username.getText().toString(), password.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideKeyboard(view);
                        finishAffinity();
                    }
                });

        if(LoginActivity.username != null) {
            builder.setNeutralButton("View Saved Grades", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    hideKeyboard(view);
                    gotoMain();
                }
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        if(usernameText != null && !usernameText.isEmpty()) {
            password.requestFocus();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }, 300L);
        }
    }


    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void testLogin(String usernameInput, final String password) {
        if(usernameInput.isEmpty()) {
            openLoginDialog("", "Username cannot be blank");
            return;
        }
        if(password.isEmpty()) {
            openLoginDialog(username, "Password cannot be blank");
            return;
        }

        usernameInput = usernameInput.trim();
        final String username = usernameInput;

        pageFinished = false;
        WebView web = findViewById(R.id.webView);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(final WebView view, final boolean isDialog, boolean isUserGesture, Message resultMsg) {
                hasLoggedIn = true;
                view.clearCache(true);
                view.loadUrl("about:blank");
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        view.destroy();
                    }
                });
                saveCredentials(username, password);
                gotoMain();
                return true;
            }
        });
        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(final WebView view, String url) {
                super.onPageFinished(view, url);
                if(pageFinished) {
                    return;
                }

                pageFinished = true;
                view.evaluateJavascript("javascript:(function(){" +
                        "editInputs = document.getElementsByClassName('EditInput');" +
                        "editInputs[0].value = '" + username + "';" +
                        "editInputs[1].value = '" + password.replace("\\", "\\\\") + "';" +
                        "document.getElementById('bLogin').click();" +
                        "})()"
                        , null);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!hasLoggedIn) {
                            openLoginDialog(username, "Incorrect combination");
                        }
                    }
                }, 1000L);
            }
        });

        web.loadUrl("https://www2.saas.wa-k12.net/scripts/cgiip.exe/WService=wlkwashs71/fwemnu01.w");
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        if(password == null) {
            return null;
        }
        return password.replace("\\", "\\\\");
    }

    public static String getAccountPath(Context context) {
        return context.getFilesDir().getAbsolutePath() + "/" + currentUUID;
    }

    private SecretKey generateKey() {
        try {
            SecureRandom random = new SecureRandom();
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(256, random);
            return gen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    private byte[] encrypt(String str, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String decrypt(byte[] text, SecretKey key) throws NoSuchPaddingException, IllegalBlockSizeException {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(text), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | InvalidKeyException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
