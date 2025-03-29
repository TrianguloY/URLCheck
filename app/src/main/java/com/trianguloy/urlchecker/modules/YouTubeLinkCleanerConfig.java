package com.trianguloy.urlchecker.modules;

import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.modules.list.YouTubeLinkCleaner;

/**
 * Configuration for the YouTube link cleaner module
 */
public class YouTubeLinkCleanerConfig extends AModuleConfig {

    private Switch enabled;
    private Switch verbose;
    private Switch auto;

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
        YouTubeLinkCleaner.ENABLED_PREF(getActivity()).attachToSwitch(enabled);
        YouTubeLinkCleaner.VERBOSE_PREF(getActivity()).attachToSwitch(verbose);
        YouTubeLinkCleaner.AUTO_PREF(getActivity()).attachToSwitch(auto);
    }
} 
