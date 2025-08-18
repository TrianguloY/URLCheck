package com.trianguloy.forceurl.utilities.methods;

import java.util.List;

public interface JavaUtils {
    /**
     * Returns the element at the specified position if there is one, null if there is none
     */
    static <T> T getSafe(List<T> list, int index) {
        if (list != null && 0 <= index && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    /**
     * java.lang.Math.clamp requires java 21
     */
    static int clamp(int value, int min, int max){
        if (max < min) {
            throw new IllegalArgumentException("Max can't be less than min");
        }
        return value < min ? min : max < value ? max : value;
    }

    // ---
    // Everything, starting here, is copied from URLChecker
    // TODO: move to external library?
    // ---

    /**
     * java.util.function.Consumer requires api 24
     */
    @FunctionalInterface
    interface Consumer<T> {
        void accept(T t);
    }

    /**
     * java.util.function.Function requires api 24
     */
    @FunctionalInterface
    interface Function<T, R> {
        R apply(T t);
    }

    /**
     * java.util.function.UnaryOperator requires api 24
     */
    @FunctionalInterface
    interface UnaryOperator<T> extends Function<T, T> {
    }
}
