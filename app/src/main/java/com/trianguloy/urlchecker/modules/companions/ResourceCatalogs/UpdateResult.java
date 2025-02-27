package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.Enums;

public enum UpdateResult implements Enums.StringEnum {
    // TODO: rename strings
    URL_ERROR(R.string.mClear_urlError),
    HASH_ERROR(R.string.mClear_hashError),
    HASH_MISMATCH(R.string.mClear_hashMismatch),
    INVALID(R.string.toast_invalid),
    UPDATED(R.string.mClear_updated),
    UP_TO_DATE(R.string.mClear_upToDate),
    ERROR(R.string.mClear_error);

    private final int string;

    UpdateResult(int string) {
        this.string = string;
    }


    @Override
    public int getStringResource() {
        return string;
    }
}
