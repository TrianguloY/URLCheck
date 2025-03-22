package com.trianguloy.forceurl.lib;

import android.content.Context;
import android.content.Intent;

import com.trianguloy.forceurl.data.Apps;
import com.trianguloy.forceurl.helpers.Helpers;
import com.trianguloy.forceurl.data.incognito.forks.Chromium;
import com.trianguloy.forceurl.data.incognito.forks.Fenix;
import com.trianguloy.forceurl.services.UrlHelperService;
import com.trianguloy.forceurl.utilities.methods.AndroidUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ForceUrl {

    Map<String, Map<String, Map<String, Apps>>> data = buildData(); // mode.group.app

    // keep the keys exactly as the packages
    // --- mode ---
    String KEY_INCOGNITO = "incognito";

    // --- group ---
    String KEY_STANDALONE = "standalone";
    String KEY_FORKS = "forks";
    // String keys in the order they must be iterated
    List<String> groupOrder = new ArrayList<>(Arrays.asList(KEY_STANDALONE, KEY_FORKS));

    private static Map<String, Map<String, Map<String, Apps>>> buildData() {
        // TODO: auto-load with reflection?

        // data
        Map<String, Map<String, Map<String, Apps>>> data = new HashMap<>();

        // --------
        // - INCOGNITO
        Map<String, Map<String, Apps>> incognito = new HashMap<>();
        data.put(KEY_INCOGNITO, incognito);
        {
            // -- individual
            // code
        }
        {
            // -- forks
            Map<String, Apps> forks = new HashMap<>();
            incognito.put(KEY_FORKS, forks);
            // --- Chromium
            forks.put(Chromium.class.getSimpleName(),new Chromium());
            // --- Fenix
            forks.put(Fenix.class.getSimpleName(), new Fenix());
        }
        // --------

        return data;
    }

    static Map<String, Map<String, Map<String, Apps>>> getData() {
        return data;
    }

    static Apps getApps(String id) {
        Apps apps = null;

        var idPath = id.split("\\.");
        var mode = data.get(idPath[idPath.length - 3]);
        if (mode != null){
            var group = mode.get(idPath[idPath.length - 2]);
            if (group != null){
                apps = group.get(idPath[idPath.length - 1]);
            }
        }

        return apps;
    }

    static boolean modeExists(String mode){
        return data.get(mode) != null;
    }

    /**
     * @return Return the key for the dictionaries if the app can be opened in mode, null if not
     */
    static String findId(Context context, String pckg, String mode) {
        var dataMode = data.get(mode);

        for (var appsKey : ForceUrl.groupOrder) {
            var group = dataMode.get(appsKey);
            if (group != null) {
                for (var entry : group.values()) {
                    if (entry.isThis(context, pckg)) {
                        return Apps.getId(entry);
                    }
                }
            }

        }
        return null;
    }

    /**
     * @return Extras related to opening any app in a certain mode
     */
    static Set<String> getModeExtras(String mode){
        var dataMode = data.get(mode);

        var extras = new HashSet<String>();
        for (var group : dataMode.values()) {
            for (var apps : group.values()) {
                extras.addAll(apps.getExtras());
            }
        }
        return extras;
    }

    /**
     * @return If an intent will launch mode for a given intent/app, only checks extras
     */
    static boolean isMode(Intent intent, String mode){
        boolean res = false;
        // Find any extra
        for (String extra : getModeExtras(mode)) {
            res = res | intent.getBooleanExtra(extra, false);
        }

        return res;
    }

    /**
     * Cleans the intent before applying mode
     */
    static void removeMode(Intent intent, String mode){
        // Mimics the matching in isMode
        for (String extra : getModeExtras(mode)) {
            // remove all incognito extras
            intent.removeExtra(extra);
        }
    }

    static void applyAndLaunchHelper(Context context, Intent intent, String url, boolean apply, String mode){
        var pckg = AndroidUtils.getPackage(intent);
        var id = findId(context, pckg, mode);
        if (id == null) return;
        // Package can be opened in mode

        removeMode(intent, mode); // clear intent

        if (apply){
            var app = getApps(id);
            var urlHelper = Preferences.CURRENT_PREF(context).get();
            // If url needs help but there is no helper, do not apply mode
            if (urlHelper == Helpers.none && Apps.needsHelp(app)){
                // Do nothing
            } else {
                // Apply mode
                app.transform(intent);
                if (Apps.needsHelp(app)){
                    urlHelper.getHelper().run(context, url, pckg, app);
                }
            }
        }
    }

    static boolean isServiceConnected(){
        return UrlHelperService.isConnected();
    }

    static boolean isServiceEnabled(Context context){
        return UrlHelperService.isEnabled(context);
    }
}
