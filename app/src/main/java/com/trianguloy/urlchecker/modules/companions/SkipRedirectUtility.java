package com.trianguloy.urlchecker.modules.companions;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.sUTF_8;

import android.util.Base64;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logic to skip url redirections that are embedded in a query parameter
 * (for example "https://tracker.com/?url=https%3A%2F%2Fexample.com").
 * Supports plain/url-encoded values, (nested) base64 values and json values.
 * Pure logic, no android views/resources involved.
 */
public class SkipRedirectUtility {

    /** Protocols considered as a valid redirection target */
    private static final List<String> VALID_PROTOCOLS = Arrays.asList(
            "finger:",
            "ftp://",
            "ftps://",
            "freenet:",
            "gemini:",
            "gopher:",
            "http://",
            "https://",
            "ipfs:",
            "mailto:",
            "magnet:",
            "wap:",
            "xmpp:"
    );

    /** Query parameters (and json fields) that may contain a redirection. Case sensitive. */
    private static final List<String> REDIRECT_PARAMS = Arrays.asList(
            "dl_target_url",
            "ds_dest_url",
            "kaRdt",
            "lp",
            "lpurl",
            "rdr",
            "redirect",
            "redirect_uri",
            "spld",
            "target",
            "tURL",
            "u",
            "uri",
            "url",
            "url64fb"
    );

    /** scheme + authority of an url, to compare origins */
    private static final Pattern ORIGIN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*:)(//[^/?#]*)?");

    /** Result of skipping redirections */
    public static class Result {
        /** The final url (same as the input if nothing was skipped) */
        public final String url;
        /** Names of the parameters followed, in order */
        public final List<String> steps;

        Result(String url, List<String> steps) {
            this.url = url;
            this.steps = steps;
        }

        public boolean changed(String original) {
            return !url.equals(original);
        }
    }

    // ------------------- public api -------------------

    /** Follows all the embedded redirections of [url] and returns the final one */
    public static Result skip(String url) {
        var steps = new ArrayList<String>();
        return new Result(skip(url, steps), steps);
    }

    // ------------------- redirect skipping -------------------

    private static String skip(String url, List<String> steps) {
        if (url == null) return url;

        var originalOrigin = originOf(url);
        var lastValid = url;

        var queryParams = parseQuery(url);
        for (var param : REDIRECT_PARAMS) {
            var values = queryParams.get(param);
            if (values == null) continue;
            for (var rawValue : values) {
                var decodedRedirect = tryDecodeRedirectUrl(rawValue);
                if (decodedRedirect == null || decodedRedirect.equals(url)) continue;

                steps.add(param);
                if (!originOf(decodedRedirect).equalsIgnoreCase(originalOrigin)) {
                    // different origin: this is the redirection, continue from there
                    return skip(decodedRedirect, steps);
                } else {
                    // same origin: keep it as candidate, but keep looking
                    lastValid = skip(decodedRedirect, steps);
                }
            }
        }

        return lastValid;
    }

    /** scheme + authority of a url (lowercase), or empty if can't be parsed */
    private static String originOf(String url) {
        Matcher matcher = ORIGIN.matcher(url);
        return matcher.find() ? matcher.group().toLowerCase() : "";
    }

    // ------------------- query parsing -------------------

    /** Returns the (url-decoded once) query parameters, without requiring a valid uri */
    private static java.util.Map<String, List<String>> parseQuery(String url) {
        var params = new java.util.HashMap<String, List<String>>();

        int start = url.indexOf('?');
        if (start == -1) return params;
        int end = url.indexOf('#', start + 1);
        if (end == -1) end = url.length();

        for (var pair : url.substring(start + 1, end).split("&")) {
            if (pair.isEmpty()) continue;
            var parts = pair.split("=", 2);
            var key = tryUrlDecode(parts[0]);
            if (key == null) continue;
            var value = parts.length > 1 ? tryUrlDecode(parts[1]) : "";
            if (value == null) continue;
            var list = params.get(key);
            if (list == null) params.put(key, list = new ArrayList<>());
            list.add(value);
        }
        return params;
    }

    // ------------------- decoding -------------------

    /** Tries to extract a redirection url from a parameter value. Null if none. */
    private static String tryDecodeRedirectUrl(String value) {
        if (value == null || value.isEmpty()) return null;

        // 1. plain (already decoded once by the query parser)
        if (startsWithProtocol(value)) return value;

        // 1b. still url-encoded (double encoded)
        var urlDecoded = tryUrlDecode(value);
        if (urlDecoded != null && startsWithProtocol(urlDecoded)) return urlDecoded;

        // 2. base64, ignoring up to 3 junk leading chars (some trackers prefix the payload)
        var temp = value;
        for (int i = 0; i < 4 && temp.length() > 1; i++, temp = temp.substring(1)) {
            var base64Decoded = tryBase64Decode(temp);
            if (base64Decoded == null) continue;

            for (var candidate : Arrays.asList(base64Decoded, tryUrlDecode(base64Decoded))) {
                if (candidate == null) continue;
                var found = findProtocol(candidate);
                if (found != null) return found;
            }
        }

        // 3. json
        var jsonFields = tryExtractJsonFields(value);
        if (jsonFields != null) {
            for (var param : REDIRECT_PARAMS) {
                var possible = jsonFields.get(param);
                if (possible == null) continue;
                var candidate = tryDecodeRedirectUrl(possible);
                if (candidate != null) return candidate;
            }
        }

        return null;
    }

    private static boolean startsWithProtocol(String value) {
        var lower = value.toLowerCase();
        for (var protocol : VALID_PROTOCOLS) {
            if (lower.startsWith(protocol)) return true;
        }
        return false;
    }

    /** Returns the substring starting at the first valid protocol, or null if none */
    private static String findProtocol(String text) {
        var lower = text.toLowerCase();
        int best = -1;
        for (var protocol : VALID_PROTOCOLS) {
            int index = lower.indexOf(protocol);
            if (index != -1 && (best == -1 || index < best)) best = index;
        }
        return best == -1 ? null : text.substring(best);
    }

    private static String tryUrlDecode(String value) {
        if (value == null) return null;
        try {
            return URLDecoder.decode(value, sUTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String tryBase64Decode(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            var base64 = new StringBuilder(value.replace('-', '+').replace('_', '/'));
            while (base64.length() % 4 != 0) base64.append('=');
            var bytes = Base64.decode(base64.toString(), Base64.DEFAULT);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** Extracts the known redirection fields from a (maybe url-encoded) json string */
    private static java.util.Map<String, String> tryExtractJsonFields(String value) {
        var json = value.trim();
        if (!json.startsWith("{")) {
            var decoded = tryUrlDecode(value);
            if (decoded == null || !decoded.trim().startsWith("{")) return null;
            json = decoded.trim();
        }

        var map = new java.util.HashMap<String, String>();
        for (var param : REDIRECT_PARAMS) {
            var matcher = Pattern.compile("\"" + Pattern.quote(param) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
            if (matcher.find()) {
                // minimal json unescape
                map.put(param, matcher.group(1).replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\"));
            }
        }
        return map.isEmpty() ? null : map;
    }
}
