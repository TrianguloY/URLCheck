package com.trianguloy.urlchecker.modules.list;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.ITALIC;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import android.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

/** This module shows the current url and allows manual editing */
public class TextInputModule extends AModuleData {

    @Override
    public String getId() {
        return "text";
    }

    @Override
    public int getName() {
        return R.string.mInput_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new TextInputDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new DescriptionConfig(R.string.mInput_desc);
    }
}

class TextInputDialog extends AModuleDialog {

    private TextView txt_url;

    public TextInputDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_text;
    }

    @Override
    public void onInitialize(View views) {
        txt_url = views.findViewById(R.id.url);

        // Show fullscreen editor with the cursor in the clicked position when clicked
        AndroidUtils.setOnClickWithPositionListener(txt_url, cursor -> showEditor(txt_url.getOffsetForPosition(cursor.first, cursor.second)));

        // Show fullscreen editor with everything selected when long clicked
        txt_url.setOnLongClickListener(v -> {
            showEditor(-1);
            return true;
        });
    }

    /**
     * Show a popup editor for the url text.
     * The cursor is placed at [position], or everything is selected if position is negative
     */
    public void showEditor(int position) {
        // init view
        var editText = new EditText(getActivity());
        editText.setText(getUrl());
        if (position >= 0) editText.setSelection(position);
        else editText.setSelection(0, editText.length());
        editText.requestFocus();

        // init dialog
        var dialog = new AlertDialog.Builder(getActivity())
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (d, w) -> setUrl(new UrlData(editText.getText().toString()).disableUpdates()))
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setSoftInputMode(SOFT_INPUT_STATE_VISIBLE | SOFT_INPUT_ADJUST_RESIZE);

        // show
        dialog.show();
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        txt_url.setText(getSpannableUriText(urlData.url));
    }

    private CharSequence getSpannableUriText(String rawUri) {
        var str = new SpannableStringBuilder(rawUri);

        // bold host
        try {
            var start = rawUri.indexOf("://");
            if (start != -1) {
                start += 3;
                var end = rawUri.indexOf("/", start);

                var userinfo = rawUri.indexOf("@", start);
                if (userinfo != -1 && userinfo < end) start = userinfo + 1;

                var port = rawUri.lastIndexOf(":", end);
                if (port != -1 && port > start) end = port;

                str.setSpan(new StyleSpan(BOLD), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }

        // italic query+fragment
        try {
            var start = rawUri.indexOf("?");
            if (start == -1) start = rawUri.indexOf("#");
            if (start != -1) str.setSpan(new StyleSpan(ITALIC), start, rawUri.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }

        return str;
    }
}
