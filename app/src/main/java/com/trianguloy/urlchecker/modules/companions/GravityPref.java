package com.trianguloy.urlchecker.modules.companions;

import android.content.Context;
import android.view.Gravity;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.Enums;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;

public interface GravityPref {


    /** The vertical gravity pref */
    static GenericPref.EnumerationPref<VerticalPref> VERTICAL_PREF(Context cntx) {
        return new GenericPref.EnumerationPref<>("vertical_gravity", VerticalPref.DEFAULT, VerticalPref.class, cntx);
    }

    /** The vertical gravity pref enum */
    enum VerticalPref implements Enums.IdEnum, Enums.StringEnum {
        DEFAULT(0, Gravity.NO_GRAVITY, R.string.deviceDefault),
        TOP(1, Gravity.TOP, R.string.spin_top),
        CENTER(2, Gravity.CENTER_VERTICAL, R.string.spin_center),
        BOTTOM(3, Gravity.BOTTOM, R.string.spin_bottom),
        ;

        private final int id;
        private final int constant;
        private final int string;

        VerticalPref(int id, int constant, int string) {
            this.id = id;
            this.constant = constant;
            this.string = string;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int getStringResource() {
            return string;
        }

        public int getConstant() {
            return constant;
        }
    }

    /** The horizontal gravity pref */
    static GenericPref.EnumerationPref<HorizontalPref> HORIZONTAL_PREF(Context cntx) {
        return new GenericPref.EnumerationPref<>("horizontal_gravity", HorizontalPref.DEFAULT, HorizontalPref.class, cntx);
    }

    /** The horizontal gravity pref enum */
    enum HorizontalPref implements Enums.IdEnum, Enums.StringEnum {
        DEFAULT(0, Gravity.NO_GRAVITY, R.string.deviceDefault),
        START(1, Gravity.START, R.string.spin_start),
        CENTER(2, Gravity.CENTER_HORIZONTAL, R.string.spin_center),
        END(3, Gravity.END, R.string.spin_end),
        ;

        private final int id;
        private final int constant;

        private final int string;

        HorizontalPref(int id, int constant, int string) {
            this.id = id;
            this.constant = constant;
            this.string = string;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int getStringResource() {
            return string;
        }

        public int getConstant() {
            return constant;
        }

    }
}
