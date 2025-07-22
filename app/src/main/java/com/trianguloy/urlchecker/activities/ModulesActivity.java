package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.ModuleManager;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Animations;
import com.trianguloy.urlchecker.utilities.methods.Inflater;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An activity that shows the list of modules that can be enabled/disabled */
public class ModulesActivity extends Activity {

    private LinearLayout list;
    private final Map<AModuleConfig, Switch> switches = new HashMap<>();
    private GenericPref.LstStr order;

    // ------------------- listeners -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_modules);
        setTitle(R.string.a_modules);
        AndroidUtils.configureUp(this);

        list = findViewById(R.id.list);
        order = ModuleManager.ORDER_PREF(this);

        // initialize modules
        for (AModuleData module : ModuleManager.getModules(true, this)) {
            initModule(module);
        }

        // init buttons
        updateMovableButtons();
    }

    // method to inflate the options menu when
    // the user opens the menu for the first time
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_modules, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return switch (item.getItemId()) {
            case android.R.id.home -> {
                // press the 'back' button in the action bar to go back
                onBackPressed();
                yield true;
            }
            case R.id.menu_reset -> {
                // reset order entry
                resetOrder();
                yield true;
            }
            case R.id.menu_decorations -> {
                // toggle decorations entry
                toggleAllDecorations();
                yield true;
            }

            default -> super.onOptionsItemSelected(item);
        };

    }

    // ------------------- actions -------------------

    /** Initializes and adds a module to the list */
    private void initModule(AModuleData module) {
        final AModuleConfig config = module.getConfig(this);

        // inflate
        View parent = Inflater.inflate(R.layout.config_module, list);
        parent.setTag(module.getId());
        Animations.enableAnimationsRecursively(parent); // instead of full activity to avoid 'replacing' when reordering

        // configure enable toggle
        Switch toggleEnable = parent.findViewById(R.id.enable);
        AndroidUtils.longTapForDescription(toggleEnable);
        final GenericPref.Bool enabled_pref = ModuleManager.getEnabledPrefOfModule(module, this);
        toggleEnable.setChecked(enabled_pref.get());
        toggleEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                var resId = config.cannotEnableErrorId();
                if (resId != -1) {
                    // error: notify and keep disabled
                    Toast.makeText(ModulesActivity.this, getString(R.string.toast_cantEnable, getString(resId)), Toast.LENGTH_LONG).show();
                    buttonView.setChecked(false);
                    return;
                }
            }
            enabled_pref.set(isChecked);
        });
        switches.put(config, toggleEnable);

        // configure up/down buttons
        var move_up = parent.findViewById(R.id.move_up);
        move_up.setOnClickListener(v -> moveModule(parent, -1));
        AndroidUtils.longTapForDescription(move_up);
        var move_down = parent.findViewById(R.id.move_down);
        move_down.setOnClickListener(v -> moveModule(parent, 1));
        AndroidUtils.longTapForDescription(move_down);
        // the enable/disable status will be set later with {@link this#updateMovableButtons()}

        // configure label
        final TextView title = parent.findViewById(R.id.label);
        title.setText(getString(R.string.dd, getString(module.getName())));
        AndroidUtils.setAsClickable(title);

        // configure decorations toggle
        var decorationsPref = ModuleManager.getDecorationsPrefOfModule(module, this);
        var toggleDecorations = parent.<ImageView>findViewById(R.id.decorations);
        AndroidUtils.toggleableListener(
                toggleDecorations,
                v -> decorationsPref.toggle(),
                v -> v.setImageResource(decorationsPref.get() ? R.drawable.t : R.drawable.t_slash)
        );
        AndroidUtils.longTapForDescription(toggleDecorations);

        // configuration of the module
        var child = Inflater.inflate(config.getLayoutId(), parent.findViewById(R.id.box));
        config.onInitialize(child);

        // configure toggleable description
        var description = parent.findViewById(R.id.details);
        description.setVisibility(View.GONE); // initially hidden
        AndroidUtils.toggleableListener(
                title,
                v -> description.setVisibility(description.getVisibility() == View.GONE ? View.VISIBLE : View.GONE),
                v -> title.setCompoundDrawablesRelativeWithIntrinsicBounds(description.getVisibility() != View.GONE ? R.drawable.arrow_down : R.drawable.arrow_right, 0, 0, 0)
        );
    }

    /** Moves a module a specific number of positions in the list */
    private void moveModule(View moduleView, int delta) {
        int position = list.indexOfChild(moduleView);
        if (position == -1) return; // no view? impossible
        int newPosition = JavaUtils.clamp(0, position + delta, list.getChildCount() - 1);
        if (newPosition == position) return; // same position? just ignore

        // swap
        list.removeView(moduleView);
        list.addView(moduleView, newPosition);
        updateMovableButtons();

        // update preferences order
        List<String> modules = new ArrayList<>();
        for (int i = list.getChildCount() - 1; i >= 0; i--) {
            // reversed, because -1 means bottom
            modules.add(list.getChildAt(i).getTag().toString());
        }
        order.set(modules);
    }

    /** Disables a module specified by its config */
    public void disableModule(AModuleConfig module) {
        final Switch vswitch = switches.get(module);
        if (vswitch != null) vswitch.setChecked(false);
    }

    /** Updates the enable status of all the movable buttons */
    private void updateMovableButtons() {
        var listSize = list.getChildCount();
        for (int i = 0; i < listSize; i++) {
            View child = list.getChildAt(i);
            // enable up unless already at the top
            View up = child.findViewById(R.id.move_up);
            up.setEnabled(i > 0);
            up.setAlpha(i > 0 ? 1 : 0.5f);
            // enable down unless already at the bottom
            View down = child.findViewById(R.id.move_down);
            down.setEnabled(i < listSize - 1);
            down.setAlpha(i < listSize - 1 ? 1 : 0.5f);
        }
    }

    /** Resets the order of the modules */
    private void resetOrder() {
        // updates preference
        order.clear();

        // get and remove all views
        List<View> views = new ArrayList<>();
        for (int i = 0; i < list.getChildCount(); i++) {
            views.add(list.getChildAt(i));
        }
        list.removeAllViews();

        // sort views based on the order they should have
        List<String> modules = ModuleManager.getOrderedModulesId(this);
        //noinspection ComparatorCombinators requires api 24
        Collections.sort(views, (a, b) -> modules.indexOf(a.getTag().toString()) - modules.indexOf(b.getTag().toString()));

        // add
        for (View view : views) {
            list.addView(view);
        }
        updateMovableButtons();
    }

    /** Clicks all the decorations switches (effectively toggling them) */
    private void toggleAllDecorations() {
        for (int i = 0; i < list.getChildCount(); i++) {
            list.getChildAt(i).findViewById(R.id.decorations).performClick();
        }
    }
}