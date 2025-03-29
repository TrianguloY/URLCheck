package com.trianguloy.urlchecker.modules.list;

import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.GuiActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.views.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Module that cleans YouTube links by removing tracking parameters
 */
public class YouTubeLinkCleaner extends AModuleDialog {

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
    public void onPrepareDialog(GuiActivity activity, MainDialog dialog) {
        // No preparation needed
    }

    @Override
    public void onShowDialog(GuiActivity activity, MainDialog dialog) {
        // No UI needed
    }

    @Override
    public void onUrlSelected(UrlData urlData, MainDialog dialog) {
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
        }
    }
} 
