package com.trianguloy.urlchecker.utilities.methods;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.sUTF_8;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.wrappers.IntentApp;

import java.net.URLDecoder;
import java.util.ArrayList;

/** Static utilities related to urls */
public interface UrlUtils {

    /** Returns an intent that will open the given [url], with an optional [intentApp] */
    static Intent getViewIntent(String url, IntentApp intentApp) {
        var intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intentApp != null) intent.setComponent(intentApp.getComponent());
        return intent;
    }

    /** Opens an [url] removing this app from the chooser */
    static void openUrlRemoveThis(String url, Context cntx) {
        // get intents that can open the url
        var intents = new ArrayList<Intent>();
        for (var pack : IntentApp.getOtherPackages(getViewIntent(url, null), cntx)) {
            intents.add(getViewIntent(url, pack));
        }

        // check if none
        if (intents.isEmpty()) {
            Toast.makeText(cntx, R.string.toast_noBrowser, Toast.LENGTH_SHORT).show();
            return;
        }

        // create chooser
        var chooserIntent = Intent.createChooser(intents.remove(0), cntx.getString(R.string.title_choose));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[0]));

        // open
        PackageUtils.startActivity(chooserIntent, R.string.toast_noBrowser, cntx);
    }

    /** Calls URLDecoder.decode but returns the input string if the decoding failed */
    static String decode(String string) {
        try {
            return URLDecoder.decode(string, sUTF_8);
        } catch (Exception e) {
            AndroidUtils.assertError("Unable to decode string", e);
            // can't decode, just leave it
            return string;
        }
    }
}
