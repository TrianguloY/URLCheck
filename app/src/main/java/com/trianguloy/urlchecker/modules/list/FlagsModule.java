package com.trianguloy.urlchecker.modules.list;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.valueOrDefault;

import android.app.AlertDialog;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.companions.Flags;
import com.trianguloy.urlchecker.utilities.Enums;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Inflater;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.wrappers.DefaultTextWatcher;
import com.trianguloy.urlchecker.utilities.wrappers.InternalFile;
import com.trianguloy.urlchecker.views.CycleImageButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** This module allows flag edition */
public class FlagsModule extends AModuleData {


    public static final String DEFAULT_GROUP = "default";

    @Override
    public String getId() {
        return "flagsEditor";
    }

    @Override
    public int getName() {
        return R.string.mFlags_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new FlagsDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new FlagsConfig(cntx);
    }
}

class FlagsDialog extends AModuleDialog {

    private final Flags defaultFlags;
    private final Flags currentFlags;

    private Map<String, FlagsConfig.FlagState> flagsStatePref;

    private ViewGroup shownFlagsVG;
    private EditText searchInput;
    private ViewGroup hiddenFlagsVG;
    private ImageView overflowButton;

    private JSONObject groups;

    public FlagsDialog(MainDialog dialog) {
        super(dialog);
        defaultFlags = new Flags(getActivity().getIntent().getFlags());
        currentFlags = new Flags();
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_flags;
    }

    @Override
    public void onInitialize(View views) {
        initGroups();

        shownFlagsVG = views.findViewById(R.id.shownFlags);
        searchInput = views.findViewById(R.id.search);
        hiddenFlagsVG = views.findViewById(R.id.hiddenFlags);

        // Button to open the `box` with the hidden flags (more indicator)
        overflowButton = views.findViewById(R.id.overflowButton);

        // Hide hidden flags
        hiddenFlagsVG.setVisibility(View.GONE);
        AndroidUtils.toggleableListener(overflowButton,
                v -> hiddenFlagsVG.setVisibility(hiddenFlagsVG.getVisibility() == View.GONE ? View.VISIBLE : View.GONE),
                v -> {
                    searchInput.setVisibility(hiddenFlagsVG.getVisibility());
                    updateMoreIndicator();
                });

        // SEARCH
        // Set up search text
        searchInput.addTextChangedListener(new DefaultTextWatcher() {
            @Override
            public void afterTextChanged(Editable text) {
                for (int i = 0; i < hiddenFlagsVG.getChildCount(); i++) {
                    var checkbox_text = hiddenFlagsVG.getChildAt(i);
                    String flag = ((TextView) checkbox_text.findViewById(R.id.text)).getText().toString();
                    String search = text.toString();
                    // Set visibility based on search text
                    checkbox_text.setVisibility(JavaUtils.containsWords(flag, search) ? View.VISIBLE : View.GONE);
                }
            }
        });

        // TODO spinner with groups
        loadGroup(FlagsModule.DEFAULT_GROUP);
    }

    private void initGroups() {
        String fileString = new InternalFile(FlagsConfig.CONF_FILE, getActivity()).get();
        groups = new JSONObject();
        if (fileString != null) {
            try {
                groups = new JSONObject(fileString).getJSONObject("groups");
            } catch (JSONException ignore) {
            }
        }
    }

    void loadGroup(String group) {
        currentFlags.setFlags(0);

        // Load json
        JSONObject groupPref = null;
        try {
            groupPref = groups.getJSONObject(group);
        } catch (JSONException ignore) {
        }


        // Put shown flags
        Set<String> shownFlagsSet = new TreeSet<>();
        // Get state preference of flag from json and then store it in a map
        flagsStatePref = new HashMap<>();
        if (groupPref != null) {
            for (Iterator<String> it = groupPref.keys(); it.hasNext(); ) {
                String flag = it.next();
                try {
                    flagsStatePref.put(flag, Enums.toEnum(FlagsConfig.FlagState.class,
                            groupPref.getJSONObject(flag).getInt("state")));
                    if (groupPref.getJSONObject(flag).getBoolean("show")) {
                        shownFlagsSet.add(flag);
                    }
                } catch (JSONException ignored) {
                }
            }
        } else {
            // default flags
            for (var flag_state : Flags.DEFAULT_STATE.entrySet()) {
                flagsStatePref.put(flag_state.getKey(), flag_state.getValue() ? FlagsConfig.FlagState.ON : FlagsConfig.FlagState.OFF);
            }
        }

        // If it is not in shownFlags it must be in hiddenFlags
        Set<String> hiddenFlagsSet = new TreeSet<>(Flags.getCompatibleFlags().keySet());
        hiddenFlagsSet.removeAll(shownFlagsSet);

        // Fill boxes with flags, load flags into currentFlags too
        fillWithFlags(shownFlagsSet, shownFlagsVG);
        fillWithFlags(hiddenFlagsSet, hiddenFlagsVG);

        // Update global
        Flags.setGlobalFlags(currentFlags, this);

        updateMoreIndicator();
    }

