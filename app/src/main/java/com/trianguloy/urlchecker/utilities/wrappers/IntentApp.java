package com.trianguloy.urlchecker.utilities.wrappers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.trianguloy.urlchecker.modules.companions.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents a way to open something (A wrapper of {@link ResolveInfo}) */
public class IntentApp {

    /**
     * Returns a list of packages that can open an intent, removing this app from the list
     *
     * @param baseIntent intent that the packages will be able to open
     * @param cntx       base context (and the app that will be filtered)
     * @return the list of other packages
     */
    public static List<IntentApp> getOtherPackages(Intent baseIntent, Context cntx) {
        // get all packages
        var resolveInfos = cntx.getPackageManager().queryIntentActivities(
                baseIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PackageManager.MATCH_ALL : 0);

        var intentApps = new ArrayList<IntentApp>();
        for (var resolveInfo : resolveInfos) {
            // filter the current app
            if (!resolveInfo.activityInfo.packageName.equals(cntx.getPackageName())) {
                intentApps.add(new IntentApp(resolveInfo));
            }
        }
        return intentApps;
    }

    /* ------------------- wrapper ------------------- */

    private final ResolveInfo resolveInfo;
    private static final Map<ComponentName, CharSequence> labelsCache = new HashMap<>();
    private static final Map<ComponentName, Drawable> iconsCache = new HashMap<>();

    private IntentApp(ResolveInfo resolveInfo) {
        this.resolveInfo = resolveInfo;
    }

    /** Returns the package only */
    public String getPackage() {
        return resolveInfo.activityInfo.packageName;
    }

    /** Returns the component (package + class) */
    public ComponentName getComponent() {
        return new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
    }

    /** Returns the label, cached */
    public CharSequence getLabel(Context activity) {
        var component = getComponent();
        if (!labelsCache.containsKey(component)) labelsCache.put(component, resolveInfo.loadLabel(activity.getPackageManager()));
        return labelsCache.get(component);
    }

    /** Returns the drawable, cached */
    public Drawable getIcon(Context activity, Size size) {
        // in dp, so it looks the same on any screen density. NORMAL matches the visible
        // size of the icons next to it: 24dp icons, but their glyph is about 20dp
        var dp = switch (size) {
            case NONE -> 0;
            case SMALL -> 10;
            case NORMAL -> 20;
            case BIG -> 30;
        };
        if (dp == 0) return null;
        var dim = Math.round(dp * activity.getResources().getDisplayMetrics().density);

        var component = getComponent();
        if (!iconsCache.containsKey(component)) {
            iconsCache.put(component, resolveInfo.loadIcon(activity.getPackageManager()));
        }
        // set the bounds each time, the size may have changed since it was cached
        var icon = iconsCache.get(component);
        icon.setBounds(0, 0, dim, dim);
        return icon;
    }

    @Override
    public String toString() {
        return "IntentApp{" + resolveInfo + '}';
    }
}
