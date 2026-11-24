package com.trianguloy.urlchecker.modules.list;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.companions.CustomModulesCatalog;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;

/**
 * Manager of the user-defined modules.
 * When enabled, each custom module of the {@link CustomModulesCatalog} is loaded as a {@link CustomModule}.
 * It doesn't show anything on the dialog by itself.
 */
public class CustomModulesModule extends AModuleData {

    public static final String ID = "customModules";
    public static final String DOCS_URL = "https://github.com/jivandabeast/URLCheck-Modules";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getName() {
        return R.string.mCustom_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new AModuleDialog(cntx) {
            @Override
            public int getLayoutId() {
                return -1; // nothing to show
            }

            @Override
            public void onInitialize(View views) {
            }
        };
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new CustomModulesConfig(cntx);
    }
}

class CustomModulesConfig extends AModuleConfig {

    private final ModulesActivity activity;
    private final CustomModulesCatalog catalog;

    public CustomModulesConfig(ModulesActivity activity) {
        super(activity);
        this.activity = activity;
        catalog = new CustomModulesCatalog(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_custom_manager;
    }

    @Override
    public void onInitialize(View views) {
        views.<TextView>findViewById(R.id.text).setText(activity.getString(R.string.mCustom_desc, CustomModulesModule.DOCS_URL));
        views.findViewById(R.id.edit).setOnClickListener(v -> catalog.showEditor());
        views.findViewById(R.id.importUrl).setOnClickListener(v -> askImportUrl());
    }

    /** Asks for a url to import modules from */
    private void askImportUrl() {
        var input = new EditText(activity);
        input.setHint(R.string.mCustom_import_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setSingleLine();

        new AlertDialog.Builder(activity)
                .setTitle(R.string.mCustom_import)
                .setView(input)
                .setPositiveButton(R.string.mCustom_import, (d, w) -> download(input.getText().toString().trim()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Downloads and parses the modules */
    private void download(String url) {
        if (url.isEmpty()) return;
        new Thread(() -> {
            var response = CustomModule.request(url, "GET", new LinkedHashMap<>(), null);
            activity.runOnUiThread(() -> {
                if (!response.ok()) {
                    error(response.error() != null ? response.error() : "HTTP " + response.code());
                    return;
                }
                try {
                    var json = new JSONObject(response.body());
                    // allow a single module, or a catalog of modules
                    var modules = CustomModulesCatalog.isSingleModule(json)
                            ? new JSONObject().put(CustomModulesCatalog.keyFromName(json.getString("name")), json)
                            : json;
                    var invalid = CustomModulesCatalog.validate(modules);
                    if (invalid != null) {
                        error(invalid);
                    } else if (modules.length() == 0) {
                        error("no modules found");
                    } else {
                        confirm(modules);
                    }
                } catch (JSONException e) {
                    error(e.getMessage());
                }
            });
        }).start();
    }

    /** Shows what will be imported, and imports it if accepted */
    private void confirm(JSONObject modules) {
        var current = catalog.getCatalog();
        var summary = new StringBuilder();
        for (var key : JavaUtils.toList(modules.keys())) {
            var module = modules.optJSONObject(key);
            summary.append("• ").append(module.optString("name")).append(" (").append(key).append(")");
            if (current.has(key)) summary.append(activity.getString(R.string.mCustom_import_replaces));
            summary.append("\n");
            var hosts = CustomModulesCatalog.requestHosts(module);
            if (!hosts.isEmpty()) {
                summary.append("   ").append(activity.getString(R.string.mCustom_sendsTo, android.text.TextUtils.join(", ", hosts))).append("\n");
            }
        }

        new AlertDialog.Builder(activity)
                .setTitle(R.string.mCustom_import)
                .setMessage(activity.getString(R.string.mCustom_import_confirm, summary.toString()))
                .setPositiveButton(R.string.mCustom_import, (d, w) -> {
                    try {
                        for (var key : JavaUtils.toList(modules.keys())) {
                            current.put(key, modules.get(key));
                        }
                    } catch (JSONException e) {
                        error(e.getMessage());
                        return;
                    }
                    if (catalog.save(current)) {
                        Toast.makeText(activity, R.string.mCustom_imported, Toast.LENGTH_SHORT).show();
                        activity.recreate(); // reload the list of modules
                    } else {
                        error(activity.getString(R.string.invalid));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void error(String message) {
        Toast.makeText(activity, activity.getString(R.string.mCustom_import_error, message), Toast.LENGTH_LONG).show();
    }
}
