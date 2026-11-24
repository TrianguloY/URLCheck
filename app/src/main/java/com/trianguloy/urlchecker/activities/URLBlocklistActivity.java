package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.URLBlocklist;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;

import java.util.List;

/** Native editor for incoming-URL domain rules. */
public class URLBlocklistActivity extends Activity {
    private URLBlocklist blocklist;
    private EditText input;
    private List<String> domains;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_url_blocklist);
        setTitle(R.string.a_urlBlocklist);
        AndroidUtils.configureUp(this);

        blocklist = new URLBlocklist(this);
        input = findViewById(R.id.blocklist_input);
        findViewById(R.id.blocklist_add).setOnClickListener(v -> addDomain());
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void addDomain() {
        var host = URLBlocklist.normalize(input.getText().toString());
        if (host == null) {
            input.setError(getString(R.string.blocklist_invalid));
            return;
        }
        domains = blocklist.getDomains();
        if (domains.contains(host)) {
            input.setError(getString(R.string.blocklist_duplicate));
            return;
        }
        domains.add(host);
        blocklist.save(domains);
        input.setText("");
        input.setError(null);
        refresh();
    }

    private void refresh() {
        domains = blocklist.getDomains();
        findViewById(R.id.blocklist_corrupted).setVisibility(blocklist.isCorrupted() ? View.VISIBLE : View.GONE);
        findViewById(R.id.blocklist_empty).setVisibility(domains.isEmpty() ? View.VISIBLE : View.GONE);
        var list = this.<LinearLayout>findViewById(R.id.blocklist_domains);
        list.removeAllViews();
        for (var domain : domains) {
            var row = getLayoutInflater().inflate(R.layout.item_url_blocklist, list, false);
            ((TextView) row.findViewById(R.id.blocklist_domain)).setText(domain);
            var remove = (Button) row.findViewById(R.id.blocklist_remove);
            remove.setContentDescription(getString(R.string.blocklist_removeDomain, domain));
            remove.setOnClickListener(v -> {
                var current = blocklist.getDomains();
                current.remove(domain);
                blocklist.save(current);
                refresh();
            });
            list.addView(row);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
