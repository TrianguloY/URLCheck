package com.trianguloy.urlchecker.utilities.generics;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.trianguloy.urlchecker.utilities.Enums;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Consumer;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Function;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.UnaryOperator;
import com.trianguloy.urlchecker.utilities.wrappers.DefaultTextWatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A generic type preference
 *
 * @param <T> type of the preference
 */
public abstract class GenericPref<T> {

    /** Returns the sharedPrefs used by everything in the app */
    public static SharedPreferences getPrefs(Context cntx) {
        return cntx.getSharedPreferences(cntx.getPackageName(), Context.MODE_PRIVATE);
    }

    /** android sharedprefs */
    protected final SharedPreferences prefs;

    /** this preference name */
    protected final String prefName;

    /** This preference default value */
    public final T defaultValue;

    /**
     * Constructs a generic pref with name and default value, uninitialized
     *
     * @param prefName     this preference name
     * @param defaultValue this preference default value
     */
    public GenericPref(String prefName, T defaultValue, Context cntx) {
        this.prefName = prefName;
        this.defaultValue = defaultValue;
        prefs = getPrefs(cntx);
    }

    /** @return the value of this preference */
    public abstract T get();

    /**
     * Sets a [value] for this preference.
     * Note: if the value is the default, it will be cleared instead.
     */
    public void set(T value) {
        if (!Objects.equals(value, defaultValue)) {
            // non-default, save
            save(value);
        } else {
            // default, clear
            clear();
        }
    }

    /** Sets a [value] for this preference. */
    protected abstract void save(T value);

    /** Clears this preference value */
    public void clear() {
        prefs.edit().remove(prefName).apply();
    }


    @Override
    public String toString() {
        return prefName + " = " + get();
    }

