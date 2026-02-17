package com.trianguloy.urlchecker.modules.list;

import static com.trianguloy.urlchecker.utilities.methods.AndroidUtils.MARKER;

import android.app.AlertDialog;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.StringPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * A module that checks the page status code by performing a GET request
 * Allows checking for redirection
 */
public class StatusModule extends AModuleData {

    public static BoolPref AUTOREDIR_PREF(Context cntx) {
        return new BoolPref("statusCode_autoRedir", false, cntx);
    }

    public static StringPref USERAGENT_PREF(Context cntx) {
        return new StringPref("statusCode_useragent", "", cntx);
    }

    @Override
    public String getId() {
        return "statusCode";
    }

    @Override
    public int getName() {
        return R.string.mStatus_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new StatusDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new StatusConfig(cntx);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) StatusDialog.AUTOMATIONS;
    }
}

class StatusConfig extends AModuleConfig {
    private static final List<Pair<Integer, String>> USERAGENTS = List.of(
            Pair.create(R.string.mStatus_ua_android, "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.3"),
            Pair.create(R.string.mStatus_ua_windows, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.3"),
            Pair.create(R.string.mStatus_ua_mac, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.10 Safari/605.1.1"),
            Pair.create(R.string.mStatus_ua_iphone, "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3.1 Mobile/15E148 Safari/604."),
            Pair.create(R.string.mStatus_ua_default, "")
    );

    public StatusConfig(ModulesActivity cntx) {
        super(cntx);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_status;
    }

    @Override
    public void onInitialize(View views) {
        // autoredirect
        StatusModule.AUTOREDIR_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.autoredirect));

        // useragent
        var userAgentView = views.<EditText>findViewById(R.id.useragent);
        StatusModule.USERAGENT_PREF(getActivity()).attachToEditText(userAgentView);

        var choose_useragent = views.findViewById(R.id.set_useragent);
        AndroidUtils.longTapForDescription(choose_useragent);
        choose_useragent.setOnClickListener(view -> new AlertDialog.Builder(getActivity())
                .setTitle(R.string.mStatus_ua_choose)
                .setItems(JavaUtils.mapEach(USERAGENTS, p -> getActivity().getString(p.first)).toArray(new CharSequence[0]),
                        (dialogInterface, i) -> userAgentView.setText(USERAGENTS.get(i).second))
                .show());
    }
}

class StatusDialog extends AModuleDialog {
    private static final String PREVIOUS = "redirected.redirected";

    static final List<AutomationRules.Automation<StatusDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>("checkStatus", R.string.auto_checkStatus, dialog ->
                    dialog.check(dialog.getUrlData().disableUpdates))
    );

    private Button check;
    private TextView previous;
    private TextView info;
    private TextView redirect;

    private Thread thread = null;

    private BoolPref autoRedir;
    private StringPref userAgent;

    public StatusDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_status;
    }

    @Override
    public void onInitialize(View views) {
        check = views.findViewById(R.id.check);
        check.setOnClickListener(v -> {
            AndroidUtils.setHideableText(previous, null);
            check(false);
        });

        previous = views.findViewById(R.id.previous);
        AndroidUtils.setRoundedColor(R.color.good, previous);

        info = views.findViewById(R.id.info);

        redirect = views.findViewById(R.id.redirect);
        redirect.setMovementMethod(LinkMovementMethod.getInstance());

        autoRedir = StatusModule.AUTOREDIR_PREF(getActivity());
        userAgent = StatusModule.USERAGENT_PREF(getActivity());
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
        check.setEnabled(true);
        check.setText(R.string.mStatus_check);
        AndroidUtils.setHideableText(previous, urlData.getData(PREVIOUS));
        AndroidUtils.setHideableText(info, null);
        updateRedirect(null);
    }

    /** Starts the checking process */
    private void check(boolean disableUpdates) {
        // disable button
        check.setEnabled(false);
        check.setText(R.string.mStatus_recheck);
        AndroidUtils.setHideableText(info, getActivity().getString(R.string.mStatus_checking));
        updateRedirect(null);

        // check in background
        thread = new Thread(() -> _check(disableUpdates));
        thread.start();
    }

    /**
     * Checks a redirect, in background
     * https://stackoverflow.com/questions/1884230/urlconnection-doesnt-follow-redirect
     */
    private void _check(boolean disableUpdates) {
        // get url
        var url = getUrl();
        Log.d("STATUS", "Checking: " + url);
        String message;

        var redirectionUrl = (String) null;

        HttpURLConnection conn = null;
        try {
            // perform GET to the url
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);   // Make the logic below easier to detect redirections
            conn.setConnectTimeout(HttpUtils.CONNECT_TIMEOUT);
            if (!userAgent.get().isEmpty()) {
                conn.setRequestProperty("User-Agent", userAgent.get());
            }
            var responseCode = conn.getResponseCode();
            Log.d("RESPONSE_CODE", url + ": " + responseCode);

            // prepare message
            message = null;
            var codesArray = getActivity().getResources().getStringArray(R.array.mStatus_codes);
            for (var s : codesArray) {
                if (s.startsWith(String.valueOf(responseCode))) {
                    // known status code
                    message = s;
                    break;
                }
            }
            if (message == null) {
                // unknown status code
                message = getActivity().getString(R.string.mStatus_unknownCode, responseCode);
            }

            // redirection
            var location = conn.getHeaderField("Location");
            if (location != null) {
                // this should be removed, the uri needs to be kept encoded
                // location = URLDecoder.decode(location, sUTF-8);
                redirectionUrl = new URL(new URL(url), location).toExternalForm(); // Deal with relative URLs
            }
        } catch (IOException e) {
            // io error
            AndroidUtils.assertError("Network error while checking url", e);
            message = getActivity().getString(R.string.mStatus_ioerror, e.getMessage());
        } catch (Exception e) {
            // other error
            AndroidUtils.assertError("Unknown exception while checking url", e);
            message = getActivity().getString(R.string.mStatus_error, e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // exit if was canceled
        if (Thread.currentThread().isInterrupted()) {
            Log.d("THREAD", "Interrupted");
            return;
        }

        // notify
        var finalMessage = message;
        var finalRedirectionUrl = redirectionUrl;
        getActivity().runOnUiThread(() -> {
            info.setText(finalMessage);
            check.setEnabled(true);

            if (!disableUpdates && autoRedir.get() && finalRedirectionUrl != null) {
                // autoredirect, replace url
                var previousMessage = previous.getText().toString() + (previous.length() == 0 ? "" : "\n") + "--> " + finalMessage;
                setUrl(new UrlData(finalRedirectionUrl).putData(PREVIOUS, previousMessage));
            } else {
                updateRedirect(finalRedirectionUrl);
            }

        });
    }

    /** Updates the redirect textview */
    private void updateRedirect(String redirectionUrl) {
        if (redirectionUrl == null) {
            AndroidUtils.setHideableText(redirect, null);
            return;
        }

        var text = AndroidUtils.underlineUrl(getActivity().getString(R.string.mStatus_redir, MARKER), redirectionUrl, this::setUrl);

        AndroidUtils.setHideableText(redirect, text);
    }
}
