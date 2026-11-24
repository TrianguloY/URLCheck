package com.trianguloy.urlchecker.modules;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.valueOrDefault;

import android.app.Activity;
import android.content.Context;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.generics.JsonCatalog;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.BiConsumer;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Consumer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The automation rules, plus some automation related things (maybe consider splitting into other classes) */
public class AutomationRules extends JsonCatalog {

    /* ------------------- inner classes ------------------- */

    /** Represents an available automation */
    public record Automation<T extends AModuleDialog>(
            String key, int description, BiConsumer<T, JSONObject> action) {
        public Automation(String key, int description, Consumer<T> action) {
            this(key, description, (dialog, ignoredArgs) -> action.accept(dialog));
        }
    }

    /** Represents an automation that matched a url */
    public record MatchedAutomation(List<String> actions, boolean stop, JSONObject args) {
    }

    /* ------------------- static ------------------- */

    /** Preference: automations availability */
    public static BoolPref AUTOMATIONS_ENABLED_PREF(Context cntx) {
        return new BoolPref("auto_enabled", true, cntx);
    }

    /** Preference: show error when automations fail */
    public static BoolPref AUTOMATIONS_ERROR_TOAST_PREF(Context cntx) {
        return new BoolPref("auto_error_toast", false, cntx);
    }

    /* ------------------- class ------------------- */

    public final BoolPref automationsEnabledPref;
    public final BoolPref automationsShowErrorToast;

    public AutomationRules(Activity cntx) {
        super(cntx, "automations", cntx.getString(R.string.auto_editor)
                + "\n\n- - - - - - - - - -\n\n"
                + getAvailableAutomations(cntx));

        automationsEnabledPref = AutomationRules.AUTOMATIONS_ENABLED_PREF(cntx);
        automationsShowErrorToast = AutomationRules.AUTOMATIONS_ERROR_TOAST_PREF(cntx);
    }

    @Override
    public JSONObject buildBuiltIn(Context cntx) throws JSONException {
        return new JSONObject()
                .put(cntx.getString(R.string.auto_rule_bitly), new JSONObject()
                        .put("regex", "https?://bit\\.ly/.*")
                        .put("action", "unshort")
                        .put("enabled", false)
                )
                .put(cntx.getString(R.string.auto_rule_webhook), new JSONObject()
                        .put("regex", ".*")
                        .put("action", "webhook")
                        .put("enabled", false)
                )
                .put(cntx.getString(R.string.auto_rule_toast), new JSONObject()
                        .put("regex", cntx.getString(R.string.trianguloy))
                        .put("action", "toast")
                        .put("args", new JSONObject()
                                .put("text", "👋🙂")))
                ;
    }

    /** Returns the automations that matched a specific [urlData] */
    public List<MatchedAutomation> check(UrlData urlData, Activity cntx) {
        var matches = new ArrayList<MatchedAutomation>();

        var catalog = getCatalog();
        for (var key : JavaUtils.toList(catalog.keys())) {
            try {
                var automation = catalog.getJSONObject(key);
                if (!automation.optBoolean("enabled", true)) continue;

                // match at least one referrer, if any
                var referrer = AndroidUtils.getReferrer(cntx);
                if (referrer != null && JavaUtils.nonePresentMatch(JavaUtils.parseArrayOrElement(automation.opt("referrer"), String.class), referrer::equals)) {
                    continue;
                }

                // match at least one regex, if any
                if (JavaUtils.nonePresentMatch(JavaUtils.parseArrayOrElement(automation.opt("regex"), String.class), urlData.url::matches)) {
                    continue;
                }

                // don't match any excluded regex, if any
                if (JavaUtils.anyMatch(JavaUtils.parseArrayOrElement(automation.opt("excludeRegex"), String.class), urlData.url::matches)) {
                    continue;
                }

                // add as matched
                matches.add(new MatchedAutomation(
                        JavaUtils.parseArrayOrElement(automation.opt("action"), String.class),
                        automation.optBoolean("stop"), valueOrDefault(automation.optJSONObject("args"), new JSONObject())));

            } catch (Exception e) {
                AndroidUtils.assertError("Invalid automation", e);
            }
        }
        return matches;
    }

    /** Generates the list of available automation keys, as text. */
    private static String getAvailableAutomations(Context cntx) {
        var stringBuilder = new StringBuilder(cntx.getString(R.string.auto_available_prefix)).append("\n");

        for (var module : ModuleManager.getModules(true, cntx)) {
            var automations = module.getAutomations();
            if (automations.isEmpty()) continue;

            stringBuilder.append("\n").append(cntx.getString(module.getName())).append(":\n");
            if (!ModuleManager.getEnabledPrefOfModule(module, cntx).get()) {
                stringBuilder.append("⚠ ").append(cntx.getString(R.string.auto_available_disabled)).append(" ⚠\n");
            }
            for (var automation : automations) {
                stringBuilder.append("- \"").append(automation.key()).append("\": ")
                        .append(cntx.getString(automation.description())).append("\n");
            }
        }

        return stringBuilder.toString();
    }
}
