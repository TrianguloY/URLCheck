package com.trianguloy.urlchecker.modules.companions;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.EnumerationPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

/** Manages the incognito feature */
public class Incognito {

    public static final String FIREFOX_EXTRA = "private_browsing_mode";
    // for Chrome with custom tabs (even if it doesn't work for standard browsing)
    public static final String CHROME_EXTRA = "androidx.browser.customtabs.extra.ENABLE_EPHEMERAL_BROWSING";

    public static EnumerationPref<OnOffConfig> PREF(Context cntx) {
        return new EnumerationPref<>("open_incognito", OnOffConfig.AUTO, OnOffConfig.class, cntx);
    }

    private final EnumerationPref<OnOffConfig> pref;
    private boolean state = false;
    private ImageButton button;

    public Incognito(Context cntx) {
        this.pref = PREF(cntx);
    }

    /** Initialization from a given intent and a button to toggle */
    public void initFrom(Intent intent, ImageButton button) {
        this.button = button;
        // init state
        state = switch (pref.get()) {
            case DEFAULT_ON, ALWAYS_ON -> true;
            case DEFAULT_OFF, ALWAYS_OFF -> false;
            case HIDDEN, AUTO -> isIncognito(intent);
        };

        // init button
        boolean visible = switch (pref.get()) {
            case ALWAYS_ON, ALWAYS_OFF, HIDDEN -> false;
            case DEFAULT_ON, DEFAULT_OFF, AUTO -> true;
        };
        if (visible) {
            // show and configure
            button.setVisibility(View.VISIBLE);
            AndroidUtils.longTapForDescription(button);
            button.setOnClickListener(v1 -> setState(!state));
            setState(state);
        } else {
            // hide
            button.setVisibility(View.GONE);
        }
    }

    /** Sets the incognito state */
    public void setState(boolean state) {
        this.state = state;
        button.setImageResource(state ? R.drawable.incognito : R.drawable.no_incognito);
    }

    /** Returns the current state */
    public boolean getState() {
        return state;
    }

    /** applies the setting to a given intent */
    public void apply(Intent intent) {
        intent.putExtra(FIREFOX_EXTRA, state);
        intent.putExtra(CHROME_EXTRA, state);
    }

    /** true iff the intent has at least one incognito extra */
    private boolean isIncognito(Intent intent) {
        return intent.getBooleanExtra(FIREFOX_EXTRA, false)
                || intent.getBooleanExtra(CHROME_EXTRA, false);
    }
}
