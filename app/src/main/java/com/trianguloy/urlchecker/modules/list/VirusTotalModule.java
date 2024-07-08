package com.trianguloy.urlchecker.modules.list;

import static com.trianguloy.urlchecker.utilities.methods.AndroidUtils.getStringWithPlaceholder;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.modules.companions.VirusTotalUtility;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.wrappers.DefaultTextWatcher;

import java.util.List;

/** This module uses the VirusTotal api (https://developers.virustotal.com/reference) for url reports */
public class VirusTotalModule extends AModuleData {

    public static final String PREF = "api_key";

    static GenericPref.Str API_PREF(Context cntx) {
        return new GenericPref.Str(PREF, "", cntx);
    }

    @Override
    public String getId() {
        return "virustotal";
    }

    @Override
    public int getName() {
        return R.string.mVT_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new VirusTotalDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new VirusTotalConfig(cntx);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) VirusTotalDialog.AUTOMATIONS;
    }
}

class VirusTotalConfig extends AModuleConfig {

    final GenericPref.Str api_key;

    public VirusTotalConfig(ModulesActivity cntx) {
        super(cntx);
        api_key = VirusTotalModule.API_PREF(cntx);
    }

    @Override
    public int cannotEnableErrorId() {
        final String key = api_key.get();
        return key != null && !key.isEmpty() ? -1 : R.string.mVT_noKey;
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_virustotal;
    }

    @Override
    public void onInitialize(View views) {
        var edit_key = views.<TextView>findViewById(R.id.api_key);
        var test = views.<Button>findViewById(R.id.test);
        var result = views.<TextView>findViewById(R.id.result);
        var testing = views.<ProgressBar>findViewById(R.id.testing);

        // init output
        testing.setVisibility(View.GONE);

        // set and configure input
        edit_key.addTextChangedListener(new DefaultTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                api_key.set(s.toString());
                if (cannotEnableErrorId() != -1) {
                    disable();
                }

                test.setEnabled(s.length() != 0);
                result.setText("");
            }
        });
        edit_key.setText(api_key.get());

        // set onclick
        test.setOnClickListener(v -> {
            // mark as testing
            testing.setVisibility(View.VISIBLE);
            test.setEnabled(false);
            edit_key.setEnabled(false);
            result.setText("");
            new Thread(() -> {
                // check
                var user = VirusTotalUtility.getUser(api_key.get());
                getActivity().runOnUiThread(() -> {
                    // display result
                    testing.setVisibility(View.GONE);
                    test.setEnabled(true);
                    edit_key.setEnabled(true);
                    if (user == null) result.setText(R.string.mVT_error);
                    else result.setText(getActivity().getString(R.string.mVT_validKey, user));
                });
            }).start();
        });

        views.<TextView>findViewById(R.id.label).setText(getStringWithPlaceholder(getActivity(), R.string.mVT_desc, R.string.vtLogin_url));
    }
}

class VirusTotalDialog extends AModuleDialog {

    static final List<AutomationRules.Automation<VirusTotalDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>("scan", R.string.auto_scan, VirusTotalDialog::scanOrCancel)
    );

    private static final int RETRY_TIMEOUT = 5000;
    private Button btn_scan;
    private TextView txt_result;

    private boolean scanning = false;
    private VirusTotalUtility.InternalResponse result = null;

    private final GenericPref.Str api_key;

    public VirusTotalDialog(MainDialog dialog) {
        super(dialog);
        api_key = VirusTotalModule.API_PREF(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.button_text;
    }

    @Override
    public void onInitialize(View views) {
        btn_scan = views.findViewById(R.id.button);
        btn_scan.setText(R.string.mVT_scan);
        btn_scan.setOnClickListener(v -> scanOrCancel());

        txt_result = views.findViewById(R.id.text);
        txt_result.setOnClickListener(v -> showInfo(false));
        txt_result.setOnLongClickListener(v -> {
            showInfo(true);
            return true;
        });
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        // TODO: cancel on onPrepare
        scanning = false;
        result = null;
        updateUI();
    }

    /**
     * Performs a scan of the current url, in background.
     * Or cancels current scan
     */
    private void scanOrCancel() {
        if (scanning) {
            // already scanning? cancel
            scanning = false;
        } else {
            // start scan
            scanning = true;
            new Thread(this::_scanUrl).start();
        }
        updateUI();
    }

    /** Manages the scanning in the background */
    private void _scanUrl() {
        VirusTotalUtility.InternalResponse response;
        while (scanning) {
            // asks for the report
            response = VirusTotalUtility.scanUrl(getUrl(), api_key.get(), getActivity());

            // check valid report
            if (response != null) {
                result = response;
                scanning = false;
                getActivity().runOnUiThread(this::updateUI);
                return;
            }

            // retry if still no report
            try {
                Thread.sleep(RETRY_TIMEOUT);
            } catch (InterruptedException e) {
                AndroidUtils.assertError("You may not rest now, there are monsters nearby", e);
            }
        }

    }

    /** Updates the ui */
    private void updateUI() {
        if (scanning) {
            // scanning in progress, show cancel
            btn_scan.setText(R.string.mVT_cancel);
            setResult(getActivity().getString(R.string.mVT_scanning), 0);
            btn_scan.setEnabled(true);
        } else {
            // not a scanning in progress
            btn_scan.setText(R.string.mVT_scan);
            if (result == null) {
                // no result available, new url
                setResult("", 0);
                btn_scan.setEnabled(true);
            } else {
                // result available
                if (result.error != null) {
                    // an error ocurred
                    setResult(result.error, Color.TRANSPARENT);
                    btn_scan.setEnabled(true);
                } else {
                    // valid result
                    btn_scan.setEnabled(false);
                    if (result.detectionsPositive > 2) {
                        // more that two bad detection, bad url
                        setResult(getActivity().getString(R.string.mVT_badUrl, result.detectionsPositive, result.detectionsTotal, result.date), R.color.bad);
                    } else if (result.detectionsPositive > 0) {
                        // 1 or 2 bad detections, warning
                        setResult(getActivity().getString(R.string.mVT_warningUrl, result.detectionsPositive, result.detectionsTotal, result.date), R.color.warning);
                    } else {
                        // no detections, good
                        setResult(getActivity().getString(R.string.mVT_goodUrl, result.detectionsTotal, result.date), R.color.good);
                    }
                }
            }
        }

    }

    /**
     * Utility to update the ui
     *
     * @param message with this message
     * @param color   and this background color
     */
    private void setResult(String message, int color) {
        txt_result.setText(message);
        if (color != 0) {
            AndroidUtils.setRoundedColor(color, txt_result);
        } else {
            AndroidUtils.clearRoundedColor(txt_result);
        }
    }

    /**
     * Shows the report results
     *
     * @param details if true, the virustotal page is opened, if false just a basic dialog with the json
     */
    private void showInfo(boolean details) {
        if (result == null || result.error != null) return;

        if (details) {
            setUrl(result.scanUrl);
//            UrlUtils.openUrlRemoveThis(result.scanUrl, getActivity());
        } else {
            // TODO: beautify this
            new AlertDialog.Builder(getActivity())
                    .setMessage(result.info)
                    .show();
        }
    }
}