    void updateMoreIndicator() {
        if (hiddenFlagsVG.getChildCount() == 0) {
            overflowButton.setImageDrawable(null);
        } else {
            overflowButton.setImageResource(hiddenFlagsVG.getVisibility() == View.VISIBLE ? R.drawable.arrow_down : R.drawable.arrow_right);
        }
    }

    /**
     * Sets up a ViewGroup with the flags received. The state of the flag is read from flagsStatePref.
     * The default state of the flag is read from defaultFlags. Also sets currentFlags.
     *
     * @param flags flags to add
     * @param vg    ViewGroup to fill with flags
     */
    private void fillWithFlags(Set<String> flags, ViewGroup vg) {
        vg.removeAllViews();

        for (String flag : flags) {
            var checkbox_text = Inflater.inflate(R.layout.dialog_flags_entry, vg);

            // Checkbox
            var checkBox = checkbox_text.<ImageView>findViewById(R.id.state);
            var bool = switch (valueOrDefault(flagsStatePref.get(flag), FlagsConfig.FlagState.AUTO)) {
                case ON -> true;
                case OFF -> false;
                case AUTO -> defaultFlags.isSet(flag);
            };
            currentFlags.setFlag(flag, bool);

            checkBox.setTag(R.id.text, flag);
            AndroidUtils.toggleableListener(checkBox,
                    v -> {
                        currentFlags.setFlag(flag, !currentFlags.isSet(flag));

                        // Update global
                        // this will not trigger an update, consider creating an onGlobalData handler if needed
                        Flags.setGlobalFlags(currentFlags, this);
                    },
                    v -> checkBox.setImageResource(currentFlags.isSet(flag) ? R.drawable.flag_on : R.drawable.flag_off)
            );

            // Text
            ((TextView) checkbox_text.findViewById(R.id.text)).setText(flag);

            // Color indicators
            var defaultIndicator = checkbox_text.findViewById(R.id.defaultIndicator);
            var preferenceIndicator = checkbox_text.findViewById(R.id.preferenceIndicator);

            checkBox.setTag(R.id.defaultIndicator, defaultIndicator);
            checkBox.setTag(R.id.preferenceIndicator, preferenceIndicator);

            setColors(flag, defaultIndicator, preferenceIndicator);
        }

    }

    void setColors(String flag, View defaultIndicator, View preferenceIndicator) {
        AndroidUtils.setRoundedColor(defaultFlags.isSet(flag) ? R.color.good : R.color.bad, defaultIndicator);

        var color = switch (valueOrDefault(flagsStatePref.get(flag), FlagsConfig.FlagState.AUTO)) {
            case ON -> R.color.good;
            case OFF -> R.color.bad;
            case AUTO -> R.color.grey;
        };

        AndroidUtils.setRoundedColor(color, preferenceIndicator);
    }

}

class FlagsConfig extends AModuleConfig {

    protected static final String CONF_FILE = "flags_editor_settings";

