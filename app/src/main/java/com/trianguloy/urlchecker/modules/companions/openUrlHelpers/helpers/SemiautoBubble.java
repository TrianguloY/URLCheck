package com.trianguloy.urlchecker.modules.companions.openUrlHelpers.helpers;

import android.content.Context;

import com.trianguloy.urlchecker.modules.companions.openUrlHelpers.ClipboardBorrower;
import com.trianguloy.urlchecker.modules.companions.openUrlHelpers.HelperManager;
import com.trianguloy.urlchecker.modules.companions.openUrlHelpers.UrlHelper;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.wrappers.Bubble;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SemiautoBubble implements UrlHelper {
    @Override
    public boolean isCompatible() {
        return true;
    }

    // Uses clipboard

    @Override
    public HelperManager.Type getType() {
        return HelperManager.Type.bubble;
    }

    @Override
    public HelperManager.Autonomy getAutonomy() {
        return HelperManager.Autonomy.semiauto;
    }

    @Override
    public JavaUtils.BiConsumer<Context, String> getFunction() {
        return new Function();
    }

    // Only one bubble
    private static volatile Bubble bubble = null;
    private static volatile ScheduledThreadPoolExecutor executor = null;
    private static volatile ScheduledFuture<?> task = null;
    private static final Object lock = new Object();

    private class Function implements JavaUtils.BiConsumer<Context, String> {
        /**
         * Launches a Bubble so we can access the clipboard "from the background", if tapped it
         * restores the clipboard at that moment.
         * When restoring the clipboard the Bubble dissapears.
         */
        @Override
        public void accept(Context context, String url) {
            synchronized (lock) {
                // Is it possible this could run before relaseSafely(from background) finishes
                // releasing? I think not, but I'm not sure
                ClipboardBorrower.borrow(context, url);

                // The bubble will probably outlive any context
                Context finalContext = context.getApplicationContext();

                if (executor == null) {
                    executor = new ScheduledThreadPoolExecutor(1);
                }
                if (task != null) {
                    task.cancel(true);
                }
                if (bubble == null) {
                    // bubble wil be popped synchronized UNTIL this action is executed
                    bubble = new Bubble(finalContext, bubbleContext -> {
                        synchronized (lock) {
                            if (bubble != null) {
// FIXME: breakpoint here, run app, open in incognito, tap bubble, wait for "URLCheck isn't
//  responding", continue execution, can't access clipboard
//  just after getPrimaryClip() on getPrimaryClip() when URLCheck isn't responding
                                ClipboardBorrower.releaseSmart(bubbleContext);
                                task.cancel(true);
                                executor = null;
                                task = null;
                                bubble = null;
                            }
                        }
                    });
                }
                task = executor.schedule(() -> {
                    synchronized (lock) {
                        if (!Thread.interrupted()) {
                            bubble.pop();
                        }
                    }
                }, HelperManager.timerSeconds, TimeUnit.SECONDS);
            }
        }
    }
}
