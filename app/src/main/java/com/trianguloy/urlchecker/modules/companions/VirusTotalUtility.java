package com.trianguloy.urlchecker.modules.companions;

import android.content.Context;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Class that manages the virusTotal connection
 * TODO: replace with generic POST class and move logic to VirusTotalModule
 */
public class VirusTotalUtility {

    static private final String urlGetReport = "https://www.virustotal.com/vtapi/v2/url/report";

    static public class InternalReponse {
        public String error = "Unknown error";
        public int detectionsPositive;
        public int detectionsTotal;
        public String date;
        public String scanUrl;
        public String info;
    }

    public static InternalReponse scanUrl(String urlToScan, String key, Context cntx) {
        InternalReponse result = new InternalReponse();

        String responseJSON;
        try {
            responseJSON = HttpUtils.performPOST(urlGetReport, getPOSTparameters(urlToScan, key));
        } catch (IOException e) {
            AndroidUtils.assertError("Can't connect to VirusTotal", e);
            result.error = cntx.getString(R.string.mVT_connectError);
            return result;
        }

        // parse response
        try {
            JSONObject response = new JSONObject(responseJSON);

            result.info = response.toString(1);

            if (response.getInt("response_code") == 1) {
                result.detectionsPositive = response.optInt("positives", -1);
                result.detectionsTotal = response.optInt("total", -1);
                result.date = response.optString("scan_date", "");
                result.scanUrl = response.optString("permalink", "");

                result.error = null;
                return result;
            } else {
                result.error = response.getString("verbose_msg"); // untranslated
            }
        } catch (JSONException e) {
            AndroidUtils.assertError("Can't parse VirusTotal response", e);
            result.error = cntx.getString(R.string.mVT_jsonError);
        }

        return result;

    }

    static private String getPOSTparameters(String url, String key) {
        String data = null;
        try {
            data = URLEncoder.encode("resource", "UTF-8")
                    + "=" + URLEncoder.encode(url, "UTF-8");

            data += "&" + URLEncoder.encode("scan", "UTF-8") + "="
                    + URLEncoder.encode("1", "UTF-8");

            data += "&" + URLEncoder.encode("apikey", "UTF-8")
                    + "=" + URLEncoder.encode(key, "UTF-8");

            data += "&" + URLEncoder.encode("allinfo", "UTF-8")
                    + "=" + URLEncoder.encode("true", "UTF-8");

        } catch (UnsupportedEncodingException e) {
            AndroidUtils.assertError("Can't encode url parameters", e);
        }
        return data;
    }


}
