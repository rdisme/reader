# 小说阅读器 Android App

一个支持 TXT 文件阅读和 TTS 语音朗读的 Android 应用。

## 功能

- 📂 选择本地 TXT 小说文件
- 📖 TXT 阅读器，支持字号调节（12-32sp）和主题切换（浅色/深色/护眼）
- 🎧 TTS 听书功能，支持语速调节、段落切换、自动朗读下一段
- 📱 最近打开文件记录（最多 5 个）

## 构建

需要安装 Android Studio 或 JDK 17+。

```bash
# Linux / macOS
chmod +x gradlew
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

生成的 APK 在 `app/build/outputs/apk/debug/app-debug.apk`

## 运行

```bash
# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 直接运行
./gradlew installDebug
```

## 技术栈

- 最小 SDK: 21 (Android 5.0)
- 目标 SDK: 34 (Android 14)
- 使用系统原生 TextToSpeech API
- Material Design 3 主题
- Java 8

## 项目结构

```
app/src/main/java/com/rdisme/reader/
├── MainActivity.java       # 主界面：文件选择 + 最近文件
├── ReaderActivity.java     # 阅读器：TXT 内容显示 + 主题/字号
└── TtsPlayerActivity.java  # 听书：TTS 语音朗读
```
