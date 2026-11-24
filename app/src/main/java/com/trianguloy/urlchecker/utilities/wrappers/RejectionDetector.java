package com.trianguloy.urlchecker.utilities.wrappers;

import android.app.Activity;
import android.content.ComponentName;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.ListStringPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Saves the package and time of the last opened url.
 * If it is requested again in a short amount of time, that package is considered to 'reject' the url and is hidden.
 */
public class RejectionDetector {

    private static final long TIMEFRAME = 5000;
    private final ListStringPref rejectLast; // [openedTimeMillis, component, url]
    private final Activity cntx;

    public RejectionDetector(Activity cntx) {
        rejectLast = new ListStringPref("reject_last", "\n", 3, Collections.emptyList(), cntx);
        this.cntx = cntx;
    }

    /** Marks a url as opened from a component (at this moment) */
    public void markAsOpen(String url, ComponentName component) {
        rejectLast.set(List.of(Long.toString(System.currentTimeMillis()), component.flattenToString(), url));
    }

    /**
     * returns the last component that opened the url if
     * - it happened in a short amount of time
     * - (and) the url is the same
     * - (and) the referrer app is the same
     * null otherwise
     */
    public ComponentName getPrevious(String url) {

        try {
            var data = rejectLast.get();
            if (data.isEmpty()) return null;

            var componentName = ComponentName.unflattenFromString(data.get(1));
            if (componentName == null) return null;

            // checks
            return System.currentTimeMillis() - Long.parseLong(data.get(0)) < TIMEFRAME
                    && Objects.equals(data.get(2), url)
                    && Objects.equals(AndroidUtils.getReferrer(cntx), componentName.getPackageName())

                    ? componentName
                    : null;
        } catch (Exception ignore) {
            // just ignore errors while retrieving the data
            return null;
        }
    }
}
