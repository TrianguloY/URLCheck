package com.trianguloy.urlchecker.utilities.methods;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Consumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Generic utilities related to streams (urls, strings, bytes...) */
public interface StreamUtils {

    /** Reads an input stream and returns its content as a string. The stream is closed afterwards. */
    static String inputStream2String(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() != 0) sb.append("\n");
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /** Reads an input stream and transfers its content to a file. The stream is NOT closed. */
    static void inputStream2File(InputStream in, File file) throws IOException {
        try (var out = new FileOutputStream(file)) {
            inputStream2OutputStream(in, out);
        }
    }

    /** Reads an input stream and transfers its content to an output stream. The streams are NOT closed. */
    static void inputStream2OutputStream(InputStream in, OutputStream out) throws IOException {
        var buffer = new byte[10240];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /** Reads an input stream and streams its lines. */
    static void consumeLines(InputStream is, Consumer<String> function) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                function.accept(line);
            }
        } catch (IOException e) {
            AndroidUtils.assertError("Unable to consume stream", e);
        }
    }

    /** Returns the SHA-256 hash of a string. */
    static String sha256(String string) {
        try {
            // get byte array
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(string.getBytes(UTF_8));

            // convert to string
            // adapted from https://stackoverflow.com/questions/7166129/how-can-i-calculate-the-sha-256-hash-of-a-string-in-android/7166240#7166240
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b & 0xFF));
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            AndroidUtils.assertError("this should never happen, all Androids must support sha-256", e);
            return "";
        }
    }

}
