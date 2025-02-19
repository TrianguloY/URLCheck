package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

import android.app.Activity;

import com.trianguloy.urlchecker.utilities.wrappers.InternalFile;

public abstract class ModifiableResource<T> implements ResourceGuideInterface<T>, ResourceContextInterface {
    protected final BuiltInResource<T> builtIn;
    protected final InternalFile writableFile;


    public ModifiableResource(BuiltInResource<T> builtIn,
                              String modifiableFile) {
        this.builtIn = builtIn;

        this.writableFile = new InternalFile(modifiableFile, getContext());
    }

    public T getCatalog() {
        // get the updated file first
        String internal = writableFile.get();
        if (internal != null) return toObject(internal);

        // no updated file or can't read, use built-in one
        return builtIn.getBuiltIn();
    }

    public void clear() {
        writableFile.delete();
    }

    // ------------------- delegation -------------------

    @Override
    public Activity getContext() {
        return builtIn.getContext();
    }

    @Override
    public String toString(T object) {
        return builtIn.toString(object);
    }

    @Override
    public T toObject(String string) {
        return builtIn.toObject(string);
    }

    @Override
    public T toObjectThrows(String string) throws Exception {
        return builtIn.toObjectThrows(string);
    }

    @Override
    public T getEmpty() {
        return builtIn.getEmpty();
    }
}
