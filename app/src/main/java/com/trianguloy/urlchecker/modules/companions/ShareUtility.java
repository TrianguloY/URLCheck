package com.trianguloy.urlchecker.modules.companions;


import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;
import com.trianguloy.urlchecker.modules.list.OpenModule;

/**
 * The share functionality.
 * Currently in the OpenDialog, but may be moved to an independent dialog in the future.
 */
public interface ShareUtility {
    static BoolPref CLOSESHARE_PREF(Context cntx) {
        return new BoolPref("open_closeshare", true, cntx);
    }

    static BoolPref CLOSECOPY_PREF(Context cntx) {
        return new BoolPref("open_closecopy", false, cntx);
    }

    static BoolPref MERGECOPY_PREF(Context cntx) {
        return new BoolPref("open_mergeCopy", false, cntx);
    }

    /** The onInitialize of an AModuleConfig */
    static void onInitializeConfig(View views, Activity activity) {
        CLOSESHARE_PREF(activity).attachToSwitch(views.findViewById(R.id.closeshare_pref));
        CLOSECOPY_PREF(activity).attachToSwitch(views.findViewById(R.id.closecopy_pref));
        MERGECOPY_PREF(activity).attachToSwitch(views.findViewById(R.id.mergeCopy_pref));
    }

    /** The equivalent of an AModuleDialog */
    class Dialog {

        private final MainDialog mainDialog;
        private final BoolPref closeSharePref;
        private final BoolPref closeCopyPref;
        private final BoolPref mergeCopyPref;

        public Dialog(MainDialog mainDialog) {
            this.mainDialog = mainDialog;
            closeSharePref = ShareUtility.CLOSESHARE_PREF(mainDialog);
            closeCopyPref = ShareUtility.CLOSECOPY_PREF(mainDialog);
            mergeCopyPref = ShareUtility.MERGECOPY_PREF(mainDialog);
        }

        public void onInitialize(View views) {

            // init copy & share
            var btn_copy = views.findViewById(R.id.copyUrl);
            var btn_share = views.findViewById(R.id.share);
            btn_share.setOnClickListener(v -> shareUrl());
            if (mergeCopyPref.get()) {
                // merge mode (single button)
                btn_copy.setVisibility(View.GONE);
                btn_share.setOnLongClickListener(v -> {
                    copyUrl();
                    return true;
                });
            } else {
                // split mode (two buttons)
                btn_copy.setOnClickListener(v -> copyUrl());
                AndroidUtils.longTapForDescription(btn_share);
                AndroidUtils.longTapForDescription(btn_copy);
            }
        }

        /* ------------------- Buttons ------------------- */

        /** Shares the url as text */
        public void shareUrl() {

            // creates send intent
            Intent baseIntent = new Intent(Intent.ACTION_SEND);
            baseIntent.putExtra(Intent.EXTRA_TEXT, mainDialog.getUrlData().url);
            baseIntent.setType("text/plain");


            DenylistOption deny = OpenModule.DENYLIST_PREF(mainDialog).get();
            String packageToRemove = deny != null ? deny.getPackageName() : null;

            // Gets the denied preference and applied it
            PackageManager pm = mainDialog.getPackageManager();
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(baseIntent, 0);

            // Shows specific apps based on user preference
            List<Intent> targetedIntents = new ArrayList<>();
            for (ResolveInfo ri : resolveInfos) {
                String packageName = ri.activityInfo.packageName;
                if (packageToRemove != null && packageName.equals(packageToRemove)) {
                    continue; // skip denied app (doesn't add it)
                }

                Intent targeted = new Intent(baseIntent);
                targeted.setPackage(packageName);
                targeted.setClassName(packageName, ri.activityInfo.name);
                targetedIntents.add(targeted);
            }

            //If nothing is shown, it shows toast
            if (targetedIntents.isEmpty()) {
                Toast.makeText(mainDialog, R.string.mOpen_noapps, Toast.LENGTH_SHORT).show();
                return;
            }

            //Build chooser so we can provide the denied list
            Intent chooser = Intent.createChooser(targetedIntents.remove(0),
                    mainDialog.getString(R.string.mOpen_share));

            // add the rest of the filtered as EXTRA_INITIAL_INTENTS
            if (!targetedIntents.isEmpty()) {
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        targetedIntents.toArray(new Intent[0]));
            }

            PackageUtils.startActivity(chooser, R.string.mOpen_noapps, mainDialog);

            if (closeSharePref.get()) {
                mainDialog.finish();
            }
        }

        /** Copy the url */
        public void copyUrl() {
            AndroidUtils.copyToClipboard(mainDialog, R.string.mOpen_clipboard, mainDialog.getUrlData().url);
            if (closeCopyPref.get()) {
                mainDialog.finish();
            }
        }
    }

}
