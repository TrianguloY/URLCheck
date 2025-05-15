package com.trianguloy.urlchecker.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.view.Window;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;

import java.util.Collections;
import java.util.Set;

/** This activity opens (on this app) a link detected on the clipboard text. If multiple asks. */
public class ShortcutsActivity extends Activity {

    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            // old android method
            setResult(RESULT_OK, new Intent()
                    .putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(this, getClass())
                            .setAction(Intent.ACTION_VIEW)
                    )
                    .putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcut_checkClipboard))
                    .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.mipmap.clipboard_launcher))
            );
            finish();
            return;
        }

        // set theme without action bar
        AndroidSettings.setTheme(this, true);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        waitForFocus(0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // reused activity, cancel previous dialog
        if (dialog != null) dialog.dismiss();
        waitForFocus(0);
    }

    /**
     * Waits for the app to has focus (up to 5 seconds) then runs.
     * The clipboard isn't available until the app is fully visible and with focus
     */
    private void waitForFocus(int retry) {
        if (!hasWindowFocus() && retry < 50)
            getWindow().getDecorView().postDelayed(() -> waitForFocus(retry + 1), 100);
        else run();
    }

    /** To run when the clipboard is available */
    private void run() {
        var links = getLinksFromClipboard();
        switch (links.size()) {
            case 0:
                // no links, notify
                Toast.makeText(this, R.string.noLinks, Toast.LENGTH_SHORT).show();
                finish();
                break;
            case 1:
                // 1 link, open
                open(links.iterator().next());
                finish();
                break;
            default:
                // multiple links, choose
                var links_array = links.toArray(new String[0]);
                dialog = new AlertDialog.Builder(this)
                        .setItems(links_array, (dialog, which) -> {
                            open(links_array[which]);
                            dialog.cancel();
                        })
                        .setOnCancelListener(o -> this.finish())
                        .show();
        }
    }

    /** Opens a link on our app */
    private void open(String link) {
        PackageUtils.startActivity(
                new Intent()
                        .setAction(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_TEXT, link)
                        .setType("text/plain")
                        .setPackage(getPackageName()),
                R.string.toast_noApp,
                this
        );
    }

    /** Returns all links detected on the clipboard */
    private Set<String> getLinksFromClipboard() {

        var clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return Collections.emptySet();

        var primaryClip = clipboard.getPrimaryClip();
        if (primaryClip == null || primaryClip.getItemCount() < 1) return Collections.emptySet();

        return AndroidUtils.getLinksFromText(primaryClip.getItemAt(0).coerceToText(this));
    }

    /** The tile, just a shortcut to the activity above */
    @TargetApi(Build.VERSION_CODES.N)
    public static class ShortcutsTile extends TileService {

        @SuppressLint("StartActivityAndCollapseDeprecated")
        @Override
        public void onClick() {
            super.onClick();
            // just call the activity to handle it
            var intent = new Intent(this, ShortcutsActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                ));
            } else {
                startActivityAndCollapse(intent);
            }
        }
    }
}
