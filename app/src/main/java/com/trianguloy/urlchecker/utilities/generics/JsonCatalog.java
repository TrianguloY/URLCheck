package com.trianguloy.urlchecker.utilities.generics;

import android.app.Activity;
import android.content.Context;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.JsonEditorInterface;
import com.trianguloy.urlchecker.utilities.wrappers.InternalFile;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a generic catalog
 */
public abstract class JsonCatalog implements JsonEditorInterface {

    private final Activity cntx;
    private final InternalFile custom;
    private final String editorDescription;

    public JsonCatalog(Activity cntx, String fileName, int editorDescription) {
        this(cntx, fileName, cntx.getString(editorDescription));
    }

    public JsonCatalog(Activity cntx, String fileName, String editorDescription) {
        this.cntx = cntx;
        this.editorDescription = editorDescription;
        custom = new InternalFile(fileName, cntx);
    }

    /**
     * Returns the current catalog
     */
    public JSONObject getCatalog() {
        // get the updated file first
        try {
            String content = custom.get();
            if (content != null) return new JSONObject(content);
        } catch (JSONException ignored) {
        }

        // no updated file or can't read, use built-in one
        return getBuiltIn();
    }

    /**
     * Gets the builtin catalog
     */
    public JSONObject getBuiltIn() {
        try {
            return buildBuiltIn(cntx);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    /**
     * Builds the builtIn catalog
     */
    abstract public JSONObject buildBuiltIn(Context cntx) throws JSONException;

    /**
     * Saves a json as new catalog
     */
    public boolean save(JSONObject content) {

        // same as builtin (maybe a reset?), delete custom
        if (content.toString().equals(getBuiltIn().toString())) {
            custom.delete();
            return true;
        }

        // store
        return custom.set(content.toString());
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
        return save(data) ? null : cntx.getString(R.string.invalid);
    }

    @Override
    public String getEditorDescription() {
        return editorDescription;
    }

    /** Displays a generic editor for json content. */
    public void showEditor() {
        showEditor(cntx);
    }

}