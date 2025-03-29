package com.trianguloy.urlchecker.modules.list;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.GuiActivity;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.url.UrlData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Module that cleans YouTube links by removing tracking parameters
 */
public class YouTubeLinkCleaner extends AModuleData {

    @Override
    public String getId() {
        return "youtubeCleaner";
    }

    @Override
    public int getName() {
        return R.string.mYoutubeCleaner_name;
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
        return null; // No automations needed
    }
}

class YouTubeLinkCleanerConfig extends AModuleConfig {
    public YouTubeLinkCleanerConfig(ModulesActivity activity) {
        super(activity);
    }

    @Override
    public int getLayoutId() {
        return 0; // No layout needed
    }

    @Override
    public void onInitialize(View views) {
        // No initialization needed
    }
}

class YouTubeLinkCleanerDialog extends AModuleDialog {
    // YouTube domains
    private static final Set<String> YOUTUBE_DOMAINS = new HashSet<>(Arrays.asList(
            "youtube.com", "www.youtube.com", "youtu.be"
    ));

    // Tracking parameters to remove
    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            "si", "feature", "utm_source", "utm_medium", "utm_campaign",
            "utm_term", "utm_content", "fbclid", "ref", "mc_cid",
            "mc_eid", "_ga", "gclid", "msclkid", "dclid"
    ));

    public YouTubeLinkCleanerDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return 0; // No layout needed
    }

    @Override
    public void onInitialize(View views) {
        // No initialization needed
    }

    @Override
    public void onModifyUrl(UrlData urlData, JavaUtils.Function<UrlData, Boolean> setNewUrl) {
        String url = urlData.url;
        Uri uri = Uri.parse(url);
        
        // Check if it's a YouTube URL
        if (YOUTUBE_DOMAINS.contains(uri.getHost().toLowerCase())) {
            Uri.Builder builder = uri.buildUpon();
            builder.clearQuery();
            
            // Keep only essential parameters
            for (String param : uri.getQueryParameterNames()) {
                if (!TRACKING_PARAMS.contains(param.toLowerCase())) {
                    String value = uri.getQueryParameter(param);
                    if (value != null) {
                        builder.appendQueryParameter(param, value);
                    }
                }
            }
            
            // Update the URL with cleaned version
            urlData.url = builder.build().toString();
            setNewUrl.apply(urlData);
        }
    }
} 
