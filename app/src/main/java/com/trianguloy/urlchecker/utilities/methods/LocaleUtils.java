package com.trianguloy.urlchecker.utilities.methods;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Utilities related to translations */
public interface LocaleUtils {

    /** Returns a locale for the given tag (language[-country[-variant]]) */
    static Locale parseLocale(String locale) {
        if (locale.isEmpty()) return null;
        try {
            var parts = locale.split("-");
            return new Locale(parts[0], parts.length > 1 ? parts[1] : "", parts.length > 2 ? parts[2] : "");
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns a configuration object for the given locale */
    static Configuration getConfig(Locale locale) {
        var config = new Configuration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        return config;
    }

    /** The locale pref */
    static GenericPref.Str LOCALE_PREF(Context cntx) {
        return new GenericPref.Str("locale", "", cntx);
    }

    /** Sets the locale to an activity */
    static void setLocale(Activity cntx) {
        cntx.getResources().updateConfiguration(
                getConfig(parseLocale(LOCALE_PREF(cntx).get())),
                cntx.getResources().getDisplayMetrics()
        );
    }

    /** Container for a locale with a given name */
    class AvailableLocale {
        public final String tag;
        public final String name;

        public AvailableLocale(String tag, String name) {
            this.tag = tag;
            this.name = name;
        }

        public AvailableLocale(String name) {
            this.tag = "";
            this.name = name;
        }

        @Override
        public String toString() {
            return name + (tag.isEmpty() ? "" : " (" + tag + ")");
        }
    }

    /** Returns the list of available/installed locales (plus a 'default' at the top) */
    static List<AvailableLocale> getLocales(Context cntx) {
        // check each locale
        var available = new ArrayList<AvailableLocale>();
        for (var tag : BuildConfig.LOCALES) {
            var locale = parseLocale(tag);

            // check if available on this device (with split apks the device may not have the translation downloaded)
            var localeName = getStringForLocale(R.string.locale, locale, cntx);
            if (available.isEmpty() || !available.get(0).name.equals(localeName)) {
                // either english (first one) or a translation exists
                // note that translations may not exists because PlayStore only installs locales configured by the user
                available.add(new AvailableLocale(tag, localeName));
            } else {
                if (BuildConfig.DEBUG) Log.d("LOCALE", "Locale " + tag + " is not present");
            }
        }
        //noinspection ComparatorCombinators requires api 24
        Collections.sort(available, (a, b) -> a.toString().compareTo(b.toString()));
        available.add(0, new AvailableLocale(cntx.getString(R.string.deviceDefault)));
        return available;
    }

    /** returns a specific string in a specific locale */
    static String getStringForLocale(int id, Locale locale, Context cntx) {
        return cntx.createConfigurationContext(getConfig(locale)).getString(id);
    }

}