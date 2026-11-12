package com.example.tapper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class TapService extends AccessibilityService {

    static TapService instance;

    private WindowManager wm;
    private LinearLayout panel;
    private WindowManager.LayoutParams panelLp;
    private View selector;
    private AreaFrame frame;
    private TextView freqLabel;
    private Button startBtn;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private boolean running = false;
    private RectF area = null;
    private int freq = 1;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            doTap();
            handler.postDelayed(this, 1000L / freq);
        }
    };

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() { stopTapping(); }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        refreshFreq();
        showPanel();
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        stopTapping();
        removeViews();
        instance = null;
        return super.onUnbind(intent);
    }

    void refreshFreq() {
        SharedPreferences sp = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        freq = Math.max(1, Math.min(10, sp.getInt(MainActivity.KEY_FREQ, 1)));
        if (freqLabel != null) freqLabel.setText(freq + "/с");
    }

    private void saveFreq(int f) {
        freq = Math.max(1, Math.min(10, f));
        getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).edit().putInt(MainActivity.KEY_FREQ, freq).apply();
        if (freqLabel != null) freqLabel.setText(freq + "/с");
    }

    void showPanel() {
        if (panel != null) return;
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setBackgroundColor(Color.argb(220, 30, 30, 30));
        panel.setGravity(Gravity.CENTER_VERTICAL);

        TextView handle = label("≡");
        Button areaBtn = btn("▭");
        startBtn = btn("▶");
        Button minus = btn("−");
        freqLabel = label(freq + "/с");
        Button plus = btn("+");
        Button close = btn("✕");

        areaBtn.setOnClickListener(v -> startSelecting());
        startBtn.setOnClickListener(v -> { if (running) stopTapping(); else startTapping(); });
        minus.setOnClickListener(v -> saveFreq(freq - 1));
        plus.setOnClickListener(v -> saveFreq(freq + 1));
        close.setOnClickListener(v -> { stopTapping(); removeViews(); });

        panel.addView(handle);
        panel.addView(areaBtn);
        panel.addView(startBtn);
        panel.addView(minus);
        panel.addView(freqLabel);
        panel.addView(plus);
        panel.addView(close);

        panelLp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        panelLp.gravity = Gravity.TOP | Gravity.START;
        panelLp.x = 20;
        panelLp.y = 200;

        handle.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy; float tx, ty;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = panelLp.x; sy = panelLp.y; tx = e.getRawX(); ty = e.getRawY(); return true;
                    case MotionEvent.ACTION_MOVE:
                        panelLp.x = sx + (int) (e.getRawX() - tx);
                        panelLp.y = sy + (int) (e.getRawY() - ty);
                        wm.updateViewLayout(panel, panelLp);
                        return true;
                }
                return false;
            }
        });

        wm.addView(panel, panelLp);
    }

    private Button btn(String t) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextColor(Color.WHITE);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(24, 8, 24, 8);
        return b;
    }

    private TextView label(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(Color.WHITE);
        v.setTextSize(18);
        v.setPadding(24, 8, 24, 8);
        return v;
    }

    private void removeViews() {
        try { if (panel != null) wm.removeView(panel); } catch (Exception ignored) {}
        try { if (selector != null) wm.removeView(selector); } catch (Exception ignored) {}
        try { if (frame != null) wm.removeView(frame); } catch (Exception ignored) {}
        panel = null; selector = null; frame = null;
    }

    private WindowManager.LayoutParams fullScreen(boolean touchable) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        if (!touchable) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        return lp;
    }

    private void startSelecting() {
        stopTapping();
        if (selector != null) return;
        Toast.makeText(this, "Проведите пальцем, чтобы выделить область", Toast.LENGTH_SHORT).show();

        final AreaFrame drawer = new AreaFrame(this);
        drawer.dim = true;
        selector = drawer;
        drawer.setOnTouchListener(new View.OnTouchListener() {
            float x0, y0;
            @Override public boolean onTouch(View v, MotionEvent e) {
                int[] loc = new int[2];
                v.getLocationOnScreen(loc);
                float x = e.getX() + loc[0], y = e.getY() + loc[1];
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x0 = x; y0 = y;
                        drawer.setRectScreen(new RectF(x, y, x, y));
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        drawer.setRectScreen(new RectF(Math.min(x0, x), Math.min(y0, y), Math.max(x0, x), Math.max(y0, y)));
                        return true;
                    case MotionEvent.ACTION_UP:
                        RectF r = new RectF(Math.min(x0, x), Math.min(y0, y), Math.max(x0, x), Math.max(y0, y));
                        if (r.width() < 30 || r.height() < 30) {
                            r = new RectF(x - 15, y - 15, x + 15, y + 15);
                        }
                        finishSelecting(r);
                        return true;
                }
                return true;
            }
        });
        wm.addView(selector, fullScreen(true));
    }

    private void finishSelecting(RectF r) {
        area = r;
        try { wm.removeView(selector); } catch (Exception ignored) {}
        selector = null;
        try { if (frame != null) wm.removeView(frame); } catch (Exception ignored) {}
        frame = new AreaFrame(this);
        frame.setRectScreen(area);
        wm.addView(frame, fullScreen(false));
        if (panel != null) {
            try { wm.removeView(panel); wm.addView(panel, panelLp); } catch (Exception ignored) {}
        }
    }

    private void startTapping() {
        if (area == null) {
            Toast.makeText(this, "Сначала выберите область (▭)", Toast.LENGTH_SHORT).show();
            return;
        }
        running = true;
        startBtn.setText("■");
        handler.post(tick);
    }

    private void stopTapping() {
        running = false;
        handler.removeCallbacks(tick);
        if (startBtn != null) startBtn.setText("▶");
    }

    private void doTap() {
        if (area == null) return;
        float x = area.left + rnd.nextFloat() * Math.max(1, area.width());
        float y = area.top + rnd.nextFloat() * Math.max(1, area.height());

        if (panel != null) {
            int[] l = new int[2];
            panel.getLocationOnScreen(l);
            Rect pr = new Rect(l[0], l[1], l[0] + panel.getWidth(), l[1] + panel.getHeight());
            if (pr.contains((int) x, (int) y)) return;
        }

        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 20);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    static class AreaFrame extends View {
        RectF screenRect = null;
        boolean dim = false;
        final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint fill = new Paint();
        final Paint shade = new Paint();

        AreaFrame(Context c) {
            super(c);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(5);
            border.setColor(Color.rgb(255, 60, 90));
            border.setPathEffect(new DashPathEffect(new float[]{25, 15}, 0));
            fill.setColor(Color.argb(40, 255, 60, 90));
            shade.setColor(Color.argb(90, 0, 0, 0));
        }

        void setRectScreen(RectF r) { screenRect = r; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            if (dim) c.drawRect(0, 0, getWidth(), getHeight(), shade);
            if (screenRect == null) return;
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            RectF r = new RectF(screenRect);
            r.offset(-loc[0], -loc[1]);
            c.drawRect(r, fill);
            c.drawRect(r, border);
        }
    }
}
