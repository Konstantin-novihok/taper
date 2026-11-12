package com.example.tapper;

import android.app.Activity;
import android.app.AlertDialog;
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
        info.setText("Тапер выполняет автоматические нажатия только в области, которую вы сами выделили. "
                + "Нажатия начинаются после выбора области и нажатия ▶ на плавающей панели. "
                + "Остановите их кнопкой ■ или закройте панель.\n\n"
                + "1. Ознакомьтесь с пояснением и включите «Тапер» в Специальных возможностях.\n"
                + "2. Задайте частоту нажатий.\n"
                + "3. Нажмите «Показать панель».\n"
                + "4. На панели: ▭ — выбрать область, ▶ — старт/стоп.");
        root.addView(info);

        Button access = new Button(this);
        access.setText("Открыть настройки спец. возможностей");
        access.setOnClickListener(v -> showAccessibilityDisclosure());
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

    private void showAccessibilityDisclosure() {
        new AlertDialog.Builder(this)
                .setTitle("Зачем нужен доступ к специальным возможностям")
                .setMessage("Тапер использует службу специальных возможностей только для выполнения жестов нажатия, которые вы запускаете сами. "
                        + "Вы вручную выделяете прямоугольную область экрана и нажимаете ▶. "
                        + "Пока автокликер запущен, приложение выполняет нажатия внутри этой области с выбранной частотой, "
                        + "пока вы не нажмёте ■ или не закроете панель.\n\n"
                        + "Тапер не запрашивает содержимое окон, не читает текст на экране, "
                        + "не собирает и не отправляет данные через сеть. "
                        + "Служба не запускает нажатия сама по себе: для начала работы требуется ваше действие на панели.\n\n"
                        + "Если вы не согласны с этим использованием доступа, нажмите «Отмена».")
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Продолжить в настройки", (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .show();
    }
}
