package com.trianguloy.urlchecker.utilities.wrappers;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.Switch;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;

import java.util.regex.Matcher;

/**
 * On Android 10 and under, optional groups may yield a "null" in the replacement output instead of an empty string.
 * Therefore, we just copy the implementation from a newer version of Android
 * https://github.com/TrianguloY/URLCheck/issues/237
 */
public class RegexFix {

    /** Android 11 and up have already the fix, disable in those cases */
    public static final boolean IS_ANDROID_FIXED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;

    private final BoolPref pttrn_regexfix;

    public RegexFix(Context cntx) {
        // fix enabled by default
        pttrn_regexfix = new BoolPref("pttrn_regexfix", true, cntx);
    }

    /** Attach the setting to a given switch view (or disabled if not needed) */
    public static void attachSetting(Switch view) {
        if (IS_ANDROID_FIXED) {
            // hide, already native
            view.setVisibility(View.GONE);
        } else {
            // show, required
            new RegexFix(view.getContext()).pttrn_regexfix.attachToSwitch(view);
            view.setVisibility(View.VISIBLE);
        }
    }

    /** Use this instead of: matcher = pattern.matcher(text); result = matcher.replaceAll(replacement) */
    public String replaceAll(String text, Matcher matcher, String replacement) {
        if (IS_ANDROID_FIXED || !pttrn_regexfix.get()) {
            // no fix required or explicitly disabled, just use native function
            return matcher.replaceAll(replacement);
        }

        // Copied from https://android.googlesource.com/platform/libcore/+/refs/heads/android13-release/ojluni/src/main/java/java/util/regex/Matcher.java#837
        boolean result = matcher.reset().find();
        if (result) {
            StringBuffer sb = new StringBuffer();
            int appendPos = 0;
            do {
                appendPos = appendReplacement(text, matcher, sb, replacement, appendPos);
                result = matcher.find();
            } while (result);
            appendTail(text, sb, appendPos);
            return sb.toString();
        }
        return text;
    }

    private static int appendReplacement(String text, Matcher matcher, StringBuffer sb, String replacement, int appendPos) {
        // Copied from https://android.googlesource.com/platform/libcore/+/refs/heads/android13-release/ojluni/src/main/java/java/util/regex/Matcher.java#714
        sb.append(text.substring(appendPos, matcher.start()));
        appendEvaluated(matcher, sb, replacement);
        return matcher.end();
    }

    /** @noinspection UnnecessaryContinue, DataFlowIssue , ConstantValue */
    private static void appendEvaluated(Matcher matcher, StringBuffer buffer, String s) {
        // Copied from https://android.googlesource.com/platform/libcore/+/refs/heads/android13-release/ojluni/src/main/java/java/util/regex/Matcher.java#731
        boolean escape = false;
        boolean dollar = false;
        boolean escapeNamedGroup = false;
        int escapeNamedGroupStart = -1;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && !escape) {
                escape = true;
            } else if (c == '$' && !escape) {
                dollar = true;
            } else if (c >= '0' && c <= '9' && dollar && !escapeNamedGroup) {
                String groupValue = matcher.group(c - '0');
                if (groupValue != null) {
                    buffer.append(groupValue);
                }
                dollar = false;
            } else if (c == '{' && dollar) {
                escapeNamedGroup = true;
                escapeNamedGroupStart = i;
            } else if (c == '}' && dollar && escapeNamedGroup) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    throw new IllegalArgumentException("your android version does not support named-capturing groups");
                }
                String groupValue = matcher.group(s.substring(escapeNamedGroupStart + 1, i));
                if (groupValue != null) {
                    buffer.append(groupValue);
                }
                dollar = false;
                escapeNamedGroup = false;
            } else if (c != '}' && dollar && escapeNamedGroup) {
                continue;
            } else {
                buffer.append(c);
                dollar = false;
                escape = false;
                escapeNamedGroup = false;
            }
        }

        if (escape) {
            throw new IllegalArgumentException("character to be escaped is missing");
        }

        if (dollar) {
            throw new IllegalArgumentException("Illegal group reference: group index is missing");
        }

        if (escapeNamedGroup) {
            throw new IllegalArgumentException("Missing ending brace '}' from replacement string");
        }
    }

    /** @noinspection UnusedReturnValue */
    private static StringBuffer appendTail(String text, StringBuffer sb, int appendPos) {
        // Copied from https://android.googlesource.com/platform/libcore/+/refs/heads/android13-release/ojluni/src/main/java/java/util/regex/Matcher.java#796
        int to = text.length();
        if (appendPos < to) {
            sb.append(text.substring(appendPos, to));
        }
        return sb;
    }
}
