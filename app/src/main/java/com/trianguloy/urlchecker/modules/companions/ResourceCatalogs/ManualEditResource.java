package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

public class ManualEditResource<T> extends ModifiableResource<T> {
    public ManualEditResource(BuiltInResource<T> builtIn, String modifiableFile) {
        super(builtIn, modifiableFile);
    }

    /**
     * Saves the new content to a file
     */
    public boolean save(T newContent) {
        String neww = toString(newContent);
        String builtin = toString(builtIn.getBuiltIn());
        // same as builtin (maybe a reset?), delete custom
        if (neww.equals(builtin)) {
            writableFile.delete();
            return true;
        }

        // store
        return writableFile.set(neww);
    }
}
