package com.trianguloy.urlchecker.modules.list;

import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class YouTubeLinkCleanerDialog extends AModuleDialog {
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
                String cleanedUrl = builder.build().toString();
                setUrl(cleanedUrl);
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
                
                // Create new UrlData with cleaned URL
                String cleanedUrl = builder.build().toString();
                UrlData newUrlData = new UrlData(cleanedUrl);
                newUrlData.mergeData(urlData); // Preserve any additional data from original
                setNewUrl.apply(newUrlData);
                
                // Update UI to show it was cleaned
                if (info != null) {
                    if (!cleanedUrl.equals(originalUrl)) {
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
