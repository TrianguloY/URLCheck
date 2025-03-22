package com.trianguloy.forceurl.data.incognito.forks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.trianguloy.forceurl.data.Apps;
import com.trianguloy.forceurl.utilities.methods.AndroidUtils;

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
        // all fenix apps share the same home activity
        var activity = "org.mozilla.fenix.HomeActivity";
        if (exclude.contains(pckg)) return false;
        Set<String> activities = AndroidUtils.getActivities(cntx, pckg);
        return activities.contains(activity);
    }

    @Override
    public void transform(Intent intent) {
        intent.setComponent(null);
        /*intent.setComponent(new ComponentName(
                AndroidUtils.getPackage(intent),
                "org.mozilla.fenix.IntentReceiverActivity"));*/
        intent.putExtra("private_browsing_mode", true);
    }

    @Override
    public Set<String> getExtras() {
        return Collections.unmodifiableSet(possibleExtras);
    }
}
