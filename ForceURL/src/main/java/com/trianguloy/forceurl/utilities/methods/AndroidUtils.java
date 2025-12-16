package com.trianguloy.forceurl.utilities.methods;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.trianguloy.forceurl.BuildConfig;
import com.trianguloy.forceurl.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface AndroidUtils {
    static final String LABEL_DUMMY = "ForceURL_empty_dummy";

    /**
     * Returns the main activity of a package
     */
    static String getMainActivity(Context cntx, String pckg) {
        return cntx.getPackageManager()
                .getLaunchIntentForPackage(pckg)
                .getComponent().getClassName();
    }

    /**
     * Returns a Set with all the activities of a package
     */
    static Set<String> getActivities(Context cntx, String pckg) {
        Set<String> activities = new HashSet<>();
        try {
            ActivityInfo[] activityInfos = (cntx.getPackageManager()
                    .getPackageInfo(pckg, PackageManager.GET_ACTIVITIES).activities);

            for (var act : activityInfos) {
                activities.add(act.name);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        return activities;
    }

    /**
     * Get primary clip from clipboard, retrieves string from id
     */
    static ClipData getPrimaryClip(Context context, int id) {
        return getPrimaryClip(context, context.getString(id), context.getString(R.string.clipboard_getError));
    }

    /**
     * Get primary clip from clipboard, retrieves string from id
     */
    static ClipData getPrimaryClip(Context context, int id, int failId) {
        return getPrimaryClip(context, context.getString(id), context.getString(failId));
    }

    /**
     * Get primary clip from clipboard
     */
    static ClipData getPrimaryClip(Context context, String toast) {
        return getPrimaryClip(context, toast, context.getString(R.string.clipboard_getError));
    }

    static ClipData getPrimaryClip(Context context, String toast, String failToast) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            safeToast(context, R.string.clipboard_serviceError, Toast.LENGTH_LONG);
            return null;
        }

        ClipData res = clipboard.getPrimaryClip();
        // XXX: So, here we have a HUGE problem, res can be:
        //      - Null
        //      - Non null
        //  Non null means that is the content of the clipboard, and we were able to read it
        //  successfully.
        //  BUT, if it is null it can be:
        //      - The clipboard is empty
        //      - We can't access the clipboard
        //  As far as I know there is no way to tell the difference (one exception, explained later),
        //  but it is really important for us to know which one it is.
        //  So we NEED to do the checks ourselves. This is done on canAccessClip()

        if (res == null) {
            if (!canAccessClip()) {
                safeToast(context, failToast, Toast.LENGTH_LONG);
                return null;
            }
            // empty
            res = getDummyClipData();
        }

        // show toast to notify it was read (except on Android 13+, where the device shows a popup itself)
        if (Build.VERSION.SDK_INT < /*Build.VERSION_CODES.TIRAMISU*/33)
            safeToast(context, toast, Toast.LENGTH_LONG);
        return res;
    }

    /**
     * Set primary clip from clipboard, retrieves string from id
     */
    static void setPrimaryClip(Context context, int id, ClipData clipData) {
        setPrimaryClip(context, context.getString(id), clipData);
    }

    /**
     * Set primary clip from clipboard
     */
    static void setPrimaryClip(Context context, String toast, ClipData clipData) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            safeToast(context, R.string.clipboard_setError, Toast.LENGTH_LONG);
            return;
        }

        clipboard.setPrimaryClip(clipData);

        // show toast to notify it was written (except on Android 13+, where the device shows a popup itself)
        if (Build.VERSION.SDK_INT < /*Build.VERSION_CODES.TIRAMISU*/33)
            safeToast(context, toast, Toast.LENGTH_LONG);
    }

    /**
     * Checks if the clipboard can be read at this moment.
     *
     * @return True if can be read, false if uncertain.
     */
    static boolean canAccessClip() {
        //  The documentation of clipboard.getPrimaryClip() says:
        //  "If the application is not the default IME or does not have input focus this return null."
        //  We are not the default IME, so we only need to check input focus.

        var version = (Build.VERSION.SDK_INT < /*Build.VERSION_CODES.Q*/29); // Always available
        var ui = isUiThread(); // Should have "input focus" if true
        var foreground = Build.VERSION.SDK_INT >= /*Build.VERSION_CODES.JELLY_BEAN*/16 &&
                isForeground(); // Should have "input focus" if true
        // isUiThread and isForeground      might seem redundant, but
        // true       and false             respectively, when calling from onPreDraw bubble
        // false      and true              respectively, when calling from background but the user
        //                                  has opened any Activity of the main app,
        // both should be the same value,   in any other case.
        return version ||
                ui ||
                foreground;

        //  If bugs arise this info should be useful
        //  Possible checks:
        //  - This article
        //      https://fingerprint.com/blog/android-14-clipboard-security-leak/
        //  - Check the logcat, here an extract:
        //      2024-09-11 13:07:02.808   640-4035  ClipboardService        system_process                       E  Denying clipboard access to com.trianguloy.urlchecker.incognito, application is not in focus nor is it a system service for user 0
        //  - Check for permissions from rooted devices "READ_CLIPBOARD_IN_BACKGROUND":
        //      https://tasker.helprace.com/i487-access-clipboard-in-background-android-q/1/votes
        //  - Check for "READ_LOGS"
        //      https://joaoapps.com/AutoApps/Help/Info/com.joaomgcd.join/android_10_read_logs.html
        //      https://forum.joaoapps.com/index.php?threads/clipboard-monitor-listener-no-longer-works-on-android-10.49808/
        //  - Source code, to investigate
        //      boolean hasPrimaryClip
        //      boolean getPrimaryClip
        //      https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/content/ClipboardManager.java;l=251;
        //      boolean clipboardAccessAllowed
        //      boolean getPrimaryClip
        //      boolean hasPrimaryClip
        //      https://github.com/aosp-mirror/platform_frameworks_base/blob/master/services/core/java/com/android/server/clipboard/ClipboardService.java#L1304
        //      https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/clipboard/ClipboardService.java;l=1315
    }

    // https://stackoverflow.com/a/40804658
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static boolean isForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }

    /**
     * Just {@link #safeToast(Context, int, int)} but retrieves string from id
     */
    static void safeToast(Context context, int resId, int duration) {
        safeToast(context, context.getString(resId), duration);
    }

    /**
     * A way to show {@link Toast#makeText(Context, CharSequence, int)} if unsure wether this is the
     * UI thread
     */
    static void safeToast(Context context, String text, int duration) {
        runOnUiThread(() -> {
            Toast.makeText(context, text, duration).show();
        });
    }

    /**
     * @return Wether it was called from an UI thread
     */
    static boolean isUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    /**
     * To be used when uncertain if this is the UI thread. If it is the UI thread, it runs
     * immediately, if not, it is added to the message queue of the UI thread via
     * {@link Handler#post(Runnable)}.
     *
     * @param run A runnable which will be ran on the UI thread
     * @return Wether it was called from an UI thread
     */
    static boolean runOnUiThread(Runnable run) {
        var isUiThread = isUiThread();
        if (isUiThread) {
            run.run();
        } else {
            new Handler(Looper.getMainLooper()).post(run);
        }
        return isUiThread;
    }

    static void emptyClipboard(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        // Adapted from
        // https://github.com/TrianguloY/SimpleClipboardEditor/blob/main/app/src/main/java/com/trianguloy/clipboardeditor/Editor.java#L264
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip();
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
        }
    }

    static ClipData getDummyClipData() {
        return ClipData.newPlainText(LABEL_DUMMY, null);
    }

    static boolean areClipDataEquals(ClipData clip1, ClipData clip2, Context context) {
        // ---
        // XXX: For debugging purposes: emulator shared clipboard may trip this up, as it is
        //  constantly syncing and messes up the label.
        //  It is recommended to disable it.
        //      - Android studio: File -> Settings -> Tools -> Emulator -> Enable clipboard sharing
        //
        //  Still it didn't work for me and it kept changing the label

        // Use case where the label is relevant:
        //   User opens URL in incognito, pastes URL, then remembers it wants to copy it to the
        //   clipboard so it copies it. Now the label is different, but the text is not.
        var checkLabel = !BuildConfig.DEBUG;

        // ---
        var count1 = clip1.getItemCount();
        var count2 = clip2.getItemCount();
        var label1 = clip1.getDescription().getLabel();
        var label2 = clip2.getDescription().getLabel();
        // ---


        if (count1 == count2 &&
                (!checkLabel || label1.equals(label2))) {
            for (int i = 0; i < clip1.getItemCount(); i++) {
                // ---
                var sameMimeType = clip1.getDescription().getMimeType(i).equals(
                        clip2.getDescription().getMimeType(i));
                var sameText = clip1.getItemAt(i).coerceToText(context).equals(
                        clip2.getItemAt(i).coerceToText(context));
                // ---

                if (sameMimeType && sameText) {
                    // more conditions if needed
                } else {
                    return false;
                }
            }

            // no differences, they are equal
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves the package from the intent.
     * Used to read intents that come from outside the library.
     */
    static String getPackage(Intent intent){
        return intent.getPackage();
    }


    // https://stackoverflow.com/a/40568194
    static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(),  Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }

        return false;
    }
}
