package com.trianguloy.urlchecker.utilities.wrappers;

import android.content.Context;

import com.trianguloy.urlchecker.utilities.methods.StreamUtils;

import java.io.IOException;

/** Represents a file from assets (read-only) */
public class AssetFile {
    private final String fileName;
    private final Context cntx;

    public AssetFile(String fileName, Context cntx) {
        this.fileName = fileName;
        this.cntx = cntx;
    }

    /** Returns the content of the file, or null if can't be read) */
    public String get() {
        // get the updated file first
        try {
            return StreamUtils.inputStream2String(cntx.getAssets().open(fileName));
        } catch (IOException ignored) {
            return null;
        }
    }

}
