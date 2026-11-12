package com.example.tapper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    static final String PREFS = "tapper";
    static final String KEY_FREQ = "freq";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        int freq = sp.getInt(KEY_FREQ, 1);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        root.setPadding(p, p, p, p);

        TextView info = new TextView(this);
        info.setTextSize(15);
        info.setTextColor(Color.BLACK);
        info.setText("1. Включите «Тапер» в Специальных возможностях.\n"
                + "2. Задайте частоту нажатий.\n"
                + "3. Нажмите «Показать панель», откройте TikTok.\n"
                + "4. На панели: ▭ — выбрать область, ▶ — старт/стоп.");
        root.addView(info);

        Button access = new Button(this);
        access.setText("Открыть настройки спец. возможностей");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);

        final TextView freqLabel = new TextView(this);
        freqLabel.setTextSize(18);
        freqLabel.setTextColor(Color.BLACK);
        freqLabel.setPadding(0, p, 0, 0);
        freqLabel.setText("Частота: " + freq + " нажатий/сек");
        root.addView(freqLabel);

        SeekBar bar = new SeekBar(this);
        bar.setMax(9);
        bar.setProgress(freq - 1);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int prog, boolean user) {
                int f = prog + 1;
                sp.edit().putInt(KEY_FREQ, f).apply();
                freqLabel.setText("Частота: " + f + " нажатий/сек");
                if (TapService.instance != null) TapService.instance.refreshFreq();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(bar);

        Button show = new Button(this);
        show.setText("Показать панель");
        show.setOnClickListener((View v) -> {
            if (TapService.instance == null) {
                Toast.makeText(this, "Сначала включите службу «Тапер» в спец. возможностях", Toast.LENGTH_LONG).show();
            } else {
                TapService.instance.showPanel();
            }
        });
        root.addView(show);

        setContentView(root);
    }
}
