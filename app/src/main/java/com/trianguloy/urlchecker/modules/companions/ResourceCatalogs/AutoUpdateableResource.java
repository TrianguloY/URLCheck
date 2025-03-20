package com.trianguloy.urlchecker.modules.companions.ResourceCatalogs;

import android.util.Log;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

public class AutoUpdateableResource<T> extends UpdateableResource<T> {
    protected final int AUTOUPDATE_PERIOD;
    protected final GenericPref.Bool autoUpdate;
    protected final GenericPref.Bool lastAuto;

    public AutoUpdateableResource(BuiltInResource<T> builtIn,
                                  String modifiableFile,
                                  String key,
                                  String catalogUrlDefault,
                                  String hashUrlDefault,
                                  long lastUpdateDefault,
                                  int autoUpdatePeriod) {
        super(builtIn, modifiableFile, key, catalogUrlDefault, hashUrlDefault, lastUpdateDefault);
        var context = getContext();

        this.autoUpdate = new GenericPref.Bool(key + "_autoUpdate", false, context);
        this.lastAuto = new GenericPref.Bool(key + "_lastAuto", false, context);
        this.AUTOUPDATE_PERIOD = autoUpdatePeriod;

        updateIfNecessary();
    }

    /* ------------------- ui ------------------- */

    @Override
    public void updateText() {
        super.updateText();
        var context = getContext();
        JavaUtils.ifPresent(txt_check, () ->
                txt_check.append((lastAuto.get() ? " [" + context.getString(R.string.auto) + "]" : "")));
    }

    // ------------------- internal -------------------

    @Override
    public void _update(boolean background) {
        lastAuto.set(background);   // If called from background, then it must be automatic
        super._update(background);
    }

    /**
     * If the catalog is old, updates it in background. Otherwise does nothing.
     */
    private void updateIfNecessary() {
        var context = getContext();
        if (autoUpdate.get() && lastUpdate.get() + AUTOUPDATE_PERIOD < System.currentTimeMillis()) {
            new Thread(() -> {
                // run
                lastAuto.set(true);
                var code = _updateResource();

                // don't show message to user, but log it
                Log.d("UPDATE", context.getString(code.getStringResource()));
            }).start();
        }
    }

    @Override
    public void clear() {
        lastAuto.clear();
        super.clear();
    }
}
