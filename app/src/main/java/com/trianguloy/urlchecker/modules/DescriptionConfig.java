package com.trianguloy.urlchecker.modules;

import android.view.View;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;

/**
 * A simple Module configuration where just a description is needed
 * This module can always be enabled
 */
public class DescriptionConfig extends AModuleConfig {

    private final String descriptionString;
    private final int description;

    public DescriptionConfig(int description) {
        this.descriptionString = null;
        this.description = description;
    }

    public DescriptionConfig(String descriptionString) {
        this.descriptionString = descriptionString;
        this.description = -1;
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_description;
    }

    @Override
    public void onInitialize(View views) {
        if (descriptionString != null) ((TextView) views).setText(descriptionString);
        else ((TextView) views).setText(description);
    }
}
