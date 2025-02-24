package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonGuide extends ResourceGuide<JSONObject> {

    @Override
    public JSONObject getEmpty() {
        // FIXME: new JSONObject() instead?
        return JavaUtils.toJson("{\"empty\":{}}");
    }

    @Override
    public JSONObject toObjectThrows(String string) throws JSONException {
        return new JSONObject(string);
    }

    @Override
    public String toString(JSONObject object) {
        return object.toString();
    }
}
