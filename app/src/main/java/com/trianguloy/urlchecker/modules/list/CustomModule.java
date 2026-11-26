package com.trianguloy.urlchecker.modules.list;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.sUTF_8;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.companions.CustomModulesCatalog;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Inflater;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Function;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A user-defined module, built from a json definition of the {@link CustomModulesCatalog}.
 * See https://github.com/jivandabeast/URLCheck-Modules/blob/main/docs/format.md for the format.
 */
public class CustomModule extends AModuleData {

    public static final String ID_PREFIX = "custom.";

    final String key;
    final JSONObject definition;

    public CustomModule(String key, JSONObject definition) {
        this.key = key;
        this.definition = definition;
    }

    @Override
    public String getId() {
        return ID_PREFIX + key;
    }

    @Override
    public int getName() {
        return R.string.mCustom_name;
    }

    @Override
    public String getName(Context cntx) {
        return definition.optString("name", key);
    }

    @Override
    public boolean isEnabledByDefault() {
        return definition.optBoolean("enabled", true);
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new CustomModuleDialog(cntx, this);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new CustomModuleConfig(cntx, this);
    }

    /* ------------------- shared logic (also used by tests/other classes) ------------------- */

    private static final Pattern ALL_MATCH = Pattern.compile("^.*$", Pattern.DOTALL);
    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\$(\\d)|\\$([A-Z_]+)\\$");

    /** Returns whether a url matches the 'regex' and not the 'excludeRegex' of a json object */
    static boolean matches(JSONObject data, String url) {
        return matcher(data, url) != null;
    }

    /**
     * Returns a matcher (already 'found') of the first 'regex' of a json object that matches the url, or null if none match or an 'excludeRegex' matches.
     * If no 'regex' exists, a matcher of the whole url is returned.
     */
    static Matcher matcher(JSONObject data, String url) {
        try {
            Matcher matcher = null;
            if (!data.has("regex")) {
                matcher = ALL_MATCH.matcher(url);
                if (!matcher.find()) return null;
            } else {
                for (var regex : JavaUtils.parseArrayOrElement(data.get("regex"), String.class)) {
                    var candidate = Pattern.compile(regex).matcher(url);
                    if (candidate.find()) {
                        matcher = candidate;
                        break;
                    }
                }
                if (matcher == null) return null;
            }

            if (data.has("excludeRegex")) {
                for (var exclusion : JavaUtils.parseArrayOrElement(data.get("excludeRegex"), String.class)) {
                    if (Pattern.compile(exclusion).matcher(url).find()) return null;
                }
            }
            return matcher;
        } catch (Exception e) {
            AndroidUtils.assertError("Invalid regex in custom module", e);
            return null;
        }
    }

    /** Variables available in templates */
    static Map<String, String> variables(String url, String referrer) {
        var vars = new LinkedHashMap<String, String>();
        vars.put("URL", url);
        try {
            vars.put("URL_ENCODED", URLEncoder.encode(url, sUTF_8));
        } catch (IOException e) {
            vars.put("URL_ENCODED", url);
        }
        var host = Uri.parse(url).getHost();
        vars.put("HOST", host == null ? "" : host);
        var format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        vars.put("TIMESTAMP", format.format(new Date()));
        vars.put("REFERRER", referrer == null ? "" : referrer);
        return vars;
    }

    /**
     * Fills a template in a single pass (so values are never re-expanded):
     * $0..$9 are replaced by the regex groups, $NAME$ by the variables. Unknown tokens are kept as-is.
     */
    static String fill(String template, Matcher groups, Map<String, String> vars) {
        var token = TEMPLATE_TOKEN.matcher(template);
        var result = new StringBuilder();
        int last = 0;
        while (token.find()) {
            result.append(template, last, token.start());
            String value = null;
            if (token.group(1) != null) {
                int group = Integer.parseInt(token.group(1));
                if (groups != null && group <= groups.groupCount()) value = groups.group(group);
                if (value == null && groups != null && group <= groups.groupCount()) value = ""; // optional group that didn't match
            } else {
                value = vars.get(token.group(2));
            }
            result.append(value != null ? value : token.group());
            last = token.end();
        }
        result.append(template, last, template.length());
        return result.toString();
    }

