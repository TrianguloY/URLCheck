package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

public abstract class ResourceGuide<T> implements ResourceGuideInterface<T> {
    /**
     * Fallbacks to {@link #getEmpty()}
     */
    public T toObject(String string) {
        try {
            return toObjectThrows(string);
        } catch (Exception e) {
            // invalid, return empty
            return getEmpty();
        }
    }

    @Override
    public abstract T toObjectThrows(String string) throws Exception;

    @Override
    public abstract String toString(T object);

    @Override
    public abstract T getEmpty();
}
