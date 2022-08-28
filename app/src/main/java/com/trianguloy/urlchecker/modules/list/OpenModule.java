package com.trianguloy.urlchecker.modules.list;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ConfigActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.companions.CTabs;
import com.trianguloy.urlchecker.modules.companions.LastOpened;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.GenericPref;
import com.trianguloy.urlchecker.utilities.PackageUtilities;
import com.trianguloy.urlchecker.utilities.UrlUtilities;

import java.util.List;

/**
 * This module contains an open and share buttons
 */
public class OpenModule extends AModuleData {

    @Override
    public String getId() {
        return "open";
    }

    @Override
    public int getName() {
        return R.string.mOpen_name;
    }

    @Override
    public boolean canBeDisabled() {
        return false;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new OpenDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ConfigActivity cntx) {
        return new OpenConfig(cntx);
    }
}

class OpenDialog extends AModuleDialog implements View.OnClickListener, PopupMenu.OnMenuItemClickListener, View.OnLongClickListener {

    private LastOpened lastOpened;

    private final GenericPref.Enumeration<CTabs.Config> ctabsPref = CTabs.PREF();
    private boolean ctabs = false;

    private List<String> packages;
    private Button btn_open;
    private ImageButton btn_openWith;
    private Menu menu;
    private PopupMenu popup;
    private ImageButton btn_ctabs;

