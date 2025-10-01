package com.trianguloy.urlchecker.modules.companions;

import static android.util.Base64.NO_PADDING;
import static android.util.Base64.NO_WRAP;
import static android.util.Base64.URL_SAFE;

import android.content.Context;
import android.util.Base64;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.wrappers.Connection;

import org.json.JSONException;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class that manages the virusTotal connection
 */
public class VirusTotalUtility {

    static private final String URLS_ENDPOINT = "https://www.virustotal.com/api/v3/urls";
    static private final String USERS_ENDPOINT = "https://www.virustotal.com/api/v3/users";
    public static final String HEADER_KEY = "x-apikey";

    /** Returns the username whose owner is the provided key */
    static public String getUser(String key) {
        var result = Connection.to(USERS_ENDPOINT + "/" + key)
                .addHeader(HEADER_KEY, key)
                .acceptJson()
                .connect()
                .getResultAsJson();
        if (result == null) return null;
        try {
            return result.getJSONObject("data").getString("id");
        } catch (JSONException e) {
            AndroidUtils.assertError("Unable to parse virustotal user response", e);
            return null;
        }
    }

    static public class InternalResponse {
        public String error;
        public int detectionsPositive;
        public int detectionsTotal;
        public String date;
        public String scanUrl;
        public String info;
        public String debugData;
    }

    /** Returns the analysis of an url, or null if the analysis is in progress */
    public static InternalResponse scanUrl(String urlToScan, String key, Context cntx) {

        // connect
        var encodedUrl = Base64.encodeToString(urlToScan.getBytes(), NO_PADDING | NO_WRAP | URL_SAFE);
        var connection = Connection.to(URLS_ENDPOINT + "/" + encodedUrl)
                .addHeader(HEADER_KEY, key)
                .acceptJson()
                .connect();

        if (connection.getStatusCode() == 404) {
            // not analyzed yet
            return analyzeUrl(urlToScan, key, cntx);
        }

        var result = new InternalResponse();
        var response = connection.getResultAsJson();
        if (response == null || connection.getStatusCode() != 200) {
            // error
            result.error = cntx.getString(R.string.mVT_error);
            return result;
        }

        // parse response
        try {
            result.debugData = response.toString(2);
            result.scanUrl = "https://www.virustotal.com/gui/url/" + encodedUrl;

            // parse attributes
            var attributes = response.getJSONObject("data").getJSONObject("attributes");

            if (!attributes.has("last_analysis_date")) {
                // still analyzing
                return null;
            }

            // get stats
            var stats = attributes.getJSONObject("last_analysis_stats");
            result.detectionsPositive = stats.optInt("malicious", 0)
                    + stats.optInt("suspicious", 0);
            result.detectionsTotal = stats.optInt("undetected", 0)
                    + stats.optInt("harmless", 0);
            result.date = DateFormat.getInstance().format(new Date(attributes.getLong("last_analysis_date") * 1000));

            // build summary info
            var info = new StringBuilder();
            info.append(cntx.getString(R.string.mVt_title)).append(" ").append(attributes.optString("title")).append("\n");
            info.append(cntx.getString(R.string.mVt_finalUrl)).append(" ").append(attributes.optString("last_final_url")).append("\n");
            info.append(cntx.getString(R.string.mVt_reputation)).append(" ").append(attributes.optString("reputation")).append("\n");

            var votes = attributes.optJSONObject("total_votes");
            if (votes != null) {
                info.append(cntx.getString(R.string.mVt_votes)).append(" +").append(votes.optInt("harmless")).append(" / -").append(votes.optInt("malicious")).append("\n");
            }

            var categories = attributes.optJSONObject("categories");
            if (categories != null && categories.length() > 0) {
                info.append(cntx.getString(R.string.mVt_categories)).append("\n");
                for (var engine : JavaUtils.toList(categories.keys())) {
                    var category = categories.getString(engine);
                    info.append(" - ").append(category).append(" (").append(engine).append(")\n");
                }
            }

            var results = attributes.getJSONObject("last_analysis_results");
            var none = true;
            if (results.length() > 0) {
                info.append(cntx.getString(R.string.mVt_detections));
                for (var k : JavaUtils.toList(results.keys())) {
                    var engine = results.getJSONObject(k);
                    if (List.of("malicious", "suspicious").contains(engine.getString("category"))) {
                        info.append("\n - ").append(engine.getString("result")).append(" (").append(engine.getString("engine_name")).append(")");
                        none = false;
                    }
                }
            }
            if (none) info.append(" ").append(cntx.getString(R.string.none));


            result.info = info.toString();
            result.error = null;
        } catch (JSONException e) {
            AndroidUtils.assertError("Can't parse VirusTotal response", e);
            result.error = cntx.getString(R.string.mVT_error);
        }

        return result;
    }

    /** Requests an analysis (if not done already) */
    static private InternalResponse analyzeUrl(String urlToScan, String key, Context cntx) {
        var code = Connection.to(URLS_ENDPOINT)
                .addHeader(HEADER_KEY, key)
                .acceptJson()
                .postFormUrlEncoded(Map.of("url", urlToScan))
                .getStatusCode();

        if (code != 200) {
            // error
            var result = new InternalResponse();
            result.error = cntx.getString(R.string.mVT_error);
            return result;
        }

        // ok
        return null;
    }

}
