package com.trianguloy.urlchecker.modules;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.valueOrDefault;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.generics.JsonCatalog;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The automation rules, plus some automation related things (maybe consider splitting into other classes) */
public class AutomationRules extends JsonCatalog {

    /* ------------------- inner classes ------------------- */

    /**
     * Represents an available automation.
     *
     * @param key         the identifier of the automation, must be unique
     * @param description string resource that explains this automation
     * @param action      action that will be executed for this automation.
     *                    Two parameters: DialogModule (the actual automation dialog) and arguments [optional] (the user defined automation arguments)
     */
    public record Automation<T extends AModuleDialog>(String key, int description, JavaUtils.BiConsumer<T, JSONObject> action) {
        public Automation(String key, int description, JavaUtils.Consumer<T> noArgsAction) {
            this(key, description, (t, args) -> noArgsAction.accept(t));
        }
    }

    /* ------------------- static ------------------- */

    /** Preference: automations availability */
    public static GenericPref.Bool AUTOMATIONS_ENABLED_PREF(Context cntx) {
        return new GenericPref.Bool("auto_enabled", true, cntx);
    }

    /** Preference: show error when automations fail */
    public static GenericPref.Bool AUTOMATIONS_ERROR_TOAST_PREF(Context cntx) {
        return new GenericPref.Bool("auto_error_toast", false, cntx);
    }

    /* ------------------- class ------------------- */

    public final GenericPref.Bool automationsEnabledPref;
    public final GenericPref.Bool automationsShowErrorToast;

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
                ;
    }

    /** Returns the automation ids+arguments that match a specific [urlData] */
    public List<Pair<String, JSONObject>> check(UrlData urlData) {
        var matches = new ArrayList<Pair<String, JSONObject>>();

        var catalog = getCatalog();
        for (var key : JavaUtils.toList(catalog.keys())) {
            try {
                var automation = catalog.getJSONObject(key);
                if (!automation.optBoolean("enabled", true)) continue;

                for (var pattern : JavaUtils.getArrayOrElement(automation.get("regex"), String.class)) {
                    if (urlData.url.matches(pattern)) {
                        matches.add(Pair.create(
                                automation.getString("action"),
                                valueOrDefault(automation.optJSONObject("args"), new JSONObject())
                        ));
                        break;
                    }
                }

            } catch (JSONException e) {
                AndroidUtils.assertError("Invalid automation rule", e);
            } catch (ClassCastException e) {
                AndroidUtils.assertError("Invalid automation regex", e);
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
