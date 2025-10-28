package com.trianguloy.urlchecker.flavors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.trianguloy.forceurl.lib.ForceUrl;
import com.trianguloy.forceurl.lib.Preferences;

public interface IncognitoDimension {
    final String mode_key = ForceUrl.KEY_INCOGNITO;
    static boolean isIncognito(Intent intent) {
        return ForceUrl.isMode(intent, mode_key);
    }

    static void showSettings(Activity activity) {
        Preferences.showSettings(activity);
    }

    static void applyAndLaunchHelper(Context context, Intent intent, String url, boolean state) {
        ForceUrl.applyAndLaunchHelper(context, intent, url, state, mode_key);
    }

    static boolean isServiceConnected(){
        return ForceUrl.isServiceConnected();
    }

    static boolean isServiceEnabled(Context context){
        return ForceUrl.isServiceEnabled(context);
    }
}
