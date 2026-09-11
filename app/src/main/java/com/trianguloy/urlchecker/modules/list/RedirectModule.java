package com.trianguloy.urlchecker.modules.list;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.companions.RedirectCatalog;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Function;
import com.trianguloy.urlchecker.utilities.wrappers.DefaultTextWatcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Redirects URLs by replacing the host according to user-defined rules.
 * Base implementation by coderj001.
 */
public class RedirectModule extends AModuleData {

    @Override
    public String getId() {
        return "redirect";
    }

    @Override
    public int getName() {
        return R.string.mRedir_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new RedirectDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new RedirectConfig(cntx);
    }
}

// ------------------- dialog -------------------

class RedirectDialog extends AModuleDialog {

    private final RedirectCatalog catalog;
    private LinearLayout box;

    private final List<PendingRedirect> pending = new ArrayList<>();

    public RedirectDialog(MainDialog dialog) {
        super(dialog);
        catalog = new RedirectCatalog(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_redirect;
    }

    @Override
    public void onInitialize(View views) {
        box = views.findViewById(R.id.box);
    }

    @Override
    public void onPrepareUrl(UrlData urlData) {
        pending.clear();
    }

    @Override
    public void onModifyUrl(UrlData urlData, Function<UrlData, Boolean> setNewUrl) {
        var uri = Uri.parse(urlData.url);
        var host = uri.getHost();
        if (host == null) return;

        var rules = catalog.getCatalog();
        for (var key : JavaUtils.toList(rules.keys())) {
            try {
                var data = rules.getJSONObject(key);
                var from = data.optString("from");
                var to = data.optString("to");
                if (from.isEmpty() || to.isEmpty()) continue;

                if (!from.equalsIgnoreCase(host)) continue;

                var newUrl = uri.buildUpon().authority(to).build().toString();

                if (data.optBoolean("auto")) {
                    if (setNewUrl.apply(new UrlData(newUrl))) return;
                } else {
                    pending.add(new PendingRedirect(newUrl, to));
                }
            } catch (JSONException e) {
                AndroidUtils.assertError("Invalid rule", e);
            }
        }
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        box.removeAllViews();

        if (!pending.isEmpty()) {
            setVisibility(true);
            for (var pendingRedirect : pending) {
                var row = LayoutInflater.from(getActivity()).inflate(R.layout.button_text, box, false);
                var btn = row.<Button>findViewById(R.id.button);
                var txt = row.<TextView>findViewById(R.id.text);
                btn.setText(R.string.mRedir_apply);
                txt.setText(pendingRedirect.toHost);
                var urlToApply = pendingRedirect.newUrl;
                btn.setOnClickListener(v -> setUrl(urlToApply));
                box.addView(row);
            }
        } else {
            setVisibility(false);
        }
    }

    private record PendingRedirect(String newUrl, String toHost) {
    }
}

// ------------------- config -------------------

class RedirectConfig extends AModuleConfig {

    private final RedirectCatalog catalog;
    private LinearLayout rulesContainer;

    public RedirectConfig(ModulesActivity activity) {
        super(activity);
        catalog = new RedirectCatalog(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_redirect;
    }

    @Override
    public void onInitialize(View views) {
        rulesContainer = views.findViewById(R.id.rules_container);
        views.findViewById(R.id.add).setOnClickListener(v -> addRule("Name", "", "", false));
        views.findViewById(R.id.json).setOnClickListener(v -> catalog.showEditor());

        var rules = catalog.getCatalog();
        for (var name : JavaUtils.toList(rules.keys())) {
            try {
                var data = rules.getJSONObject(name);
                addRule(
                        name,
                        data.optString("from"),
                        data.optString("to"),
                        data.optBoolean("auto"));
            } catch (JSONException e) {
                AndroidUtils.assertError("Invalid rule", e);
            }
        }
    }

    private void addRule(String name, String from, String to, boolean auto) {
        var row = LayoutInflater.from(getActivity()).inflate(R.layout.config_redirect_row, rulesContainer, false);
        var nameEdit = row.<EditText>findViewById(R.id.name);
        var fromEdit = row.<EditText>findViewById(R.id.from);
        var toEdit = row.<EditText>findViewById(R.id.to);
        var autoCheck = row.<CheckBox>findViewById(R.id.auto);

        nameEdit.setText(name);
        fromEdit.setText(from);
        toEdit.setText(to);
        autoCheck.setChecked(auto);

        var watcher = new DefaultTextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                saveRules();
            }
        };
        nameEdit.addTextChangedListener(watcher);
        fromEdit.addTextChangedListener(watcher);
        toEdit.addTextChangedListener(watcher);
        autoCheck.setOnCheckedChangeListener((b, checked) -> saveRules());

        row.findViewById(R.id.delete).setOnClickListener(v -> {
            rulesContainer.removeView(row);
            saveRules();
        });

        rulesContainer.addView(row);
    }

    private void saveRules() {
        var rules = new JSONObject();
        for (var i = 0; i < rulesContainer.getChildCount(); i++) {
            try {
                var row = rulesContainer.getChildAt(i);
                var name = row.<EditText>findViewById(R.id.name).getText().toString().trim();
                var from = row.<EditText>findViewById(R.id.from).getText().toString().trim();
                var to = row.<EditText>findViewById(R.id.to).getText().toString().trim();
                var auto = row.<CheckBox>findViewById(R.id.auto).isChecked();
                if (!from.isEmpty() || !to.isEmpty()) {
                    rules.put(name, new JSONObject()
                            .put("from", from)
                            .put("to", to)
                            .put("auto", auto));
                }
            } catch (JSONException e) {
                AndroidUtils.assertError("Invalid rule", e);
            }
        }
        catalog.save(rules);
    }
}
