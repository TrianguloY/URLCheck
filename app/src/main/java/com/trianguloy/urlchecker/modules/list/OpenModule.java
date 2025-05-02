package com.trianguloy.urlchecker.modules.list;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.modules.companions.CTabs;
import com.trianguloy.urlchecker.modules.companions.Flags;
import com.trianguloy.urlchecker.modules.companions.Incognito;
import com.trianguloy.urlchecker.modules.companions.LastOpened;
import com.trianguloy.urlchecker.modules.companions.ShareUtility;
import com.trianguloy.urlchecker.modules.companions.Size;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;
import com.trianguloy.urlchecker.utilities.methods.UrlUtils;
import com.trianguloy.urlchecker.utilities.wrappers.IntentApp;
import com.trianguloy.urlchecker.utilities.wrappers.RejectionDetector;

import java.util.List;
import java.util.Objects;

/** This module contains an open and share buttons */
public class OpenModule extends AModuleData {

    public static GenericPref.Bool CLOSEOPEN_PREF(Context cntx) {
        return new GenericPref.Bool("open_closeopen", true, cntx);
    }

    public static GenericPref.Bool NOREFERRER_PREF(Context cntx) {
        return new GenericPref.Bool("open_noReferrer", false, cntx);
    }

    public static GenericPref.Bool REJECTED_PREF(Context cntx) {
        return new GenericPref.Bool("open_rejected", true, cntx);
    }

    public static GenericPref.Enumeration<Size> ICONSIZE_PREF(Context cntx) {
        return new GenericPref.Enumeration<>("open_iconsize", Size.NORMAL, Size.class, cntx);
    }

    @Override
    public String getId() {
        return "open";
    }

    @Override
    public int getName() {
        return R.string.mOpen_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new OpenDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new OpenConfig(cntx);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) OpenDialog.AUTOMATIONS;
    }
}

class OpenDialog extends AModuleDialog {

