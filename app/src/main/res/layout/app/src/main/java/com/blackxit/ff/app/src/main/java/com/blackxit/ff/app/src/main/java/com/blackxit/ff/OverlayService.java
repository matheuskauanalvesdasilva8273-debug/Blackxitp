package com.blackxit.ff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class OverlayService extends Service {

    private WindowManager wm;
    private View panelView;
    private EspOverlayView espView;
    private WindowManager.LayoutParams panelParams;
    private WindowManager.LayoutParams espParams;
    private LinearLayout tabAimbot, tabEsp, tabMisc;
    private BroadcastReceiver espReceiver;
    private static final String CHANNEL_ID = "blackxit_fg";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupEspOverlay();
        setupPanel();
        registerEspReceiver();
    }

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
            CHANNEL_ID, "BLACKXIT FF", NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BLACKXIT FF")
            .setContentText("Hack ativo")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build();
    }

    private void setupEspOverlay() {
        espView = new EspOverlayView(this);
        espParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        wm.addView(espView, espParams);
    }

    private void setupPanel() {
        panelView = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null);
        panelParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = 0;
        panelParams.y = 100;
        wm.addView(panelView, panelParams);
        bindPanelViews();
        setupDrag();
    }

    private void bindPanelViews() {
        ImageButton btnTabAimbot = panelView.findViewById(R.id.btn_tab_aimbot);
        ImageButton btnTabEsp    = panelView.findViewById(R.id.btn_tab_esp);
        ImageButton btnTabMisc   = panelView.findViewById(R.id.btn_tab_misc);
        ImageButton btnClose     = panelView.findViewById(R.id.btn_close);

        tabAimbot = panelView.findViewById(R.id.tab_aimbot);
        tabEsp    = panelView.findViewById(R.id.tab_esp);
        tabMisc   = panelView.findViewById(R.id.tab_misc);

        showTab(0);

        btnTabAimbot.setOnClickListener(v -> showTab(0));
        btnTabEsp.setOnClickListener(v -> showTab(1));
        btnTabMisc.setOnClickListener(v -> showTab(2));
        btnClose.setOnClickListener(v -> stopSelf());

        CheckBox cbAimbot  = panelView.findViewById(R.id.cb_aimbot);
        SeekBar  sbFov     = panelView.findViewById(R.id.sb_fov);
        TextView tvFovVal  = panelView.findViewById(R.id.tv_fov_value);

        cbAimbot.setOnCheckedChangeListener((b, checked) ->
            HackAccessibilityService.aimbotEnabled = checked);

        sbFov.setMax(360);
        sbFov.setProgress((int) HackAccessibilityService.fovValue);
        sbFov.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                HackAccessibilityService.fovValue = p;
                tvFovVal.setText(p + ".0");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        CheckBox cbEsp     = panelView.findViewById(R.id.cb_esp);
        CheckBox cbEspBox  = panelView.findViewById(R.id.cb_esp_box);
        CheckBox cbEspLine = panelView.findViewById(R.id.cb_esp_line);
        CheckBox cbEspSkel = panelView.findViewById(R.id.cb_esp_skel);
        CheckBox cbEspName = panelView.findViewById(R.id.cb_esp_name);
        CheckBox cbEspHp   = panelView.findViewById(R.id.cb_esp_hp);

        cbEsp.setOnCheckedChangeListener((b, checked) -> {
            HackAccessibilityService.espEnabled = checked;
            espView.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        cbEspBox.setOnCheckedChangeListener((b, c)  -> espView.drawBox  = c);
        cbEspLine.setOnCheckedChangeListener((b, c) -> espView.drawLine = c);
        cbEspSkel.setOnCheckedChangeListener((b, c) -> espView.drawSkel = c);
        cbEspName.setOnCheckedChangeListener((b, c) -> espView.drawName = c);
        cbEspHp.setOnCheckedChangeListener((b, c)   -> espView.drawHp   = c);

        CheckBox cbNoRecoil = panelView.findViewById(R.id.cb_no_recoil);
        CheckBox cbSpeed    = panelView.findViewById(R.id.cb_speed);
        CheckBox cbAimKill  = panelView.findViewById(R.id.cb_aimkill);

        cbNoRecoil.setOnCheckedChangeListener((b, c) ->
            HackAccessibilityService.noRecoilEnabled = c);
        cbSpeed.setOnCheckedChangeListener((b, c) ->
            HackAccessibilityService.speedEnabled = c);
    }

    private void showTab(int tab) {
        tabAimbot.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        tabEsp.setVisibility(tab    == 1 ? View.VISIBLE : View.GONE);
        tabMisc.setVisibility(tab   == 2 ? View.VISIBLE : View.GONE);
    }

    private void setupDrag() {
        final int[] initialX = {0}, initialY = {0};
        final float[] initialTouchX = {0}, initialTouchY = {0};
        View header = panelView.findViewById(R.id.panel_header);
        header.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX[0] = panelParams.x;
                    initialY[0] = panelParams.y;
                    initialTouchX[0] = event.getRawX();
                    initialTouchY[0] = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    panelParams.x = initialX[0] + (int)(event.getRawX() - initialTouchX[0]);
                    panelParams.y = initialY[0] + (int)(event.getRawY() - initialTouchY[0]);
                    wm.updateViewLayout(panelView, panelParams);
                    return true;
            }
            return false;
        });
    }

    private void registerEspReceiver() {
        espReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String data = intent.getStringExtra("data");
                if (data != null && espView != null) espView.updateEnemies(data);
            }
        };
        IntentFilter filter = new IntentFilter("com.blackxit.ff.ESP_DATA");
        registerReceiver(espReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (panelView  != null) wm.removeView(panelView);
        if (espView    != null) wm.removeView(espView);
        if (espReceiver != null) unregisterReceiver(espReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    static class EspOverlayView extends View {
        boolean drawBox  = true;
        boolean drawLine = false;
        boolean drawSkel = false;
        boolean drawName = false;
        boolean drawHp   = false;

        private final Paint boxPaint  = new Paint();
        private final Paint linePaint = new Paint();
        private final Paint textPaint = new Paint();
        private float[][] enemies = new float[0][];

        public EspOverlayView(Context ctx) {
            super(ctx);
            boxPaint.setColor(Color.RED);
            boxPaint.setStyle(Paint.Style.STROKE);
            boxPaint.setStrokeWidth(3f);
            linePaint.setColor(Color.YELLOW);
            linePaint.setStrokeWidth(2f);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(28f);
        }

        public void updateEnemies(String data) {
            String[] parts = data.split(";");
            enemies = new float[parts.length][4];
            for (int i = 0; i < parts.length; i++) {
                String[] vals = parts[i].split(",");
                if (vals.length == 4) {
                    enemies[i][0] = Float.parseFloat(vals[0]);
                    enemies[i][1] = Float.parseFloat(vals[1]);
                    enemies[i][2] = Float.parseFloat(vals[2]);
                    enemies[i][3] = Float.parseFloat(vals[3]);
                }
            }
            postInvalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int screenH  = getHeight();
            int screenCX = getWidth() / 2;
            for (float[] e : enemies) {
                float x = e[0], y = e[1], w = e[2], h = e[3];
                if (drawBox)  canvas.drawRect(x, y, x + w, y + h, boxPaint);
                if (drawLine) canvas.drawLine(screenCX, screenH, x + w/2, y + h, linePaint);
                if (drawName) canvas.drawText("Enemy", x, y - 5, textPaint);
                if (drawHp) {
                    Paint hpPaint = new Paint();
                    hpPaint.setColor(Color.RED);
                    canvas.drawRect(x, y - 15, x + w, y - 8, hpPaint);
                }
            }
        }
    }
                                                     }
