package com.trianguloy.forceurl.utilities;

import static com.trianguloy.forceurl.utilities.methods.JavaUtils.clamp;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.trianguloy.forceurl.R;
import com.trianguloy.forceurl.utilities.generics.GenericPref;
import com.trianguloy.forceurl.utilities.methods.AndroidUtils;
import com.trianguloy.forceurl.utilities.methods.JavaUtils;

public class Bubble {
    // ------------------- origin -------------------
    public static GenericPref.Int X_POS(Context cntx) {
        return new GenericPref.Int("bubble_x", 0, cntx);
    }

    public static GenericPref.Int Y_POS(Context cntx) {
        return new GenericPref.Int("bubble_y", 0, cntx);
    }

    // ------------------- Bubble -------------------
    private View floatView;
    private final WindowManager windowManager;
    private final LayoutParams layoutParams;
    private final JavaUtils.Consumer<Context> popAction;

    public Bubble(Context context, JavaUtils.Consumer<Context> popAction) {
        // The bubble is independent from any activity
        context = context.getApplicationContext();
        this.popAction = popAction;

        floatView = LayoutInflater.from(context).inflate(R.layout.bubble, null);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        floatView.findViewById(R.id.bubbleImage).setOnTouchListener(onTouchListener);
        var x_pref = X_POS(context);
        var y_pref = Y_POS(context);
        var x_pos = x_pref.get();
        var y_pos = y_pref.get();
        layoutParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                x_pos,
                y_pos,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        LayoutParams.TYPE_APPLICATION_OVERLAY :
                        LayoutParams.TYPE_TOAST, // TYPE_PHONE?
                LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        floatView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                floatView.getViewTreeObserver().removeOnPreDrawListener(this);
                clampPos();
                windowManager.updateViewLayout(floatView, layoutParams);
                return true;
            }
        });

        AndroidUtils.runOnUiThread(() -> {
            windowManager.addView(floatView, layoutParams);
        });
    }

    private int lastX = 0;
    private int lastY = 0;
    private int firstX = 0;
    private int firstY = 0;
    private boolean isMoving = false;
    private final int THRESHOLD = 5;

    // Adapted from https://stackoverflow.com/a/53092436
    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            boolean touchConsumedByMove = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = Math.round(event.getRawX());
                    lastY = Math.round(event.getRawY());
                    firstX = lastX;
                    firstY = lastY;
                    isMoving = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    var rawX = Math.round(event.getRawX());
                    var rawY = Math.round(event.getRawY());
                    var totalDeltaX = lastX - firstX;
                    var totalDeltaY = lastY - firstY;
                    var deltaX = rawX - lastX;
                    var deltaY = rawY - lastY;
                    lastX = rawX;
                    lastY = rawY;
                    if (Math.abs(totalDeltaX) >= THRESHOLD || Math.abs(totalDeltaY) >= THRESHOLD) {
                        if (event.getPointerCount() == 1) {
                            layoutParams.x += deltaX;
                            layoutParams.y += deltaY;
                            clampPos();
                            windowManager.updateViewLayout(floatView, layoutParams);
                            touchConsumedByMove = true;
                            isMoving = true;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    if (isMoving) {
                        Context context = view.getContext();
                        var x_pref = X_POS(context);
                        var y_pref = Y_POS(context);
                        x_pref.set(layoutParams.x);
                        y_pref.set(layoutParams.y);
                    } else {
                        pop();
                    }
                    isMoving = false;
                    return false;
                default:
                    break;
            }
            return touchConsumedByMove;
        }
    };

    private void clampPos() {
        var size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        layoutParams.x = clamp(layoutParams.x,
                0,
                size.x - floatView.getHeight());
        layoutParams.y = clamp(layoutParams.y,
                0,
                size.y - floatView.getHeight());
    }

    public void pop() {
        popAction.accept(floatView.getContext());
        windowManager.removeView(floatView);
    }
}
