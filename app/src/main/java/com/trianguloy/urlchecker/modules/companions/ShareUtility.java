package com.trianguloy.urlchecker.modules.companions;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;

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
            shareUrl(null, null);
        }

        /** Shares the url as text, optionally to a specific app (for automation rules) */
        public void shareUrl(String packageName, String componentStr) {
            // create send intent
            var sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, mainDialog.getUrlData().url);
            sendIntent.setType("text/plain");

            if (componentStr != null && !componentStr.isEmpty()) {
                // direct to a specific component (skips app's internal chooser)
                sendIntent.setComponent(ComponentName.unflattenFromString(componentStr));
                PackageUtils.startActivity(sendIntent, R.string.mOpen_noapps, mainDialog);
            } else if (packageName != null && !packageName.isEmpty()) {
                // direct to a specific app package
                sendIntent.setPackage(packageName);
                PackageUtils.startActivity(sendIntent, R.string.mOpen_noapps, mainDialog);
            } else {
                // show system chooser
                var chooser = Intent.createChooser(sendIntent, mainDialog.getString(R.string.mOpen_share));
                PackageUtils.startActivity(chooser, R.string.mOpen_noapps, mainDialog);
            }

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
