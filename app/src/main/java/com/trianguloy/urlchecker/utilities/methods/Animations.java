package com.trianguloy.urlchecker.utilities.methods;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;

/** Animations-related functionality */
public interface Animations {

    static BoolPref ANIMATIONS(Context cntx) {
        return new BoolPref("animations", true, cntx);
    }

    /** Enables animations on all views from this activity */
    static void enableAnimationsRecursively(Activity cntx) {
        if (ANIMATIONS(cntx).get()) enableAnimationsRecursively(cntx.findViewById(android.R.id.content));
    }

    /** Enables animations from all views starting from this parent */
    static void enableAnimationsRecursively(View parent) {
        if (ANIMATIONS(parent.getContext()).get()) _enableAnimationsRecursively(parent);
    }

    static void _enableAnimationsRecursively(View view) {
        if (view instanceof ViewGroup group) {
            var lt = new LayoutTransition();
            lt.enableTransitionType(LayoutTransition.CHANGING);
            group.setLayoutTransition(lt);

            for (var i = 0; i < group.getChildCount(); ++i) _enableAnimationsRecursively(group.getChildAt(i));
        }
    }
}
