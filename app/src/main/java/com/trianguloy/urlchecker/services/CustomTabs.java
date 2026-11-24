package com.trianguloy.urlchecker.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
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
        return new CustomTabsBinder();
    }

    // ------------------- binder -------------------

    private static final String DESCRIPTOR = "android.support.customtabs.ICustomTabsService";

    // ids from ICustomTabsService.aidl (transaction code = FIRST_CALL_TRANSACTION + id)
    private static final int WARMUP = 1;
    private static final int NEW_SESSION = 2;
    private static final int MAY_LAUNCH_URL = 3;
    private static final int UPDATE_VISUALS = 5;
    private static final int NEW_SESSION_WITH_EXTRAS = 9;

    /**
     * Answers the custom tabs protocol so clients accept this app as provider (instead of falling back to another browser).
     * Sessions, warmup and prefetch hints are accepted and ignored; everything else is reported as unsupported.
     */
    private static class CustomTabsBinder extends Binder {
        CustomTabsBinder() {
            attachInterface(null, DESCRIPTOR);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code < FIRST_CALL_TRANSACTION || code > LAST_CALL_TRANSACTION) {
                return super.onTransact(code, data, reply, flags);
            }
            data.enforceInterface(DESCRIPTOR);
            var id = code - FIRST_CALL_TRANSACTION;
            Log.d(TAG, "transaction " + id); // binder thread, no toast here
            if (reply != null) {
                reply.writeNoException();
                switch (id) {
                    case WARMUP:
                    case NEW_SESSION:
                    case MAY_LAUNCH_URL:
                    case UPDATE_VISUALS:
                    case NEW_SESSION_WITH_EXTRAS:
                        reply.writeInt(1);
                        break;
                    default:
                        // false, null Bundle, or 0
                        reply.writeInt(0);
                }
            }
            return true;
        }
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
