package com.trianguloy.urlchecker.utilities.methods;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;


/** HttpUtils class contains the method related to url. */
public class HttpUtils {
    public static final int CONNECT_TIMEOUT = 5000;

    /** GETs an URL and returns the content as a string. */
    public static String readFromUrl(String url) throws IOException {
        var connection = new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        return StreamUtils.inputStream2String(connection.getInputStream());
    }

    /** GETs an URL and streams its lines. */
    public static void streamFromUrl(String url, JavaUtils.Consumer<String> consumer) throws IOException {
        var connection = new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        StreamUtils.consumeLines(connection.getInputStream(), consumer);
    }

    /** POSTs something (a body) to an URL and returns its content as a string. */
    public static String performPOST(String url, String body) throws IOException {
        // Send POST data request
        var conn = (HttpsURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        try (var wr = new OutputStreamWriter(conn.getOutputStream())) {
            wr.write(body);
            wr.flush();
        }
        // Get the server response
        return StreamUtils.inputStream2String(
                conn.getResponseCode() >= 200 && conn.getResponseCode() < 300
                        ? conn.getInputStream()
                        : conn.getErrorStream()
        );
    }

    /**
     * Same as performPOST, but for a different module.
     * TODO: use a unique method for all connections (merge with the virusTotal v3 branch issue)
     */
    public static int performPOSTJSON(String url, String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return conn.getResponseCode();
    }
}
