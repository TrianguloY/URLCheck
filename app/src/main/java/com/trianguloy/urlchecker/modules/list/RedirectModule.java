package com.trianguloy.urlchecker.modules.list;

import static com.trianguloy.urlchecker.modules.list.RedirectModule.*;

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
import com.trianguloy.urlchecker.utilities.wrappers.DefaultTextWatcher;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Redirects URLs by replacing the host according to user-defined rules.
 * Base implementation by coderj001.
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
        rulesPref = RULES_PREF(dialog);
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

        for (var rule : rulesPref.get()) {
            var parts = rule.split(Pattern.quote(FIELD_SEP), 3);
            if (parts.length < 3) continue;
            var from = parts[0].trim();
            var to = parts[1].trim();
            var auto = Boolean.parseBoolean(parts[2].trim());

            if (!from.equalsIgnoreCase(host)) continue;

            var newUrl = uri.buildUpon().authority(to).build().toString();

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

    private final ListStringPref rulesPref;
    private LinearLayout rulesContainer;

    public RedirectConfig(ModulesActivity activity) {
        super(activity);
        rulesPref = RULES_PREF(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_redirect;
    }

    @Override
    public void onInitialize(View views) {
        rulesContainer = views.findViewById(R.id.rules_container);
        views.findViewById(R.id.add).setOnClickListener(v -> addRule("", "", false));

        for (var rule : rulesPref.get()) {
            var parts = rule.split(Pattern.quote(FIELD_SEP), 3);
            var from = parts.length > 0 ? parts[0] : "";
            var to = parts.length > 1 ? parts[1] : "";
            var auto = parts.length > 2 && Boolean.parseBoolean(parts[2]);
            addRule(from, to, auto);
        }
    }

    private void addRule(String from, String to, boolean auto) {
        var row = LayoutInflater.from(getActivity()).inflate(R.layout.config_redirect_row, rulesContainer, false);
        var fromEdit = row.<EditText>findViewById(R.id.from);
        var toEdit = row.<EditText>findViewById(R.id.to);
        var autoCheck = row.<CheckBox>findViewById(R.id.auto);

        fromEdit.setText(from);
        toEdit.setText(to);
        autoCheck.setChecked(auto);

        var watcher = new DefaultTextWatcher() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                saveRules();
            }
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
        var rules = new ArrayList<String>();
        for (var i = 0; i < rulesContainer.getChildCount(); i++) {
            var row = rulesContainer.getChildAt(i);
            var from = row.<EditText>findViewById(R.id.from).getText().toString().trim();
            var to = row.<EditText>findViewById(R.id.to).getText().toString().trim();
            var auto = row.<CheckBox>findViewById(R.id.auto).isChecked();
            if (!from.isEmpty() || !to.isEmpty()) {
                rules.add(from + FIELD_SEP + to + FIELD_SEP + auto);
            }
        }
        rulesPref.set(rules);
    }
}
