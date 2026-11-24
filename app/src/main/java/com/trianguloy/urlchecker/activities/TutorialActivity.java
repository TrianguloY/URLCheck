package com.trianguloy.urlchecker.activities;

import static android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.fragments.BrowserButtonsFragment;
import com.trianguloy.urlchecker.fragments.ResultCodeInjector;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;
import com.trianguloy.urlchecker.utilities.wrappers.DoubleEvent;
import com.trianguloy.urlchecker.utilities.wrappers.FixedViewFlipper;

import java.util.Locale;

public class TutorialActivity extends Activity {

    private final DoubleEvent doubleClick = new DoubleEvent(500); // to avoid exiting when pressing next/prev very fast

    private Button prevButton;
    private Button nextButton;
    private BoolPref tutorialDone;
    private FixedViewFlipper flipper;
    private TextView pageIndexText;

    public static BoolPref DONE(Context cntx) {
        return new BoolPref("tutorial_done", false, cntx);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_tutorial);
        setTitle(R.string.tutorial);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(PRIORITY_DEFAULT, this::onBackPressed);
        }

        tutorialDone = DONE(this);

        flipper = new FixedViewFlipper(findViewById(R.id.flipper));
        prevButton = findViewById(R.id.bBack);
        nextButton = findViewById(R.id.bNext);
        pageIndexText = findViewById(R.id.pageIndex);

        configureBrowserButtons();
        updateButtons();

    }

    /* ------------------- browser fragment ------------------- */

    private final ResultCodeInjector resultCodeInjector = new ResultCodeInjector();
    private final BrowserButtonsFragment browserButtons = new BrowserButtonsFragment(this, resultCodeInjector);

    private void configureBrowserButtons() {
        browserButtons.onInitialize(findViewById(browserButtons.getLayoutId()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!resultCodeInjector.onActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    /* ------------------- buttons ------------------- */

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        prev(null);
    }

    public void prev(View view) {
        if (flipper.getDisplayedChild() == 0) {
            // first page, exit
            if (view != null && doubleClick.checkAndTrigger()) return; // unless clicked too fast
            exit();
        } else {
            // show prev
            flipper.showPrevious();
            doubleClick.trigger();
            updateButtons();
        }
    }

    public void next(View view) {
        if (flipper.getDisplayedChild() == flipper.getChildCount() - 1) {
            // last page, exit
            if (doubleClick.checkAndTrigger()) return; // unless clicked too fast
            exit();
        } else {
            // show next
            flipper.showNext();
            doubleClick.trigger();
            updateButtons();
        }
    }

    public void openModulesActivity(View view) {
        PackageUtils.startActivity(new Intent(this, ModulesActivity.class), R.string.toast_noApp, this);
    }

    /* ------------------- actions ------------------- */

    /** Updates the buttons and index texts */
    private void updateButtons() {
        var current = flipper.getDisplayedChild();
        var max = flipper.getChildCount();

        prevButton.setText(current == 0 ? R.string.btn_tutorialSkip : R.string.back);
        nextButton.setText(current != max - 1 ? R.string.next : R.string.btn_tutorialEnd);

        pageIndexText.setText(String.format(Locale.getDefault(), "%d/%d", current + 1, max));
    }

    /** Marks the tutorial as completed and exits */
    private void exit() {
        tutorialDone.set(true);
        this.finish();
    }

}
