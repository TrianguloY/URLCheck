package com.trianguloy.urlchecker.utilities.wrappers;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils.Consumer;
import com.trianguloy.urlchecker.utilities.methods.StreamUtils;

import java.io.FileOutputStream;
import java.io.IOException;

/** Represents an internal file, can be modified */
public class InternalFile {
    private final String fileName;
    private final Context cntx;

    public InternalFile(String fileName, Context cntx) {
        this.fileName = fileName;
        this.cntx = cntx;
    }

    /** Returns the content, null if the file doesn't exists or can't be read */
    public String get() {
        try {
            return StreamUtils.inputStream2String(cntx.openFileInput(fileName));
        } catch (IOException ignored) {
            return null;
        }
    }

    /** Streams the lines */
    public void stream(Consumer<String> function) {
        try {
            StreamUtils.consumeLines(cntx.openFileInput(fileName), function);
        } catch (IOException ignored) {
            // do nothing
        }
    }

    /** Sets a new file content */
    public boolean set(String content) {

        // the same, already saved
        if (content.equals(get())) {
            return true;
        }

        // store
        try (FileOutputStream fos = cntx.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes(UTF_8));
            return true;
        } catch (IOException e) {
            AndroidUtils.assertError("Unable to store file content", e);
            return false;
        }
    }

    /** Deletes the file */
    public void delete() {
        cntx.deleteFile(fileName);
    }

}
