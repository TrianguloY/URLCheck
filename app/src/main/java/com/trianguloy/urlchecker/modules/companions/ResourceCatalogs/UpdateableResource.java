package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.StreamUtils;

import java.io.IOException;

public class UpdateableResource<T> extends ModifiableResource<T> implements ResourceGuideInterface<T>, ResourceContextInterface {
    /* ------------------- prefs ------------------- */

    protected final GenericPref.Str catalogURL;
    protected final GenericPref.Str hashURL;
    protected final GenericPref.Lng lastUpdate;
    protected final GenericPref.Lng lastCheck;

    /* ------------------- constructor ------------------- */

    public UpdateableResource(BuiltInResource<T> builtIn,
                              String modifiableFile,
                              String key,
                              String catalogUrlDefault,
                              String hashUrlDefault,
                              long lastUpdateDefault) {
        super(builtIn, modifiableFile);

        this.catalogURL = new GenericPref.Str(key + "_catalogURL", catalogUrlDefault, getContext());
        this.hashURL = new GenericPref.Str(key + "_hashURL", hashUrlDefault, getContext());
        this.lastUpdate = new GenericPref.Lng(key + "lastUpdate", lastUpdateDefault, getContext());
        this.lastCheck = new GenericPref.Lng(key + "lastCheck", -1L, getContext());
    }

    // ------------------- ui -------------------

    Button updateButton;
    TextView txt_check;
    TextView txt_update;


    public void attachToUpdateButton(Button button) {
        this.updateButton = button;
        updateButton.setOnClickListener(v -> {
            _update(false);
        });
    }

    public void attachToCheckText(TextView text) {
        this.txt_check = text;
    }

    public void attachToUpdateText(TextView text) {
        this.txt_update = text;
    }

    public void updateText() {
        var context = getContext();
        JavaUtils.ifPresent(txt_check, () ->
                txt_check.setText(AndroidUtils.formatMillis(lastCheck.get(), context)));
        JavaUtils.ifPresent(txt_update, () ->
                txt_update.setText(AndroidUtils.formatMillis(lastUpdate.get(), context)));
    }

    // ------------------- internal -------------------
    @Override
    public void clear() {
        lastUpdate.clear();
        lastCheck.clear();
        super.clear();
    }

    public void _update(boolean background) {
        if (!background) {
            JavaUtils.ifPresent(updateButton, () -> updateButton.setEnabled(false));
        }
        var context = getContext();
        new Thread(() -> {
            // run
            var code = _updateResource();

            if (background) {
                Log.d("UPDATE", context.getString(code.getStringResource()));
            } else {
                context.runOnUiThread(() -> {
                    JavaUtils.ifPresent(updateButton, () -> updateButton.setEnabled(true));
                    updateText();  // text listeners
                    Toast.makeText(context, code.getStringResource(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Replaces the resource with a new one.
     * Network operation, designed to be run in a background thread.
     * Returns the message to display to the user about the result
     */
    protected UpdateResult _updateResource() {
        long now = System.currentTimeMillis();
        lastCheck.set(now);
        // read content
        String rawContent;
        try {
            rawContent = HttpUtils.readFromUrl(catalogURL.get());
        } catch (IOException e) {
            e.printStackTrace();
            return UpdateResult.URL_ERROR;
        }

        // check hash if provided
        if (!hashURL.get().trim().isEmpty()) {

            // read hash
            String hash;
            try {
                hash = HttpUtils.readFromUrl(hashURL.get()).trim();
            } catch (IOException e) {
                e.printStackTrace();
                return UpdateResult.HASH_ERROR;
            }

            // if different, notify
            if (!StreamUtils.sha256(rawContent).equalsIgnoreCase(hash)) {
                return UpdateResult.HASH_MISMATCH;
            }
        }

        // valid, save and update
        var res = _save(rawContent);
        if (res == UpdateResult.UPDATED) {
            lastUpdate.set(now);
        }
        return res;
    }

    /**
     * Saves a new local file. Returns if it was up to date, updated or an error happened.
     */
    private UpdateResult _save(String rawContent) {
        // parse
        T content;
        try {
            content = toObjectThrows(rawContent);
        } catch (Exception e) {
            e.printStackTrace();
            return UpdateResult.INVALID;
        }

        // Compact
        String string = toString(content);

        // nothing changed, nothing to update
        if (string.equals(getCatalog().toString())) {
            return UpdateResult.UP_TO_DATE;
        }

        // same as builtin (probably a reset), clear custom
        if (string.equals(toString(builtIn.getBuiltIn()))) {
            clear();
            return UpdateResult.UPDATED;
        }

        // something new, save
        return writableFile.set(string) ? UpdateResult.UPDATED : UpdateResult.ERROR;
    }
}
