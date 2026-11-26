package com.trianguloy.urlchecker.modules.companions;

import android.app.Activity;
import android.content.Context;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.JsonCatalog;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.wrappers.InternalFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

/**
 * Catalog of user-defined (custom) modules.
 * Each key is the module key, each value its definition.
 * See https://github.com/jivandabeast/URLCheck-Modules/blob/main/docs/format.md for the format.
 */
public class CustomModulesCatalog extends JsonCatalog {

    public static final String FILE_NAME = "custom_modules";

    /** Allowed action types */
    public static final Set<String> ACTION_TYPES = Set.of("replace", "request");

    public CustomModulesCatalog(Activity cntx) {
        super(cntx, FILE_NAME, R.string.mCustom_editor);
    }

    /** Reads the catalog without requiring an activity (built-in is used if there is no valid saved file) */
    public static JSONObject load(Context cntx) {
        try {
            var content = new InternalFile(FILE_NAME, cntx).get();
            if (content != null) return new JSONObject(content);
        } catch (JSONException ignored) {
        }
        try {
            return builtIn();
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    @Override
    public JSONObject buildBuiltIn(Context cntx) throws JSONException {
        return builtIn();
    }

    private static JSONObject builtIn() throws JSONException {
        return new JSONObject()
                .put("example", new JSONObject()
                        .put("name", "Example: Wayback Machine")
                        .put("description", "Sample custom module. Edit or remove it from the 'Custom modules' module configuration.")
                        .put("enabled", false)
                        .put("regex", "^https?://")
                        .put("actions", new JSONArray()
                                .put(new JSONObject()
                                        .put("type", "replace")
                                        .put("label", "Archived")
                                        .put("replacement", "https://web.archive.org/web/$URL$")
                                )
                                .put(new JSONObject()
                                        .put("type", "request")
                                        .put("label", "Latest snapshot")
                                        .put("url", "https://archive.org/wayback/available?url=$URL_ENCODED$")
                                        .put("extract", "archived_snapshots.closest.url")
                                        .put("setUrl", true)
                                )
                        )
                );
    }

    @Override
    public String saveJson(JSONObject data) {
        var error = validate(data);
        if (error != null) return error;
        return super.saveJson(data);
    }

    /** Returns null if the catalog is valid, a description of the first problem otherwise */
    public static String validate(JSONObject catalog) {
        for (var key : JavaUtils.toList(catalog.keys())) {
            var error = validateModule(catalog.opt(key));
            if (error != null) return "'" + key + "': " + error;
        }
        return null;
    }

    /** Returns null if the module definition is valid, a description of the problem otherwise */
    public static String validateModule(Object module) {
        if (!(module instanceof JSONObject definition)) return "must be an object";
        if (definition.optString("name").isEmpty()) return "missing 'name'";
        var actions = definition.optJSONArray("actions");
        if (actions == null) return "missing 'actions' array";
        for (int i = 0; i < actions.length(); i++) {
            var action = actions.optJSONObject(i);
            if (action == null) return "action " + i + " must be an object";
            var type = action.optString("type");
            if (!ACTION_TYPES.contains(type)) return "action " + i + " has unknown type '" + type + "' (valid: " + ACTION_TYPES + ")";
            if (type.equals("replace") && !action.has("replacement")) return "action " + i + " is missing 'replacement'";
            if (type.equals("request") && action.optString("url").isEmpty()) return "action " + i + " is missing 'url'";
        }
        return null;
    }

    /** Returns whether an imported json is a single module definition (instead of a catalog of them) */
    public static boolean isSingleModule(JSONObject json) {
        return json.has("name") && json.has("actions");
    }

    /** Generates a key for a module from its name */
    public static String keyFromName(String name) {
        var key = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return key.isEmpty() ? "module" : key;
    }

    /** Returns the hosts that the request actions of a module definition connect to (for display to the user) */
    public static List<String> requestHosts(JSONObject definition) {
        var hosts = new java.util.ArrayList<String>();
        var actions = definition.optJSONArray("actions");
        if (actions == null) return hosts;
        for (int i = 0; i < actions.length(); i++) {
            var action = actions.optJSONObject(i);
            if (action == null || !"request".equals(action.optString("type"))) continue;
            var url = action.optString("url");
            var host = android.net.Uri.parse(url).getHost();
            hosts.add(host != null ? host : url);
        }
        return hosts;
    }
}
