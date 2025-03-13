package com.trianguloy.urlchecker.modules.list;

import static com.trianguloy.urlchecker.utilities.methods.UrlUtils.decode;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.modules.companions.ClearUrlCatalog;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This module clears the url using the ClearUrl catalog
 */
public class ClearUrlModule extends AModuleData {

    public static GenericPref.Bool REFERRAL_PREF(Context cntx) {
        return new GenericPref.Bool("clearurl_referral", false, cntx);
    }

    public static GenericPref.Bool VERBOSE_PREF(Context cntx) {
        return new GenericPref.Bool("clearurl_verbose", false, cntx);
    }

    public static GenericPref.Bool AUTO_PREF(Context cntx) {
        return new GenericPref.Bool("clearurl_auto", false, cntx);
    }

    @Override
    public String getId() {
        return "clearUrl";
    }

    @Override
    public int getName() {
        return R.string.mClear_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new ClearUrlDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new ClearUrlConfig(cntx);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) ClearUrlDialog.AUTOMATIONS;
    }
}

class ClearUrlConfig extends AModuleConfig {

    private final GenericPref.Bool allowReferral;
    private final GenericPref.Bool verbose;
    private final GenericPref.Bool auto;

    private final ClearUrlCatalog catalog;

    public ClearUrlConfig(ModulesActivity activity) {
        super(activity);
        allowReferral = ClearUrlModule.REFERRAL_PREF(activity);
        verbose = ClearUrlModule.VERBOSE_PREF(activity);
        auto = ClearUrlModule.AUTO_PREF(activity);
        catalog = new ClearUrlCatalog(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_clearurls;
    }

    @Override
    public void onInitialize(View views) {
        allowReferral.attachToSwitch(views.findViewById(R.id.referral));
        verbose.attachToSwitch(views.findViewById(R.id.verbose));
        auto.attachToSwitch(views.findViewById(R.id.auto));

        views.findViewById(R.id.update).setOnClickListener(v -> catalog.showUpdater());
        views.findViewById(R.id.edit).setOnClickListener(v -> catalog.showEditor());
    }

}

class ClearUrlDialog extends AModuleDialog {

    static final List<AutomationRules.Automation<ClearUrlDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>("clear", R.string.auto_clear, ClearUrlDialog::clear)
    );

    public static final String CLEARED = "clearUrl.cleared";

    private final GenericPref.Bool allowReferral;
    private final GenericPref.Bool verbose;
    private final GenericPref.Bool auto;

    private final List<Pair<String, JSONObject>> rules;
    private TextView info;
    private Button fix;

    private String cleared = null;
    private Data data = null;

    public ClearUrlDialog(MainDialog dialog) {
        super(dialog);
        allowReferral = ClearUrlModule.REFERRAL_PREF(dialog);
        verbose = ClearUrlModule.VERBOSE_PREF(dialog);
        auto = ClearUrlModule.AUTO_PREF(dialog);

        rules = ClearUrlCatalog.getRules(getActivity());
    }

    @Override
    public int getLayoutId() {
        return R.layout.button_text;
    }

    @Override
    public void onInitialize(View views) {
        info = views.findViewById(R.id.text);
        fix = views.findViewById(R.id.button);
        fix.setText(R.string.mClear_clear);
        fix.setOnClickListener(v -> clear());
    }

