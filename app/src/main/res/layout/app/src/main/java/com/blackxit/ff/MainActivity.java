package com.blackxit.ff;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_OVERLAY = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvStatus = findViewById(R.id.tv_status);
        Button btnAcessibilidade = findViewById(R.id.btn_acessibilidade);
        Button btnOverlay = findViewById(R.id.btn_overlay);
        Button btnIniciar = findViewById(R.id.btn_iniciar);

        btnAcessibilidade.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Permissão Necessária")
                .setMessage("Ative o serviço 'BLACKXIT FF' em Acessibilidade.\n\n1. Toque em OK\n2. Encontre 'BLACKXIT FF'\n3. Ative o serviço")
                .setPositiveButton("OK", (d, w) -> {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .show();
        });

        btnOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY);
            } else {
                Toast.makeText(this, "Overlay já permitido!", Toast.LENGTH_SHORT).show();
            }
        });

        btnIniciar.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Ative a Acessibilidade primeiro!", Toast.LENGTH_LONG).show();
                return;
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Permita o Overlay primeiro!", Toast.LENGTH_LONG).show();
                return;
            }
            startService(new Intent(this, OverlayService.class));
            Toast.makeText(this, "BLACKXIT FF Ativado!", Toast.LENGTH_SHORT).show();
            finish();
        });

        updateStatus(tvStatus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tvStatus = findViewById(R.id.tv_status);
        updateStatus(tvStatus);
    }

    private void updateStatus(TextView tv) {
        boolean acc = isAccessibilityEnabled();
        boolean ov  = Settings.canDrawOverlays(this);
        tv.setText("Acessibilidade: " + (acc ? "✅" : "❌") +
                   "\nOverlay: "       + (ov  ? "✅" : "❌"));
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager)
            getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> list =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : list) {
            if (info.getId().contains(getPackageName())) return true;
        }
        return false;
    }
    }
