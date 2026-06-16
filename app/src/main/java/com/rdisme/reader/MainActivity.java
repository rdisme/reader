package com.rdisme.reader;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 主界面 — 文件选择和最近文件列表
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ_FILE = 1001;
    private static final String PREFS_KEY = "reader_prefs";
    private static final String KEY_RECENT_COUNT = "recent_count";
    private static final String KEY_RECENT_PREFIX = "recent_";

    private LinearLayout layoutRecentFiles;
    private TextView tvEmptyFiles;
    private List<String> recentFiles = new ArrayList<>();
    private android.content.SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_KEY, MODE_PRIVATE);
        loadRecentFiles();

        layoutRecentFiles = findViewById(R.id.layoutRecentFiles);
        tvEmptyFiles = findViewById(R.id.tvEmptyFiles);

        Button btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(v -> openFileChooser());

        renderRecentFiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentFiles();
        renderRecentFiles();
    }

    /**
     * 打开文件选择器
     */
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain", "text/x-txt"});
        startActivityForResult(Intent.createChooser(intent, "选择 TXT 文件"), REQUEST_READ_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == REQUEST_READ_FILE && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String fileName = getFileName(uri);

            // Copy file to internal storage
            File destFile = copyToFile(uri, fileName);
            if (destFile != null) {
                openReader(destFile.getAbsolutePath(), fileName);
            }
        }
    }

    /**
     * 从 URI 获取文件名
     */
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut >= 0) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * 将 URI 内容拷贝到内部存储
     */
    private File copyToFile(Uri uri, String fileName) {
        try {
            File destFile = new File(getFilesDir(), fileName);
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return destFile;
        } catch (Exception e) {
            Toast.makeText(this, "文件读取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    /**
     * 打开阅读器
     */
    private void openReader(String filePath, String fileName) {
        addToRecentFiles(filePath);

        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("filePath", filePath);
        intent.putExtra("fileName", fileName);
        startActivity(intent);
    }

    /**
     * 保存最近文件
     */
    private void addToRecentFiles(String filePath) {
        recentFiles.remove(filePath);
        recentFiles.add(0, filePath);
        if (recentFiles.size() > 5) {
            recentFiles = new ArrayList<>(recentFiles.subList(0, 5));
        }
        saveRecentFiles();
    }

    /**
     * 加载最近文件
     */
    private void loadRecentFiles() {
        recentFiles.clear();
        int count = prefs.getInt(KEY_RECENT_COUNT, 0);
        for (int i = 0; i < count; i++) {
            String path = prefs.getString(KEY_RECENT_PREFIX + i, null);
            if (path != null) {
                recentFiles.add(path);
            }
        }
    }

    /**
     * 保存最近文件
     */
    private void saveRecentFiles() {
        android.content.SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < recentFiles.size(); i++) {
            editor.putString(KEY_RECENT_PREFIX + i, recentFiles.get(i));
        }
        editor.putInt(KEY_RECENT_COUNT, recentFiles.size());
        // Clean up old entries
        for (int i = recentFiles.size(); i < 5; i++) {
            editor.remove(KEY_RECENT_PREFIX + i);
        }
        editor.apply();
    }

    /**
     * 渲染最近文件列表
     */
    private void renderRecentFiles() {
        // Clear existing views except empty text
        for (int i = layoutRecentFiles.getChildCount() - 1; i >= 0; i--) {
            View v = layoutRecentFiles.getChildAt(i);
            if (v != tvEmptyFiles) {
                layoutRecentFiles.removeViewAt(i);
            }
        }

        if (recentFiles.isEmpty()) {
            tvEmptyFiles.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyFiles.setVisibility(View.GONE);

        for (String filePath : recentFiles) {
            File file = new File(filePath);
            if (!file.exists()) continue;

            String fileName = file.getName();
            long size = file.length();

            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setPadding(16, 16, 16, 16);
            tv.setTextColor(getColor(R.color.text_primary));
            tv.setTextSize(14);
            tv.setText(fileName);
            tv.setTag(filePath);

            TextView sizeTv = new TextView(this);
            sizeTv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            sizeTv.setPadding(16, 0, 16, 12);
            sizeTv.setTextColor(getColor(R.color.text_secondary));
            sizeTv.setTextSize(11);
            sizeTv.setText(formatFileSize(size));

            layoutRecentFiles.addView(tv);
            layoutRecentFiles.addView(sizeTv);

            tv.setOnClickListener(v -> openReader(filePath, fileName));
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }
}
