package com.trianguloy.forceurl.helpers.list;

import android.content.Context;
import android.widget.Toast;

import com.trianguloy.forceurl.data.AccessibilityFunction;
import com.trianguloy.forceurl.data.Apps;
import com.trianguloy.forceurl.helpers.AHelper;
import com.trianguloy.forceurl.utilities.methods.AndroidUtils;
import com.trianguloy.forceurl.R;
import com.trianguloy.forceurl.services.UrlHelperService;

public class AccessibilityServiceHelper implements AHelper {
    @Override
    public void run(Context context, String url, String pckg, Apps app) {
        var instance = UrlHelperService.getInstance();
        if (instance == null) {
            // FIXME: is this enough if it is not online? maybe do the check in open module?
            AndroidUtils.safeToast(context, R.string.service_notConnected, Toast.LENGTH_LONG);
        } else {
            instance.openService(context, url, pckg, (node) -> ((AccessibilityFunction) app).putUrl(node, url, pckg));
        }
    }
}
