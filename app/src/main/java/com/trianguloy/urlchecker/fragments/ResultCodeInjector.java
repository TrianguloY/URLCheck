package com.trianguloy.urlchecker.fragments;

import android.content.Intent;
import android.util.Log;

import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * An injector to allow registering for activityResult and requestPermissionsResult.
 * Alternative to using the deprecate android fragments (and without using the huge compatibility library)
 */
public class ResultCodeInjector {
    private static final int RESERVED = 123; // just in case, registered listeners will have requestCode >= this
    private final List<ActivityResultListener> activityResultListeners = new ArrayList<>();

    /* ------------------- client use ------------------- */

    public interface ActivityResultListener {
        /** Called when the event fires for a particular registrar */
        void onActivityResult(int resultCode, Intent data);
    }

    /** Call this to register an onActivityResult listener. Returns the requestCode you must use in the startActivityForResult call */
    public int registerActivityResult(ActivityResultListener activityResultListener) {
        activityResultListeners.add(activityResultListener);
        return RESERVED + activityResultListeners.size() - 1;
    }

    /* ------------------- activity use ------------------- */

    /**
     * An activity must use this as:
     * <pre>
     *     @ Override
     *     public void onActivityResult(int requestCode, int resultCode, Intent data) {
     *         if (!resultCodeInjector.onActivityResult(requestCode, resultCode, data))
     *             super.onActivityResult(requestCode, resultCode, data);
     *     }
     * </pre>
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        var index = requestCode - RESERVED;
        if (index < 0) {
            // external
            Log.d("ACTIVITY_RESULT", "External request code (" + requestCode + "), consider using ResultCodeInjector");
            return false;
        }
        if (index >= activityResultListeners.size()) {
            // external?
            AndroidUtils.assertError("Invalid request code (" + requestCode + "), consider using ResultCodeInjector or a requestCode less than " + RESERVED);
            return false;
        }
        activityResultListeners.get(index).onActivityResult(resultCode, data);
        return true;
    }

}
