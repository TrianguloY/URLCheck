package com.trianguloy.urlchecker.modules.companions;

import static com.trianguloy.urlchecker.utilities.methods.AndroidUtils.getStringWithPlaceholder;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.JsonEditorInterface;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.StreamUtils;
import com.trianguloy.urlchecker.utilities.wrappers.AssetFile;
import com.trianguloy.urlchecker.utilities.wrappers.InternalFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Manages the local catalog with the rules */
public class ClearUrlCatalog implements JsonEditorInterface {

    /* ------------------- constants ------------------- */

    private final InternalFile custom;
    private final AssetFile builtIn;
    private static final int AUTOUPDATE_PERIOD = /* 1 week (in milliseconds) */
            7/*days*/ * 24/*hours*/ * 60/*minutes*/ * 60/*seconds*/ * 1000/*milliseconds*/;

    /* ------------------- prefs ------------------- */

    private final GenericPref.Str catalogURL;
    private final GenericPref.Str hashURL;
    private final GenericPref.Bool autoUpdate;
    private final GenericPref.Lng lastUpdate;
    private final GenericPref.Lng lastCheck;
    private final GenericPref.Bool lastAuto;

    /* ------------------- constructor ------------------- */

    private final Activity cntx;

    public ClearUrlCatalog(Activity cntx) {
        this.cntx = cntx;
        custom = new InternalFile("clearUrlCatalog", cntx);
        builtIn = new AssetFile("data.minify.json", cntx);
        catalogURL = new GenericPref.Str("clearurl_catalogURL", "https://rules2.clearurls.xyz/data.minify.json", cntx);
        hashURL = new GenericPref.Str("clearurl_hashURL", "https://rules2.clearurls.xyz/rules.minify.hash", cntx);
        autoUpdate = new GenericPref.Bool("clearurl_autoUpdate", false, cntx);
        lastUpdate = new GenericPref.Lng("clearurl_lastUpdate", /*data.minify.json-timestamp*/1754183829000L/*data.minify.json-timestamp*/, cntx);
        lastCheck = new GenericPref.Lng("clearurl_lastCheck", -1L, cntx);
        lastAuto = new GenericPref.Bool("clearurl_lastAuto", false, cntx);

        updateIfNecessary();
    }

    /* ------------------- catalog ------------------- */

    /** Returns the catalog content */
    public JSONObject getCatalog() {
        // get the updated file first
        String internal = custom.get();
        if (internal != null) return JavaUtils.toJson(internal);

        // no updated file or can't read, use built-in one
        return getBuiltIn();
    }

    /** Returns the built-in catalog */
    public JSONObject getBuiltIn() {
        // read internal file
        String builtIn = this.builtIn.get();
        if (builtIn != null) return JavaUtils.toJson(builtIn);

        // can't read either? panic! return empty
        return JavaUtils.toJson("{\"providers\":{}}");
    }

