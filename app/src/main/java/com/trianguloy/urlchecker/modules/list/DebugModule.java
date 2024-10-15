package com.trianguloy.urlchecker.modules.list;

import static java.util.Objects.requireNonNullElse;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.services.CustomTabs;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.UrlUtils;
import com.trianguloy.urlchecker.utilities.wrappers.IntentApp;

import java.util.List;

/**
 * This modules marks the insertion point of new modules
 * If enabled, shows a textview with debug info.
 * Allows also to enable/disable ctabs toasts
 */
public class DebugModule extends AModuleData {
    public static String ID = "debug";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getName() {
        return R.string.mD_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new DebugDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new DebugConfig(cntx);
    }
}

class DebugDialog extends AModuleDialog {

    public static final String SEPARATOR = "";
    private TextView data;
    private UrlData urlData;

    public DebugDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_debug;
    }

    @Override
    public void onInitialize(View views) {
        data = views.findViewById(R.id.data);
        views.findViewById(R.id.showData).setOnClickListener(v -> showData());
    }

    private void showData() {
        data.setVisibility(View.VISIBLE);
        // data to display
        data.setText(String.join("\n", List.of(
                "Intent:",
                getActivity().getIntent().toUri(0),

                SEPARATOR,

                "queryIntentActivities:",
                IntentApp.getOtherPackages(UrlUtils.getViewIntent(urlData.url, null), getActivity()).toString(),

                SEPARATOR,

                "queryIntentActivityOptions:",
                getActivity().getPackageManager().queryIntentActivityOptions(
                        new ComponentName(getActivity(), MainDialog.class.getName()),
                        null,
                        UrlUtils.getViewIntent(urlData.url, null),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PackageManager.MATCH_ALL : 0
                ).toString(),

                SEPARATOR,

                "UrlData:",
                urlData.toString(),

                SEPARATOR,

                "GlobalData:",
                getGlobalData().toString(),

                SEPARATOR,

                "Referrer:",
                requireNonNullElse(AndroidUtils.getReferrer(getActivity()), "null")
        )));
    }

    @Override
    public void onPrepareUrl(UrlData urlData) {
        data.setVisibility(View.GONE);
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        this.urlData = urlData;
    }
}

class DebugConfig extends AModuleConfig {


    public DebugConfig(ModulesActivity activity) {
        super(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_debug;
    }

    @Override
    public void onInitialize(View views) {
        CustomTabs.SHOWTOAST_PREF(getActivity())
                .attachToSwitch(views.findViewById(R.id.chk_ctabs));
    }
}
