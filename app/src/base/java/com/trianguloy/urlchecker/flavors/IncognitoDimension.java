package com.trianguloy.urlchecker.flavors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.modules.companions.OnOffConfig;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import java.util.List;

/**
 * This should only be called when flavor is incognito, we need to first check the build config
 */
public interface IncognitoDimension {
    final String errormsg = "Dummy method called. This method should ONLY be called when flavor is incognito";

    // These assertions are contradictions, this file is on base so that means IS_INCOGNITO is
    // ALWAYS false.

    static boolean isIncognito(Intent intent) {
        assert BuildConfig.IS_INCOGNITO : errormsg;
        return false;
    }

    static void showSettings(Activity activity) {
        assert BuildConfig.IS_INCOGNITO : errormsg;
    }

    static void applyAndLaunchHelper(Context context, Intent intent, String url, boolean state) {
        assert BuildConfig.IS_INCOGNITO : errormsg;
    }

    static boolean isServiceConnected(){
        assert BuildConfig.IS_INCOGNITO : errormsg;
        return false;
    }

    static boolean isServiceEnabled(Context context){
        assert BuildConfig.IS_INCOGNITO : errormsg;
        return false;
    }

    // NoSuchMethodException ?
    // UnsupportedOperationException ?
}
