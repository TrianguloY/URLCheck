package com.trianguloy.urlchecker.activities;

import static com.trianguloy.urlchecker.utilities.methods.AndroidUtils.getStringWithPlaceholder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Inflater;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Function;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;
import com.trianguloy.urlchecker.utilities.methods.StreamUtils;

import java.io.IOException;
import java.util.List;

public class AboutActivity extends Activity {

    // ------------------- links -------------------

    private static final List<Link> LINKS = List.of(
            new Link(R.string.link_changelog, "https://github.com/TrianguloY/URLCheck/releases/"), // TODO: link to the correct translation, and link to latest file from the fastlane folder
            new Link(R.string.link_source, "https://github.com/TrianguloY/URLCheck"),
            new Link(R.string.link_privacy, "https://github.com/TrianguloY/URLCheck/blob/master/docs/PRIVACY%20POLICY.md"),
            new Link(R.string.lnk_fDroid, "https://f-droid.org/packages/com.trianguloy.urlchecker"),
            new Link(R.string.lnk_playStore, "https://play.google.com/store/apps/details?id=com.trianguloy.urlchecker"),
            new Link(R.string.lnk_izzy, "https://apt.izzysoft.de/fdroid/index/apk/com.trianguloy.urlchecker"),
            new Link(cntx -> getStringWithPlaceholder(cntx, R.string.link_blog, R.string.trianguloy), "https://triangularapps.blogspot.com/")
    );

    private record Link(int labelResource, Function<Context, String> label, String link) {
        private Link(int labelResource, String link) {
            this(labelResource, null, link);
        }

        private Link(Function<Context, String> label, String link) {
            this(-1, label, link);
        }

        void setLabel(TextView textView) {
            if (label != null) textView.setText(label.apply(textView.getContext()));
            else textView.setText(labelResource);
        }

    }

    // ------------------- listeners -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_about);
        setTitle(R.string.a_about);
        AndroidUtils.configureUp(this);

        setTitle(getTitle()
                + " (V" + BuildConfig.VERSION_NAME
                + (!"release".equals(BuildConfig.BUILD_TYPE) ? " - " + BuildConfig.BUILD_TYPE : "")
                + ")");

        // fill contributors and translators
        this.<TextView>findViewById(R.id.txt_about).setText(
                getString(R.string.txt_about,
                        getString(R.string.trianguloy),
                        getString(R.string.contributors),
                        getString(R.string.all_translators)
                )
        );

        // trademarks
        this.<TextView>findViewById(R.id.tm_clear).setText(getStringWithPlaceholder(this, R.string.mClear_tm, R.string.clearRules_url));
        this.<TextView>findViewById(R.id.tm_hosts).setText(getStringWithPlaceholder(this, R.string.mHosts_tm, R.string.stevenBlack_url));

        // create links
        ViewGroup v_links = findViewById(R.id.links);
        for (var link : LINKS) {
            var v_link = Inflater.<TextView>inflate(R.layout.about_link, v_links);
            link.setLabel(v_link);
            AndroidUtils.setAsClickable(v_link);
            v_link.setTag(link.link);
            // click to open, longclick to share
            v_link.setOnClickListener(v -> open(((String) v.getTag())));
            v_link.setOnLongClickListener(v -> {
                share(((String) v.getTag()));
                return true;
            });
        }

        // show logcat
        if (BuildConfig.DEBUG) {
            findViewById(R.id.trianguloy).setOnClickListener(v -> {

                // get log
                String log;
                try {
                    log = StreamUtils.inputStream2String(Runtime.getRuntime().exec("logcat -d").getInputStream());
                } catch (IOException e) {
                    log = e.toString();
                }

                // generate dialog
                var textView = new TextView(this);
                textView.setText(log);
                textView.setTextIsSelectable(true);

                // wrap into a padded scrollview+horizontalscrollview for nice scrolling
                var scrollView = new ScrollView(this);
                scrollView.addView(textView);
                scrollView.post(() -> scrollView.scrollTo(0, textView.getHeight())); // start at bottom (new)
                var horizontalScrollView = new HorizontalScrollView(this);
                int pad = getResources().getDimensionPixelSize(R.dimen.smallPadding);
                horizontalScrollView.setPadding(pad, pad, pad, pad);
                horizontalScrollView.addView(scrollView);

                new AlertDialog.Builder(this)
                        .setTitle("Logcat")
                        .setView(horizontalScrollView)
                        .setPositiveButton("close", null)
                        .setNeutralButton("clear", (dialog, which) -> {
                            try {
                                Runtime.getRuntime().exec("logcat -c");
                                dialog.cancel();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .show();
            });
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // press the 'back' button in the action bar to go back
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------- actions -------------------

    /** Open a url in the browser */
    private void open(String url) {
        PackageUtils.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), R.string.toast_noBrowser, this);
    }

    /** Share an url as plain text */
    private void share(String url) {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // Add data to the intent, the receiving app will decide what to do with it.
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, url);

        PackageUtils.startActivity(Intent.createChooser(share, getString(R.string.share)), R.string.toast_noApp, this);
    }
}