    static final List<AutomationRules.Automation<OpenDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>("open", R.string.auto_open, dialog ->
                    dialog.openUrl(0)),
            new AutomationRules.Automation<>("share", R.string.auto_share, dialog ->
                    dialog.shareUtility.shareUrl()),
            new AutomationRules.Automation<>("copy", R.string.auto_copy, dialog ->
                    dialog.shareUtility.copyUrl()),
            new AutomationRules.Automation<>("ctabs", R.string.auto_ctabs, dialog ->
                    dialog.cTabs.setState(true)),
            new AutomationRules.Automation<>("incognito", R.string.auto_incognito, dialog ->
                    dialog.incognito.setState(true)),
            new AutomationRules.Automation<>("close", R.string.auto_close, dialog -> dialog.getActivity().finish())
    );

    private final GenericPref.Bool closeOpenPref;
    private final GenericPref.Bool noReferrerPref;
    private final GenericPref.Bool rejectedPref;
    private final GenericPref.Enumeration<Size> iconSizePref;

    private final LastOpened lastOpened;
    private final CTabs cTabs;
    private final Incognito incognito;
    private final RejectionDetector rejectionDetector;
    private final ShareUtility.Dialog shareUtility;

    private List<IntentApp> intentApps;
    private Button btn_open;
    private ImageButton btn_openWith;
    private View openParent;
    private Menu menu;
    private PopupMenu popup;

    public OpenDialog(MainDialog dialog) {
        super(dialog);
        lastOpened = new LastOpened(dialog);
        cTabs = new CTabs(dialog);
        incognito = new Incognito(dialog);
        rejectionDetector = new RejectionDetector(dialog);
        shareUtility = new ShareUtility.Dialog(dialog);
        closeOpenPref = OpenModule.CLOSEOPEN_PREF(dialog);
        noReferrerPref = OpenModule.NOREFERRER_PREF(dialog);
        rejectedPref = OpenModule.REJECTED_PREF(dialog);
        iconSizePref = OpenModule.ICONSIZE_PREF(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_open;
    }

    @Override
    public void onInitialize(View views) {
        var intent = getActivity().getIntent();

        // ctabs
        cTabs.initFrom(intent, views.findViewById(R.id.ctabs));

        // incognito
        incognito.initFrom(intent, views.findViewById(R.id.mode_incognito));

        // init open
        openParent = views.findViewById(R.id.open_parent);
        btn_open = views.findViewById(R.id.open);
        btn_open.setOnClickListener(v -> openUrl(0));

        // init openWith
        btn_openWith = views.findViewById(R.id.open_with);
        btn_openWith.setOnClickListener(v -> showList());

        // init openWith popup
        popup = new PopupMenu(getActivity(), btn_open);
        popup.setOnMenuItemClickListener(item -> {
            openUrl(item.getItemId());
            return false;
        });
        menu = popup.getMenu();

        // share
        shareUtility.onInitialize(views);
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        updateSpinner(urlData.url);
    }

    // ------------------- Spinner -------------------

    /** Populates the spinner with the apps that can open it, in preference order */
    private void updateSpinner(String url) {
        intentApps = IntentApp.getOtherPackages(UrlUtils.getViewIntent(url, null), getActivity());

        // remove referrer
        if (noReferrerPref.get()) {
            var referrer = AndroidUtils.getReferrer(getActivity());
            JavaUtils.removeIf(intentApps, ri -> Objects.equals(ri.getPackage(), referrer));
        }

        // remove rejected if desired (and is not a non-view action, like share)
        // note: this will be called each time, so a rejected package will not be rejected again if the user changes the url and goes back. This is expected
        if (rejectedPref.get() && Intent.ACTION_VIEW.equals(getActivity().getIntent().getAction())) {
            var rejected = rejectionDetector.getPrevious(url);
            JavaUtils.removeIf(intentApps, ri -> Objects.equals(ri.getComponent(), rejected));
        }

        // check no apps
        if (intentApps.isEmpty()) {
            btn_open.setText(R.string.mOpen_noapps);
            btn_open.setCompoundDrawables(null, null, null, null);
            AndroidUtils.setEnabled(openParent, false);
            btn_open.setEnabled(false);
            btn_openWith.setVisibility(View.GONE);
            return;
        }

        // sort
        lastOpened.sort(intentApps, getUrl());

        // set
        var label = intentApps.get(0).getLabel(getActivity());
//        label = getActivity().getString(R.string.mOpen_with, label);
        btn_open.setText(label);
        btn_open.setCompoundDrawables(intentApps.get(0).getIcon(getActivity(), iconSizePref.get()), null, null, null);
        AndroidUtils.setEnabled(openParent, true);
        btn_open.setEnabled(true);
        menu.clear();
        if (intentApps.size() == 1) {
            btn_openWith.setVisibility(View.GONE);
        } else {
            btn_openWith.setVisibility(View.VISIBLE);
            for (int i = 1; i < intentApps.size(); i++) {
                label = intentApps.get(i).getLabel(getActivity());
//                label = getActivity().getString(R.string.mOpen_with, label);
                menu.add(Menu.NONE, i, i, label);//.setIcon(intentApps.get(i).getIcon(getActivity()));
            }
        }

    }

    // ------------------- Buttons -------------------

    /**
     * Open url in a specific app
     *
     * @param index index from the intentApps list of the app to use
     */
    private void openUrl(int index) {
        // get
        if (index < 0 || index >= intentApps.size()) return;
        var chosen = intentApps.get(index);

        // update as preferred over the rest
        lastOpened.prefer(chosen, intentApps, getUrl());

        // open
        var intent = new Intent(getActivity().getIntent());
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // preserve original VIEW intent
            intent.setData(Uri.parse(getUrl()));
            intent.setComponent(chosen.getComponent());
        } else {
            // replace with new VIEW intent
            intent = UrlUtils.getViewIntent(getUrl(), chosen);
        }

        // ctabs
        cTabs.apply(intent);

        // incognito
        incognito.apply(intent);

        // apply flags from global data (probably set by flags module, if active) or by default
        Flags.applyGlobalFlags(intent, this);

        // rejection detector: mark as open
        rejectionDetector.markAsOpen(getUrl(), chosen);

        // open
        PackageUtils.startActivity(intent, R.string.toast_noApp, getActivity());

        // finish activity
        if (closeOpenPref.get()) {
            this.getActivity().finish();
        }
    }

    /** Show the popup with the rest of the apps */
    private void showList() {
        popup.show();
    }

}

class OpenConfig extends AModuleConfig {

    public OpenConfig(ModulesActivity activity) {
        super(activity);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_open;
    }

    @Override
    public void onInitialize(View views) {
        CTabs.PREF(getActivity()).attachToSpinner(views.findViewById(R.id.ctabs_pref), null);
        Incognito.PREF(getActivity()).attachToSpinner(views.findViewById(R.id.incognito_pref), null);
        OpenModule.CLOSEOPEN_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.closeopen_pref));
        OpenModule.NOREFERRER_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.noReferrer));
        OpenModule.REJECTED_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.rejected));
        LastOpened.PERDOMAIN_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.perDomain));
        OpenModule.ICONSIZE_PREF(getActivity()).attachToSpinner(views.findViewById(R.id.iconsize_pref), null);

        // share
        ShareUtility.onInitializeConfig(views, getActivity());
    }
}

