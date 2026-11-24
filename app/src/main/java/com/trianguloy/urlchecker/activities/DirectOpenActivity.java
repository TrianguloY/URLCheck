package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.DirectOpen;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;

import java.util.List;

/** Settings for forwarding allowed URLs to an explicitly selected external browser. */
public class DirectOpenActivity extends Activity {
    private Switch enabled;
    private List<DirectOpen.Browser> browsers;
    private boolean refreshing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_direct_open);
        setTitle(R.string.a_directOpen);
        AndroidUtils.configureUp(this);
        enabled = findViewById(R.id.direct_open_enabled);
        enabled.setOnCheckedChangeListener((button, checked) -> {
            if (refreshing) return;
            // Requery at activation time; an app may have been removed since onResume.
            var available = DirectOpen.getBrowsers(this);
            var valid = DirectOpen.hasBrowser(available, DirectOpen.PACKAGE_PREF(this).get());
            DirectOpen.ENABLED_PREF(this).set(checked && valid);
            refresh();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        refreshing = true;
        browsers = DirectOpen.getBrowsers(this);
        var selected = DirectOpen.PACKAGE_PREF(this).get();
        var valid = DirectOpen.hasBrowser(browsers, selected);
        if (!valid) DirectOpen.ENABLED_PREF(this).set(false);
        enabled.setChecked(valid && DirectOpen.ENABLED_PREF(this).get());
        enabled.setEnabled(valid);
        var status = this.<TextView>findViewById(R.id.direct_open_status);
        status.setText(browsers.isEmpty() ? R.string.direct_open_no_browsers
                : valid ? R.string.direct_open_selected : selected.isEmpty()
                ? R.string.direct_open_select_browser : R.string.direct_open_unavailable);

        var list = this.<LinearLayout>findViewById(R.id.direct_open_browsers);
        list.removeAllViews();
        for (var browser : browsers) {
            var row = getLayoutInflater().inflate(R.layout.item_direct_open_browser, list, false);
            ((ImageView) row.findViewById(R.id.direct_open_icon)).setImageDrawable(browser.icon());
            ((TextView) row.findViewById(R.id.direct_open_label)).setText(browser.label());
            // Package names disambiguate applications with identical display names.
            ((TextView) row.findViewById(R.id.direct_open_package)).setText(browser.packageName());
            var radio = (RadioButton) row.findViewById(R.id.direct_open_radio);
            radio.setChecked(browser.packageName().equals(selected));
            radio.setContentDescription(getString(R.string.direct_open_choose, browser.label()));
            View.OnClickListener choose = v -> {
                DirectOpen.PACKAGE_PREF(this).set(browser.packageName());
                refresh();
            };
            radio.setOnClickListener(choose);
            row.setOnClickListener(choose);
            list.addView(row);
        }
        refreshing = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
