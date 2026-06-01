# NVGT Android 汉化版

## 项目简介

本项目是 [NVGT (NonVisual Gaming Toolkit)](https://github.com/samtupy/nvgt) 的 Android 移植版本，经过完整汉化，保留了电脑版的所有功能。

NVGT 是一个跨平台音频游戏开发引擎，与现已停止维护的 Blastbay Gaming Toolkit 兼容。它集成了许多开源库的功能，通过 Angelscript 引擎让游戏开发者能够将 .nvgt 脚本编译成可分发的产品。

## 主要特性

- **完整功能保留**: 与电脑版功能完全一致
- **跨平台兼容**: 支持 Android 设备
- **音频引擎**: 基于 SDL3 和 miniaudio
- **语音合成**: 支持 Android TTS 引擎
- **网络功能**: 完整的网络编程支持
- **数据库支持**: SQLite 和 Redis
- **物理引擎**: ReactPhysics3D 集成
- **脚本语言**: 强大的 Angelscript 支持

## 构建说明

本项目使用 GitHub Actions 自动构建 Android APK。

### 自动构建

每次推送到 main 分支或手动触发工作流时，系统会自动构建 Android APK。

### 本地构建

如果你想在本地构建，需要：

1. 安装 Android SDK
2. 安装 Android NDK (r27)
3. 安装 Java JDK 17
4. 下载 [droidev.zip](https://nvgt.dev/droidev.zip) 并解压到项目根目录
5. 进入 `jni` 目录并运行：
   ```bash
   ./gradlew assembleRunnerRelease
   ```

## 使用方法

1. 从 [Releases](https://github.com/YOUR_USERNAME/nvgt-android/releases) 页面下载最新 APK
2. 安装到 Android 设备
3. 运行应用，享受与电脑版完全一致的体验

## 开发指南

### 项目结构

- `src/` - NVGT 核心源代码
- `jni/` - Android NDK 构建配置
- `ASAddon/` - Angelscript 附加组件
- `dep/` - 第三方依赖
- `vcpkg/` - 依赖管理
- `release/` - 编译输出目录

### 支持的平台

- Android 9.0+ (API 28+)
- ARM64 架构

## 许可证

本项目遵循 NVGT 原项目的许可证条款。详情请参阅 [license.md](license.md)。

## 致谢

- 感谢 [Sam Tupy](https://github.com/samtupy) 创建和维护 NVGT
- 所有开源库的贡献者

## 问题反馈

如果你在使用过程中遇到任何问题，请在 GitHub Issues 页面提交。

---

**NVGT Android 汉化版** - 让音频游戏开发触手可及
