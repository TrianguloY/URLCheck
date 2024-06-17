package com.trianguloy.forceurllib.data.incognito.forks;

import android.content.Context;
import android.content.Intent;

import com.trianguloy.forceurllib.data.Apps;
import com.trianguloy.forceurllib.utilities.methods.AndroidUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Fenix implements Apps {
    // https://bugzilla.mozilla.org/show_bug.cgi?id=1807531
    // https://github.com/search?q=repo%3Amozilla-mobile%2Ffirefox-android+PRIVATE_BROWSING_MODE&type=code

    private final Set<String> possibleExtras = new HashSet<>();
    private final Set<String> exclude = new HashSet<>();

    public Fenix() {
        possibleExtras.add("private_browsing_mode");
        // exclude tor browser, as it is always in incognito
        exclude.add("org.torproject.torbrowser");
    }

    @Override
    public boolean isThis(Context cntx, String pckg) {
        // all firefox apps share the same home activity
        var activity = "org.mozilla.fenix.HomeActivity";
        if (exclude.contains(pckg)) return false;
        Set<String> activities = AndroidUtils.getActivities(cntx, pckg);
        return activities.contains(activity);
    }

    @Override
    public boolean transform(Intent intent) {
        intent.putExtra("private_browsing_mode", true);
        return needsHelp();
    }

    @Override
    public boolean needsHelp() {
        return false;
    }

    @Override
    public Set<String> getExtras() {
        return Collections.unmodifiableSet(possibleExtras);
    }
}