    /**
     * Attaches this value to a spinner+label.
     * pref2seekBar must map a pref->(seekBar,label) and seekBar2pref from spinner->pref.
     */
    public void attachToSeekBar(SeekBar seekBar, TextView label, Function<T, Pair<Integer, String>> pref2seekBar, Function<Integer, T> seekBar2pref) {
        var valueLabel = pref2seekBar.apply(get());
        seekBar.setProgress(valueLabel.first);
        label.setText(valueLabel.second);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                set(seekBar2pref.apply(progress));

                var valueLabel = pref2seekBar.apply(get());
                seekBar.setProgress(valueLabel.first);
                label.setText(valueLabel.second);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    // ------------------- Implementations -------------------

    /** An Int preference */
    static public class IntPref extends GenericPref<Integer> {
        public IntPref(String prefName, Integer defaultValue, Context cntx) {
            super(prefName, defaultValue, cntx);
        }

        @Override
        public Integer get() {
            return prefs.getInt(prefName, defaultValue);
        }

        @Override
        protected void save(Integer value) {
            prefs.edit().putInt(prefName, value).apply();
        }

        /**
         * This editText will be set to the pref value, and when the editText changes the value will too.
         * The special empty value will be set when the input is empty.
         */
        public void attachToEditText(EditText editText, int empty) {
            editText.setText(get() == empty ? "" : get().toString());
            editText.addTextChangedListener(new DefaultTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        // empty -> set empty
                        if (s.length() == 0) set(empty);
                        else {
                            var value = Integer.parseInt(s.toString());
                            if (value == empty) s.clear(); // empty input -> clear
                            set(value);
                        }
                    } catch (NumberFormatException e) {
                        // shouldn't be possible, but just in case
                        s.clear();
                        s.append(get() == empty ? "" : get().toString());
                    }
                }
            });
        }
    }

    /** A Long preference */
    static public class LongPref extends GenericPref<Long> {
        public LongPref(String prefName, Long defaultValue, Context cntx) {
            super(prefName, defaultValue, cntx);
        }

        @Override
        public Long get() {
            return prefs.getLong(prefName, defaultValue);
        }

        @Override
        protected void save(Long value) {
            prefs.edit().putLong(prefName, value).apply();
        }
    }

    /** A Float preference */
    static public class FloatPref extends GenericPref<Float> {
        public FloatPref(String prefName, Float defaultValue, Context cntx) {
            super(prefName, defaultValue, cntx);
        }

        @Override
        public Float get() {
            return prefs.getFloat(prefName, defaultValue);
        }

        @Override
        protected void save(Float value) {
            prefs.edit().putFloat(prefName, value).apply();
        }
    }

    /** A boolean preference */
    static public class BoolPref extends GenericPref<Boolean> {
        public BoolPref(String prefName, Boolean defaultValue, Context cntx) {
            super(prefName, defaultValue, cntx);
        }

        @Override
        public Boolean get() {
            return prefs.getBoolean(prefName, defaultValue);
        }

        @Override
        protected void save(Boolean value) {
            prefs.edit().putBoolean(prefName, value).apply();
        }

        /** This switch will be set to the pref value, and when the switch changes the value will too */
        public void attachToSwitch(Switch vSwitch) {
            vSwitch.setChecked(get());
            vSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> set(isChecked));
        }

        /** Toggles this setting */
        public void toggle() {
            set(!get());
        }
    }

    /** A string preference */
    static public class StringPref extends GenericPref<String> {
        public StringPref(String prefName, String defaultValue, Context cntx) {
            super(prefName, defaultValue, cntx);
        }

        @Override
        public String get() {
            return prefs.getString(prefName, defaultValue);
        }

        @Override
        protected void save(String value) {
            prefs.edit().putString(prefName, value).apply();
        }

        /** This editText will be set to the pref value, and when the editText changes the value will too */
        public void attachToEditText(EditText editText) {
            this.attachToEditText(editText, str -> str, str -> str);
        }

        /** This editText will be set to the pref value modified by loadMod, and when the editText changes the value will be modified by storeMod and saved */
        public void attachToEditText(EditText editText, UnaryOperator<String> loadMod, UnaryOperator<String> storeMod) {
            editText.setText(loadMod.apply(get()));
            editText.addTextChangedListener(new DefaultTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    set(storeMod.apply(s.toString()));
                }
            });
        }
    }


    /**
     * A list of strings preference.
     * Saved as string concatenated with separator
     * Optionally limit the number of entries
     */
    static public class ListStringPref extends GenericPref<List<String>> {

        final String separator;
        final int limit;

        public ListStringPref(String prefName, String separator, List<String> defaultValue, Context cntx) {
            this(prefName, separator, 0, defaultValue, cntx);
        }

        public ListStringPref(String prefName, String separator, int limit, List<String> defaultValue, Context cntx) {
            super(prefName, defaultValue, cntx);
            this.separator = separator;
            this.limit = limit;
        }

        @Override
        public List<String> get() {
            return split(prefs.getString(prefName, join(defaultValue)));
        }

        @Override
        protected void save(List<String> value) {
            prefs.edit().putString(prefName, join(value)).apply();
        }

        private String join(List<String> value) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < value.size(); i++) {
                if (i != 0) sb.append(separator);
                sb.append(value.get(i));
            }
            return sb.toString();
        }

        private List<String> split(String value) {
            ArrayList<String> list = new ArrayList<>();
            if (value != null) list.addAll(Arrays.asList(value.split(separator, limit)));
            return list;
        }
    }

    /** A list of options (enumeration) preference */
    static public class EnumerationPref<T extends Enum<T> & Enums.IdEnum & Enums.StringEnum> extends GenericPref<T> {
        private final Class<T> type;

        public EnumerationPref(String prefName, T defaultValue, Class<T> type, Context cntx) {
            super(prefName, defaultValue, cntx);
            this.type = type;
        }

        @Override
        public T get() {
            int value = prefs.getInt(prefName, defaultValue.getId());
            for (T entry : type.getEnumConstants()) {
                if (entry.getId() == value) return entry;
            }
            return defaultValue;
        }

        @Override
        protected void save(T value) {
            prefs.edit().putInt(prefName, value.getId()).apply();
        }

        /**
         * Populate a spinner with this preference
         * if listener is not null, it will be called each time the spinner changes value
         */
        public void attachToSpinner(Spinner spinner, Consumer<T> listener) {
            // Put elements in the spinner
            T[] values = type.getEnumConstants();
            List<String> names = new ArrayList<>(values.length);
            for (T value : values) {
                names.add(spinner.getContext().getString(value.getStringResource()));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    spinner.getContext(),
                    android.R.layout.simple_spinner_item,
                    names
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            // select current option
            T selection = get();
            for (int i = 0; i < values.length; i++) {
                if (values[i] == selection) spinner.setSelection(i);
            }

            // add listener to auto-change it
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    // set+notify if changed
                    if (get() != values[i]) {
                        set(values[i]);
                        if (listener != null) listener.accept(values[i]);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
        }

    }
}
