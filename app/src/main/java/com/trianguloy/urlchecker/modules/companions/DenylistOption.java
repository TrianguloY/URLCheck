package com.trianguloy.urlchecker.modules.companions;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.Enums;

/**
 * Enum representing the app that can be hidden from "Open with" and "Share via".

 * Each entry has:
 * An id (for saving as a preference),
 * A label (string resource for UI),
 * The actual package name of the app to filter out.

 */

public enum DenylistOption implements Enums.IdEnum, Enums.StringEnum {
    NONE(0, R.string.denylist_none, null),
    CHROME(1, R.string.denylist_chrome, "com.android.chrome"),
    FIREFOX(2, R.string.denylist_firefox, "org.mozilla.firefox"),
    EDGE(3, R.string.denylist_edge, "com.microsoft.emmx"),
    OPERA(4, R.string.denylist_opera, "com.opera.browser"),
    BRAVE(5, R.string.denylist_brave, "com.brave.browser");

    private final int id;
    private final int labelRes;
    private final String packageName;

    DenylistOption(int id, int labelRes, String packageName) {
        this.id = id;
        this.labelRes = labelRes;
        this.packageName = packageName;
    }

    @Override
    public int getId() {
        return id;
    }

    public int getStringResource() {
        return labelRes;
    }

    /**
     * This is the name that will be denied
     * Could update to a list of dynamically made sites in the future
     */
    public String getPackageName() {
        return packageName;
    }
}
