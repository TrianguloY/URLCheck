package com.trianguloy.urlchecker.utilities;

public interface Enums {

    interface StringEnum {
        /** This must return the string resourced associated with this enum value */
        int getStringResource();
    }

    interface IdEnum {
        /** The id of the saved preference. Must never change */
        int getId();
    }

    interface ImageEnum {
        int getImageResource();
    }

    /** Get an enum from an id */
    static <TE extends IdEnum> TE toEnum(Class<TE> te, int id) {
        TE[] enumConstants = te.getEnumConstants();
        for (TE constant : enumConstants) {
            if (constant.getId() == id) {
                return constant;
            }
        }
        return null;
    }
}
