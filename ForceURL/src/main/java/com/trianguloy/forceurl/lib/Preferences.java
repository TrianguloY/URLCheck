package com.trianguloy.forceurl.lib;

import static com.trianguloy.forceurl.utilities.methods.PackageUtils.startActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.trianguloy.forceurl.helpers.Helpers;
import com.trianguloy.forceurl.R;
import com.trianguloy.forceurl.utilities.generics.GenericPref;
import com.trianguloy.forceurl.utilities.methods.JavaUtils;

public class Preferences {
    public static GenericPref.Enumeration<Helpers> CURRENT_PREF(Context cntx) {
        return new GenericPref.Enumeration<>("urlHelper_urlHelper",
                Helpers.none,
                Helpers.class,
                cntx);
    }

    public static GenericPref.Bool STOREBEFORERELEASE_PREF(Context cntx) {
        return new GenericPref.Bool("urlHelper_storeBeforeRelease",
                true,
                cntx);
    }

    public static GenericPref.Int TIMER_PREF(Context cntx) {
        return new GenericPref.Int("urlHelper_timer",
                10,
                cntx);
    }

    // ---
    public static void showSettings(Activity cntx) {
        new Config(cntx).showSettings();
    }
}

class Config {

    /* ------------------- prefs ------------------- */
    private final GenericPref.Enumeration<Helpers> current;
    private final GenericPref.Bool storeBeforeRestore;
    private final GenericPref.Int timer;

    /* ------------------- constructor ------------------- */

    private final Activity cntx;

    public Config(Activity cntx) {
        this.cntx = cntx;

        current = Preferences.CURRENT_PREF(cntx);
        storeBeforeRestore = Preferences.STOREBEFORERELEASE_PREF(cntx);
        timer = Preferences.TIMER_PREF(cntx);
    }

    /**
     * Show the settings dialog
     */
    public void showSettings() {
        // prepare dialog content
        View views = cntx.getLayoutInflater().inflate(R.layout.config_urlhelper, null);

        // configure
        Spinner helperView = views.findViewById(R.id.helperType_pref);
        Switch storeView = views.findViewById(R.id.storeBeforeRelease_pref);
        EditText timerView = views.findViewById(R.id.seconds_pref);
        var timerText = views.findViewById(R.id.timerText);

        JavaUtils.Consumer<Helpers> updateFields = helpers -> {
            boolean timerState;
            boolean storeState;
            switch (helpers) {
                case none:
                case accessibilityService:
                    timerState = false;
                    storeState = false;
                    break;
                case manualBubble:
                    timerState = false;
                    storeState = true;
                    break;
                case semiAutoBubble:
                case autoBackground:
                default:
                    timerState = true;
                    storeState = true;
                    break;
            }
            timerText.setEnabled(timerState);
            timerView.setEnabled(timerState);
            storeView.setEnabled(storeState);
        };
        updateFields.accept(current.get());
        current.attachToSpinner(helperView, updateFields);
        storeBeforeRestore.attachToSwitch(storeView);
        timer.attachToEditText(timerView, 0);
        views.findViewById(R.id.draw_permissions).setOnClickListener(view -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + cntx.getPackageName()));
            startActivity(intent, 1, cntx);
        });
        views.findViewById(R.id.accessibility_permissions).setOnClickListener(view -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent, 1, cntx);
        });

        // prepare dialog
        AlertDialog dialog = new AlertDialog.Builder(cntx)
                .setView(views)
                .setTitle(R.string.settings_title)
                .setCancelable(true)
                .show();

        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}