    @Override
    public void onModifyUrl(UrlData urlData, JavaUtils.Function<UrlData, Boolean> setNewUrl) {
        cleared = urlData.url;
        data = new Data();

        if (urlData.getData(CLEARED) != null) {
            // was cleared
            data.addInfo(R.string.mClear_cleared);
            data.setColor(R.color.good);
        }

        whileProvider:
        for (Pair<String, JSONObject> rule : rules) {
            // evaluate each provider
            String provider = rule.first;
            JSONObject providerData = rule.second;
            try {
                if (!matcher(providerData.getString("urlPattern"), cleared).find()) {
                    continue;
                }

                // info
                if (verbose.get()) data.addInfo(R.string.mClear_matches, provider);

                // check blocked completeProvider
                if (providerData.optBoolean("completeProvider", false)) {
                    // provider blocked
                    data.addInfo(R.string.mClear_blocked);
                    data.setColor(R.color.bad);
                    continue;
                }

                // check exceptions
                if (providerData.has("exceptions")) {
                    JSONArray exceptions = providerData.getJSONArray("exceptions");
                    for (int i = 0; i < exceptions.length(); i++) {
                        String exception = exceptions.getString(i);
                        if (matcher(exception, cleared).find()) {
                            // exception matches, ignore provider
                            if (verbose.get()) data.addInfo(R.string.mClear_exception, exception);
                            continue whileProvider;
                        }
                    }
                }

                // apply redirections
                if (providerData.has("redirections")) {
                    JSONArray redirections = providerData.getJSONArray("redirections");
                    for (int i = 0; i < redirections.length(); i++) {
                        String redirection = redirections.getString(i);
                        Matcher matcher = Pattern.compile(redirection).matcher(cleared);
                        if (matcher.find() && matcher.groupCount() >= 1) {
                            // redirection found
                            if (providerData.optBoolean("forceRedirection", false)) {
                                // maybe do something special?
                                data.addInfo(R.string.mClear_forcedRedirection);
                            } else {
                                data.addInfo(R.string.mClear_redirection);
                            }
                            if (verbose.get()) data.addDetails(redirection);
                            cleared = decodeURIComponent(matcher.group(1)); // can't be null, checked in the if
                            data.setColor(R.color.warning);
                            continue whileProvider;
                        }
                    }
                }

                // apply rawRules
                if (providerData.has("rawRules")) {
                    JSONArray rawRules = providerData.getJSONArray("rawRules");
                    for (int i = 0; i < rawRules.length(); i++) {
                        String rawRule = rawRules.getString(i);
                        Matcher matcher = matcher(rawRule, cleared);
                        if (matcher.find()) {
                            // rawrule matches, apply
                            cleared = matcher.replaceAll("");
                            data.addInfo(R.string.mClear_rawRule);
                            if (verbose.get()) data.addDetails(rawRule);
                            data.setColor(R.color.warning);
                        }
                    }
                }

                // apply rules
                if (providerData.has("rules")) {
                    JSONArray paths = providerData.getJSONArray("rules");
                    for (int i = 0; i < paths.length(); i++) {
                        String path = "([?&#])" + paths.getString(i) + "=[^&#]*";
                        Matcher matcher = matcher(path, cleared);
                        while (matcher.find()) {
                            // rule applies
                            cleared = matcher.replaceFirst("$1");
                            matcher.reset(cleared);
                            data.addInfo(R.string.mClear_rule);
                            if (verbose.get()) data.addDetails(paths.getString(i));
                            data.setColor(R.color.warning);
                        }
                    }
                }

                // apply referral rules
                if (!allowReferral.get() && providerData.has("referralMarketing")) {
                    JSONArray referrals = providerData.getJSONArray("referralMarketing");
                    for (int i = 0; i < referrals.length(); i++) {
                        String referral = "([?&#])" + referrals.getString(i) + "=[^&#]*";
                        Matcher matcher = matcher(referral, cleared);
                        while (matcher.find()) {
                            // raw rule applies
                            cleared = matcher.replaceFirst("$1");
                            matcher.reset(cleared);
                            data.addInfo(R.string.mClear_referral);
                            if (verbose.get()) data.addDetails(referrals.getString(i));
                            data.setColor(R.color.warning);
                        }
                    }
                }

                // if changed, fix cleaning artifacts
                if (!cleared.equals(urlData.url)) {

                    // remove empty elements
                    cleared = cleared
                            .replaceAll("\\?&+", "?")
                            .replaceAll("\\?#", "#")
                            .replaceAll("\\?$", "")
                            .replaceAll("&&+", "&")
                            .replaceAll("&#", "#")
                            .replaceAll("&$", "")
                            .replaceAll("#&+", "#")
                            .replaceAll("#$", "")
                    ;

                    // restore missing domain
                    if (!cleared.matches("^https?://.*")) {
                        cleared = "http://" + cleared;
                    }
                }
            } catch (JSONException | UnsupportedEncodingException e) {
                e.printStackTrace();
                if (verbose.get()) {
                    data.addInfo(R.string.mClear_error);
                    data.addDetails(provider);
                }
            }
        }

        // url changed
        if (!cleared.equals(urlData.url)) {
            // apply automatically if required
            if (auto.get())
                if (setNewUrl.apply(new UrlData(cleared).putData(CLEARED, CLEARED))) return;

            // enable button
            data.enabled = true;
            if (verbose.get()) {
                data.info += "\n\n -> " + cleared;
            }
        }
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        // update button
        fix.setEnabled(data.enabled);

        // update text
        if (data.info.isEmpty()) {
            // nothing found
            info.setText(R.string.mClear_noRules);
            setVisibility(false);
        } else {
            // something found
            info.setText(data.info);
            setVisibility(true);
        }
        if (data.color != 0) AndroidUtils.setRoundedColor(data.color, info);
        else AndroidUtils.clearRoundedColor(info);
    }

    /* ------------------- internal ------------------- */

    /**
     * Dataclass for transferring data
     */
    private class Data {
        public boolean enabled = false;
        String info = "";
        int color = 0;

        /**
         * Changes the color, managing importance
         */
        void setColor(int newColor) {
            if (color == R.color.bad && newColor == R.color.warning) return; // keep bad instead of replacing with warning
            color = newColor;
        }

        /**
         * Appends a line to the info textview, manages linebreaks
         */
        void addInfo(int line, Object... formatArgs) {
            if (!info.isEmpty()) info += "\n";
            info += getActivity().getString(line, formatArgs);
        }

        /**
         * utility to append details to the info textview, after calling append
         */
        void addDetails(String data) {
            info += ": " + data;
        }

    }

    /**
     * Clear the url
     */
    private void clear() {
        if (cleared != null) setUrl(new UrlData(cleared).putData(CLEARED, CLEARED));
    }

    // ------------------- utils -------------------

    /**
     * Hopefully the same as javascript's decodeURIComponent
     * Idea from https://stackoverflow.com/a/6926987, but using own implementation
     */
    private static String decodeURIComponent(String text) throws UnsupportedEncodingException {
        var result = new StringBuilder();
        var parts = text.split("\\+");
        for (var part : parts) {
            if (result.length() != 0) result.append('+');
            result.append(decode(part));
        }
        return result.toString();
    }

    /**
     * Matches case insensitive regex into an input
     *
     * @param regex regex to use
     * @param input input to use
     * @return the matcher object
     */
    private static Matcher matcher(String regex, String input) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(input);
    }

}
