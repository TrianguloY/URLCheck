package com.trianguloy.urlchecker.modules.companions;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.Enums;

/** Enum for a toggle with on/off/auto x hide/show states */
public enum OnOffConfig implements Enums.IdEnum, Enums.StringEnum {
    // TODO: think of better labels. "Show/Hide button + enabled/disabled/default state"?
    AUTO(0, R.string.auto),
    HIDDEN(5, R.string.hidden),
    DEFAULT_ON(1, R.string.defaultOn),
    DEFAULT_OFF(2, R.string.defaultOff),
    ALWAYS_ON(3, R.string.alwaysOn),
    ALWAYS_OFF(4, R.string.alwaysOff),
    ;

    // -----

    private final int id;
    private final int stringResource;

    OnOffConfig(int id, int stringResource) {
        this.id = id;
        this.stringResource = stringResource;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getStringResource() {
        return stringResource;
    }
}
