# 掌上五子棋

![宣传图](docs/promo-1280x640.png)

一款专为 **1:1 方形小屏智能手表** 设计的本地五子棋 Android 应用。

- 单人对弈（六档 AI）/ 多人对弈 / 练习模式
- 完全本地运行：**无网络权限、无账号、无广告、不申请任何权限**
- 240×240 ~ 390×390 方形屏自适应，棋盘支持 1.0~3.0 缩放与拖动
- 十套配色主题 · 木纹/深色/高对比棋盘 · 渐变半透明按钮
- 图形效果预设（精致/均衡/简约/自定义）与文字大小调节
- 包名：`com.example.watchgomoku`

> HIM 开发 · MIT 许可

详细的产品方案、架构、验收清单见 [docs/开发说明.md](docs/开发说明.md)。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose（BOM 2024.09.03 / Material3）+ Wear Compose Material 1.4.0 |
| 架构 | MVVM：ViewModel + StateFlow + 不可变 GameState 快照 |
| AI | 纯 Kotlin：棋型评估 + 候选点 + Alpha-Beta（负极大）+ 迭代加深 + Zobrist 置换表 |
| 存储 | DataStore（设置）/ JSON 文件（自动存档、最近 20 局棋谱） |
| 测试 | JUnit4 单元测试 + Compose UI Test |

## 导入 Android Studio

1. `File → Open` 选择本项目根目录；
2. 等待 Gradle Sync 完成（需联网下载依赖；如 Google Maven 不可达，项目已内置阿里云镜像回退）；
3. `local.properties` 指向本机 Android SDK（Gradle 通常自动生成）；
4. 直接 `Run` 到手表设备或模拟器。

## 命令行构建

需要：JDK 17+、Android SDK（platform 35 + build-tools 34.0.0 + platform-tools）。

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17+"
$env:ANDROID_HOME = "C:\path\to\android-sdk"

# Debug APK → app/build/outputs/apk/debug/app-debug.apk
gradle :app:assembleDebug

# Release APK（已配混淆 + 资源收缩，需自行签名）
gradle :app:assembleRelease

# 单元测试（纯 JVM，无需设备）
gradle :app:testDebugUnitTest

# UI 测试（需设备 / 模拟器）
gradle :app:connectedDebugAndroidTest
```

> 建议：中文路径在个别 Android 构建工具链上不稳定，请把项目放在纯 ASCII 路径下构建。

## 安装

Debug 签名包可直接安装（Android 8.0+ / Wear OS 2+）。

- 手机：传 APK 到手机 → 文件管理器点开 → 允许安装未知应用；
- 手表（推荐）：手表开启 ADB 调试 → `adb connect <ip>:5555` → `adb install -r app-debug.apk`。

## 数据与隐私

- 设置：DataStore（`gomoku_settings.preferences_pb`）
- 自动存档：`files/autosave.json`；棋谱：`files/history.json`（最多 20 局）
- 清除数据：设置 → 数据与存档；或系统设置中“清除应用数据”

## 许可

MIT License，Copyright (c) 2026 HIM，详见 [LICENSE.txt](LICENSE.txt)。
