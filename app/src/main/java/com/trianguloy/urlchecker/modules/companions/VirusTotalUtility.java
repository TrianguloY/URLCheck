package com.trianguloy.urlchecker.modules.companions;

import static android.util.Base64.NO_PADDING;
import static android.util.Base64.NO_WRAP;

import android.content.Context;
import android.util.Base64;
import android.util.Pair;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Class that manages the virusTotal connection
 * TODO: replace with generic POST class and move logic to VirusTotalModule
 */
public class VirusTotalUtility {

    static private final String URLS_ENDPOINT = "https://www.virustotal.com/api/v3/urls";

    static public class InternalReponse {
        public String error = "Unknown error";
        public int detectionsPositive;
        public int detectionsTotal;
        public String date;
        public String scanUrl;
        public String info;
    }

    /** Returns the analysis of an url, or null if the analysis is in progress */
    public static InternalReponse scanUrl(String urlToScan, String key, Context cntx) {
        var result = new InternalReponse();

        // get analysis
        Pair<Integer, String> responsePair;
        try {
            responsePair = HttpUtils.readFromUrl(URLS_ENDPOINT + "/" + Base64.encodeToString(urlToScan.getBytes(), NO_PADDING | NO_WRAP), Map.of(
                    "accept", "application/json",
                    "x-apikey", key
            ));
        } catch (IOException e) {
            AndroidUtils.assertError("Can't connect to VirusTotal", e);
            result.error = cntx.getString(R.string.mVT_connectError);
            return result;
        }

        if (responsePair.first == 404) {
            // not analyzed yet
            return analyzeUrl(urlToScan, key, cntx);
        }

        if (responsePair.first != 200) {
            // error
            result.error = cntx.getString(R.string.mVT_jsonError);
            return result;
        }

        // parse response
        try {
            JSONObject response = new JSONObject(responsePair.second);
            result.info = response.toString(1);
            var data = response.getJSONObject("data");
            result.scanUrl = data.getJSONObject("links").getString("self");

            // parse attributes
            var attributes = data.getJSONObject("attributes");

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

            result.error = null;
            return result;
        } catch (JSONException e) {
            AndroidUtils.assertError("Can't parse VirusTotal response", e);
            result.error = cntx.getString(R.string.mVT_jsonError);
        }

        return result;
    }

    /** Requests an analysis (if not one already) */
    static private InternalReponse analyzeUrl(String urlToScan, String key, Context cntx) {
        try {
            var output = HttpUtils.performPOST(URLS_ENDPOINT, "url=" + URLEncoder.encode(urlToScan, "UTF-8"), Map.of(
                    "accept", "application/json",
                    "x-apikey", key,
                    "content-type", "application/x-www-form-urlencoded"
            ));

            if (output.first != 200) {
                var result = new InternalReponse();
                result.error = cntx.getString(R.string.mVT_jsonError);
                return result;
            }

            return null;
        } catch (IOException e) {
            AndroidUtils.assertError("Can't analyze url", e);
            var result = new InternalReponse();
            result.error = cntx.getString(R.string.mVT_jsonError);
            return result;
        }
    }

}