    public FlagsConfig(ModulesActivity activity) {
        super(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_flags;
    }

    @Override
    public void onInitialize(View views) {
        views.findViewById(R.id.button).setOnClickListener(showDialog -> {
            View flagsDialogLayout = getActivity().getLayoutInflater().inflate(R.layout.flags_editor, null);
            ViewGroup box = flagsDialogLayout.findViewById(R.id.box);
            InternalFile file = new InternalFile(CONF_FILE, getActivity());

            // Get all flags
            fillBoxViewGroup(box, file, FlagsModule.DEFAULT_GROUP);

            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setView(flagsDialogLayout)
                    .setPositiveButton(views.getContext().getText(R.string.save), (dialog, which) -> {
                        // Save the settings
                        storePreferences(box, file, FlagsModule.DEFAULT_GROUP);
                    })
                    .setNegativeButton(views.getContext().getText(android.R.string.cancel), null)
                    .setNeutralButton(views.getContext().getText(R.string.reset), null)
                    .show();

            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(listener -> {
                // Reset current group flags (does not save)
                resetFlags(box);
            });

            // Search
            ((EditText) flagsDialogLayout.findViewById(R.id.search)).addTextChangedListener(new DefaultTextWatcher() {
                @Override
                public void afterTextChanged(Editable text) {
                    for (int i = 0; i < box.getChildCount(); i++) {
                        var entryView = box.getChildAt(i);
                        String flag = ((TextView) entryView.findViewById(R.id.text)).getText().toString();
                        String search = text.toString();
                        // Set visibility based on search text
                        entryView.setVisibility(JavaUtils.containsWords(flag, search) ? View.VISIBLE : View.GONE);
                    }
                }
            });

            // TODO add dialog button to set all to on/off/auto
        });

    }

    private void fillBoxViewGroup(ViewGroup vg, InternalFile file, String group) {

        String prefString = file.get();
        JSONObject oldPref = null; // Null if there is no file or fails to parse
        try {
            oldPref = prefString == null ? null : new JSONObject(file.get()).getJSONObject("groups").getJSONObject(group);
        } catch (JSONException ignored) {
        }

        // Fill the box
        for (String flag : Flags.getCompatibleFlags().keySet()) {
            var entryView = Inflater.inflate(R.layout.flags_editor_entry, vg);

            TextView textView = entryView.findViewById(R.id.text);
            textView.setText(flag);

            var flagState = entryView.<CycleImageButton<FlagState>>findViewById(R.id.state);
            flagState.setStates(List.of(FlagState.values()));

            // Load preferences from settings
            Boolean showValue = null;
            FlagState stateValue = null;
            // Get current preferences
            if (oldPref != null) {
                JSONObject flagPref;
                try {
                    flagPref = oldPref.getJSONObject(flag);
                    showValue = flagPref.getBoolean("show");
                    stateValue = Enums.toEnum(FlagState.class, flagPref.getInt("state"));
                } catch (JSONException ignored) {
                }
            } else {
                var defaultState = Flags.DEFAULT_STATE.get(flag);
                if (defaultState != null) {
                    stateValue = defaultState ? FlagState.ON : FlagState.OFF;
                    showValue = false;
                }
            }
            flagState.setCurrentState(valueOrDefault(stateValue, FlagState.AUTO));
            var show = entryView.<ImageButton>findViewById(R.id.show);
            show.setTag(valueOrDefault(showValue, false));
            AndroidUtils.toggleableListener(show,
                    v -> v.setTag(v.getTag() == Boolean.FALSE),
                    v -> v.setImageResource(v.getTag() == Boolean.TRUE ? R.drawable.show : R.drawable.hide)
            );
        }
    }

    private void storePreferences(ViewGroup vg, InternalFile file, String group) {
        // Retrieve previous config, to keep other groups
        JSONObject oldSettings = null;
        String content = file.get();
        // It's ok if there is no file yet
        if (content != null) {
            try {
                oldSettings = new JSONObject(content);
            } catch (JSONException ignore) {
                // If the json fails to parse then we will create a new file
            }
        }

        try {
            // Collect all the settings of the vg
            JSONObject newSettings = new JSONObject();
            for (int i = 0; i < vg.getChildCount(); i++) {
                View v = vg.getChildAt(i);

                FlagState state = v.<CycleImageButton<FlagState>>findViewById(R.id.state).getCurrentState();
                boolean show = v.findViewById(R.id.show).getTag() == Boolean.TRUE;
                newSettings.put(((TextView) v.findViewById(R.id.text)).getText().toString(),
                        new JSONObject()
                                .put("state", state.getId())
                                .put("show", show));
            }
            // If there are no old settings, create a new one
            // Replace the old settings from group with the new ones
            newSettings = oldSettings == null ?
                    new JSONObject().put("groups", new JSONObject().put(group, newSettings)) :
                    oldSettings.put("groups", oldSettings.getJSONObject("groups").put(group, newSettings));
            // TODO should groups be sorted?
            file.set(newSettings.toString());
        } catch (JSONException e) {
            AndroidUtils.assertError("Invalid json flags", e);
            Toast.makeText(getActivity(), R.string.invalid, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetFlags(ViewGroup vg) {
        // Set everything to default values
        for (int i = 0; i < vg.getChildCount(); i++) {
            var v = vg.getChildAt(i);

            var defaultState = Flags.DEFAULT_STATE.get(v.<TextView>findViewById(R.id.text).getText().toString());
            v.<CycleImageButton<FlagState>>findViewById(R.id.state).setCurrentState(defaultState == null ? FlagState.AUTO : defaultState ? FlagState.ON : FlagState.OFF);
            var visible = v.<ImageButton>findViewById(R.id.show);
            visible.setImageResource(R.drawable.hide);
            visible.setTag(Boolean.FALSE);
        }
    }

    public enum FlagState implements Enums.IdEnum, Enums.ImageEnum {
        AUTO(0, R.drawable.flag_auto),
        ON(1, R.drawable.flag_on),
        OFF(2, R.drawable.flag_off),
        ;

        // -----

        private final int id;
        private final int imageResource;

        FlagState(int id, int imageResource) {
            this.id = id;
            this.imageResource = imageResource;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int getImageResource() {
            return imageResource;
        }
    }
}