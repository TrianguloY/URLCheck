package com.trianguloy.urlchecker.modules.list;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.ITALIC;
import static android.graphics.Typeface.MONOSPACE;
import static android.text.InputType.TYPE_TEXT_VARIATION_URI;
import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

/** This module shows the current url and allows manual editing */
public class TextInputModule extends AModuleData {

    public static GenericPref.BoolPref MONOSPACE_PREF(Context cntx) {
        return new GenericPref.BoolPref("text_monospace", true, cntx);
    }

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
        return new TextInputConfig(cntx);
    }
}

class TextInputDialog extends AModuleDialog {

    private TextView txt_url;
    private GenericPref.BoolPref monospacePref;

    public TextInputDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_text;
    }

    @Override
    public void onInitialize(View views) {
        monospacePref = TextInputModule.MONOSPACE_PREF(getActivity());

        txt_url = views.findViewById(R.id.url);
        if (monospacePref.get()) {
            txt_url.setTypeface(MONOSPACE);
        }

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
        editText.setImeOptions(IME_ACTION_DONE);
        editText.setInputType(TYPE_TEXT_VARIATION_URI);
        editText.setSingleLine(false);
        if (monospacePref.get()) editText.setTypeface(MONOSPACE);
        if (position >= 0) editText.setSelection(position);
        else editText.setSelection(0, editText.length());
        editText.requestFocus();

        // init dialog
        DialogInterface.OnClickListener accept = (d, w) -> setUrl(new UrlData(editText.getText().toString()).disableUpdates());
        var dialog = new AlertDialog.Builder(getActivity())
                .setView(editText)
                .setPositiveButton(android.R.string.ok, accept)
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(true)
                .create();

        // resize with keyboard
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(SOFT_INPUT_STATE_VISIBLE | SOFT_INPUT_ADJUST_RESIZE);

        // accept on done/enter
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == IME_ACTION_DONE || (event.getAction() == ACTION_DOWN && event.getKeyCode() == KEYCODE_ENTER)) {
                accept.onClick(null, 0);
                dialog.dismiss();
                return true;
            }
            return false;
        });

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
                if (end == -1) end = rawUri.length();

                var userinfo = rawUri.indexOf("@", start);
                if (userinfo != -1 && userinfo < end) start = userinfo + 1;

                var port = rawUri.lastIndexOf(":", end);
                if (port != -1 && port > start) end = port;

                str.setSpan(new StyleSpan(BOLD), start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception e) {
            AndroidUtils.assertError("Unable to set host as bold", e);
        }

        // italic query+fragment
        try {
            var start = rawUri.indexOf("?");
            if (start == -1) start = rawUri.indexOf("#");
            if (start != -1)
                str.setSpan(new StyleSpan(ITALIC), start, rawUri.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        } catch (Exception e) {
            AndroidUtils.assertError("Unable to set query+fragment as italic", e);
        }

        return str;
    }
}

class TextInputConfig extends AModuleConfig {

    public TextInputConfig(ModulesActivity cntx) {
        super(cntx);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_text;
    }

    @Override
    public void onInitialize(View views) {
        TextInputModule.MONOSPACE_PREF(getActivity()).attachToSwitch(views.findViewById(R.id.chk_monospace));
    }
}