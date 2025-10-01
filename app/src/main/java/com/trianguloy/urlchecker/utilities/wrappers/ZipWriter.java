package com.trianguloy.urlchecker.utilities.wrappers;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.net.Uri;

import com.trianguloy.urlchecker.utilities.methods.StreamUtils;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Utility class to manage creating zips */
public class ZipWriter implements Closeable {

    private final ZipOutputStream zip;

    /** Creates a zip from a uri */
    public ZipWriter(Uri uri, Context cntx) throws FileNotFoundException {
        zip = new ZipOutputStream(cntx.getContentResolver().openOutputStream(uri));
    }

    /** Sets the zip comment */
    public void setComment(String comment) {
        zip.setComment(comment);
    }

    /** Adds a string as a file */
    public void addStringFile(String name, String content) throws IOException {
        var entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(content.getBytes(UTF_8));
    }

    /** Adds the content of a stream as a file. The stream is NOT closed */
    public void addStreamFile(String name, InputStream stream) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        StreamUtils.inputStream2OutputStream(stream, zip);
    }

    @Override
    public void close() throws IOException {
        if (zip != null) zip.close();
    }
}
