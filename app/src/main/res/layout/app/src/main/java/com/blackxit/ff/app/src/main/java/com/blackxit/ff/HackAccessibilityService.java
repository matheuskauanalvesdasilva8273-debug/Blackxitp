package com.blackxit.ff;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

public class HackAccessibilityService extends AccessibilityService {

    public static boolean aimbotEnabled   = false;
    public static boolean espEnabled      = false;
    public static boolean noRecoilEnabled = false;
    public static boolean speedEnabled    = false;
    public static float   fovValue        = 360f;
    public static String  aimbotBone      = "Cabeça";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable noRecoilLoop;
    private Runnable aimbotLoop;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        startNoRecoilLoop();
        startAimbotLoop();
        Intent i = new Intent("com.blackxit.ff.SERVICE_CONNECTED");
        sendBroadcast(i);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (!pkg.equals("com.dts.freefireth") && !pkg.equals("com.dts.freefiremax")) return;
        if (espEnabled) scrapeGameWindows();
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void startNoRecoilLoop() {
        noRecoilLoop = new Runnable() {
            @Override
            public void run() {
                if (noRecoilEnabled) injectNoRecoilGesture();
                handler.postDelayed(this, 60);
            }
        };
        handler.post(noRecoilLoop);
    }

    private void injectNoRecoilGesture() {
        int cx = getResources().getDisplayMetrics().widthPixels  / 2;
        int cy = getResources().getDisplayMetrics().heightPixels / 2;
        Path path = new Path();
        path.moveTo(cx, cy);
        path.lineTo(cx, cy + 2);
        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(path, 0, 50);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke).build();
        dispatchGesture(gesture, null, null);
    }

    private void startAimbotLoop() {
        aimbotLoop = new Runnable() {
            @Override
            public void run() {
                if (aimbotEnabled) performAimbot();
                handler.postDelayed(this, 100);
            }
        };
        handler.post(aimbotLoop);
    }

    private void performAimbot() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return;
        int centerX = getResources().getDisplayMetrics().widthPixels  / 2;
        int centerY = getResources().getDisplayMetrics().heightPixels / 2;
        for (AccessibilityWindowInfo window : windows) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) continue;
            findAndAimEnemy(root, centerX, centerY);
            root.recycle();
        }
    }

    private void findAndAimEnemy(AccessibilityNodeInfo node, int cx, int cy) {
        if (node == null) return;
        String viewId = node.getViewIdResourceName();
        if (viewId != null && (viewId.contains("enemy") || viewId.contains("target") ||
                               viewId.contains("player"))) {
            android.graphics.Rect bounds = new android.graphics.Rect();
            node.getBoundsInScreen(bounds);
            int targetX = bounds.centerX();
            int targetY = aimbotBone.equals("Cabeça")
                ? bounds.top + (bounds.height() / 5)
                : bounds.centerY();
            double dist = Math.sqrt(Math.pow(targetX - cx, 2) + Math.pow(targetY - cy, 2));
            float fovPixels = (fovValue / 360f) * Math.min(
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels) / 2f;
            if (dist <= fovPixels) snapAimTo(cx, cy, targetX, targetY);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            findAndAimEnemy(node.getChild(i), cx, cy);
        }
    }

    private void snapAimTo(int fromX, int fromY, int toX, int toY) {
        Path path = new Path();
        path.moveTo(fromX, fromY);
        path.lineTo(toX, toY);
        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(path, 0, 80);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke).build();
        dispatchGesture(gesture, null, null);
    }

    private void scrapeGameWindows() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return;
        StringBuilder enemies = new StringBuilder();
        for (AccessibilityWindowInfo window : windows) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) continue;
            collectEnemyNodes(root, enemies);
            root.recycle();
        }
        if (enemies.length() > 0) {
            Intent i = new Intent("com.blackxit.ff.ESP_DATA");
            i.putExtra("data", enemies.toString());
            sendBroadcast(i);
        }
    }

    private void collectEnemyNodes(AccessibilityNodeInfo node, StringBuilder out) {
        if (node == null) return;
        String viewId = node.getViewIdResourceName();
        if (viewId != null && (viewId.contains("enemy") || viewId.contains("hp") ||
                               viewId.contains("player"))) {
            android.graphics.Rect b = new android.graphics.Rect();
            node.getBoundsInScreen(b);
            out.append(b.left).append(",").append(b.top).append(",")
               .append(b.width()).append(",").append(b.height()).append(";");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectEnemyNodes(node.getChild(i), out);
        }
    }
    }
