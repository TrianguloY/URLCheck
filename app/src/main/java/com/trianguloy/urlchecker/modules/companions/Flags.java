package com.trianguloy.urlchecker.modules.companions;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents intent flags
 * Stores all the flags and it states, can be retrieved as list or as int
 * Non-compatible flags are ignored
 */
public class Flags {

    /** Default flags (name, on/off). Default visibility is 'hidden' */
    public static final Map<String, Boolean> DEFAULT_STATE = Map.of(
            "ACTIVITY_NEW_TASK", true,
            "ACTIVITY_EXCLUDE_FROM_RECENTS", false
    );

    // ------------------- static -------------------

    // https://github.com/MuntashirAkon/AppManager/blob/19782da4c8556c817ba5795554a1cc21f38af13a/app/src/main/java/io/github/muntashirakon/AppManager/intercept/ActivityInterceptor.java#L92
    private static final Set<String> ALL_FLAGS = Set.of(
            // all have a 'FLAG_' prefix
            "GRANT_READ_URI_PERMISSION",
            "GRANT_WRITE_URI_PERMISSION",
            "FROM_BACKGROUND",
            "DEBUG_LOG_RESOLUTION",
            "EXCLUDE_STOPPED_PACKAGES",
            "INCLUDE_STOPPED_PACKAGES",
            "GRANT_PERSISTABLE_URI_PERMISSION",
            "GRANT_PREFIX_URI_PERMISSION",
            "DIRECT_BOOT_AUTO",
            "IGNORE_EPHEMERAL",
            "ACTIVITY_NO_HISTORY",
            "ACTIVITY_SINGLE_TOP",
            "ACTIVITY_NEW_TASK",
            "ACTIVITY_MULTIPLE_TASK",
            "ACTIVITY_CLEAR_TOP",
            "ACTIVITY_FORWARD_RESULT",
            "ACTIVITY_PREVIOUS_IS_TOP",
            "ACTIVITY_EXCLUDE_FROM_RECENTS",
            "ACTIVITY_BROUGHT_TO_FRONT",
            "ACTIVITY_RESET_TASK_IF_NEEDED",
            "ACTIVITY_LAUNCHED_FROM_HISTORY",
            "ACTIVITY_NEW_DOCUMENT",
            "ACTIVITY_NO_USER_ACTION",
            "ACTIVITY_REORDER_TO_FRONT",
            "ACTIVITY_NO_ANIMATION",
            "ACTIVITY_CLEAR_TASK",
            "ACTIVITY_TASK_ON_HOME",
            "ACTIVITY_RETAIN_IN_RECENTS",
            "ACTIVITY_LAUNCH_ADJACENT",
            "ACTIVITY_MATCH_EXTERNAL",
            "ACTIVITY_REQUIRE_NON_BROWSER",
            "ACTIVITY_REQUIRE_DEFAULT"
    );

    private static final Map<String, Integer> compatibleFlags = new TreeMap<>(); // TreeMap to have the entries sorted by key

    static {
        // Only get flags that are present in the current Android version
        for (var flag : ALL_FLAGS) {
            try {
                compatibleFlags.put(flag, (Integer) Intent.class.getField("FLAG_" + flag).get(null));
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException e) {
                AndroidUtils.assertError("Unable to get flag field", e);
            }
        }
    }

    public static Map<String, Integer> getCompatibleFlags() {
        return new TreeMap<>(compatibleFlags);
    }

    // ------------------- CRUD -------------------

    private int flags;

    /** New empty flags */
    public Flags() {
        this(0x00000000);
    }

    /** New flags from existing source */
    public Flags(int hex) {
        setFlags(hex);
    }

    /** check if a flag by name is set */
    public boolean isSet(String flagName) {
        var flag = compatibleFlags.get(flagName);
        return flag != null && (flags & flag) != 0;
    }

    /** get Flags */
    public int getFlags() {
        return flags;
    }

    /** Replaces the stored flags with the received hex */
    public void setFlags(int hex) {
        this.flags = hex;
    }

    /** Sets the flag to the received boolean */
    public void setFlag(String flagName, boolean set) {
        var flag = compatibleFlags.get(flagName);
        if (flag != null) {
            if (set) {
                flags |= flag;
            } else {
                flags &= ~flag;
            }
        }
    }

    // ------------------- utils -------------------

    /**
     * parses a text as an hexadecimal flags string.
     * Returns null if invalid
     */
    private static Integer toInteger(String text) {
        if (text != null && text.matches(REGEX)) {
            return Integer.parseInt(text.replaceAll("^0x", ""), BASE);
        } else {
            return null;
        }
    }

    /** Converts an int flags to string */
    private static String toHexString(int flags) {
        return "0x" + Integer.toString(flags, BASE);
    }

    // ------------------- store/load flags -------------------
    // this handles the store and load of the flags, if something wants to get the flags
    // it should always use these methods.

    private static final String DATA_FLAGS = "flagsEditor.flags";
    private static final String REGEX = "(0x)?[a-fA-F\\d]{1,8}";
    private static final int BASE = 16;

    /** Applies the custom (or default) flags to an intent */
    @SuppressLint("WrongConstant")
    public static void applyGlobalFlags(Intent intent, AModuleDialog instance) {
        var flags = toInteger(instance.getData(DATA_FLAGS));
        if (flags == null) {
            // the flags module is disabled, apply the default
            var computedFlags = new Flags(intent.getFlags());
            for (var flag_state : DEFAULT_STATE.entrySet()) {
                computedFlags.setFlag(flag_state.getKey(), flag_state.getValue());
            }
            flags = computedFlags.getFlags();
        }

        intent.setFlags(flags);
    }

    /** Stores the flags in GlobalData */
    public static void setGlobalFlags(Flags flags, AModuleDialog instance) {
        instance.putData(DATA_FLAGS, flags == null ? null : toHexString(flags.getFlags()));
    }
}
