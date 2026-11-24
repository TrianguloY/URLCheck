package com.trianguloy.urlchecker.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;

/**
 * Empty service for fake custom tabs.
 * <p>
 * referrer: https://chromium.googlesource.com/chromium/src/+/b71e98cdf14f18cb967a73857826f6e8c568cea0/chrome/android/java/src/org/chromium/chrome/browser/customtabs/CustomTabsConnectionService.java
 */
public class CustomTabs extends Service {

    public static BoolPref SHOWTOAST_PREF(Context cntx) {
        return new BoolPref("ctabs_toast", false, cntx);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand\n" + intent.toUri(0));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("onBind\n" + intent.toUri(0)); // a toast here, for some reason, isn't shown and later it crashes
        return new Binder();
    }

    // ------------------- logging -------------------

    private static final String TAG = "CUSTOMTABS";

    private void log(String message) {
        Log.d(TAG, message);
        if (SHOWTOAST_PREF(this).get()) {
            Toast.makeText(this, TAG + ": " + message, Toast.LENGTH_LONG).show();
        }
    }
}
