package com.trianguloy.urlchecker.modules.list;

import static com.trianguloy.urlchecker.utilities.methods.AndroidUtils.MARKER;

import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Module to Unshort links by using https://unshorten.me/
 * Consider adding other services, or even allow custom
 * TODO: the redirect logic here and in the Status check module is very similar, consider extracting a common wrapper
 */
public class UnshortenModule extends AModuleData {

    @Override
    public String getId() {
        return "unshorten";
    }

    @Override
    public int getName() {
        return R.string.mUnshort_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new UnshortenDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new DescriptionConfig(R.string.mUnshort_desc);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) UnshortenDialog.AUTOMATIONS;
    }
}

class UnshortenDialog extends AModuleDialog {

    static List<AutomationRules.Automation<UnshortenDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>("unshort", R.string.auto_unshort, dialog ->
                    dialog.unshort(dialog.getUrlData().disableUpdates))
    );

    private Button unshort;
    private TextView info;

    private Thread thread = null;

    public UnshortenDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.button_text;
    }

    @Override
    public void onInitialize(View views) {
        unshort = views.findViewById(R.id.button);
        unshort.setText(R.string.mUnshort_unshort);
        unshort.setOnClickListener(v -> unshort(false));

        info = views.findViewById(R.id.text);
        info.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onPrepareUrl(UrlData urlData) {
        // cancel previous check if pending
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        // reset all
        unshort.setEnabled(true);
        info.setText("");
        AndroidUtils.clearRoundedColor(info);
    }

    /** Unshorts the current url */
    private void unshort(boolean disableUpdates) {
        // disable button and run in background
        unshort.setEnabled(false);
        info.setText(R.string.mUnshort_checking);
        AndroidUtils.clearRoundedColor(info);

        thread = new Thread(() -> _check(disableUpdates));
        thread.start();
    }

    private void _check(boolean disableUpdates) {

        try {
            // get response
            var response = new JSONObject(HttpUtils.readFromUrl("https://unshorten.me/json/" + getUrl()));
            var resolved_url = response.getString("resolved_url");
            var usage_count = Integer.parseInt(response.optString("usage_count", "0"));
            var ref = new Object() { // reference object to allow using these inside lambdas
                int usage_limit = 10; // documented but hardcoded
                int remaining_calls = usage_limit - usage_count;
            };
            var error = response.optString("error", "(no reported error)");
            var success = response.getBoolean("success");

            // remaining_calls is not documented, but if it's present use it and replace the hardcoded usage_limit
            try {
                ref.remaining_calls = Integer.parseInt(response.optString("remaining_calls", ""));
                ref.usage_limit = usage_count + ref.remaining_calls;
            } catch (NumberFormatException ignore) {
                // not present, ignore
            }

            // exit if was canceled
            if (Thread.currentThread().isInterrupted()) {
                Log.d("THREAD", "Interrupted");
                return;
            }

            if (!success) {
                // server error, maybe too many checks
                getActivity().runOnUiThread(() -> {
                    info.setText(getActivity().getString(R.string.mUnshort_error, error));
                    AndroidUtils.setRoundedColor(R.color.warning, info);
                    // allow retries
                    unshort.setEnabled(true);
                });
            } else if (Objects.equals(resolved_url, getUrl())) {
                // same, nothing to replace
                getActivity().runOnUiThread(() -> {
                    info.setText(getActivity().getString(R.string.mUnshort_notFound));
                    if (ref.remaining_calls <= ref.usage_limit / 2)
                        info.append(" (" + getActivity().getString(R.string.mUnshort_pending, ref.remaining_calls, ref.usage_limit) + ")");
                    AndroidUtils.clearRoundedColor(info);
                });
            } else {
                // correct, replace
                getActivity().runOnUiThread(() -> {

                    if (!disableUpdates) {
                        // unshort to new url
                        unshortTo(resolved_url);
                    } else {
                        // show unshorted url
                        info.setText(AndroidUtils.underlineUrl(getActivity().getString(R.string.mUnshort_to, MARKER), resolved_url, this::unshortTo));
                    }

                    if (ref.remaining_calls <= ref.usage_limit / 2)
                        info.append(" (" + getActivity().getString(R.string.mUnshort_pending, ref.remaining_calls, ref.usage_limit) + ")"
                        );

                    // a short url can be unshorted to another short url
                    unshort.setEnabled(true);
                });
            }

        } catch (IOException | JSONException e) {
            // internal error
            e.printStackTrace();

            // exit if was canceled
            if (Thread.currentThread().isInterrupted()) {
                Log.d("THREAD", "Interrupted");
                return;
            }

            getActivity().runOnUiThread(() -> {
                info.setText(getActivity().getString(R.string.mUnshort_internal, e.getMessage()));
                AndroidUtils.setRoundedColor(R.color.bad, info);
                unshort.setEnabled(true);
            });
        }

    }

    /** Unshorts to another url */
    private void unshortTo(String url) {
        setUrl(new UrlData(url).dontTriggerOwn());
        info.setText(getActivity().getString(R.string.mUnshort_ok));
        AndroidUtils.setRoundedColor(R.color.good, info);
    }

}
