package com.trianguloy.urlchecker.modules;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.modules.list.YouTubeLinkCleaner;

/**
 * Configuration for the YouTube link cleaner module
 */
public class YouTubeLinkCleanerConfig extends AModuleConfig {

    private CheckBox enabled;
    private CheckBox verbose;
    private CheckBox auto;

    public YouTubeLinkCleanerConfig() {
        super();
    }

    public YouTubeLinkCleanerConfig(ModulesActivity activity) {
        super(activity);
    }

    @Override
    public int cannotEnableErrorId() {
        return -1; // Module can always be enabled
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_youtube_cleaner;
    }

    @Override
    public void onInitialize(View views) {
        enabled = views.findViewById(R.id.enabled);
        verbose = views.findViewById(R.id.verbose);
        auto = views.findViewById(R.id.auto);

        // Attach preferences to UI elements
        YouTubeLinkCleaner.ENABLED_PREF(getActivity()).attach(enabled);
        YouTubeLinkCleaner.VERBOSE_PREF(getActivity()).attach(verbose);
        YouTubeLinkCleaner.AUTO_PREF(getActivity()).attach(auto);
    }
} 
