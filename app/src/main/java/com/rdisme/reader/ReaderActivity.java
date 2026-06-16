package com.rdisme.reader;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 阅读器 — 显示 TXT 文件内容，支持字号调节和主题切换
 */
public class ReaderActivity extends AppCompatActivity {

    private TextView tvContent;
    private TextView tvFileName;
    private ScrollView scrollView;
    private LinearLayout topBar;
    private LinearLayout bottomBar;

    private String filePath;
    private String fileName;
    private String fullText = "";
    private int fontSize = 17;
    private int themeMode = 0; // 0=paper, 1=dark, 2=light

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        // Get file info from intent
        filePath = getIntent().getStringExtra("filePath");
        fileName = getIntent().getStringExtra("fileName");

        if (filePath == null || !new File(filePath).exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Init views
        tvContent = findViewById(R.id.tvContent);
        tvFileName = findViewById(R.id.tvFileName);
        scrollView = findViewById(R.id.scrollView);
        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);

        tvFileName.setText(fileName);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // TTS button
        ImageButton btnTts = findViewById(R.id.btnTts);
        btnTts.setOnClickListener(v -> startTts());

        // Font size seek bar
        SeekBar seekBarFontSize = findViewById(R.id.seekBarFontSize);
        seekBarFontSize.setProgress(fontSize - 12);
        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fontSize = 12 + progress;
                tvContent.setTextSize(fontSize);
                findViewById(R.id.tvFontSize).setText(fontSize + "sp");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Theme buttons
        findViewById(R.id.btnLight).setOnClickListener(v -> setTheme(2));
        findViewById(R.id.btnDark).setOnClickListener(v -> setTheme(1));
        findViewById(R.id.btnPaper).setOnClickListener(v -> setTheme(0));

        // Load text
        loadText();
    }

    /**
     * 加载 TXT 文件内容
     */
    private void loadText() {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            fullText = sb.toString();
            if (fullText.isEmpty()) {
                Toast.makeText(this, "文件为空或无法读取", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            tvContent.setText(fullText);

            // Show bars
            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Toast.makeText(this, "读取文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * 设置主题
     */
    private void setTheme(int mode) {
        themeMode = mode;
        switch (mode) {
            case 0: // 护眼（米色纸）
                tvContent.setBackgroundColor(getColor(R.color.paper));
                tvContent.setTextColor(getColor(R.color.text_on_paper));
                break;
            case 1: // 深色
                tvContent.setBackgroundColor(getColor(R.color.paper_dark));
                tvContent.setTextColor(getColor(R.color.text_on_paper_dark));
                break;
            case 2: // 浅色
                tvContent.setBackgroundColor(Color.WHITE);
                tvContent.setTextColor(Color.BLACK);
                break;
        }
    }

    /**
     * 启动听书模式
     */
    private void startTts() {
        if (fullText.isEmpty()) {
            Toast.makeText(this, "没有可朗读的内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split text into paragraphs/chunks
        String[] paragraphs = fullText.split("\n");

        Intent intent = new Intent(this, TtsPlayerActivity.class);
        intent.putExtra("fileName", fileName);
        intent.putExtra("paragraphs", paragraphs);
        startActivity(intent);
    }
}
