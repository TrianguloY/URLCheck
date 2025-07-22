package com.trianguloy.urlchecker.modules.companions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

/**
 * Static elements related to CTabs
 * Maybe move here all the other CTabs logic?
 */
public class CTabs {

    /** Ctabs extra intent */
    private static final String EXTRA = "android.support.customtabs.extra.SESSION";

    /** Ctabs opt_out extra intent */
    private static final String EXTRA_OPTOUT = "android.support.customtabs.extra.user_opt_out";

    /** CTabs preference */
    public static GenericPref.Enumeration<OnOffConfig> PREF(Context cntx) {
        return new GenericPref.Enumeration<>("open_ctabs", OnOffConfig.AUTO, OnOffConfig.class, cntx);
    }


    /* ------------------- state ------------------- */

    private final GenericPref.Enumeration<OnOffConfig> pref;
    private boolean state = false;
    private ImageButton button;

    public CTabs(Context cntx) {
        pref = PREF(cntx);
    }

    /** Initialization from a given intent and a button to toggle */
    public void initFrom(Intent intent, ImageButton button) {
        this.button = button;
        // configure
        state = switch (pref.get()) {
            // If auto or hidden we get it from the intent
            case AUTO, HIDDEN -> intent.hasExtra(CTabs.EXTRA);
            case DEFAULT_ON, ALWAYS_ON -> true;
            case DEFAULT_OFF, ALWAYS_OFF -> false;
        };
        var visible = switch (pref.get()) {
            case AUTO, DEFAULT_ON, DEFAULT_OFF -> true;
            case HIDDEN, ALWAYS_ON, ALWAYS_OFF -> false;
        };

        // set
        if (visible) {
            // show
            AndroidUtils.longTapForDescription(button);
            button.setOnClickListener(v -> setState(!state));
            setState(state);
            button.setVisibility(View.VISIBLE);
        } else {
            // hide
            button.setVisibility(View.GONE);
        }
    }

    /** Sets the cTabs state */
    public void setState(boolean state) {
        this.state = state;
        button.setImageResource(state ? R.drawable.ctabs_on : R.drawable.ctabs_off);
    }

    /** applies the setting to a given intent */
    public void apply(Intent intent) {
        if (state && !intent.hasExtra(CTabs.EXTRA)) {
            // enable Custom tabs
            Bundle extras = new Bundle();
            extras.putBinder(CTabs.EXTRA, null); //  Set to null for no session
            intent.putExtras(extras);
        }

        if (!state && intent.hasExtra(CTabs.EXTRA)) {
            // disable Custom tabs
            intent.removeExtra(CTabs.EXTRA);
        }

        if (state && intent.hasExtra(EXTRA_OPTOUT)) {
            // explicitly remove the application opt_out request if the user do want to enable custom tabs
            intent.removeExtra(EXTRA_OPTOUT);
        }
    }
}
