package com.rdisme.reader;

import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 听书播放器 — 使用系统 TTS 引擎朗读 TXT 文本
 */
public class TtsPlayerActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean isInitialized = false;

    private TextView tvBookTitle;
    private TextView tvProgress;
    private TextView tvSpokenText;
    private ImageButton btnPlayPause;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private Button btnStop;
    private SeekBar seekBarSpeed;
    private TextView tvSpeed;

    private String[] paragraphs;
    private int currentParagraph = 0;
    private boolean isPlaying = false;
    private float speechRate = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts_player);

        // Get paragraphs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            paragraphs = getStringArrayExtra("paragraphs");
        } else {
            @SuppressWarnings("deprecation")
            String[] arr = getIntent().getStringArrayExtra("paragraphs");
            paragraphs = arr;
        }

        String bookTitle = getIntent().getStringExtra("fileName");
        if (bookTitle == null) bookTitle = "小说";

        // Init views
        tvBookTitle = findViewById(R.id.tvBookTitle);
        tvProgress = findViewById(R.id.tvProgress);
        tvSpokenText = findViewById(R.id.tvSpokenText);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnStop = findViewById(R.id.btnStop);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        tvSpeed = findViewById(R.id.tvSpeed);

        tvBookTitle.setText(bookTitle);

        // Init TTS
        tts = new TextToSpeech(this, this);

        // Setup controls
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPrev.setOnClickListener(v -> prevParagraph());
        btnNext.setOnClickListener(v -> nextParagraph());
        btnStop.setOnClickListener(v -> stopTts());

        // Speed control
        seekBarSpeed.setMax(20);
        seekBarSpeed.setProgress(10);
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speechRate = 0.5f + (progress * 0.05f);
                tvSpeed.setText(String.format("%.1fx", speechRate));
                if (isInitialized && tts != null) {
                    tts.setSpeechRate(speechRate);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updateProgress();
    }

    /**
     * TTS 初始化回调
     */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default language
                tts.setLanguage(Locale.getDefault());
            }
            tts.setSpeechRate(speechRate);
            tts.setPitch(1.0f);

            // Utterance progress listener
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        isPlaying = true;
                        updatePlayButton();
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        // Auto advance to next paragraph
                        if (currentParagraph < paragraphs.length - 1) {
                            currentParagraph++;
                            speakCurrentParagraph();
                        } else {
                            // Finished all paragraphs
                            isPlaying = false;
                            updatePlayButton();
                            Toast.makeText(TtsPlayerActivity.this, "朗读完毕", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        isPlaying = false;
                        updatePlayButton();
                    }
                });
            } else {
                // Older API: use deprecated listener
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {
                        isPlaying = true;
                        updatePlayButton();
                    }
                    @Override public void onDone(String utteranceId) {
                        if (currentParagraph < paragraphs.length - 1) {
                            currentParagraph++;
                            speakCurrentParagraph();
                        } else {
                            isPlaying = false;
                            updatePlayButton();
                            Toast.makeText(TtsPlayerActivity.this, "朗读完毕", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onError(String utteranceId) {
                        isPlaying = false;
                        updatePlayButton();
                    }
                });
            }

            isInitialized = true;
            speakCurrentParagraph();
        } else {
            Toast.makeText(this, "TTS 初始化失败，请检查是否安装了语音引擎", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * 朗读当前段落
     */
    private void speakCurrentParagraph() {
        if (tts == null || !isInitialized) return;

        // Clean paragraph: remove excessive whitespace
        String text = paragraphs[currentParagraph].trim();
        if (text.isEmpty()) {
            // Skip empty paragraphs
            if (currentParagraph < paragraphs.length - 1) {
                currentParagraph++;
                speakCurrentParagraph();
            }
            return;
        }

        tvSpokenText.setText(text);
        updateProgress();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "paragraph_" + currentParagraph);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }

    /**
     * 切换播放/暂停
     */
    private void togglePlayPause() {
        if (!isInitialized) return;

        if (isPlaying) {
            tts.stop();
            isPlaying = false;
        } else {
            speakCurrentParagraph();
        }
        updatePlayButton();
    }

    /**
     * 上一段
     */
    private void prevParagraph() {
        if (currentParagraph > 0) {
            currentParagraph--;
            if (isPlaying) {
                tts.stop();
                speakCurrentParagraph();
            } else {
                tvSpokenText.setText(paragraphs[currentParagraph].trim());
                updateProgress();
            }
        }
    }

    /**
     * 下一段
     */
    private void nextParagraph() {
        if (currentParagraph < paragraphs.length - 1) {
            currentParagraph++;
            if (isPlaying) {
                tts.stop();
                speakCurrentParagraph();
            } else {
                tvSpokenText.setText(paragraphs[currentParagraph].trim());
                updateProgress();
            }
        }
    }

    /**
     * 停止朗读
     */
    private void stopTts() {
        if (tts != null) {
            tts.stop();
        }
        isPlaying = false;
        currentParagraph = 0;
        updatePlayButton();
        updateProgress();
        tvSpokenText.setText("");
    }

    /**
     * 更新播放按钮图标
     */
    private void updatePlayButton() {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_stop);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * 更新进度显示
     */
    private void updateProgress() {
        tvProgress.setText(String.format("第 %d / %d 段", currentParagraph + 1, paragraphs.length));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tts != null) {
            tts.stop();
            isPlaying = false;
        }
    }
}
