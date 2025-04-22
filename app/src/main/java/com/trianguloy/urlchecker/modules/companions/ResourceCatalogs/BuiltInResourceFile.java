package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

import android.app.Activity;

import com.trianguloy.urlchecker.utilities.wrappers.AssetFile;

import java.io.FileNotFoundException;

public class BuiltInResourceFile<T> extends BuiltInResource<T> {
    protected final AssetFile readOnlyFile;

    public BuiltInResourceFile(Activity context, ResourceGuide<T> type, String fileName) {
        super(context, type);
        this.readOnlyFile = new AssetFile(fileName, context);
    }

    @Override
    protected T buildBuiltIn() throws Exception {
        // read internal file
        String builtIn = this.readOnlyFile.get();
        if (builtIn != null) return type.toObjectThrows(builtIn);
        throw new FileNotFoundException();
    }
}
