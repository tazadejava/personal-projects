package me.tazadejava.intro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

import me.tazadejava.mainscreen.PeriodListActivity;
import me.tazadejava.standardscore.R;

public class IntroTutorialActivity extends AppIntro {

    public static final String COMPLETED_INTRODUCTION = "COMPLETED_INTRODUCTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();

        showSkipButton(true);
        setProgressButtonEnabled(true);

        int color = StandardScoreApplication.getColorId(R.color.colorHeaderItem);

        SliderPage page1 = new SliderPage();
        page1.setTitle("Welcome to Standard Score!");
        page1.setDescription("Scroll on to learn how to perform some basic tasks within the app");
        page1.setImageDrawable(R.drawable.app_icon);
        page1.setBgColor(color);
        addSlide(AppIntroFragment.newInstance(page1));

        SliderPage page2 = new SliderPage();
        page2.setTitle("Switching Terms");
        page2.setDescription("Click the banner above your classes to access the term switching dialog");
        page2.setImageDrawable(R.drawable.intro_1_alt);
        page2.setBgColor(color);
        addSlide(AppIntroFragment.newInstance(page2));

        SliderPage page3 = new SliderPage();
        page3.setTitle("Checking Graphs");
        page3.setDescription("Hold down on the teacher's banner to see your class's academic percentage history, graphically");
        page3.setImageDrawable(R.drawable.intro_2_alt);
        page3.setBgColor(color);
        addSlide(AppIntroFragment.newInstance(page3));

        SliderPage page4 = new SliderPage();
        page4.setTitle("Calculating \"what-ifs\"");
        page4.setDescription("Click on a header or individual assignment to calculate theoretical point totals for that assignment");
        page4.setImageDrawable(R.drawable.intro_3_alt);
        page4.setBgColor(color);
        addSlide(AppIntroFragment.newInstance(page4));
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);

        skipTutorial();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        skipTutorial();
    }

    private void skipTutorial() {
        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefsEditor.putBoolean(IntroTutorialActivity.COMPLETED_INTRODUCTION, true);
        prefsEditor.apply();

        Intent goToMain = new Intent(this, PeriodListActivity.class);
        startActivity(goToMain);
    }

    @Override
    public void onBackPressed() {
        skipTutorial();
    }
}
