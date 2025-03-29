package com.trianguloy.urlchecker.modules.list;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.GuiActivity;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Module that cleans YouTube links by removing tracking parameters
 */
public class YouTubeLinkCleaner extends AModuleData {

    public static GenericPref.Bool VERBOSE_PREF(Context cntx) {
        return new GenericPref.Bool("youtubeCleaner_verbose", false, cntx);
    }

    public static GenericPref.Bool AUTO_PREF(Context cntx) {
        return new GenericPref.Bool("youtubeCleaner_auto", false, cntx);
    }

    @Override
    public String getId() {
        return "youtubeCleaner";
    }

    @Override
    public int getName() {
        return R.string.mYoutubeCleaner_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new YouTubeLinkCleanerDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new YouTubeLinkCleanerConfig(cntx);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return Collections.emptyList(); // Return empty list instead of null
    }
} 
