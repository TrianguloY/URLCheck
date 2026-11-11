package com.trianguloy.urlchecker.modules.list;

import static com.trianguloy.urlchecker.utilities.methods.AndroidUtils.MARKER;

import android.content.Context;
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
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A module that checks the page status code by performing a GET request
 * Allows checking for redirection
 */
public class StatusModule extends AModuleData {

    public static BoolPref AUTOREDIR_PREF(Context cntx) {
        return new BoolPref("statusCode_autoRedir", false, cntx);
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

    public StatusConfig(ModulesActivity cntx) {
        super(cntx);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_status;
    }

    @Override
    public void onInitialize(View views) {
        StatusModule.AUTOREDIR_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.autoredirect));
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

            // redirection - check standard 3xx redirects with Location header
            var location = conn.getHeaderField("Location");
            if (location != null && !location.isEmpty()) {
                // this should be removed, the uri needs to be kept encoded
                // location = URLDecoder.decode(location, "UTF-8");
                redirectionUrl = resolveUrl(url, location); // Deal with relative URLs
            } else if (responseCode == 200) {
                // Check for redirects in HTML content (meta refresh or JavaScript)
                try {
                    String bodyString = readResponseBody(conn);
                    
                    // A. Check for HTML meta refresh tag
                    String metaUrl = extractMetaRefreshUrl(bodyString);
                    if (metaUrl != null && !metaUrl.isEmpty()) {
                        redirectionUrl = resolveUrl(url, metaUrl);
                    } else {
                        // B. Check for JavaScript redirect
                        String jsUrl = extractJavaScriptUrl(bodyString);
                        if (jsUrl != null && !jsUrl.isEmpty()) {
                            redirectionUrl = resolveUrl(url, jsUrl);
                        }
                    }
                } catch (Exception e) {
                    Log.d("STATUS", "Error reading response body: " + e.getMessage());
                }
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

    /** Reads the response body from the HTTP connection */
    private String readResponseBody(HttpURLConnection conn) throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line).append("\n");
            }
        }
        return bodyBuilder.toString();
    }

    /** Extracts URL from HTML meta refresh tag */
    private String extractMetaRefreshUrl(String html) {
        Pattern pattern = Pattern.compile(
                "<meta[^>]*http-equiv\\s*=\\s*[\"']?refresh[\"']?[^>]*url\\s*=\\s*[\"']?([^\"'>\\s]+)[\"']?",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Extracts URL from JavaScript window.location redirect */
    private String extractJavaScriptUrl(String html) {
        // 1. Extract script tags first for safer parsing
        Pattern scriptPattern = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher scriptMatcher = scriptPattern.matcher(html);

        while (scriptMatcher.find()) {
            String scriptContent = scriptMatcher.group(1).trim();

            // 2. Match window.location expressions
            Pattern locPattern = Pattern.compile(
                    "window\\.location(?:\\.href)?\\s*=\\s*(?:decodeURIComponent\\()?([a-zA-Z0-9_\"'/.:?=~%-]+)\\)?\\s*;?",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher locMatcher = locPattern.matcher(scriptContent);

            if (locMatcher.find()) {
                String rightHandSide = locMatcher.group(1).trim();

                // Case A: String literal with quotes (e.g., "https://...")
                if ((rightHandSide.startsWith("\"") && rightHandSide.endsWith("\"")) ||
                    (rightHandSide.startsWith("'") && rightHandSide.endsWith("'"))) {
                    return rightHandSide.substring(1, rightHandSide.length() - 1);
                }

                // Case B: Variable reference (e.g., URI) - find its definition
                String varName = rightHandSide;
                Pattern varPattern = Pattern.compile(
                        "(?:var|let|const)?\\s*" + Pattern.quote(varName) + "\\s*=\\s*[\"']([^\"']+)[\"']",
                        Pattern.CASE_INSENSITIVE
                );
                Matcher varMatcher = varPattern.matcher(scriptContent);
                if (varMatcher.find()) {
                    String encodedUrl = varMatcher.group(1);
                    try {
                        // Decode URL-encoded strings
                        return URLDecoder.decode(encodedUrl, "UTF-8");
                    } catch (Exception e) {
                        Log.d("STATUS", "Error decoding URL: " + e.getMessage());
                        return encodedUrl;
                    }
                }

                // Case C: Fallback - check if it looks like a URL
                if (rightHandSide.startsWith("http://") || rightHandSide.startsWith("https://")) {
                    return rightHandSide;
                }
            }
        }
        return null;
    }

    /** Resolves relative URLs to absolute URLs */
    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            URL base = new URL(baseUrl);
            URL resolved = new URL(base, relativeUrl);
            return resolved.toExternalForm();
        } catch (Exception e) {
            Log.d("STATUS", "Error resolving URL: " + e.getMessage());
            return relativeUrl;
        }
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
