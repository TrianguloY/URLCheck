package com.trianguloy.urlchecker.modules.companions;

import android.content.ComponentName;
import android.content.Context;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.IntPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.wrappers.IntentApp;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Manages a list of last opened apps, for priority purposes */
public class LastOpened {

    public static BoolPref PERDOMAIN_PREF(Context cntx) {
        return new BoolPref("lastOpen_perDomain", false, cntx);
    }

    /* ------------------- data ------------------- */

    /** Maximum 'preference' between two apps */
    private static final int MAX = 3;

    /** The prefix for the savedPrefs */
    private static final String PREFIX = "opened %s %s";
    private final BoolPref perDomainPref;
    private final Context cntx;

    /* ------------------- public ------------------- */

    /** Initializes this utility */
    public LastOpened(Context cntx) {
        this.cntx = cntx;
        perDomainPref = PERDOMAIN_PREF(cntx);
    }

    /** Sorts an existing list of [intentApps] with the preferred order */
    public void sort(List<IntentApp> intentApps, String url) {
        Collections.sort(intentApps, (from, another) ->
                comparePrefer(from.getComponent(), another.getComponent(), url));
    }

    /** Marks the [prefer] component as preferred over [others]. */
    public void prefer(ComponentName prefer, List<IntentApp> others, String url) {
        for (var other : others) {
            prefer(prefer, other.getComponent(), 1, url);
        }
    }

    /* ------------------- private ------------------- */

    /** Marks that [prefer] component is preferred over [other] as much as [amount] more */
    private void prefer(ComponentName prefer, ComponentName other, int amount, String url) {
        // skip prefer over ourselves, it's useless
        if (prefer.equals(other)) return;

        // switch order if not lexicographically sorted
        if (prefer.compareTo(other) > 0) {
            prefer(other, prefer, -amount, url);
            return;
        }

        // update preference (we subtract because negative means preferred)
        IntPref pref = getPref(prefer, other, url);
        pref.set(JavaUtils.clamp(-MAX, pref.get() - amount, MAX));
    }

    /**
     * Returns the current preference between these two components.
     * Equivalent result as [from].compareTo([another])
     */
    private int comparePrefer(ComponentName from, ComponentName another, String url) {
        // switch order if not lexicographically sorted
        if (from.compareTo(another) > 0) {
            return -comparePrefer(another, from, url);
        }

        // get preference
        return getPref(from, another, url).get();
    }

    /** The preference between two components. ([left] must be lexicographically less than [right]) */
    private IntPref getPref(ComponentName left, ComponentName right, String url) {
        String prefName = String.format(PREFIX, left.flattenToShortString(), right.flattenToShortString());
        if (perDomainPref.get()) {
            prefName = getDomain(url) + " " + prefName;
        }

        return new IntPref(prefName, 0, cntx);
    }

    /**
     * Get top level domain and first subdomain (if any) from a given url
     * a.b.c.d => c.d
     * a.b.c => b.c
     * a.b => a.b
     * a => a
     */
    private String getDomain(String url) {
        try {
            var domainParts = Arrays.asList(new URL(url).getHost().split("\\."));
            return String.join(".", domainParts.size() <= 1 ? domainParts : domainParts.subList(domainParts.size() - 2, domainParts.size()));
        } catch (Exception e) {
            AndroidUtils.assertError("Unable to parse domain", e);
            return "";
        }
    }
}
