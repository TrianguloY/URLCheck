package com.trianguloy.urlchecker.utilities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.StringPref;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Explicit forwarding to a user-selected general web browser. */
public class DirectOpen {
    public static BoolPref ENABLED_PREF(Context context) {
        return new BoolPref("directOpen_enabled", false, context);
    }

    public static StringPref PACKAGE_PREF(Context context) {
        return new StringPref("directOpen_package", "", context);
    }

    public enum Result { DISABLED, UNSUPPORTED, OPENED, FAILED }

    public record Browser(String packageName, CharSequence label, Drawable icon) {}

    /** Query both schemes; a site-specific deep-link handler is not a browser. */
    public static List<Browser> getBrowsers(Context context) {
        var pm = context.getPackageManager();
        var httpsPackages = new HashSet<String>();
        for (var info : query(context, webIntent("https://example.invalid/"))) {
            if (isGeneralBrowser(context, info)) httpsPackages.add(info.activityInfo.packageName);
        }
        var browsers = new ArrayList<Browser>();
        var seen = new HashSet<String>();
        for (var info : query(context, webIntent("http://example.invalid/"))) {
            var packageName = info.activityInfo.packageName;
            if (!httpsPackages.contains(packageName) || !isGeneralBrowser(context, info)
                    || !seen.add(packageName)) continue;
            var app = info.activityInfo.applicationInfo;
            browsers.add(new Browser(packageName, pm.getApplicationLabel(app), pm.getApplicationIcon(app)));
        }
        browsers.sort(Comparator.comparing(browser -> browser.label().toString(), String.CASE_INSENSITIVE_ORDER));
        return browsers;
    }

    public static boolean hasBrowser(List<Browser> browsers, String packageName) {
        for (var browser : browsers) {
            if (browser.packageName().equals(packageName)) return true;
        }
        return false;
    }

    private static List<ResolveInfo> query(Context context, Intent intent) {
        var flags = PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PackageManager.MATCH_ALL;
        return context.getPackageManager().queryIntentActivities(intent, flags);
    }

    private static boolean isGeneralBrowser(Context context, ResolveInfo info) {
        var activity = info.activityInfo;
        var filter = info.filter;
        if (activity == null || activity.applicationInfo == null || filter == null
                || activity.packageName.equals(context.getPackageName()) || !activity.exported
                || !filter.hasCategory(Intent.CATEGORY_DEFAULT)
                || !filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                || filter.countDataAuthorities() != 0 || filter.countDataPaths() != 0
                || filter.countDataSchemeSpecificParts() != 0) return false;
        var pm = context.getPackageManager();
        try {
            if (!isEnabled(pm.getApplicationEnabledSetting(activity.packageName), activity.applicationInfo.enabled)
                    || !isEnabled(pm.getComponentEnabledSetting(new ComponentName(activity.packageName, activity.name)), activity.enabled)) return false;
        } catch (IllegalArgumentException e) {
            // Package disappeared between discovery and validation.
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && (activity.applicationInfo.flags & ApplicationInfo.FLAG_SUSPENDED) != 0) return false;
        return activity.permission == null
                || pm.checkPermission(activity.permission, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isEnabled(int setting, boolean manifestEnabled) {
        return setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && manifestEnabled);
    }

    private static Intent webIntent(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE);
    }

    /** Preserve the complete URL; blocklist normalization deliberately returns only a host. */
    public static String getWebUrl(String url) {
        if (url == null) return null;
        var candidate = url.trim();
        if (candidate.startsWith("//")) candidate = "https:" + candidate;
        else if (!candidate.matches("(?s)^[A-Za-z][A-Za-z0-9+.-]*://.*")) {
            if (candidate.startsWith("/")
                    || candidate.matches("^[A-Za-z][A-Za-z0-9+.-]*:(?![0-9]+(?:[/?#]|$)).*")) return null;
            candidate = "https://" + candidate;
        }
        try {
            var parsed = new URI(candidate);
            var scheme = parsed.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || parsed.getRawAuthority() == null
                    || (parsed.getHost() == null && URLBlocklist.normalize(candidate) == null)) return null;
            return scheme + candidate.substring(candidate.indexOf(':'));
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    /** No implicit retry: the caller owns the normal UI fallback on failure. */
    public static Result tryOpen(Activity activity, String url) {
        if (!ENABLED_PREF(activity).get()) return Result.DISABLED;
        var webUrl = getWebUrl(url);
        if (webUrl == null) return Result.UNSUPPORTED;
        var selected = PACKAGE_PREF(activity).get();
        try {
            if (selected.isEmpty() || selected.equals(activity.getPackageName())
                    || !hasBrowser(getBrowsers(activity), selected)) return Result.FAILED;
            var intent = webIntent(webUrl).setPackage(selected);
            for (var info : query(activity, intent)) {
                if (!selected.equals(info.activityInfo.packageName) || !isGeneralBrowser(activity, info)) continue;
                intent.setComponent(new ComponentName(selected, info.activityInfo.name));
                activity.startActivity(intent);
                return Result.OPENED;
            }
        } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException e) {
            // Browser removal/disablement and policy changes may race with discovery.
        }
        return Result.FAILED;
    }
}
