package com.trianguloy.forceurl.services;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.trianguloy.forceurl.utilities.methods.AndroidUtils;
import com.trianguloy.forceurl.R;
import com.trianguloy.forceurl.utilities.methods.JavaUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UrlHelperService extends AccessibilityService {
    private boolean open = true;
    private String pckg = null;
    private String url = null;
    private JavaUtils.Function<AccessibilityNodeInfo, Boolean>  putUrl;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> task = null;
    private static UrlHelperService instance = null;

    public static UrlHelperService getInstance() {
        return instance;
    }

    public static boolean isConnected(){
        return getInstance() != null;
    }

    public static boolean isEnabled(Context context){
        return AndroidUtils.isAccessibilityServiceEnabled(context, UrlHelperService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ACCESSIBILITY", "Create");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ACCESSIBILITY", "Destroy");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d("ACCESSIBILITY", "Connect");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("ACCESSIBILITY", "Unbind");
        instance = null;
        return super.onUnbind(intent);
    }

    public synchronized void openService(Context context, String url, String pckg,
                                         JavaUtils.Function<AccessibilityNodeInfo, Boolean> putUrl) { // FIXME: just a normal function?
        open = true;

        this.pckg = pckg;
        this.url = url;
        this.putUrl = putUrl;

        // If it doesn't close automatically, we close it
        if (task != null) {
            task.cancel(true);
        }
        task = executor.schedule(() -> {
            synchronized (this) {
                closeService();
                AndroidUtils.safeToast(context, R.string.service_closed, Toast.LENGTH_SHORT);
                task = null;
            }
        }, 10, TimeUnit.SECONDS);
    }

    public synchronized void closeService() {
        open = false;

        // My hands burn! I don't want to keep user data
        this.pckg = null;
        this.url = null;
        this.putUrl = null;

        // We closed the service, no longer need the task
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // TODO: better accessibility service config, flags and event types
        synchronized (this) {
            if (open && event.getPackageName().toString().equals(pckg)) {
                // Get root node of the active window
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    if (putUrl.apply(rootNode)) {
                        // When success, close the service
                        closeService();
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d("ACCESSIBILITY", "Interrupt");
    }

    // ---- DEBUG TOOLS ----
    private final String TAG = "URL_HELPER_SERVICE";

    private List<AccessibilityNodeInfo> getAllNodes(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        Queue<AccessibilityNodeInfo> queue = new LinkedList<>();
        queue.add(rootNode);
        Log.d(TAG, "-----");
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo currentNode = queue.poll();
            if (currentNode != null) {
                Log.d(TAG, "Node: " + "Class: " + currentNode.getClassName()
                        + ", Text: " + currentNode.getText()
                        + ", Content description: " + currentNode.getContentDescription()
                        + ", Resource ID: " + currentNode.getViewIdResourceName());
                allNodes.add(currentNode);
                for (int i = 0; i < currentNode.getChildCount(); i++) {
                    queue.add(currentNode.getChild(i));
                }
            }
        }
        return allNodes;
    }
}
