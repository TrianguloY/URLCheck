package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;

/** The activity to explain and configure automations */
public class AutomationActivity extends Activity {

    private AutomationRules rules;

    // ------------------- listeners -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_automation);
        setTitle(R.string.a_automations);
        AndroidUtils.configureUp(this);

        rules = new AutomationRules(this);

        rules.automationsEnabledPref.attachToSwitch(findViewById(R.id.auto_enabled));
        rules.automationsShowErrorToast.attachToSwitch(findViewById(R.id.auto_error_toast));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // press the 'back' button in the action bar to go back
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------- actions -------------------

    public void openJsonEditor(View view) {
        rules.showEditor();
    }
}