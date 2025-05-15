package com.trianguloy.urlchecker.modules;

import android.app.Activity;

import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.fragments.Fragment;

/** Base class for a module configuration fragment */
public abstract class AModuleConfig implements Fragment {

    // ------------------- private data -------------------

    private final ModulesActivity activity;

    // ------------------- initialization -------------------

    public AModuleConfig() {
        this.activity = null;
    }

    public AModuleConfig(ModulesActivity activity) {
        this.activity = activity;
    }

    // ------------------- abstract functions -------------------

    /**
     * returns -1 if the module can be enabled, or the resource id of a string describing why it can't
     * -1 (can be enabled) by default
     */
    public int cannotEnableErrorId() {
        return -1;
    }

    // ------------------- utilities -------------------

    /** Disables this module */
    public final void disable() {
        if (activity != null) activity.disableModule(this);
    }

    /** Returns the config activity. Will be null when initialized with empty constructor */
    public final Activity getActivity() {
        return activity;
    }

}
