package com.trianguloy.urlchecker.modules.companions;

import static android.widget.LinearLayout.VERTICAL;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Inflater;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

/**
 * Visual editor for the rules catalog. In progress.
 * TODO: extract to separate class (and try to use it for the other catalogs too)
 * TODO: Add 'new rule' inline, reset button?
 */
public class RedirectVisualEditor {

    private final RedirectCatalog catalog;
    private final LayoutInflater inflater;

    public RedirectVisualEditor(Activity cntx) {
        this.catalog = new RedirectCatalog(cntx);
        inflater = LayoutInflater.from(cntx);
    }

    public void showEditor() {
        // prepare views
        var views = inflater.inflate(R.layout.dialog_editor, null);

        // init text
        views.<TextView>findViewById(R.id.text).setText(R.string.mRedir_json);

        // append rules
        var container = views.<LinearLayout>findViewById(R.id.container);
        var rules = catalog.getCatalog();
        for (var name : JavaUtils.toList(rules.keys())) {
            try {
                var data = rules.getJSONObject(name);
                addRule(container, name, data);
            } catch (JSONException e) {
                AndroidUtils.assertError("Invalid rule", e);
            }
        }

        var dialog = new AlertDialog.Builder(inflater.getContext())
                .setTitle(R.string.visual_editor)
                .setView(views)
                .setPositiveButton(R.string.save, (dialogInterface, i) -> saveRules(container))
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.mRedir_add, null)
                .setCancelable(false)
                .show();


        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // prepare more dialog
        // these are configured here to avoid auto-closing the dialog when they are pressed
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(view -> addRule(container, getNewName(container), new JSONObject()));
    }

    private String getNewName(LinearLayout container) {
        var i = 1;

        var names = new HashSet<String>();
        for (int c = 0; c < container.getChildCount(); c++) {
            names.add(container.getChildAt(c).<EditText>findViewById(R.id.name).getText().toString().trim());
        }

        while (true) {
            var name = "Rule " + i;
            if (!names.contains(name)) return name;
            i++;
        }
    }

    private void addRule(LinearLayout container, String name, JSONObject data) {
        var row = Inflater.inflate(R.layout.config_redirect_row, container);

        row.<EditText>findViewById(R.id.name).setText(name);
        row.<EditText>findViewById(R.id.from).setText(data.optString("from"));
        row.<EditText>findViewById(R.id.to).setText(data.optString("to"));
        row.<CheckBox>findViewById(R.id.auto).setChecked(data.optBoolean("auto"));
        row.findViewById(R.id.delete).setOnClickListener(v -> container.removeView(row));
        row.setTag(data); // keep original object to maintain any arbitrary user value
    }


    private void saveRules(LinearLayout container) {
        var rules = new JSONObject();
        for (var i = 0; i < container.getChildCount(); i++) {
            try {
                var row = container.getChildAt(i);
                var name = row.<EditText>findViewById(R.id.name).getText().toString().trim();
                var from = row.<EditText>findViewById(R.id.from).getText().toString().trim();
                var to = row.<EditText>findViewById(R.id.to).getText().toString().trim();
                var auto = row.<CheckBox>findViewById(R.id.auto).isChecked();
                if (!from.isEmpty() || !to.isEmpty()) {
                    rules.put(name, ((JSONObject) row.getTag())
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
