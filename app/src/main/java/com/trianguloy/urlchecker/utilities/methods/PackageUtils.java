package com.trianguloy.urlchecker.utilities.methods;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Static utilities related to packages */
public interface PackageUtils {

    /** Wrapper for {@link Context#startActivity(Intent)} to catch thrown exceptions and show a toast instead */
    static void startActivity(Intent intent, int toastError, Context cntx) {
        try {
            cntx.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(cntx, toastError, Toast.LENGTH_SHORT).show();
        }
    }

    /** Wrapper for {@link Activity#startActivityForResult(Intent, int)} to catch thrown exceptions and show a toast instead */
    static void startActivityForResult(Intent intent, int requestCode, int toastError, Activity cntx) {
        try {
            cntx.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Toast.makeText(cntx, toastError, Toast.LENGTH_SHORT).show();
        }
    }
}
