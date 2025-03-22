package com.trianguloy.forceurl.data.incognito.forks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import com.trianguloy.forceurl.data.AccessibilityFunction;
import com.trianguloy.forceurl.data.Apps;
import com.trianguloy.forceurl.utilities.methods.AndroidUtils;
import com.trianguloy.forceurl.utilities.methods.JavaUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Chromium implements Apps, AccessibilityFunction {
    // https://source.chromium.org/chromium/chromium/src/+/refs/heads/main:chrome/android/java/src/org/chromium/chrome/browser/IntentHandler.java;l=346;drc=fb3fab0be2804a2864783c326518f1acb0402968

    private final Set<String> possibleExtras = new HashSet<>();

    public Chromium() {
        possibleExtras.add("com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB");
        possibleExtras.add("EXTRA_OPEN_NEW_INCOGNITO_TAB"); // just in case, but this shouldn't be needed
    }

    @Override
    public boolean isThis(Context cntx, String pckg) {
        var mainActivity = AndroidUtils.getMainActivity(cntx, pckg);
        // all chromium apps have the same main activity
        var activity = "com.google.android.apps.chrome.Main";
        return mainActivity.equals(activity);
    }

    @Override
    public void transform(Intent intent) {
        // extras do not work in chromium
        // https://source.chromium.org/chromium/chromium/src/+/refs/heads/main:chrome/android/java/src/org/chromium/chrome/browser/IntentHandler.java;drc=e39fffa6900a679961f5992b8f24a084853b811a;l=1036
        // https://source.chromium.org/chromium/chromium/src/+/refs/heads/main:chrome/android/java/src/org/chromium/chrome/browser/IntentHandler.java;l=988;drc=fb3fab0be2804a2864783c326518f1acb0402968
        intent.setComponent(new ComponentName(
                AndroidUtils.getPackage(intent),
                "org.chromium.chrome.browser.incognito.IncognitoTabLauncher"));

        // XXX: I got a case in API 30 where without this flag, the activity wouldn't launch.
        //      Maybe it was a one time thing, if it ever happens again, this fixed it.
        //intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    public Set<String> getExtras() {
        return Collections.unmodifiableSet(possibleExtras);
    }

    @Override
    public boolean putUrl(AccessibilityNodeInfo rootNode, String url, String pckg) {
        // FIXME: Call requires API level 16
        // put URL in search bar, tap it, then tap first result of dropdown
        var searchBar = JavaUtils.getSafe(
                rootNode.findAccessibilityNodeInfosByViewId(
                        pckg + ":id/url_bar"),
                0);
        var incognitoBadge = JavaUtils.getSafe(
                rootNode.findAccessibilityNodeInfosByViewId(
                        pckg + ":id/location_bar_incognito_badge"),
                0);
        if (incognitoBadge != null && searchBar != null) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, url);
            searchBar.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            searchBar.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            var enter = JavaUtils.getSafe(
                    rootNode.findAccessibilityNodeInfosByViewId(
                            pckg + ":id/line_1"),
                    0);
            if (enter != null) {
                enter.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }
        return false;
    }
}
