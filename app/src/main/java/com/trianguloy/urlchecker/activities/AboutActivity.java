package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Inflater;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;

import java.util.List;

public class AboutActivity extends Activity {

    // ------------------- links -------------------

    private static final List<Pair<Integer, String>> LINKS = List.of(
            Pair.create(R.string.link_changelog, "https://github.com/TrianguloY/URLCheck/blob/master/app/src/main/play/release-notes/en-US/default.txt"), // TODO: link to the correct translation
            Pair.create(R.string.link_source, "https://github.com/TrianguloY/URLCheck"),
            Pair.create(R.string.link_privacy, "https://github.com/TrianguloY/URLCheck/blob/master/docs/PRIVACY%20POLICY.md"),
            Pair.create(R.string.lnk_fDroid, "https://f-droid.org/packages/com.trianguloy.urlchecker"),
            Pair.create(R.string.lnk_playStore, "https://play.google.com/store/apps/details?id=com.trianguloy.urlchecker"),
            Pair.create(R.string.lnk_izzy, "https://apt.izzysoft.de/fdroid/index/apk/com.trianguloy.urlchecker"),
            Pair.create(R.string.link_blog, "https://triangularapps.blogspot.com/")
    );

    // ------------------- listeners -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_about);
        setTitle(R.string.a_about);
        AndroidUtils.configureUp(this);

        if (!BuildConfig.DEBUG) {
            // on release, append version to the action bar title
            setTitle(getTitle() + " (V" + BuildConfig.VERSION_NAME + ")");
        } else if (!"alpha".equals(BuildConfig.BUILD_TYPE)) {
            // on no-alpha, append type
            setTitle(getTitle() + " (" + BuildConfig.BUILD_TYPE + ")");
        }

        // fill contributors and translators
        this.<TextView>findViewById(R.id.txt_about).setText(
                getString(R.string.txt_about,
                        getString(R.string.contributors),
                        getString(R.string.all_translators)
                )
        );

        // create links
        ViewGroup v_links = findViewById(R.id.links);
        for (var link : LINKS) {
            var v_link = Inflater.<TextView>inflate(R.layout.about_link, v_links);
            v_link.setText(link.first);
            AndroidUtils.setAsClickable(v_link);
            v_link.setTag(link.second);
            // click to open, longclick to share
            v_link.setOnClickListener(v -> open(((String) v.getTag())));
            v_link.setOnLongClickListener(v -> {
                share(((String) v.getTag()));
                return true;
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