    /** Follows a dotted path (like "data.items.0.title") in a json, returns null if not found */
    static String jsonPath(String json, String path) {
        try {
            Object current = json.trim().startsWith("[") ? new JSONArray(json) : new JSONObject(json);
            if (!path.isEmpty()) {
                for (var part : path.split("\\.")) {
                    if (current instanceof JSONObject object) {
                        current = object.opt(part);
                    } else if (current instanceof JSONArray array) {
                        current = array.opt(Integer.parseInt(part));
                    } else {
                        return null;
                    }
                    if (current == null || current == JSONObject.NULL) return null;
                }
            }
            return current.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Result of a request */
    record Response(int code, String body, String error) {
        boolean ok() {
            return error == null && code >= 200 && code < 300;
        }
    }

    private static final int MAX_RESPONSE = 256 * 1024;

    /** Performs an http request (blocking, don't call from the main thread) */
    static Response request(String url, String method, Map<String, String> headers, String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod(method);
            for (var header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            var code = connection.getResponseCode();
            var stream = code < 400 ? connection.getInputStream() : connection.getErrorStream();
            return new Response(code, read(stream), null);
        } catch (Exception e) {
            return new Response(-1, null, e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (stream) {
            var out = new ByteArrayOutputStream();
            var buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1 && out.size() < MAX_RESPONSE) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        }
    }
}

class CustomModuleDialog extends AModuleDialog {

    private static final Executor executor = Executors.newFixedThreadPool(2);
    private static final int MAX_DISPLAY = 500;

    private final CustomModule module;
    private final String appliedPrefix;
    private LinearLayout box;

    /** incremented on each new displayed url, to discard results of previous urls */
    private int generation = 0;

    public CustomModuleDialog(MainDialog dialog, CustomModule module) {
        super(dialog);
        this.module = module;
        this.appliedPrefix = "custom.applied." + module.key + ".";
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_custom;
    }

    @Override
    public void onInitialize(View views) {
        box = views.findViewById(R.id.box);
    }

    private List<JSONObject> getActions() {
        var list = new ArrayList<JSONObject>();
        var actions = module.definition.optJSONArray("actions");
        if (actions == null) return list;
        for (int i = 0; i < actions.length(); i++) {
            var action = actions.optJSONObject(i);
            list.add(action == null ? new JSONObject() : action);
        }
        return list;
    }

    private String label(JSONObject action, int index) {
        return action.optString("label", module.getName(getActivity()) + " #" + (index + 1));
    }

    private Map<String, String> variables(String url) {
        return CustomModule.variables(url, AndroidUtils.getReferrer(getActivity()));
    }

    @Override
    public void onModifyUrl(UrlData urlData, Function<UrlData, Boolean> setNewUrl) {
        var url = urlData.url;
        if (!CustomModule.matches(module.definition, url)) return;

        // apply automatic replacements (each one only once per chain, to avoid loops)
        var actions = getActions();
        for (int i = 0; i < actions.size(); i++) {
            var action = actions.get(i);
            if (!"replace".equals(action.optString("type")) || !action.optBoolean("automatic")) continue;
            if (urlData.getData(appliedPrefix + i) != null) continue;

            var matcher = CustomModule.matcher(action, url);
            if (matcher == null) continue;
            var newUrl = CustomModule.fill(action.optString("replacement"), matcher, variables(url));
            if (newUrl.equals(url)) continue;

            if (setNewUrl.apply(new UrlData(newUrl).putData(appliedPrefix + i, label(action, i)))) return;
        }
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        generation++;
        box.removeAllViews();
        var url = urlData.url;

        // already applied actions
        for (var applied : urlData.getDataByPrefix(appliedPrefix)) {
            var row = addRow(getActivity().getString(R.string.mCustom_applied, applied));
            row.button.setVisibility(View.GONE);
            AndroidUtils.setRoundedColor(R.color.good, row.text);
        }

        if (CustomModule.matches(module.definition, url)) {
            var actions = getActions();
            for (int i = 0; i < actions.size(); i++) {
                var action = actions.get(i);
                var matcher = CustomModule.matcher(action, url);
                if (matcher == null) continue;

                switch (action.optString("type")) {
                    case "replace" -> addReplace(action, i, matcher, url);
                    case "request" -> addRequest(action, i, matcher, urlData);
                    default -> {
                        // invalid, ignore (validated on save)
                    }
                }
            }
        }

        setVisibility(box.getChildCount() > 0);
    }

    private void addReplace(JSONObject action, int index, Matcher matcher, String url) {
        var newUrl = CustomModule.fill(action.optString("replacement"), matcher, variables(url));
        var label = label(action, index);
        var row = addRow("");
        row.button.setText(label);
        row.button.setEnabled(!newUrl.equals(url));
        row.button.setOnClickListener(v -> setUrl(new UrlData(newUrl).putData(appliedPrefix + index, label)));
    }

    private void addRequest(JSONObject action, int index, Matcher matcher, UrlData urlData) {
        var url = urlData.url;
        var vars = variables(url);
        var label = label(action, index);

        // build the request now (on the main thread), send it later
        var requestUrl = CustomModule.fill(action.optString("url"), matcher, vars);
        var body = action.has("body") ? CustomModule.fill(action.optString("body"), matcher, vars) : null;
        var method = action.optString("method", body == null ? "GET" : "POST").toUpperCase(Locale.ROOT);
        var headers = new LinkedHashMap<String, String>();
        if (body != null) {
            var trimmed = body.trim();
            headers.put("Content-Type", trimmed.startsWith("{") || trimmed.startsWith("[") ? "application/json" : "text/plain; charset=utf-8");
        }
        var headersJson = action.optJSONObject("headers");
        if (headersJson != null) {
            for (var header : JavaUtils.toList(headersJson.keys())) {
                headers.put(header, CustomModule.fill(headersJson.optString(header), matcher, vars));
            }
        }

        var row = addRow("");
        row.button.setText(label);
        Runnable send = () -> send(action, index, label, requestUrl, method, headers, body, row);
        row.button.setOnClickListener(v -> send.run());

        // automatic: send now, unless it already changed the url
        if (action.optBoolean("automatic") && urlData.getData(appliedPrefix + index) == null) {
            send.run();
        }
    }

    private void send(JSONObject action, int index, String label, String requestUrl, String method, Map<String, String> headers, String body, Row row) {
        var currentGeneration = generation;
        row.button.setEnabled(false);
        row.text.setText(R.string.mCustom_requesting);
        AndroidUtils.clearRoundedColor(row.text);

        executor.execute(() -> {
            var response = CustomModule.request(requestUrl, method, headers, body);
            getActivity().runOnUiThread(() -> {
                if (currentGeneration != generation) return; // the url changed meanwhile, discard
                row.button.setEnabled(true);
                onResponse(action, index, label, response, row);
            });
        });
    }

    private void onResponse(JSONObject action, int index, String label, CustomModule.Response response, Row row) {
        // failure
        if (!response.ok()) {
            setText(row, response.error() != null ? response.error() : "HTTP " + response.code(), R.color.bad);
            return;
        }

        // extract
        String result = response.body();
        var extracted = false;
        if (action.has("extract")) {
            result = CustomModule.jsonPath(result, action.optString("extract"));
            extracted = true;
        }
        if (result != null && action.has("extractRegex")) {
            try {
                var matcher = Pattern.compile(action.optString("extractRegex")).matcher(result);
                result = matcher.find() ? matcher.group(matcher.groupCount() >= 1 ? 1 : 0) : null;
            } catch (Exception e) {
                result = null;
            }
            extracted = true;
        }

        if (!extracted) {
            // nothing to extract, just show the status
            setText(row, "HTTP " + response.code(), R.color.good);
            return;
        }
        if (result == null || result.isEmpty()) {
            setText(row, getActivity().getString(R.string.mCustom_notFound), R.color.warning);
            return;
        }

        if (action.optBoolean("setUrl")) {
            // use as new url
            setUrl(new UrlData(result.trim()).putData(appliedPrefix + index, label));
        } else {
            // display
            var display = action.has("display")
                    ? action.optString("display").replace("$RESULT$", result)
                    : result;
            setText(row, display, R.color.good);
        }
    }

    private void setText(Row row, String text, int color) {
        row.text.setText(text.length() > MAX_DISPLAY ? text.substring(0, MAX_DISPLAY) + "…" : text);
        AndroidUtils.setRoundedColor(color, row.text);
    }

    private record Row(Button button, TextView text) {
    }

    private Row addRow(String text) {
        var view = Inflater.inflate(R.layout.button_text, box);
        var row = new Row(view.findViewById(R.id.button), view.findViewById(R.id.text));
        row.text.setText(text);
        return row;
    }
}

class CustomModuleConfig extends AModuleConfig {

    private final CustomModule module;

    public CustomModuleConfig(ModulesActivity activity, CustomModule module) {
        super(activity);
        this.module = module;
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_custom;
    }

    @Override
    public void onInitialize(View views) {
        var cntx = getActivity();
        var text = new StringBuilder(module.definition.optString("description",
                cntx.getString(R.string.mCustom_moduleDesc, module.key)));
        var hosts = CustomModulesCatalog.requestHosts(module.definition);
        if (!hosts.isEmpty()) {
            text.append("\n\n").append(cntx.getString(R.string.mCustom_sendsTo, android.text.TextUtils.join(", ", hosts)));
        }
        views.<TextView>findViewById(R.id.text).setText(text);
        views.findViewById(R.id.edit).setOnClickListener(v -> new CustomModulesCatalog(cntx).showEditor());
    }
}
