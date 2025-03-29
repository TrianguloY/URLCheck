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

    public static GenericPref.Bool ENABLED_PREF(Context cntx) {
        return new GenericPref.Bool("youtubeCleaner_enabled", true, cntx);
    }

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

class YouTubeLinkCleanerConfig extends AModuleConfig {
    private final GenericPref.Bool enabled;
    private final GenericPref.Bool verbose;
    private final GenericPref.Bool auto;

    public YouTubeLinkCleanerConfig(ModulesActivity activity) {
        super(activity);
        enabled = YouTubeLinkCleaner.ENABLED_PREF(activity);
        verbose = YouTubeLinkCleaner.VERBOSE_PREF(activity);
        auto = YouTubeLinkCleaner.AUTO_PREF(activity);
    }

    public YouTubeLinkCleanerConfig(Context context) {
        super();
        enabled = YouTubeLinkCleaner.ENABLED_PREF(context);
        verbose = YouTubeLinkCleaner.VERBOSE_PREF(context);
        auto = YouTubeLinkCleaner.AUTO_PREF(context);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_youtube_cleaner;
    }

    @Override
    public void onInitialize(View views) {
        enabled.attachToSwitch(views.findViewById(R.id.enabled));
        verbose.attachToSwitch(views.findViewById(R.id.verbose));
        auto.attachToSwitch(views.findViewById(R.id.auto));
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public boolean isVerbose() {
        return verbose.get();
    }

    public boolean isAuto() {
        return auto.get();
    }
}

class YouTubeLinkCleanerDialog extends AModuleDialog {
    static final List<AutomationRules.Automation<YouTubeLinkCleanerDialog>> AUTOMATIONS = List.of(
            // this automation doesn't work yet, as it runs the onNewUrl inside the main loop
//            new AutomationRules.Automation<>("clean", R.string.auto_clean, YouTubeLinkCleanerDialog::clean)
    );

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

    private final YouTubeLinkCleanerConfig config;
    private TextView info;
    private Button clean;
    private String originalUrl;

    public YouTubeLinkCleanerDialog(MainDialog dialog) {
        super(dialog);
        config = new YouTubeLinkCleanerConfig(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.button_text;
    }

    @Override
    public void onInitialize(View views) {
        info = views.findViewById(R.id.text);
        clean = views.findViewById(R.id.button);
        clean.setText(R.string.mYoutubeCleaner_clean);
        clean.setOnClickListener(v -> clean());
    }

    private void clean() {
        if (originalUrl != null) {
            try {
                Uri uri = Uri.parse(originalUrl);
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
                UrlData urlData = new UrlData();
                urlData.url = builder.build().toString();
                dialog.onNewUrl(urlData);
            } catch (Exception e) {
                if (info != null) {
                    info.setText(R.string.mYoutubeCleaner_error);
                }
            }
        }
    }

    @Override
    public void onModifyUrl(UrlData urlData, JavaUtils.Function<UrlData, Boolean> setNewUrl) {
        if (!config.isEnabled()) return;

        try {
            String url = urlData.url;
            Uri uri = Uri.parse(url);
            
            // Check if it's a YouTube URL
            if (YOUTUBE_DOMAINS.contains(uri.getHost().toLowerCase())) {
                originalUrl = url; // Store original URL for comparison
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
                
                // Update UI to show it was cleaned
                if (info != null) {
                    if (!urlData.url.equals(originalUrl)) {
                        info.setText(R.string.mYoutubeCleaner_desc);
                    } else {
                        info.setText(R.string.mYoutubeCleaner_noChange);
                    }
                }
            } else {
                // Not a YouTube URL
                if (info != null) {
                    info.setText(R.string.mYoutubeCleaner_notYoutube);
                }
            }
        } catch (Exception e) {
            // Handle any errors gracefully
            if (info != null) {
                info.setText(R.string.mYoutubeCleaner_error);
            }
        }
    }
} 
