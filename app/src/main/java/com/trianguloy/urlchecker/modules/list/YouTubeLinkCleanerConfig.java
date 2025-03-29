package com.trianguloy.urlchecker.modules;

import com.trianguloy.urlchecker.activities.ModulesActivity;

/**
 * Configuration for the YouTube link cleaner module
 */
public class YouTubeLinkCleanerConfig extends AModuleConfig {

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
} 
