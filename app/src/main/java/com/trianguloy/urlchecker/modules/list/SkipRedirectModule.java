package com.trianguloy.urlchecker.modules.list;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.companions.SkipRedirectUtility;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Function;

/**
 * This module skips redirections embedded in the url parameters
 * (like https://tracker.com/?url=https%3A%2F%2Fexample.com), offline.
 * Supports url-encoded, base64 and json values.
 */
public class SkipRedirectModule extends AModuleData {

    public static BoolPref AUTO_PREF(Context cntx) {
        return new BoolPref("skipRedirect_auto", false, cntx);
    }

    @Override
    public String getId() {
        return "skipRedirect";
    }

    @Override
    public int getName() {
        return R.string.mSkip_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new SkipRedirectDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new SkipRedirectConfig(cntx);
    }
}

class SkipRedirectConfig extends AModuleConfig {

    private final BoolPref auto;

    public SkipRedirectConfig(ModulesActivity activity) {
        super(activity);
        auto = SkipRedirectModule.AUTO_PREF(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_skipredirect;
    }

    @Override
    public void onInitialize(View views) {
        auto.attachToSwitch(views.findViewById(R.id.auto));
    }
}

class SkipRedirectDialog extends AModuleDialog {

    public static final String SKIPPED = "skipRedirect.skipped";

    private final BoolPref auto;

    private TextView info;
    private Button skip;

    private String target = null;
    private int steps = 0;

    public SkipRedirectDialog(MainDialog dialog) {
        super(dialog);
        auto = SkipRedirectModule.AUTO_PREF(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.button_text;
    }

    @Override
    public void onInitialize(View views) {
        info = views.findViewById(R.id.text);
        skip = views.findViewById(R.id.button);
        skip.setText(R.string.mSkip_skip);
        skip.setOnClickListener(v -> {
            if (target != null) setUrl(new UrlData(target).putData(SKIPPED, String.valueOf(steps)));
        });
    }

    @Override
    public void onModifyUrl(UrlData urlData, Function<UrlData, Boolean> setNewUrl) {
        target = null;
        steps = 0;

        var result = SkipRedirectUtility.skip(urlData.url);
        if (!result.changed(urlData.url)) return;

        target = result.url;
        steps = result.steps.size();

        // apply automatically if required
        if (auto.get() && setNewUrl.apply(new UrlData(target).putData(SKIPPED, String.valueOf(steps)))) return;
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        if (target != null) {
            // redirection found
            info.setText(steps == 1
                    ? getActivity().getString(R.string.mSkip_found1)
                    : getActivity().getString(R.string.mSkip_found, steps));
            skip.setEnabled(true);
            AndroidUtils.setRoundedColor(R.color.warning, info);
            setVisibility(true);
        } else if (urlData.getData(SKIPPED) != null) {
            // already skipped, keep showing how many were skipped
            var skippedCount = parseCount(urlData.getData(SKIPPED));
            info.setText(skippedCount == 1
                    ? getActivity().getString(R.string.mSkip_skipped1)
                    : getActivity().getString(R.string.mSkip_skipped, skippedCount));
            skip.setEnabled(false);
            AndroidUtils.setRoundedColor(R.color.good, info);
            setVisibility(true);
        } else {
            // nothing found
            info.setText(R.string.mSkip_none);
            skip.setEnabled(false);
            AndroidUtils.clearRoundedColor(info);
            setVisibility(false);
        }
    }

    private static int parseCount(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
