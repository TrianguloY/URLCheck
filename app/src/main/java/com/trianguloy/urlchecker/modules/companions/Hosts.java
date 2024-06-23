package com.trianguloy.urlchecker.modules.companions;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.util.Pair;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.JsonCatalog;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.wrappers.InternalFile;
import com.trianguloy.urlchecker.utilities.wrappers.ProgressDialog;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/** Represents and manages the hosts data */
public class Hosts {

    private final HostsCatalog data;

    private static final char SEPARATOR = '\t';
    private static final int FILES = 128;
    public static final String PREFIX = "hosts_";

    // A custom mapping from a given hash with queryable buckets
    private final HashMap<Integer, HashMap<String, Pair<String, String>>> buckets = new HashMap<>();
    private final Activity cntx;

    public Hosts(Activity cntx) {
        this.cntx = cntx;
        data = new HostsCatalog(cntx);
    }

    /** Builds the hosts database (asks first) */
    public void build(boolean showEditor, Runnable onFinished) {
        var builder = new AlertDialog.Builder(cntx)
                .setTitle(R.string.mHosts_buildTitle)
                .setMessage(R.string.mHosts_buildDesc)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        ProgressDialog.run(cntx, R.string.mHosts_buildProgress, progress -> {
                            progress.setMessage(cntx.getString(R.string.mHosts_buildInit));
                            _build(progress, onFinished);
                        })
                );

        if (showEditor) builder
                .setNeutralButton(R.string.json_editor, (dialog, which) -> data.showEditor());

        builder.show();
    }

    /** @see JsonCatalog#showEditor */
    public void showEditor() {
        data.showEditor();
    }

    /** Background thread that builds the database (and notifies progress) */
    private void _build(ProgressDialog progress, Runnable onFinished) {
        var catalog = data.getCatalog();

        // delete previous first
        var fileNames = getFileNames();
        progress.setMessage(cntx.getString(R.string.mHosts_buildClear));
        progress.setMax(fileNames.size());
        for (var fileName : fileNames) {
            cntx.deleteFile(fileName);
            progress.increaseProgress();
        }

        // iterate for each entry
        Log.d("HOSTS", "Building mapping");
        progress.setMax(catalog.length());
        for (var label : JavaUtils.toList(catalog.keys())) {
            try {
                var entry = catalog.getJSONObject(label);
                if (!entry.optBoolean("enabled", true)) continue;
                var color = entry.optString("color", "-");
                var replace = entry.optBoolean("replace", true);
                if (entry.has("file")) {
                    // download from remote file
                    var file = entry.optString("file");

                    progress.setMessage(cntx.getString(R.string.mHosts_buildDownload, label, file));

                    Log.d("HOSTS", "Downloading " + file);
                    HttpUtils.streamFromUrl(file, line -> {
                        var parts = line.replaceAll("#.*", "").trim().split("\\s+");
                        // everything except the first entry is a possible host
                        for (int i = 1; i < parts.length; i++) {
                            add(parts[i], Pair.create(label, color), replace);
                        }
                        // just the host, special syntax
                        if (parts.length == 1) add(parts[0], Pair.create(label, color), replace);
                    });
                }
                if (entry.has("hosts")) {
                    // add hosts directly
                    var hosts = entry.getJSONArray("hosts");
                    for (int i = 0; i < hosts.length(); i++) {
                        add(hosts.getString(i), Pair.create(label, color), replace);
                    }
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            progress.increaseProgress();
        }


        // save as files
        progress.setMessage(cntx.getString(R.string.mHosts_buildSave));
        progress.setMax(buckets.size());
        for (var bucket : buckets.entrySet()) {
            var builder = new StringBuilder();
            for (var entry : bucket.getValue().entrySet()) {
                // each line has <domain,label,color>
                builder.append(entry.getKey().replace(SEPARATOR, ' '))
                        .append(SEPARATOR)
                        .append(entry.getValue().first.replace(SEPARATOR, ' '))
                        .append(SEPARATOR)
                        .append(entry.getValue().second.replace(SEPARATOR, ' '))
                        .append("\n");
            }

            progress.increaseProgress();

            Log.d("HOSTS", "Creating entries " + bucket.getValue().size());
            new InternalFile(PREFIX + bucket.getKey(), cntx)
                    .set(builder.toString());
        }

        // notify finish
        Log.d("HOSTS", "Built: " + size() + " entries");
        progress.dismiss();
        cntx.runOnUiThread(() -> {
            if (onFinished != null) onFinished.run();
        });
    }

    /** return true if the database is built */
    public boolean isUninitialized() {
        return buckets.isEmpty() && getFileNames().isEmpty();
    }

    /** Returns the label and color for the host, or bigger partial host (no subdomain). null if not in the database */
    public Pair<String, String> contains(String host) {
        var contains = containsExact(host);
        if (contains != null) return contains;
        if (!host.contains(".")) return null; // no more subdomains to check
        return contains(host.split("\\.", 2)[1]); // remove leftmost subdomain
    }

    /** Returns the label and color for the exact host, null if not in the database */
    public Pair<String, String> containsExact(String host) {
        return getBucket(host, key -> {
            var values = new HashMap<String, Pair<String, String>>();
            new InternalFile(PREFIX + key, cntx).stream(line -> {
                // each line has <domain,label,color>
                var elements = line.split(String.valueOf(SEPARATOR), 3);
                if (elements.length == 3) values.put(elements[0], Pair.create(elements[1], elements[2]));
            });
            return values;
        }).get(host);
    }

    /* ------------------- internal ------------------- */

    /** returns all the files from this database */
    private ArrayList<String> getFileNames() {
        var files = new ArrayList<String>();
        for (var fileName : cntx.fileList()) {
            if (fileName.startsWith(PREFIX)) files.add(fileName);
        }
        return files;
    }

    /** The number of hosts. */
    public int size() {
        // I miss streams :(
        var s = 0;
        for (var value : buckets.values()) {
            s += value.size();
        }
        return s;
    }

    /**
     * Add a new host to the memory catalog
     * If replace is false existing entries won't be replaced
     */
    private void add(String host, Pair<String, String> data, boolean replace) {
        var bucket = getBucket(host, k -> new HashMap<>());
        if (replace || !bucket.containsKey(host)) bucket.put(host, data);
    }

    /** returns the bucket of a value, if not ready computes it */
    private HashMap<String, Pair<String, String>> getBucket(String value, JavaUtils.Function<Integer, HashMap<String, Pair<String, String>>> compute) {
        // HASHING
        var hash = Math.floorMod(value.hashCode(), FILES);

        // get
        var bucket = buckets.get(hash);
        if (bucket == null) {
            // or set
            bucket = compute.apply(hash);
            buckets.put(hash, bucket);
        }

        return bucket;
    }

}
