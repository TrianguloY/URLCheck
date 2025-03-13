package com.trianguloy.urlchecker.activities;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.negate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.fragments.ResultCodeInjector;
import com.trianguloy.urlchecker.modules.companions.Hosts;
import com.trianguloy.urlchecker.modules.companions.VersionManager;
import com.trianguloy.urlchecker.modules.list.LogModule;
import com.trianguloy.urlchecker.modules.list.VirusTotalModule;
import com.trianguloy.urlchecker.modules.list.WebhookModule;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Animations;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Function;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;
import com.trianguloy.urlchecker.utilities.wrappers.ProgressDialog;
import com.trianguloy.urlchecker.utilities.wrappers.ZipReader;
import com.trianguloy.urlchecker.utilities.wrappers.ZipWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupActivity extends Activity {

    private final ResultCodeInjector resultCodeInjector = new ResultCodeInjector();

    private Switch chk_data;
    private Switch chk_data_prefs;
    private Switch chk_data_files;
    private Switch chk_secrets;
    private Switch chk_cache;
    private Switch chk_delete;
    private Switch chk_ignoreNewer;
    private Button btn_backup;
    private Button btn_restore;
    private Button btn_delete;
    private SharedPreferences prefs;

    /* ------------------- activity ------------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_backup);
        setTitle(R.string.btn_backupRestore);
        AndroidUtils.configureUp(this);

        Animations.enableAnimationsRecursively(this);

        // find view UI elements
        prefs = GenericPref.getPrefs(this);
        chk_data = findViewById(R.id.chk_data);
        chk_data_prefs = findViewById(R.id.chk_data_prefs);
        chk_data_files = findViewById(R.id.chk_data_files);
        chk_secrets = findViewById(R.id.chk_secrets);
        chk_cache = findViewById(R.id.chk_cache);
        chk_delete = findViewById(R.id.chk_delete);
        chk_ignoreNewer = findViewById(R.id.chk_ignoreNewer);
        btn_backup = findViewById(R.id.btn_backup);
        btn_restore = findViewById(R.id.btn_restore);
        btn_delete = findViewById(R.id.btn_delete);

        // if this app was reloaded, some settings may have changed, so reload previous one too
        if (AndroidSettings.wasReloaded(this)) AndroidSettings.markForReloading(this);

        // sync data switches
        chk_data.setOnCheckedChangeListener((v, checked) -> {
            chk_data_prefs.setChecked(checked);
            chk_data_files.setChecked(checked);
        });

        // sync button enabled status
        var chks = List.of(chk_cache, chk_secrets, chk_data_files, chk_data_prefs);
        for (var chk : chks)
            chk.setOnCheckedChangeListener((b, c) -> {
                var enabled = false;
                for (var chkk : chks) if (chkk.isChecked()) enabled = true;
                btn_backup.setEnabled(enabled);
                btn_restore.setEnabled(enabled);
                btn_delete.setEnabled(enabled);
            });

        // restore advanced status
        if (getIntent().getBooleanExtra(ADVANCED_EXTRA, false)) {
            showAdvanced();
        }

        // ask to restore if a file was opened
        var data = getIntent().getData();
        if (data != null) {
            getIntent().setData(null); // avoid restoring again after reload
            askRestore(data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_backup, menu);

        // restore advanced status
        if (getIntent().getBooleanExtra(ADVANCED_EXTRA, false)) {
            menu.findItem(R.id.menu_advanced).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home -> {
                // press the 'back' button in the action bar to go back
                onBackPressed();
                return true;
            }
            case R.id.menu_advanced -> {
                // show advanced
                item.setVisible(false);
                getIntent().putExtra(ADVANCED_EXTRA, true);
                showAdvanced();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!resultCodeInjector.onActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!resultCodeInjector.onRequestPermissionsResult(requestCode, permissions, grantResults))
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /* ------------------- backup ------------------- */

    public void backup(View ignored) {
        chooseFile(Intent.ACTION_CREATE_DOCUMENT, this::backup);
    }

    /**
     * Creates a backup and saves it to [uri]
     */
    private void backup(Uri uri) {
        ProgressDialog.run(this, R.string.btn_backup, progress -> {

            progress.setMax(7);
            progress.setMessage("Initializing backup");
            try (var zip = new ZipWriter(uri, this)) {
                zip.setComment(getString(R.string.app_name) + " (" + getPackageName() + ") " + getString(R.string.btn_backup));

                // version
                progress.setMessage("Adding version");
                progress.increaseProgress();
                zip.addStringFile(FILE_VERSION, BuildConfig.VERSION_NAME);

                // readme
                progress.setMessage("Adding readme");
                progress.increaseProgress();
                try (var readme = getAssets().open("backup_readme.txt")) {
                    zip.addStreamFile("readme.txt", readme);
                }

                // rest of preferences
                progress.setMessage("Adding preferences");
                progress.increaseProgress();
                if (chk_data_prefs.isChecked()) backupPreferencesMatching(FILE_PREFERENCES, negate(IS_PREF_SECRET), zip);

                // secret preferences
                progress.setMessage("Adding secrets");
                progress.increaseProgress();
                if (chk_secrets.isChecked()) backupPreferencesMatching(FILE_SECRETS, IS_PREF_SECRET, zip);

                // rest of files
                progress.setMessage("Adding files");
                progress.increaseProgress();
                if (chk_data_files.isChecked()) backupFilesMatching(FILES_FOLDER, negate(IS_FILE_CACHE), zip);

                // cache files
                progress.setMessage("Adding cache");
                progress.increaseProgress();
                if (chk_cache.isChecked()) backupFilesMatching(CACHE_FOLDER, IS_FILE_CACHE, zip);

                runOnUiThread(() -> Toast.makeText(this, R.string.bck_backupOk, Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, R.string.bck_backupError, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void backupPreferencesMatching(String fileName, Function<String, Boolean> predicate, ZipWriter zip) throws IOException, JSONException {
        var jsonPrefs = new JSONObject();
        for (var entry : prefs.getAll().entrySet()) {
            if (!predicate.apply(entry.getKey())) continue;
            jsonPrefs.put(entry.getKey(), new JSONObject()
                    .put(PREF_VALUE, entry.getValue())
                    .put(PREF_TYPE, entry.getValue().getClass().getSimpleName()));
        }
        zip.addStringFile(fileName, jsonPrefs.toString());
    }

    private void backupFilesMatching(String folder, Function<String, Boolean> predicate, ZipWriter zip) throws IOException {
        var empty = true;
        for (var file : fileList()) {
            if (!predicate.apply(file)) continue;
            try (var in = openFileInput(file)) {
                zip.addStreamFile(folder + file, in);
                empty = false;
            }
        }
        if (empty) zip.addStringFile(folder + EMPTY, "");
    }

    /* ------------------- restore ------------------- */

    public void restore(View ignored) {
        chooseFile(Intent.ACTION_OPEN_DOCUMENT, this::askRestore);
    }

    /**
     * Asks to restore a backup from [uri]
     */
    private void askRestore(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bck_restoreTitle)
                .setMessage(R.string.bck_restoreMessage)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.bck_restoreConfirm, (d, w) -> restore(uri))
                .show();
    }

    /**
     * Restores a backup from [uri]
     */
    private void restore(Uri uri) {
        ProgressDialog.run(this, R.string.btn_restore, progress -> {
            progress.setMax(5);
            progress.setMessage("Loading backup");
            try (var zip = new ZipReader(uri, this)) {

                // check version
                if (!chk_ignoreNewer.isChecked() && VersionManager.isVersionNewer(zip.getFileString(FILE_VERSION))) {
                    runOnUiThread(() -> Toast.makeText(this, R.string.bck_newer, Toast.LENGTH_LONG).show());
                    return;
                }

                // rest of preferences
                progress.setMessage("Restoring preferences");
                progress.increaseProgress();
                if (chk_data_prefs.isChecked()) restorePreferencesMatching(FILE_PREFERENCES, negate(IS_PREF_SECRET), zip);

                // secret preferences
                progress.setMessage("Restoring secrets");
                progress.increaseProgress();
                if (chk_secrets.isChecked()) restorePreferencesMatching(FILE_SECRETS, IS_PREF_SECRET, zip);

                // rest of files
                progress.setMessage("Restoring files");
                progress.increaseProgress();
                if (chk_data_files.isChecked()) restoreFilesMatching(FILES_FOLDER, negate(IS_FILE_CACHE), zip);

                // cache files
                progress.setMessage("Restoring cache");
                progress.increaseProgress();
                if (chk_cache.isChecked()) restoreFilesMatching(CACHE_FOLDER, IS_FILE_CACHE, zip);

                runOnUiThread(() -> Toast.makeText(this, R.string.bck_restoreOk, Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, R.string.bck_restoreError, Toast.LENGTH_LONG).show());
            }

            runOnUiThread(() -> AndroidSettings.reload(this));
        });
    }

    /** Restore preferences from [fileName] in the [zip] that matches the [predicate]. */
    private void restorePreferencesMatching(String fileName, Function<String, Boolean> predicate, ZipReader zip) throws IOException, JSONException {
        var preferences = zip.getFileString(fileName);
        if (preferences == null) return;

        var jsonPrefs = new JSONObject(preferences);
        var editor = prefs.edit();

        // remove
        if (chk_delete.isChecked()) {
            for (var key : prefs.getAll().keySet()) {
                if (predicate.apply(key)) editor.remove(key);
            }
        }

        // add
        for (var key : JavaUtils.toList(jsonPrefs.keys())) {
            try {
                var ent = jsonPrefs.getJSONObject(key);
                var type = ent.getString(PREF_TYPE);
                switch (type) {
                    case "String" -> editor.putString(key, ent.getString(PREF_VALUE));
                    case "Integer" -> editor.putInt(key, ent.getInt(PREF_VALUE));
                    case "Long" -> editor.putLong(key, ent.getLong(PREF_VALUE));
                    case "Boolean" -> editor.putBoolean(key, ent.getBoolean(PREF_VALUE));
                    default -> AndroidUtils.assertError("Unknown type: " + type);
                }
            } catch (JSONException e) {
                AndroidUtils.assertError("Error when restoring key: " + key, e);
            }
        }

        editor.apply();
    }

    private void restoreFilesMatching(String folder, Function<String, Boolean> predicate, ZipReader zip) throws IOException {
        var fileNames = zip.fileNames(folder);
        if (fileNames.isEmpty()) return;

        // delete
        if (chk_delete.isChecked()) {
            for (var file : fileList()) {
                if (predicate.apply(file)) deleteFile(file);
            }
        }

        // create
        for (var fileName : fileNames) {
            if ((folder + EMPTY).equals(fileName)) continue; // ignore marker
            try (var out = openFileOutput(fileName.substring(folder.length()), MODE_PRIVATE)) {
                zip.getFileStream(fileName, out);
            }
        }
    }

    /* ------------------- delete ------------------- */

    public void delete(View ignored) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bck_deleteTitle)
                .setMessage(R.string.bck_deleteMessage)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.bck_deleteConfirm, (d, w) -> delete())
                .show();
    }

    private void delete() {
        ProgressDialog.run(this, R.string.btn_delete, progress -> {
            progress.setMax(4);
            try {

                // rest of preferences
                progress.setMessage("Deleting preferences");
                if (chk_data_prefs.isChecked()) deletePreferencesMatching(negate(IS_PREF_SECRET));

                // secret preferences
                progress.setMessage("Deleting secrets");
                progress.increaseProgress();
                if (chk_secrets.isChecked()) deletePreferencesMatching(IS_PREF_SECRET);

                // rest of files
                progress.setMessage("Deleting files");
                progress.increaseProgress();
                if (chk_data_files.isChecked()) deleteFilesMatching(negate(IS_FILE_CACHE));

                // cache files
                progress.setMessage("Deleting cache");
                progress.increaseProgress();
                if (chk_cache.isChecked()) deleteFilesMatching(IS_FILE_CACHE);

                runOnUiThread(() -> Toast.makeText(this, R.string.bck_deleteOk, Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, R.string.bck_deleteError, Toast.LENGTH_SHORT).show());
            }

            runOnUiThread(() -> AndroidSettings.reload(this));
        });
    }

    private void deletePreferencesMatching(Function<String, Boolean> predicate) {
        var editor = prefs.edit();
        for (var key : prefs.getAll().keySet()) {
            if (predicate.apply(key)) editor.remove(key);
        }
        editor.apply();
    }

    private void deleteFilesMatching(Function<String, Boolean> predicate) {
        for (var file : fileList()) {
            if (predicate.apply(file)) deleteFile(file);
        }
    }

    /* ------------------- common ------------------- */

    private static final String FILE_VERSION = "version";
    private static final String FILE_PREFERENCES = "preferences";
    private static final String FILE_SECRETS = "secrets";
    private static final String FILES_FOLDER = "files/";
    private static final String CACHE_FOLDER = "cache/";
    private static final String PREF_VALUE = "value";
    private static final String PREF_TYPE = "type";
    private static final String EMPTY = ".empty";
    private static final String ADVANCED_EXTRA = "advanced";

    private static final Function<String, Boolean> IS_PREF_SECRET = List.of(VirusTotalModule.PREF, LogModule.PREF, WebhookModule.URL_PREF)::contains;
    private static final Function<String, Boolean> IS_FILE_CACHE = s -> s.startsWith(Hosts.PREFIX);

    private void chooseFile(String action, JavaUtils.Consumer<Uri> listener) {
        // choose backup file
        var intent = new Intent(action);
        intent.putExtra(Intent.EXTRA_TITLE, getInitialFile());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, getInitialFolder());

        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        PackageUtils.startActivityForResult(
                Intent.createChooser(intent, getString(R.string.bck_chooseFile)),
                resultCodeInjector.registerActivityResult((resultCode, data) -> {
                    // file selected?
                    if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) listener.accept(data.getData());
                    else Toast.makeText(this, R.string.canceled, Toast.LENGTH_SHORT).show();
                }),
                R.string.toast_noApp,
                this);
    }

    private File getInitialFolder() {
        var folder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? new File(getSystemService(StorageManager.class).getPrimaryStorageVolume().getDirectory(), Environment.DIRECTORY_DOWNLOADS)
                : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        folder.mkdirs();
        return folder;
    }

    private String getInitialFile() {
        return "URLCheck_" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date()) + ".ucbckp";
    }

    private void showAdvanced() {
        chk_data.setThumbResource(android.R.color.transparent);
        chk_data_prefs.setVisibility(View.VISIBLE);
        chk_data_files.setVisibility(View.VISIBLE);
        chk_delete.setVisibility(View.VISIBLE);
        chk_ignoreNewer.setVisibility(View.VISIBLE);
        btn_delete.setVisibility(View.VISIBLE);
    }
}
