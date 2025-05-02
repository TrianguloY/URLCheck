package com.trianguloy.urlchecker.utilities.methods;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Consumer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/** Generic Android utilities */
public interface AndroidUtils {

    /**
     * For some reason some drawable buttons are displayed the same when enabled and disabled.
     * This method also sets an alpha as a workaround
     */
    static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.35f);
    }

    /** In debug mode, throws an AssertionError, in production just logs it and continues. */
    static void assertError(String detailMessage) {
        assertError(detailMessage, null);
    }

    /** In debug mode, throws an AssertionError, in production just logs it and continues. */
    static void assertError(String detailMessage, Throwable cause) {
        Log.d("ASSERT_ERROR", detailMessage, cause);
        if (BuildConfig.DEBUG) {
            // in debug mode, throw exception
            throw new AssertionError(detailMessage, cause);
        }
        // non-debug, just discard
    }

    /** Sets the background color (resource id) of a view using a rounded box drawable */
    static void setRoundedColor(int color, View view) {
        setRawRoundedColor(view.getContext().getResources().getColor(color), view);
    }

    /** Sets the background color (raw color) of a view using a rounded box drawable */
    static void setRawRoundedColor(int color, View view) {
        var drawable = view.getContext().getResources().getDrawable(R.drawable.round_box);
        drawable.setColorFilter(color, PorterDuff.Mode.SRC);
        view.setBackground(drawable);
    }

    /** Clears the background color of a view */
    static void clearRoundedColor(View view) {
        view.setBackground(null);
    }

    /** Makes the text of a textview display as a link (which does nothing when clicked) */
    static void setAsClickable(TextView textview) {
        SpannableStringBuilder text = new SpannableStringBuilder(textview.getText());
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View ignored) {
                // do nothing
            }
        }, 0, text.length(), 0);
        textview.setText(text);
        // TODO: remove underline (set color only). textview.setTextColor(textview.getResources().getColor(R.color.app)); doesn't work
    }

    /**
     * Returns a formatted date/time from epoch milliseconds according to the context locale.
     * If the date is invalid (negative), "---" is returned instead
     */
    static String formatMillis(long millis, Context cntx) {
        if (millis < 0) return "---";

        var locale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? cntx.getResources().getConfiguration().getLocales().get(0)
                : cntx.getResources().getConfiguration().locale;
        return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale).format(new Date(millis));
    }

    /** Copy to the clipboard, retrieves string from id */
    static void copyToClipboard(Activity activity, int id, String text) {
        copyToClipboard(activity, activity.getString(id), text);
    }

    /** Copy to the clipboard */
    static void copyToClipboard(Activity activity, String toast, String text) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;

        clipboard.setPrimaryClip(ClipData.newPlainText("", text));

        // show toast to notify it was copied (except on Android 13+, where the device shows a popup itself)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            Toast.makeText(activity, toast, Toast.LENGTH_LONG).show();
    }

    /**
     * Get the (possible) referrer activity from an existing one.
     * Null if can't find
     */
    static String getReferrer(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return null;

        // apps can 'fake' the referrer with these two extras. So remove them first, and restore them later
        var intent = activity.getIntent();
        Uri extraReferrer = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                extraReferrer = intent.getParcelableExtra(Intent.EXTRA_REFERRER, Uri.class);
                intent.removeExtra(Intent.EXTRA_REFERRER);
            } catch (Exception ignored) {
            }
        }
        var extraReferrerName = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME);
        intent.removeExtra(Intent.EXTRA_REFERRER_NAME);

        var referrer = activity.getReferrer();

        if (extraReferrer != null) intent.putExtra(Intent.EXTRA_REFERRER, extraReferrer);
        if (extraReferrerName != null) intent.putExtra(Intent.EXTRA_REFERRER_NAME, extraReferrerName);


        // the scheme must exist and be "android-app"
        if (referrer == null || !"android-app".equals(referrer.getScheme())) return null;
        // the host is the package
        return referrer.getHost();
    }

    /**
     * @see ActionBar#setDisplayHomeAsUpEnabled(boolean)
     * And don't forget to override onOptionsItemSelected!
     */
    static void configureUp(Activity activity) {
        var actionBar = activity.getActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Sets an onClickListener to a [view] so that:
     * [toggle] will be called when clicked (to change something).
     * [listener] will be called now and when clicked (to update state).
     * If you need to initialize things, do them before calling this.
     */
    static <V extends View> void toggleableListener(V view, Consumer<V> toggle, Consumer<V> listener) {
        view.setOnClickListener(v -> {
            toggle.accept(view);
            listener.accept(view);
        });
        listener.accept(view);
    }

    /** Adds an onLongClickListener that will show a toast with the contentdescription */
    static void longTapForDescription(View view) {
        view.setOnLongClickListener(v -> {
            var contentDescription = v.getContentDescription();
            if (contentDescription == null) {
                AndroidUtils.assertError("No content description for view " + view);
            } else {
                Toast.makeText(v.getContext(), contentDescription, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    /** Sets the text and the visibility of a textview (visible iff there is text) */
    static void setHideableText(TextView view, CharSequence text) {
        view.setText(text);
        view.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
    }

    /** Fixes the color of a MenuItem icon (colorFilter=textColorPrimary) */
    static void fixMenuIconColor(MenuItem menuItem, Context cntx) {
        // get color
        var resolvedAttr = new TypedValue();
        cntx.getTheme().resolveAttribute(android.R.attr.textColorPrimary, resolvedAttr, true);

        // tint
        menuItem.getIcon().setColorFilter(cntx.getResources().getColor(resolvedAttr.resourceId), PorterDuff.Mode.SRC_IN);
    }

    /** Returns all the unique links found on a given text */
    static Set<String> getLinksFromText(CharSequence text) {
        var links = new HashSet<String>();
        var matcher = Patterns.WEB_URL.matcher(text);
        while (matcher.find()) links.add(matcher.group());
        return links;
    }

    /** Copies a Uri to a file. */
    static void copyUri2File(Uri uri, File file, Context cntx) throws IOException {
        try (var in = cntx.getContentResolver().openInputStream(uri)) {
            StreamUtils.inputStream2File(in, file);
        }
    }

    /** OnClickListener that reports the click position */
    static void setOnClickWithPositionListener(View view, Consumer<Pair<Float, Float>> listener) {
        var xy = new float[2];
        view.setOnTouchListener((v, event) -> {
            // store any interaction and continue
            xy[0] = event.getX();
            xy[1] = event.getY();
            return false;
        });
        view.setOnClickListener(v -> listener.accept(Pair.create(xy[0], xy[1])));
    }

    String MARKER = "%S$S/S%";

    /**
     * this code replaces the [MARKER] in [textWithMarker] with the underlined [url]. Will call onClick with the url when clicked.
     * REMEMBER: you need to configure the textview to accept clicks with textview.setMovementMethod(LinkMovementMethod.getInstance());
     */
    static CharSequence underlineUrl(String textWithMarker, String url, Consumer<String> onClick) {
        // it does so by using a marker to underline exactly the parameter (wherever it is) and later replace it with the final url
        // all underlined looks bad, and auto-underline may not work with some malformed urls

        // "Redirects to {marker}" -> "Redirects to _{marker}_"
        var start = textWithMarker.indexOf(MARKER);
        var end = start + MARKER.length();
        SpannableStringBuilder text = new SpannableStringBuilder(textWithMarker);
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View ignored) {
                onClick.accept(url);
            }
        }, start, end, 0);

        // "Redirects to _{marker}_" -> "Redirects to _{redirectionUrl}_"
        text.replace(start, end, url);
        return text;
    }

    /** Reduces the height of a view to their children's size (if less) */
    static void limitHeight(ViewGroup view) {
        view.post(() -> {
            int childHeight = 0;
            for (int i = 0; i < view.getChildCount(); i++) {
                childHeight += view.getChildAt(i).getHeight();
            }
            if (view.getHeight() > childHeight) {
                view.setLayoutParams(new LinearLayout.LayoutParams(view.getWidth(), childHeight));
            }
        });
    }

    /**
     * Sets the width of an activity.
     * width must be either a percentage (>=0) or a special layout value (<0)
     */
    static void setActivityWidth(int width, Activity cntx) {
        var params = cntx.getWindow().getAttributes();

        if (width < 0) {
            params.width = width;
        } else {
            var metrics = new DisplayMetrics();
            cntx.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            params.width = metrics.widthPixels * width / 100;
        }
        cntx.getWindow().setAttributes(params);
    }
}
