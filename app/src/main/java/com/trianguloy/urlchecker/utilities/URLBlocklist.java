package com.trianguloy.urlchecker.utilities;

import android.content.Context;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.StringPref;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Local incoming-URL rules, shared by the editor and the intent receiver. */
public class URLBlocklist {
    private final StringPref pref;
    private boolean corrupted;

    public URLBlocklist(Context context) {
        // A null default keeps even an explicitly saved default list in preferences.
        pref = new StringPref("urlBlocklist", null, context);
    }

    public List<String> getDomains() {
        corrupted = false;
        final String saved;
        try {
            saved = pref.get();
        } catch (ClassCastException e) {
            corrupted = true;
            return new ArrayList<>();
        }
        if (saved == null) return new ArrayList<>(List.of("iltalehti.fi"));
        var domains = new TreeSet<String>();
        try {
            var array = new JSONArray(saved);
            for (int i = 0; i < array.length(); i++) {
                var entry = array.get(i);
                var host = entry instanceof String ? normalize((String) entry) : null;
                if (host == null) corrupted = true;
                else domains.add(host);
            }
        } catch (JSONException e) {
            // Keep damaged storage untouched until the user explicitly edits the list.
            corrupted = true;
        }
        return new ArrayList<>(domains);
    }

    public boolean isCorrupted() {
        return corrupted;
    }

    public void save(Collection<String> domains) {
        var normalized = new TreeSet<String>();
        for (var domain : domains) {
            var host = normalize(domain);
            if (host == null) throw new IllegalArgumentException("Invalid blocklist domain");
            normalized.add(host);
        }
        pref.set(new JSONArray(normalized).toString());
        corrupted = false;
    }

    /** Returns a DNS hostname, or null for malformed input and unsupported schemes. */
    public static String normalize(String input) {
        if (input == null) return null;
        var candidate = input.trim();
        if (candidate.isEmpty() || candidate.codePoints().anyMatch(Character::isWhitespace)) return null;
        if (candidate.startsWith("//")) candidate = "https:" + candidate;
        else if (!candidate.matches("(?s)^[A-Za-z][A-Za-z0-9+.-]*://.*")) {
            if (candidate.startsWith("/")
                    || candidate.matches("^[A-Za-z][A-Za-z0-9+.-]*:(?![0-9]+(?:[/?#]|$)).*")) return null;
            candidate = "https://" + candidate;
        }
        try {
            var uri = new URI(candidate);
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme())) return null;
            var authority = uri.getRawAuthority();
            if (authority == null) return null;
            if (authority.indexOf('@') != authority.lastIndexOf('@')) return null;
            // Credentials and ports are not part of the saved hostname.
            var host = authority.substring(authority.lastIndexOf('@') + 1);
            var colon = host.lastIndexOf(':');
            if (colon >= 0) {
                var port = host.substring(colon + 1);
                if (!port.matches("[0-9]{1,5}") || Integer.parseInt(port) > 65535) return null;
                host = host.substring(0, colon);
            }
            host = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            if (host.length() > 253) return null;
            var labels = host.split("\\.", -1);
            if (labels.length < 2 || labels[labels.length - 1].matches("[0-9]+")) return null;
            for (var label : labels) {
                if (label.length() > 63 || !label.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")) return null;
                if (label.startsWith("xn--") && IDN.toUnicode(label).equals(label)) return null;
            }
            return host;
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isBlocked(String url, Collection<String> domains) {
        var host = normalize(url);
        if (host == null) return false;
        for (var domain : domains) {
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        }
        return false;
    }
}