    public OpenDialog(MainDialog dialog) {
        super(dialog);
        ctabsPref.init(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_open;
    }

    @Override
    public void onInitialize(View views) {
        Intent intent = getActivity().getIntent();

        // init ctabs
        btn_ctabs = views.findViewById(R.id.ctabs);
        if (CTabs.isAvailable()) {
            btn_ctabs.setOnClickListener(this);
            btn_ctabs.setOnLongClickListener(this);
            switch (ctabsPref.get()) {
                case ON:
                    setCtabs(true);
                    break;
                case OFF:
                    setCtabs(false);
                    break;
                case AUTO:
                default:
                    // If auto we get it from the intent
                    setCtabs(intent.hasExtra(CTabs.EXTRA));
                    break;
                case ENABLED:
                    // enable but hide
                    setCtabs(true);
                    btn_ctabs.setVisibility(View.GONE);
                    break;
                case DISABLED:
                    // disable but hide
                    setCtabs(false);
                    btn_ctabs.setVisibility(View.GONE);
                    break;
            }
        } else {
            btn_ctabs.setVisibility(View.GONE);
        }

        // init open
        btn_open = views.findViewById(R.id.open);
        btn_open.setOnClickListener(this);
        btn_open.setOnLongClickListener(this);

        // init openWith
        btn_openWith = views.findViewById(R.id.open_with);
        btn_openWith.setOnClickListener(this);

        // init share
        View btn_share = views.findViewById(R.id.share);
        btn_share.setOnClickListener(this);
        btn_share.setOnLongClickListener(this);

        // init openWith popup
        popup = new PopupMenu(getActivity(), btn_open);
        popup.setOnMenuItemClickListener(this);
        menu = popup.getMenu();

        // init lastOpened utility
        lastOpened = new LastOpened(getActivity());
    }

    @Override
    public void onNewUrl(UrlData urlData) {
        updateSpinner();
    }

    // ------------------- Button listener -------------------

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ctabs:
                toggleCtabs();
                break;
            case R.id.open:
                openUrl(0);
                break;
            case R.id.share:
                shareUrl();
                break;
            case R.id.open_with:
                showList();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.ctabs:
                Toast.makeText(getActivity(), R.string.mOpen_tabsDesc, Toast.LENGTH_SHORT).show();
                break;
            case R.id.share:
                copyToClipboard();
                break;
            default:
                return false;
        }
        return true;
    }

    // ------------------- PopupMenu.OnMenuItemClickListener -------------------

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        openUrl(item.getItemId());
        return false;
    }

    // ------------------- Spinner -------------------

    /**
     * Populates the spinner with the apps that android says that can open it
     */
    private void updateSpinner() {
        packages = PackageUtilities.getOtherPackages(UrlUtilities.getViewIntent(getUrl(), null), getActivity());

        // check no apps
        if (packages.isEmpty()) {
            btn_open.setText(R.string.mOpen_noapps);
            btn_open.setEnabled(false);
            btn_openWith.setVisibility(View.GONE);
            return;
        }

        // sort
        lastOpened.sort(packages);

        // set
        btn_open.setText(getActivity().getString(R.string.mOpen_with, PackageUtilities.getPackageName(packages.get(0), getActivity())));
        btn_open.setEnabled(true);
        menu.clear();
        if (packages.size() == 1) {
            btn_openWith.setVisibility(View.GONE);
        } else {
            btn_openWith.setVisibility(View.VISIBLE);
            for (int i = 1; i < packages.size(); i++) {
                menu.add(Menu.NONE, i, i, getActivity().getString(R.string.mOpen_with, PackageUtilities.getPackageName(packages.get(i), getActivity())));
            }
        }

    }

    // ------------------- Buttons -------------------

    /**
     * Open url in a specific app
     *
     * @param index index from the packages list of the app to use
     */
    private void openUrl(int index) {
        // get
        if (index < 0 || index >= packages.size()) return;
        String chosen = packages.get(index);

        // update as preferred over the rest
        lastOpened.prefer(chosen, packages);

        // open
        Intent intent = new Intent(getActivity().getIntent());
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // preserve original VIEW intent
            intent.setData(Uri.parse(getUrl()));
            intent.setComponent(null);
            intent.setPackage(chosen);
        } else {
            // replace with new VIEW intent
            intent = UrlUtilities.getViewIntent(getUrl(), chosen);
        }

        if (ctabs && !intent.hasExtra(CTabs.EXTRA)) {
            // enable Custom tabs

            if (CTabs.isAvailable()) {
                Bundle extras = new Bundle();
                extras.putBinder(CTabs.EXTRA, null); //  Set to null for no session
                intent.putExtras(extras);
            }
        }

        if (!ctabs && intent.hasExtra(CTabs.EXTRA)) {
            // disable ctabs
            intent.removeExtra(CTabs.EXTRA);
        }

        PackageUtilities.startActivity(intent, R.string.toast_noApp, getActivity());
    }

    /**
     * Show the popup with the rest of the apps
     */
    private void showList() {
        popup.show();
    }

    /**
     * Shares the url as text
     */
    private void shareUrl() {
        // create send intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getUrl());
        sendIntent.setType("text/plain");

        // share intent
        PackageUtilities.startActivity(
                Intent.createChooser(sendIntent, getActivity().getString(R.string.mOpen_share)),
                R.string.mOpen_noapps,
                getActivity()
        );
    }

    /**
     * Copy the url to the clipboard
     */
    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", getUrl());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getActivity(), R.string.mOpen_clipboard, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Toggle the custom tabs state
     */
    private void toggleCtabs() {
        setCtabs(!ctabs);
    }

    /**
     * Sets the custom tabs state
     */
    private void setCtabs(boolean state) {
        btn_ctabs.setImageResource(state ? R.drawable.ctabs_on : R.drawable.ctabs_off);
        ctabs = state;
    }

}

class OpenConfig extends AModuleConfig {

    private final GenericPref.Enumeration<CTabs.Config> ctabsPref = CTabs.PREF();

    public OpenConfig(ConfigActivity activity) {
        super(activity);
        ctabsPref.init(activity);
    }

    @Override
    public boolean canBeEnabled() {
        return true;
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_open;
    }

    @Override
    public void onInitialize(View views) {
        if (CTabs.isAvailable()) {
            ctabsPref.attachToSpinner(views.findViewById(R.id.ctabs_pref));
        } else {
            views.findViewById(R.id.ctabs_parent).setVisibility(View.GONE);
        }
    }
}

