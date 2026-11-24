package com.trianguloy.urlchecker.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.fragments.ResultCodeInjector;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.EnumerationPref;

public interface AndroidSettings {

    /* ------------------- day/light theme ------------------- */

    /** The theme setting */
    enum Theme implements Enums.IdEnum, Enums.StringEnum {
        DEFAULT(0, R.string.deviceDefault),
        DARK(1, R.string.spin_darkTheme),
        LIGHT(2, R.string.spin_lightTheme),
        ;

        private final int id;
        private final int string;

        Theme(int id, int string) {
            this.id = id;
            this.string = string;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int getStringResource() {
            return string;
        }
    }

    /** The theme pref */
    static EnumerationPref<Theme> THEME_PREF(Context cntx) {
        return new EnumerationPref<>("dayNight", Theme.DEFAULT, Theme.class, cntx);
    }

    /** Sets the theme (light/dark mode) to an activity */
    static void setTheme(Context activity, boolean dialog) {
        activity.setTheme(switch (THEME_PREF(activity).get()) {
            case DEFAULT -> {
                if (!dialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // is dayNight different to manually choosing dark or light? no idea, but it exists so...
                    yield R.style.ActivityThemeDayNight;
                }

                // explicit light mode (uiMode=NIGHT_NO)
                // dark mode or device default
                if ((activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO)
                    yield dialog ? R.style.DialogThemeLight : R.style.ActivityThemeLight;
                else
                    yield dialog ? R.style.DialogThemeDark : R.style.ActivityThemeDark;
            }
            case DARK -> dialog ? R.style.DialogThemeDark : R.style.ActivityThemeDark;
            case LIGHT -> dialog ? R.style.DialogThemeLight : R.style.ActivityThemeLight;
        });
    }

    /* ------------------- reloading ------------------- */

    String RELOAD_EXTRA = "reloaded";
    int RELOAD_RESULT_CODE = Activity.RESULT_FIRST_USER;

    /** destroys and recreates the activity (to apply changes) and marks it */
    static void reload(Activity cntx) {
        Log.d("SETTINGS", "reloading");
        cntx.getIntent().putExtra(RELOAD_EXTRA, true); // keep data
        cntx.recreate();
    }

    /** Returns true if the activity was reloaded (with {@link AndroidSettings#reload}) and clears the flag */
    static boolean wasReloaded(Activity cntx) {
        var intent = cntx.getIntent();
        var reloaded = intent.getBooleanExtra(RELOAD_EXTRA, false);
        intent.removeExtra(RELOAD_EXTRA);
        return reloaded;
    }

    /** Registers an activity result to reload if the launched activity is marked as reloading using{@link AndroidSettings#markForReloading(Activity)} */
    static int registerForReloading(ResultCodeInjector resultCodeInjector, Activity cntx) {
        return resultCodeInjector.registerActivityResult((resultCode, data) -> {
            if (resultCode == RELOAD_RESULT_CODE) {
                AndroidSettings.reload(cntx);
            }
        });
    }

    /** Makes the activity that launched this one to reload, if registered with {@link AndroidSettings#registerForReloading(ResultCodeInjector, Activity)} */
    static void markForReloading(Activity cntx) {
        cntx.setResult(RELOAD_RESULT_CODE);
    }

}
