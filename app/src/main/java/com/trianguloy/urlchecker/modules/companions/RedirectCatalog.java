package com.trianguloy.urlchecker.modules.companions;

import android.app.Activity;
import android.content.Context;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.JsonCatalog;

import org.json.JSONException;
import org.json.JSONObject;

public class RedirectCatalog extends JsonCatalog {

    public RedirectCatalog(Activity cntx) {
        super(cntx, "redirect", R.string.mRedir_json);
    }

    @Override
    public JSONObject buildBuiltIn(Context cntx) throws JSONException {
        return new JSONObject()
                .put("x ➔ twitter", new JSONObject()
                        .put("from", "x")
                        .put("to", "twitter")
                        .put("auto", false)
                );
    }
}
