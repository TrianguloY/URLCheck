package com.trianguloy.urlchecker.modules.companions;

import android.app.Activity;
import android.content.Context;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.TutorialActivity;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.StringPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Manages the app version, to notify of updates */
public class VersionManager {

    private final StringPref lastVersion;

    public static StringPref LASTVERSION_PREF(Context cntx) {
        return new StringPref("changelog_lastVersion", null, cntx);
    }

    /* ------------------- static ------------------- */

    /** Check if the version must be updated */
    public static void check(Activity cntx) {
        // just call the constructor, it does the check
        new VersionManager(cntx);
    }

    /** Returns true iff [version] is newer than the current one */
    public static boolean isVersionNewer(String version) {
        // shortcut to check own version
        if (BuildConfig.VERSION_NAME.equals(version)) return false;

        var versionSplit = split(version);
        // invalid version, consider new just in case
        if (versionSplit.isEmpty()) return true;
        // compare: "1" < "2", "1" < "1.1"
        var currentSplit = split(BuildConfig.VERSION_NAME);

        for (var i = 0; i < Math.min(versionSplit.size(), currentSplit.size()); i++) {
            var versionPart = versionSplit.get(i);
            var currentPart = currentSplit.get(i);

            // version is older
            if (versionPart < currentPart) return false;
            // version is newer
            if (versionPart > currentPart) return true;
        }

        // If all parts are equal up to the minimum length, the version with more parts is newer
        // (and if both are equal, then it is not newer)
        return versionSplit.size() > currentSplit.size();
    }

    /* ------------------- instance ------------------- */

    public VersionManager(Activity cntx) {
        lastVersion = LASTVERSION_PREF(cntx);
        if (lastVersion.get() == null) {
            // no previous setting, the app is a new install, mark as seen
            // ... or maybe it was updated from an old version (where the setting was not yet implemented, 2.12 or below)
            // we check by testing the tutorial flag (which should be set if the app was used)
            if (TutorialActivity.DONE(cntx).get()) lastVersion.set("<2.12");
            else markSeen();
        }

        // --- run migrations --- //
        var prefs = GenericPref.getPrefs(cntx);

        // status module auto-check -> automation
        try {
            var regex = prefs.getString("statusCode_autoCheck", "");
            if (!regex.isEmpty()) {
                var automationRules = new AutomationRules(cntx);
                var catalog = automationRules.getCatalog();
                var name = cntx.getString(R.string.mStatus_check);
                if (!catalog.has(name)) {
                    catalog.put(name, new JSONObject()
                            .put("regex", regex)
                            .put("action", "checkStatus")
                    );
                }
                automationRules.save(catalog);
                prefs.edit().remove("statusCode_autoCheck").apply();
            }
        } catch (Exception e) {
            AndroidUtils.assertError("Unable to migrate statusCode_autoCheck to automation", e);
        }
    }

    /** returns true iff the app was updated since last time it was used */
    public boolean wasUpdated() {
        // just check inequality. If the app was downgraded, you probably also want to be notified.
        return !BuildConfig.VERSION_NAME.equals(lastVersion.get());
    }

    /** Marks the current version as seen (wasUpdated will return false until a new update happens) */
    public void markSeen() {
        lastVersion.set(BuildConfig.VERSION_NAME);
    }

    /* ------------------- private ------------------- */

    static private final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

    /** Extracts all numbers from the string: "1.2.34d" -> [1, 2, 34] */
    private static List<Integer> split(String version) {
        if (version == null) return Collections.emptyList();
        var matcher = INTEGER_PATTERN.matcher(version);
        var parts = new ArrayList<Integer>();
        while (matcher.find()) parts.add(Integer.parseInt(matcher.group()));
        return parts;
    }
}
