package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/** Activity for editing a json */
public class JsonEditorActivity extends Activity {

    public static final String EXTRA_CLASS = "data";

    private static final int INDENT_SPACES = 2;

    private JsonEditorInterface provider;
    private TextView editor;
    private View info;

    // ------------------- listeners -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_json_editor);
        setTitle(R.string.json_editor);
        AndroidUtils.configureUp(this);


        try {
            // this is a bit dangerous
            provider = ((Class<JsonEditorInterface>) getIntent().getSerializableExtra(EXTRA_CLASS)).getConstructor(Activity.class).newInstance(this);
        } catch (Exception e) {
            AndroidUtils.assertError("Unable to instantiate the JsonEditorInterface", e);
            Toast.makeText(this, R.string.invalid, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        info = findViewById(R.id.info);
        this.<TextView>findViewById(R.id.description).setText(provider.getEditorDescription());

        editor = findViewById(R.id.data);
        editor.setText(noFailToString(provider.getJson()));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_json_editor, menu);
        AndroidUtils.fixMenuIconColor(menu.findItem(R.id.menu_format), this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // press the 'back' button in the action bar to go back
            case android.R.id.home -> onBackPressed();
            // format content
            case R.id.menu_format -> format();
            // save
            case R.id.menu_save -> save();
            // discard
            case R.id.menu_discard -> discard();
            // reset
            case R.id.menu_reset -> reset();
            // toggle info visibility
            case R.id.menu_show_info -> {
                item.setChecked(!item.isChecked());
                info.setVisibility(item.isChecked() ? View.VISIBLE : View.GONE);
            }

            default -> {
                return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // validate
        JSONObject currentData;
        try {
            currentData = new JSONObject(editor.getText().toString());
        } catch (JSONException e) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.invalid)
                    .setMessage(R.string.json_ignore)
                    .setPositiveButton(R.string.json_discard_close, (dialog, which) -> finish())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }

        // check changes, exit if there are none
        if (Objects.equals(noFailToString(currentData), noFailToString(provider.getJson()))) {
            finish();
            return;
        }

        // ask to save or discard
        new AlertDialog.Builder(this)
                .setTitle(R.string.save)
                .setMessage(R.string.json_save)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    var result = provider.saveJson(currentData);
                    if (result == null) {
                        // all ok, exit
                        finish();
                    } else {
                        // error while saving
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.invalid)
                                .setMessage(getString(R.string.json_save_error, result))
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                })
                .setNeutralButton(R.string.discard, (dialog, which) -> finish())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /* ------------------- actions ------------------- */

    /** Formats the editor content, shows a dialog if invalid */
    private void format() {
        try {
            editor.setText(new JSONObject(editor.getText().toString()).toString(INDENT_SPACES));
        } catch (JSONException e) {
            // invalid json
            new AlertDialog.Builder(this)
                    .setTitle(R.string.invalid)
                    .setMessage(R.string.json_format_error)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    /** Discard the editor changes and restore the saved contents */
    private void discard() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.discard)
                .setMessage(R.string.json_discard)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> editor.setText(noFailToString(provider.getJson())))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Discard the editor changes and restore the built-in contents */
    private void reset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset)
                .setMessage(R.string.json_reset)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> editor.setText(noFailToString(provider.getBuiltInJson())))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Saved current content. Display a dialog if it's not valid json or the saving failed */
    private void save() {
        String result;
        try {
            result = provider.saveJson(new JSONObject(editor.getText().toString()));
        } catch (JSONException e) {
            // invalid json
            result = getString(R.string.json_format_error);
        }

        if (result != null) {
            // error while saving
            new AlertDialog.Builder(this)
                    .setTitle(R.string.invalid)
                    .setMessage(getString(R.string.json_save_error, result))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    /* ------------------- utils ------------------- */

    /** Formats a json into a valid string that doesn't throws a JSON exception */
    private static String noFailToString(JSONObject content) {
        try {
            return content.toString(INDENT_SPACES);
        } catch (JSONException e) {
            // panic, don't format then
            return content.toString();
        }
    }

}
