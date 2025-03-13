package com.trianguloy.urlchecker.activities;

import static com.trianguloy.urlchecker.activities.JsonEditorActivity.EXTRA_CLASS;

import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

/**
 * If a class implements this, it will be able to show a JSON editor.
 * IMPORTANT! The class must also have a constructor with a single Activity parameter.
 */
public interface JsonEditorInterface {

    /* ------------------------- implementation ------------------------- */

    /** Should return the currently saved json */
    JSONObject getJson();

    /** Should return the built-in json */
    JSONObject getBuiltInJson();

    /** Should saves a json. Return null if all ok, a message string to display if not */
    String saveJson(JSONObject data);

    /** The string id of the description to show in the editor */
    String getEditorDescription();

    /* ------------------------- usage ------------------------- */

    /** Displays a generic editor for json content. */
    default void showEditor(Context cntx) {
        cntx.startActivity(new Intent(cntx, JsonEditorActivity.class)
                .putExtra(EXTRA_CLASS, this.getClass()));
    }
}
