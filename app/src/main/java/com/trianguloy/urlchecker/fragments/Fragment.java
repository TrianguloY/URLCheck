package com.trianguloy.urlchecker.fragments;

import android.view.View;

/**
 * Android fragments are deprecated?
 * I need to use that ugly huge compatibility library?
 * No way, I'll do my own.
 */
public interface Fragment {

    /** @return the layout resource of this module */
    int getLayoutId();

    /**
     * Initializes this module from the given views (generated from {@link #getLayoutId()})
     *
     * @param views the inflated views
     */
    void onInitialize(View views);
}
