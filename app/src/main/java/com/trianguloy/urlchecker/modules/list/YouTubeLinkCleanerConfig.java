package com.trianguloy.urlchecker.modules.list;

import android.content.Context;
import android.view.View;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;

public class YouTubeLinkCleanerConfig extends AModuleConfig {
    private final GenericPref.Bool verbose;
    private final GenericPref.Bool auto;

    public YouTubeLinkCleanerConfig(ModulesActivity activity) {
        super(activity);
        verbose = YouTubeLinkCleaner.VERBOSE_PREF(activity);
        auto = YouTubeLinkCleaner.AUTO_PREF(activity);
    }

    public YouTubeLinkCleanerConfig(Context context) {
        super();
        verbose = YouTubeLinkCleaner.VERBOSE_PREF(context);
        auto = YouTubeLinkCleaner.AUTO_PREF(context);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_youtube_cleaner;
    }

    @Override
    public void onInitialize(View views) {
        verbose.attachToSwitch(views.findViewById(R.id.verbose));
        auto.attachToSwitch(views.findViewById(R.id.auto));
    }

    public boolean isVerbose() {
        return verbose.get();
    }

    public boolean isAuto() {
        return auto.get();
    }
} 
