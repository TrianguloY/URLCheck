package com.trianguloy.urlchecker.modules.list;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.AutomationRules;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.wrappers.DefaultTextWatcher;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * This module sends the current url to a custom webhook
 * Idea and base implementation by anoop-b
 */
public class WebhookModule extends AModuleData {

    public static final String URL_PREF = "webhook_url";

    public static GenericPref.Str WEBHOOK_URL_PREF(Context cntx) {
        return new GenericPref.Str(URL_PREF, "", cntx);
    }

    public static GenericPref.Str WEBHOOK_BODY_PREF(Context cntx) {
        return new GenericPref.Str("webhook_body", WebhookConfig.DEFAULT, cntx);
    }

    @Override
    public String getId() {
        return "webhook";
    }

    @Override
    public int getName() {
        return R.string.mWebhook_name;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new WebhookDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new WebhookConfig(cntx);
    }

    @Override
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) WebhookDialog.AUTOMATIONS;
    }
}

class WebhookDialog extends AModuleDialog {

    private static final Executor executor = Executors.newSingleThreadExecutor();

    private final GenericPref.Str webhookUrl;
    private final GenericPref.Str webhookBody;
    private TextView statusText;
    private Button statusButton;

    static final List<AutomationRules.Automation<WebhookDialog>> AUTOMATIONS = List.of(
            new AutomationRules.Automation<>(
                    "webhook",
                    R.string.mWebhook_auto_send,
                    WebhookDialog::sendToWebhook
            )
    );

    public WebhookDialog(MainDialog dialog) {
        super(dialog);
        webhookUrl = WebhookModule.WEBHOOK_URL_PREF(dialog);
        webhookBody = WebhookModule.WEBHOOK_BODY_PREF(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.button_text;
    }

    @Override
    public void onInitialize(View views) {
        statusText = views.findViewById(R.id.text);
        statusButton = views.findViewById(R.id.button);

        statusButton.setText(R.string.mWebhook_send);
        statusButton.setOnClickListener(v -> sendToWebhook());
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        statusText.setText("");
        AndroidUtils.clearRoundedColor(statusText);
    }

    private void sendToWebhook() {
        statusText.setText(R.string.mWebhook_sending);
        statusButton.setEnabled(false);

        executor.execute(() -> {
            var sent = send(webhookUrl.get(), getUrl(), webhookBody.get());
            getActivity().runOnUiThread(() -> {
                statusText.setText(sent ? R.string.mWebhook_success : R.string.mWebhook_error);
                if (!sent) {
                    AndroidUtils.setRoundedColor(R.color.bad, statusText);
                }
                statusButton.setEnabled(true);
            });
        });
    }

    /** Performs the send action */
    static boolean send(String webhook, String url, String body) {
        try {
            var json = body
                    .replace("$URL$", url)
                    .replace("$TIMESTAMP$", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()));

            var responseCode = HttpUtils.performPOSTJSON(webhook, json);
            return responseCode >= 200 && responseCode < 300;
        } catch (IOException e) {
            AndroidUtils.assertError("Failed to send to webhook", e);
            return false;
        }

    }
}

class WebhookConfig extends AModuleConfig {

    public static final String DEFAULT = "{\"url\":\"$URL$\",\"timestamp\":\"$TIMESTAMP$\"}";

    private static final List<Pair<String, String>> TEMPLATES = List.of(
            Pair.create("custom", DEFAULT),
            Pair.create("Discord", "{\"content\":\"$URL$ @ $TIMESTAMP$\"}"),
            Pair.create("Slack", "{\"text\":\"$URL$ @ $TIMESTAMP$\"}"),
            Pair.create("Teams", "{\"text\":\"$URL$ @ $TIMESTAMP$\"}")
    );

    private final GenericPref.Str webhookUrl;
    private final GenericPref.Str webhookBody;

    public WebhookConfig(ModulesActivity activity) {
        super(activity);
        webhookUrl = WebhookModule.WEBHOOK_URL_PREF(activity);
        webhookBody = WebhookModule.WEBHOOK_BODY_PREF(activity);
    }

    @Override
    public int cannotEnableErrorId() {
        return webhookUrl.get().isEmpty() || webhookBody.get().isEmpty() ? R.string.mWebhook_missing_config : -1;
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_webhook;
    }

    @Override
    public void onInitialize(View views) {
        var url = views.<EditText>findViewById(R.id.webhook_url);
        var body = views.<EditText>findViewById(R.id.webhook_body);
        var test = views.findViewById(R.id.webhook_test);

        // configs
        webhookUrl.attachToEditText(url);
        webhookBody.attachToEditText(body);

        // check disable
        var nonEmpty = new DefaultTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    disable();
                    test.setEnabled(false);
                } else {
                    test.setEnabled(true);
                }
            }
        };
        url.addTextChangedListener(nonEmpty);
        body.addTextChangedListener(nonEmpty);

        test.setEnabled(cannotEnableErrorId() == -1);

        // click template
        views.findViewById(R.id.webhook_templates).setOnClickListener(v ->
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.mWebhook_templates)
                        .setItems(JavaUtils.mapEach(TEMPLATES, e -> e.first).toArray(new String[0]), (dialog, which) ->
                                body.setText(TEMPLATES.get(which).second))
                        .show());

        // click test
        test.setOnClickListener(v -> {
            test.setEnabled(false);
            new Thread(() -> {
                var ok = WebhookDialog.send(webhookUrl.get(), webhookUrl.get(), webhookBody.get());
                getActivity().runOnUiThread(() -> {
                    test.setEnabled(true);
                    Toast.makeText(v.getContext(), ok ? R.string.mWebhook_success : R.string.mWebhook_error, Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
    }
} 