package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

public interface ResourceGuideInterface<T> {
    T toObject(String string);

    T toObjectThrows(String string) throws Exception;

    String toString(T object);

    T getEmpty();
}