    /**
     * Parses and returns the providers from the catalog
     * Returns a list of pairs: [(rule,data),...]
     */
    public static List<Pair<String, JSONObject>> getRules(Activity cntx) {
        try {
            // prepare
            List<Pair<String, JSONObject>> rules = new ArrayList<>();
            ClearUrlCatalog clearUrlCatalog = new ClearUrlCatalog(cntx);
            JSONObject json = clearUrlCatalog.getCatalog();

            // extract and merge each provider
            for (String provider : JavaUtils.toList(json.keys())) {
                JSONObject providerData = json.getJSONObject(provider);
                for (String rule : JavaUtils.toList(providerData.keys())) {
                    rules.add(Pair.create(rule, providerData.getJSONObject(rule)));
                }
            }
            return rules;
        } catch (JSONException e) {
            // invalid catalog, return empty
            AndroidUtils.assertError(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /** For {@link this#setRules(JSONObject, boolean)} return value */
    private enum Result {
        UP_TO_DATE,
        UPDATED,
        ERROR
    }

    /**
     * Saves a new local catalog. Returns if it was up to date, updated or an error happened.
     * When merge is true, only the top-objects with the same key are replaced.
     */
    private Result setRules(JSONObject rules, boolean merge) {
        // merge rules if required
        if (merge) {
            try {
                // replace only the top objects
                JSONObject merged = getCatalog();
                for (String key : JavaUtils.toList(rules.keys())) {
                    merged.put(key, rules.getJSONObject(key));
                }
                rules = merged;
            } catch (JSONException e) {
                AndroidUtils.assertError("Invalid JSON while trying to set rules", e);
                // can't be parsed
                return Result.ERROR;
            }
        }
        // compact
        String content = rules.toString();

        // nothing changed, nothing to update
        if (content.equals(getCatalog().toString())) {
            return Result.UP_TO_DATE;
        }

        // same as builtin (probably a reset), clear custom
        if (content.equals(getBuiltIn().toString())) {
            clear();
            return Result.UPDATED;
        }

        // something new, save
        return custom.set(content) ? Result.UPDATED : Result.ERROR;
    }

    /** Deletes the custom catalog, built-in one will be returned afterwards */
    public void clear() {
        custom.delete();
        lastUpdate.clear();
        lastCheck.clear();
        lastAuto.clear();
    }

    // ------------------- dialogs -------------------

    /** Show the rules editor dialog */
    public void showEditor() {
        showEditor(cntx);
    }

    @Override
    public JSONObject getJson() {
        return getCatalog();
    }

    @Override
    public JSONObject getBuiltInJson() {
        return getBuiltIn();
    }

    @Override
    public String saveJson(JSONObject data) {
        if (setRules(data, false) != Result.ERROR) {
            // saved data, close dialog
            return null;
        } else {
            // invalid data, keep dialog and return why
            return cntx.getString(R.string.invalid);
        }
    }

    @Override
    public String getEditorDescription() {
        return getStringWithPlaceholder(cntx, R.string.mClear_editor, R.string.clearRules_url);
    }

    /** Show the updater dialog */
    public void showUpdater() {
        // prepare dialog content
        View views = cntx.getLayoutInflater().inflate(R.layout.config_clearurls_updater, null);

        // configure
        catalogURL.attachToEditText(views.findViewById(R.id.catalog_URL));
        hashURL.attachToEditText(views.findViewById(R.id.hash_URL));
        autoUpdate.attachToSwitch(views.findViewById(R.id.autoUpdate));

        // info
        TextView txt_check = views.findViewById(R.id.last_check);
        TextView txt_update = views.findViewById(R.id.last_update);
        Runnable update = () -> {
            txt_check.setText(AndroidUtils.formatMillis(lastCheck.get(), cntx)
                    + (lastAuto.get() ? " [" + cntx.getString(R.string.auto) + "]" : "")
            );
            txt_update.setText(AndroidUtils.formatMillis(lastUpdate.get(), cntx));
        };
        update.run();

        // prepare dialog
        AlertDialog dialog = new AlertDialog.Builder(cntx)
                .setView(views)
                .setPositiveButton(R.string.mClear_updateNow, null) // set below
                .setNeutralButton(R.string.mClear_restore, null) // set below
                .setCancelable(true)
                .show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // prepare more dialog
        // these are configured here to allow auto-closing the dialog when they are pressed
        Button updateNow = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        updateNow.setOnClickListener(v -> {
            // updates the rules
            updateNow.setEnabled(false);
            new Thread(() -> {
                // run
                lastAuto.set(false);
                int toast = _updateCatalog();

                // update
                cntx.runOnUiThread(() -> {
                    updateNow.setEnabled(true);
                    update.run();
                    Toast.makeText(cntx, toast, Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            // clear and reload
            catalogURL.clear();
            hashURL.clear();
            dialog.dismiss();
            showUpdater();
        });
    }

    // ------------------- internal -------------------

    /** If the catalog is old, updates it in background. Otherwise does nothing. */
    private void updateIfNecessary() {
        if (autoUpdate.get() && lastUpdate.get() + AUTOUPDATE_PERIOD < System.currentTimeMillis()) {
            new Thread(() -> {
                // run
                lastAuto.set(true);
                int toast = _updateCatalog();

                // don't show message to user, but log it
                Log.d("UPDATE", cntx.getString(toast));
            }).start();
        }
    }

    /**
     * Replaces the catalog with a new one.
     * Network operation, designed to be run in a background thread.
     * Returns the message to display to the user about the result
     */
    private int _updateCatalog() {
        long now = System.currentTimeMillis();
        lastCheck.set(now);

        // read content
        String rawRules;
        try {
            rawRules = HttpUtils.readFromUrl(catalogURL.get());
        } catch (IOException e) {
            AndroidUtils.assertError("Unable to get remote catalog", e);
            return R.string.mClear_urlError;
        }

        // check hash if provided
        if (!hashURL.get().trim().isEmpty()) {

            // read hash
            String hash;
            try {
                hash = HttpUtils.readFromUrl(hashURL.get()).trim();
            } catch (IOException e) {
                AndroidUtils.assertError("Unable to fetch remote hash", e);
                return R.string.mClear_hashError;
            }

            // if different, notify
            if (!StreamUtils.sha256(rawRules).equalsIgnoreCase(hash)) {
                return R.string.mClear_hashMismatch;
            }
        }

        // parse json
        JSONObject json;
        try {
            json = new JSONObject(rawRules);
        } catch (JSONException e) {
            AndroidUtils.assertError("Unable to parse remote JSON", e);
            return R.string.invalid;
        }

        // valid, save and update
        return switch (setRules(json, true)) {
            case UPDATED -> {
                lastUpdate.set(now);
                yield R.string.mClear_updated;
            }
            case UP_TO_DATE -> R.string.mClear_upToDate;
            case ERROR -> R.string.invalid;
        };

    }
}
