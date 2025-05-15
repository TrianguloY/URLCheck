package com.trianguloy.urlchecker.modules.companions;

import android.app.Activity;
import android.content.Context;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.JsonCatalog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Represents the catalog of the Pattern module */
public class PatternCatalog extends JsonCatalog {

    public PatternCatalog(Activity cntx) {
        super(cntx, "patterns", R.string.mPttrn_editor);
    }

    @Override
    public JSONObject buildBuiltIn(Context cntx) throws JSONException {
        return new JSONObject()

                // built from the translated strings
                .put(cntx.getString(R.string.mPttrn_ascii), new JSONObject()
                        .put("regex", "[^\\p{ASCII}]")
                )
                .put(cntx.getString(R.string.mPttrn_upperDomain), new JSONObject()
                        .put("regex", "^.*?://[^/?#]*[A-Z]")
                )
                .put(cntx.getString(R.string.mPttrn_http), new JSONObject()
                        .put("regex", "^http://")
                        .put("replacement", "https://")
                )
                .put(cntx.getString(R.string.mPttrn_noSchemeHttp), new JSONObject()
                        .put("regex", "^(?!.*:)")
                        .put("replacement", "http://$0")
                )
                .put(cntx.getString(R.string.mPttrn_noSchemeHttps), new JSONObject()
                        .put("regex", "^(?!.*:)")
                        .put("replacement", "https://$0")
                )
                .put(cntx.getString(R.string.mPttrn_wrongSchemaHttp), new JSONObject()
                        .put("regex", "^(?!http:)[hH][tT]{2}[pP]:(.*)")
                        .put("replacement", "http:$1")
                        .put("automatic", true)
                )
                .put(cntx.getString(R.string.mPttrn_wrongSchemaHttps), new JSONObject()
                        .put("regex", "^(?!https:)[hH][tT]{2}[pP][sS]:(.*)")
                        .put("replacement", "https:$1")
                        .put("automatic", true)
                )

                // privacy redirections samples (see https://github.com/TrianguloY/URLCheck/discussions/122)
                .put("Reddit ➔ Teddit", new JSONObject()
                        .put("regex", "^https?://(?:[a-z0-9-]+\\.)*?reddit\\.com/(.*)")
                        .put("replacement", "https://teddit.net/$1")
                        .put("enabled", false)
                )
                .put("Twitter ➔ Nitter", new JSONObject()
                        .put("regex", "^https?://(?:[a-z0-9-]+\\.)*?twitter\\.com/(.*)")
                        .put("replacement", "https://nitter.net/$1")
                        .put("enabled", false)
                )
                .put("Youtube ➔ Invidious", new JSONObject()
                        .put("regex", "^https?://(?:[a-z0-9-]+\\.)*?youtube\\.com/(.*)")
                        .put("replacement", new JSONArray()
                                .put("https://yewtu.be/$1")
                                .put("https://farside.link/invidious/$1")
                        )
                        .put("enabled", false)
                )

                // excludeRegex example. Consider replacing with another service that really allows all urls
                .put("URL ➔ Songlink", new JSONObject()
                        .put("excludeRegex", "^https://song\\.link/")
                        .put("replacement", "https://song.link/$0")
                        .put("enabled", false)
                )
                ;
    }

}