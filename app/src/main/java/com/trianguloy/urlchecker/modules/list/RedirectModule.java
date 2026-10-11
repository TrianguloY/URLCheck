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
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.ListStringPref;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Redirects URLs by replacing the host according to user-defined rules.
 * Implementation by coderj001.
 */
public class RedirectModule extends AModuleData {

    static final String RULES_PREF = "redirect_rules";
    static final String SEPARATOR = "\n";
    static final String FIELD_SEP = "|";

    static ListStringPref RULES_PREF(android.content.Context cntx) {
        return new ListStringPref(RULES_PREF, SEPARATOR, Collections.emptyList(), cntx);
    }

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

    private final ListStringPref rulesPref;
    private LinearLayout box;

    private final List<PendingRedirect> pending = new ArrayList<>();

    public RedirectDialog(MainDialog dialog) {
        super(dialog);
        rulesPref = RedirectModule.RULES_PREF(dialog);
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
        Uri uri = Uri.parse(urlData.url);
        String host = uri.getHost();
        if (host == null) return;

        for (String rule : rulesPref.get()) {
            String[] parts = rule.split(Pattern.quote(RedirectModule.FIELD_SEP), 3);
            if (parts.length < 3) continue;
            String from = parts[0].trim();
            String to = parts[1].trim();
            boolean auto = Boolean.parseBoolean(parts[2].trim());

            if (!from.equalsIgnoreCase(host)) continue;

            String newUrl = uri.buildUpon().authority(to).build().toString();

            if (auto) {
                if (setNewUrl.apply(new UrlData(newUrl))) return;
            } else {
                pending.add(new PendingRedirect(newUrl, to));
            }
        }
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        box.removeAllViews();

        if (!pending.isEmpty()) {
            setVisibility(true);
            for (PendingRedirect pendingRedirect : pending) {
                View row = LayoutInflater.from(getActivity()).inflate(R.layout.button_text, box, false);
                Button btn = row.findViewById(R.id.button);
                TextView txt = row.findViewById(R.id.text);
                btn.setText(R.string.mRedir_apply);
                txt.setText(pendingRedirect.toHost);
                String urlToApply = pendingRedirect.newUrl;
                btn.setOnClickListener(v -> setUrl(urlToApply));
                box.addView(row);
            }
        } else {
            setVisibility(false);
        }
    }

    private static class PendingRedirect {
        final String newUrl;
        final String toHost;

        PendingRedirect(String newUrl, String toHost) {
            this.newUrl = newUrl;
            this.toHost = toHost;
        }
    }
}

// ------------------- config -------------------

class RedirectConfig extends AModuleConfig {

    private final ListStringPref rulesPref;
    private LinearLayout rulesContainer;

    public RedirectConfig(ModulesActivity activity) {
        super(activity);
        rulesPref = RedirectModule.RULES_PREF(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_redirect;
    }

    @Override
    public void onInitialize(View views) {
        rulesContainer = views.findViewById(R.id.rules_container);
        views.findViewById(R.id.add).setOnClickListener(v -> addRule("", "", false));

        for (String rule : rulesPref.get()) {
            String[] parts = rule.split(Pattern.quote(RedirectModule.FIELD_SEP), 3);
            String from = parts.length > 0 ? parts[0] : "";
            String to = parts.length > 1 ? parts[1] : "";
            boolean auto = parts.length > 2 && Boolean.parseBoolean(parts[2]);
            addRule(from, to, auto);
        }
    }

    private void addRule(String from, String to, boolean auto) {
        View row = LayoutInflater.from(getActivity()).inflate(R.layout.config_redirect_row, rulesContainer, false);
        EditText fromEdit = row.findViewById(R.id.from);
        EditText toEdit = row.findViewById(R.id.to);
        CheckBox autoCheck = row.findViewById(R.id.auto);

        fromEdit.setText(from);
        toEdit.setText(to);
        autoCheck.setChecked(auto);

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { saveRules(); }
        };
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
        List<String> rules = new ArrayList<>();
        for (int i = 0; i < rulesContainer.getChildCount(); i++) {
            View row = rulesContainer.getChildAt(i);
            String from = ((EditText) row.findViewById(R.id.from)).getText().toString().trim();
            String to = ((EditText) row.findViewById(R.id.to)).getText().toString().trim();
            boolean auto = ((CheckBox) row.findViewById(R.id.auto)).isChecked();
            if (!from.isEmpty() || !to.isEmpty()) {
                rules.add(from + RedirectModule.FIELD_SEP + to + RedirectModule.FIELD_SEP + auto);
            }
        }
        rulesPref.set(rules);
    }
}
