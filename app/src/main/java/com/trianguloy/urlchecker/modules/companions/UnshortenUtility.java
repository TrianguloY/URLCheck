package com.trianguloy.urlchecker.modules.companions;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.sUTF_8;

import com.trianguloy.urlchecker.utilities.methods.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/** Unshorten logic extracted from main class. */
public class UnshortenUtility {
    public record UnshortenResult(
            boolean success,
            String error,
            String finalUrl,
            int remainingCalls,
            int usageLimit
    ) {
    }

    /** Calls the unshorten api to unshorten [url]. Can be authenticated ([token]!=null) or not */
    public static UnshortenResult unshort(String url, String token) throws IOException, JSONException {
        // get response
        var response = new JSONObject(token.isEmpty()
                // unauthenticated request (technically not a public api)
                ? HttpUtils.readFromUrl("https://unshorten.me/json/" + URLEncoder.encode(url, sUTF_8))
                // authenticated request with public api
                : HttpUtils.readFromUrl("https://unshorten.me/api/v2/unshorten?url=" + URLEncoder.encode(url, sUTF_8), Map.of(
                "Content-Type", "application/json",
                "Authorization", "Token " + token)).second
        );

        var finalUrl = response.getString(token.isEmpty() ? "resolved_url" : "unshortened_url");
        var usageCount = Integer.parseInt(response.optString("usage_count", "0"));
        int usageLimit = 10; // documented but hardcoded
        int remainingCalls = usageLimit - usageCount;
        var error = response.optString("error", response.toString());
        var success = response.getBoolean("success");

        // remaining_calls is not documented, but if it's present use it and replace the hardcoded usage_limit
        try {
            remainingCalls = Integer.parseInt(response.optString("remaining_calls", ""));
            usageLimit = usageCount + remainingCalls;
        } catch (NumberFormatException ignore) {
            // not present, ignore
        }

        return new UnshortenResult(
                success,
                error,
                finalUrl,
                remainingCalls,
                usageLimit
        );
    }
}
