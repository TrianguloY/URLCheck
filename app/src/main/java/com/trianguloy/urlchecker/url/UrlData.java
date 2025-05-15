package com.trianguloy.urlchecker.url;

import com.trianguloy.urlchecker.modules.AModuleDialog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Manages an url and extra data associated with it
 * I don't like this approach, but unfortunately I can't think of something better
 * TODO: make this immutable with a builder/factory or something
 */
public class UrlData {

    // ------------------- url -------------------

    public final String url;

    public UrlData(String url) {
        this.url = url == null ? "" : url;
    }

    // ------------------- optional data -------------------

    /** The module that triggered this data (null if internal) */
    public AModuleDialog trigger;

    /** If set, the module that triggers the update will be notified (all callbacks) */
    public boolean triggerOwn = true;

    public UrlData dontTriggerOwn() {
        triggerOwn = false;
        return this;
    }

    /** If set, the url will not be changed (future setUrl calls will be ignored) */
    public boolean disableUpdates = false;

    public UrlData disableUpdates() {
        disableUpdates = true;
        return this;
    }

    // ------------------- extra data -------------------

    private final LinkedHashMap<String, String> extraData = new LinkedHashMap<>(); // keeps order

    /** saves a key-value data, will be kept with automatic updates (but not with manual ones) */
    public UrlData putData(String key, String value) {
        extraData.put(key, value);
        return this;
    }

    /** gets a key-value data, those set on this update or previous automatic ones */
    public String getData(String key) {
        return extraData.get(key);
    }

    /** Returns all entries with a given prefix, in insertion order */
    public List<String> getDataByPrefix(String prefix) {
        var entries = new ArrayList<String>();
        for (var entry : extraData.entrySet()) {
            if (entry.getKey().startsWith(prefix)) entries.add(entry.getValue());
        }
        return entries;
    }

    /** adds all data from the parameter into this object. Keeps insertion order [...urlData.extraData,...this.extraData] */
    public void mergeData(UrlData urlData) {
        // there is no putAllFirst
        var thisExtraData = new LinkedHashMap<>(extraData);
        extraData.clear();
        extraData.putAll(urlData.extraData);
        extraData.putAll(thisExtraData);
    }

    @Override
    public String toString() {
        return "UrlData{" + "url='" + url + '\'' +
                ", trigger=" + trigger +
                ", triggerOwn=" + triggerOwn +
                ", disableUpdates=" + disableUpdates +
                ", extraData=" + extraData +
                '}';
    }
}
