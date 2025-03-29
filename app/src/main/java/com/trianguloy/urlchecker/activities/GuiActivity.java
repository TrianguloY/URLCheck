package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.os.Bundle;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

/**
 * Base activity with common GUI functionality
 */
public class GuiActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidSettings.setTheme(this, false);
    }
} 
