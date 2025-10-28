// ---
// Everything, starting here, is copied from URLChecker
// TODO: move to external library?
// ---
package com.trianguloy.forceurl.utilities;

import java.util.HashMap;
import java.util.Map;

public interface Enums {

    interface StringEnum {
        /**
         * This must return the string resourced associated with this enum value
         */
        int getStringResource();
    }

    interface IdEnum {
        /**
         * The id of the saved preference. Must never change
         */
        int getId();
    }

    interface ImageEnum {
        int getImageResource();
    }

    /**
     * Get an enum from an id
     */
    static <TE extends IdEnum> TE toEnum(Class<TE> te, int id) {
        TE[] enumConstants = te.getEnumConstants();
        for (TE constant : enumConstants) {
            if (constant.getId() == id) {
                return constant;
            }
        }
        return null;
    }

    /**
     * Get a map of id and enum
     */
    static <TE extends IdEnum> Map<Integer, TE> toEnumMap(Class<TE> te) {
        Map<Integer, TE> res = new HashMap<>();
        TE[] enumConstants = te.getEnumConstants();
        for (TE constant : enumConstants) {
            res.put(constant.getId(), constant);
        }
        return res;
    }
}
