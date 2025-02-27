package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

import android.app.Activity;

public abstract class BuiltInResource<T> implements ResourceGuideInterface<T>, ResourceContextInterface {
    protected final Activity context;
    protected final ResourceGuide<T> type;

    /**
     * @param context Can't be null
     */
    public BuiltInResource(Activity context, ResourceGuide<T> resource) {
        this.context = context;
        this.type = resource;
    }

    /**
     * Gets the builtin catalog
     */
    public T getBuiltIn() {
        try {
            return buildBuiltIn();
        } catch (Exception e) {
            e.printStackTrace();
            return getEmpty();
        }
    }

    /**
     * Builds the builtin catalog
     *
     * @implNote For dynamically building, just extend this class and override the method.
     * For file reading, refer to {@link BuiltInResourceFile}.
     */
    protected abstract T buildBuiltIn() throws Exception;

    public Activity getContext() {
        return context;
    }

    // ------------------- delegation -------------------

    /**
     * @implNote Can be overridden for a more specific implementation.
     */
    @Override
    public T getEmpty() {
        return type.getEmpty();
    }

    @Override
    public T toObject(String string) {
        return type.toObject(string);
    }

    @Override
    public T toObjectThrows(String string) throws Exception {
        return type.toObjectThrows(string);
    }

    @Override
    public String toString(T object) {
        return type.toString(object);
    }
}
