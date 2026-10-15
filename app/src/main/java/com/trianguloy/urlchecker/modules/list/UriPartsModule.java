package com.trianguloy.urlchecker.modules.list;

import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Inflater;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Supplier;
import com.trianguloy.urlchecker.utilities.methods.UrlUtils;

import java.util.ArrayList;
import java.util.List;

/** This module shows all parts of the url decoded */
public class UriPartsModule extends AModuleData {

    @Override
    public String getId() {
        return "uriparts";
    }

    @Override
    public int getName() {
        return R.string.mParts_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new UriPartsDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new DescriptionConfig(R.string.mParts_desc);
    }
}

class UriPartsDialog extends AModuleDialog {

    private LinearLayout box;
    private final List<String> expandedGroups = new ArrayList<>();

    public UriPartsDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_parts;
    }

    @Override
    public void onInitialize(View views) {
        box = views.findViewById(R.id.box);
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        // clear
        box.removeAllViews();

        // parse
        var uri = Uri.parse(urlData.url);
        var urlQuerySanitizer = new UrlQuerySanitizer();
        // the default parseUrl doesn't remove the fragment
        if (uri.getEncodedQuery() != null) {
            urlQuerySanitizer.setAllowUnregisteredParamaters(true);
            urlQuerySanitizer.setUnregisteredParameterValueSanitizer(v -> v);
            urlQuerySanitizer.parseQuery(uri.getEncodedQuery()
                    // this will fix issues with the parser decoding twice
                    .replace("%", "%25"));
        }

        // domain elements
        if (uri.getAuthority() != null || uri.getScheme() != null) {
            var domain = addGroup(getActivity().getString(R.string.mParts_domain), -1, null);
            addPart(getActivity().getString(R.string.mParts_scheme), uri.getScheme(), domain, null);
            addPart(getActivity().getString(R.string.mParts_userInfo), uri.getUserInfo(), domain, null);
            addPart(getActivity().getString(R.string.mParts_host), uri.getHost(), domain, null);
            addPart(getActivity().getString(R.string.mParts_port), uri.getPort() != -1 ? String.valueOf(uri.getPort()) : null, domain, null);
        }

        // paths
        var pathSegments = uri.getPathSegments();
        if (!pathSegments.isEmpty()) {
            var paths = addGroup(getActivity().getString(R.string.mParts_paths), pathSegments.size(), uri.buildUpon().path(null));
            for (var i = 0; i < pathSegments.size(); i++) {
                int removeI = i;
                // append the path
                addPart("/", pathSegments.get(i), paths, () -> {
                    // generate the same url without this path
                    var builder = uri.buildUpon();
                    builder.path(null);
                    for (int newI = 0; newI < pathSegments.size(); newI++) {
                        if (newI != removeI) builder.appendPath(pathSegments.get(newI));
                    }
                    return builder.build().toString();
                });
            }
        }

        // query parameters
        var parameters = urlQuerySanitizer.getParameterList();
        if (!parameters.isEmpty()) {
            var queries = addGroup(getActivity().getString(R.string.mParts_parameters), parameters.size(), uri.buildUpon().query(null));
            for (var i = 0; i < parameters.size(); i++) {
                int removeI = i;
                // append the parameter
                addPart(UrlUtils.decode(parameters.get(i).mParameter), UrlUtils.decode(parameters.get(i).mValue), queries, () -> {
                    // generate same url but without this parameter
                    var builder = uri.buildUpon();
                    builder.query(null);
                    for (var newI = 0; newI < parameters.size(); newI++) {
                        if (newI != removeI) builder.appendQueryParameter(UrlUtils.decode(parameters.get(newI).mParameter), UrlUtils.decode(parameters.get(newI).mValue));
                    }
                    return builder.build().toString();
                });
            }
        }

        // fragment
        if (uri.getFragment() != null) {
            var fragment = addGroup(getActivity().getString(R.string.mParts_fragment), -1, uri.buildUpon().fragment(null));
            addPart("#", uri.getFragment(), fragment, null);
        }

        setVisibility(box.getChildCount() > 0);
    }

    /** Adds a collapsible group */
    private LinearLayout addGroup(String name, int size, Uri.Builder onDelete) {
        var title = Inflater.inflate(R.layout.uri_part, box);
        title.findViewById(R.id.key).setVisibility(View.GONE);

        var name_view = title.<TextView>findViewById(R.id.value);
        name_view.setText(name + (size <= -1 ? "" : " (" + size + ")"));
        AndroidUtils.setAsClickable(name_view);

        var delete_view = title.<Button>findViewById(R.id.delete);
        if (onDelete == null) delete_view.setVisibility(View.GONE);
        else delete_view.setOnClickListener(v -> setUrl(onDelete.build().toString()));

        var group = Inflater.<LinearLayout>inflate(R.layout.dialog_parts, box);
        group.setVisibility(expandedGroups.contains(name) ? View.VISIBLE : View.GONE);
        AndroidUtils.toggleableListener(
                title,
                v -> JavaUtils.toggleContains(expandedGroups, name),
                v -> {
                    group.setVisibility(expandedGroups.contains(name) ? View.VISIBLE : View.GONE);
                    name_view.setCompoundDrawablesRelativeWithIntrinsicBounds(expandedGroups.contains(name) ? R.drawable.arrow_down : R.drawable.arrow_right, 0, 0, 0);
                }
        );

        return group;
    }

    /** Adds a part */
    private void addPart(String name, String value, LinearLayout container, Supplier<String> onDelete) {
        if (value == null) return;
        // create row
        var part = Inflater.inflate(R.layout.uri_part, container);

        // configure key
        var key_view = part.<TextView>findViewById(R.id.key);
        key_view.setText(name.isEmpty() ? getActivity().getString(R.string.mParts_empty) : name);
        if (!name.isEmpty()) key_view.setOnLongClickListener(longTapToCopy);

        // configure value
        var value_view = part.<TextView>findViewById(R.id.value);
        value_view.setText(value);
        AndroidUtils.setAsClickable(value_view);
        value_view.setOnClickListener(v -> setUrl(value));
        value_view.setOnLongClickListener(longTapToCopy);

        // configure delete
        var delete_view = part.<Button>findViewById(R.id.delete);
        if (onDelete != null) {
            delete_view.setOnClickListener(v -> setUrl(onDelete.get()));
        } else {
            delete_view.setVisibility(View.GONE);
        }
    }

    /** OnLongClickListener to copy a part (textview text) to the clipboard */
    private final View.OnLongClickListener longTapToCopy = v -> {
        AndroidUtils.copyToClipboard(getActivity(), R.string.mParts_copy, ((TextView) v).getText().toString());
        return true;
    };

}

/*
 * Manual tests:
 * - Should show the expected output before and after removing the hide parameter.
 * - Should keep the rest of the url intact when removing the hide parameter
 *
 *
 * https://example.com?prefill_%25E6%259D%25A5%25E6%25BA%2590=%E5%B8%AE%E5%8A%A9%E4%B8%8E%E5%8F%8D%E9%A6%88&hide=true
 * ->
 * prefill_%E6%9D%A5%E6%BA%90 -> 帮助与反馈
 *
 *
 * https://example.com/?q=%F0%9F%91%8D&hide=true
 * ->
 * q -> 👍
 *
 *
 * https://example.com/?q=a%26b%3Dc&hide=true
 * q -> a&b=c
 */