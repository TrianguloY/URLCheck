package com.trianguloy.urlchecker.utilities.methods;

import static java.util.Collections.emptyMap;

import android.util.Pair;

import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Consumer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;


/** HttpUtils class contains the method related to url. */
public class HttpUtils {
    public static final int CONNECT_TIMEOUT = 5000;

    /** GETs an URL and returns the content as a string. */
    public static String readFromUrl(String url) throws IOException {
        return readFromUrl(url, emptyMap()).second;
    }

    /** GETs an URL and returns the content as a string. */
    public static Pair<Integer, String> readFromUrl(String url, Map<String, String> headers) throws IOException {
        var conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        for (var header : headers.entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }

        return Pair.create(conn.getResponseCode(), StreamUtils.inputStream2String(
                conn.getResponseCode() >= 200 && conn.getResponseCode() < 300
                        ? conn.getInputStream()
                        : conn.getErrorStream()
        ));
    }

    /** GETs an URL and streams its lines. */
    public static void streamFromUrl(String url, Consumer<String> consumer) throws IOException {
        var connection = new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        StreamUtils.consumeLines(connection.getInputStream(), consumer);
    }

}
