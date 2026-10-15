package com.trianguloy.urlchecker.utilities.methods;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.modules.list.DebugModule;
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
        // Regardless of settings, log the error
        Log.d("ASSERT_ERROR", detailMessage, cause);

        try {
            // if enabled, show toast
            var cntx = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null, (Object[]) null);
            if (DebugModule.ERRORTOAST_PREF(cntx).get()) {
                Toast.makeText(cntx, "ERROR: " + detailMessage, Toast.LENGTH_LONG).show();
            }
        } catch (final Throwable e) {
            // ignore if can't show toast, but at least log it
            Log.d("ASSERT_ERROR_TOAST", "Can't show toast", e);
        }
    }

    /** Sets the background color (resource id) of a view using a rounded box drawable */
    static void setRoundedColor(int color, View view) {
        setRawRoundedColor(view.getContext().getResources().getColor(color), view);
    }

    /** Sets the background color (raw color) of a view using a rounded box drawable */
    static void setRawRoundedColor(int color, View view) {
        var drawable = view.getContext().getResources().getDrawable(R.drawable.round_box);
        // the label colors are semi-transparent, so the color the user sees depends on the
        // window background behind them: composite them over it to know that color
        var background = flattenOverBackground(color, view.getContext());
        drawable.setColorFilter(background, PorterDuff.Mode.SRC);
        view.setBackground(drawable);

        // the text color comes from the theme and is not chosen for the label color, so it
        // can be hard to read on it. Keep the theme color for clearRoundedColor
        if (view instanceof TextView textView) {
            if (textView.getTag(R.id.themeTextColor) == null) {
                textView.setTag(R.id.themeTextColor, textView.getTextColors());
            }
            textView.setTextColor(contrastingTextColor(background));
        }
    }

    /** Returns black or white, whichever reads better on top of [background] */
    static int contrastingTextColor(int background) {
        // relative luminance, https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
        var luminance = 0.2126 * toLinear(Color.red(background))
                + 0.7152 * toLinear(Color.green(background))
                + 0.0722 * toLinear(Color.blue(background));
        // both contrast ratios are equal at this luminance
        return luminance > 0.1791 ? Color.BLACK : Color.WHITE;
    }

    private static double toLinear(int component) {
        var value = component / 255d;
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    /** Composites a (possibly semi-transparent) color over the window background */
    static int flattenOverBackground(int color, Context cntx) {
        var alpha = Color.alpha(color);
        if (alpha == 255) return color;

        var resolved = new TypedValue();
        if (!cntx.getTheme().resolveAttribute(android.R.attr.colorBackground, resolved, true)) {
            return color;
        }
        var background = resolved.resourceId != 0
                ? cntx.getResources().getColor(resolved.resourceId)
                : resolved.data;

        var ratio = alpha / 255f;
        return Color.rgb(
                Math.round(Color.red(color) * ratio + Color.red(background) * (1 - ratio)),
                Math.round(Color.green(color) * ratio + Color.green(background) * (1 - ratio)),
                Math.round(Color.blue(color) * ratio + Color.blue(background) * (1 - ratio))
        );
    }

    /** Clears the background color of a view */
    static void clearRoundedColor(View view) {
        view.setBackground(null);
        // give back the text color setRawRoundedColor replaced, or it stays black/white
        if (view instanceof TextView textView
                && textView.getTag(R.id.themeTextColor) instanceof ColorStateList themeColor) {
            textView.setTextColor(themeColor);
            textView.setTag(R.id.themeTextColor, null);
        }
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

    /**
     * Returns a resource string with a single argument passed as another resource string.
     * If @string/a="a %s" and @string/b="b" then getStringWithPlaceholder(cntx, R.string.a, R.string.b) will return "a b"
     */
    static String getStringWithPlaceholder(Context cntx, int stringResource, int placeholderStringResource) {
        return cntx.getString(stringResource, cntx.getString(placeholderStringResource));
    }
}
