# NVGT Android 汉化版

## 项目说明

本项目是 NVGT (NonVisual Gaming Toolkit) 的 Android 移植版本，经过完整汉化，保留了电脑版的所有功能。

NVGT 是一个跨平台音频游戏开发引擎，与现已停止维护的 Blastbay Gaming Toolkit 兼容。它集成了许多开源库的功能，通过 Angelscript 引擎让游戏开发者能够将脚本编译成可分发的产品。

## 功能特性

NVGT Android 版本完整支持以下所有功能：

1. 完整的 Angelscript 脚本支持
2. 音频引擎：SDL3 和 miniaudio
3. 语音合成：完整的 Android TTS 引擎支持
4. 网络功能：HTTP、FTP、TCP、UDP 等
5. 数据库支持：SQLite 和 Redis
6. 物理引擎：ReactPhysics3D
7. 屏幕阅读器支持：与 TalkBack 等无障碍应用完美兼容
8. 文件系统和流操作
9. 加密和安全功能

## 系统要求

- Android 9.0 或更高版本（API 28+）
- ARM64 架构处理器
- 约 50MB 存储空间

## 下载安装

最新版本下载链接：

nvgt_android_0.90.0-dev.apk

安装步骤：

1. 点击上方下载链接，下载 APK 文件
2. 在手机上找到下载的 APK 文件
3. 如果提示"禁止安装未知来源应用"，请在设置中允许安装
4. 点击 APK 文件开始安装
5. 安装完成后，打开应用即可使用

## 屏幕阅读器支持

本项目对视障用户友好，完全支持屏幕阅读器。

支持的屏幕阅读器包括：

- TalkBack（Android 内置）
- 其他支持 Android 无障碍 API 的屏幕阅读器

屏幕阅读器功能：

- 自动检测屏幕阅读器状态
- 系统级语音朗读
- 语速、音调、音量可调节
- 支持多种 TTS 引擎和语音选择

## 技术架构

主要组件：

- Angelscript 脚本引擎：版本 2.31.2
- SDL3：跨平台多媒体库
- miniaudio：轻量级音频库
- Poco：C++ 应用程序工具包
- ReactPhysics3D：物理引擎

## 构建信息

本项目使用 GitHub Actions 自动构建。

构建系统：

- 运行环境：macOS
- Java 版本：17
- Android NDK：r27
- Gradle 构建工具

## 项目结构

主要目录说明：

- src 目录：包含 NVGT 核心源代码
- jni 目录：Android NDK 构建配置
- ASAddon 目录：Angelscript 附加组件
- dep 目录：第三方依赖
- release 目录：编译输出目录
- doc 目录：完整文档

## 许可证

本项目遵循 NVGT 原项目的许可证条款。

## 原创性声明

重要说明：

本项目并非原创作品。

本项目是对 NVGT (NonVisual Gaming Toolkit) 的 Android 移植版本。NVGT 原项目由 Sam Tupy 创建和维护，托管于 GitHub。

本汉化版的目的：

- 将 NVGT 移植到 Android 平台
- 优化中文界面和文档
- 确保无障碍功能正常运作

本项目不声称对 NVGT 核心代码拥有任何版权。所有代码版权归属原作者 Sam Tupy 及相应开源库的贡献者。

## 致谢

感谢以下贡献者：

- Sam Tupy：创建和维护 NVGT 原项目
- 所有开源库的贡献者
- NVGT 社区的支持者

## 问题反馈

使用过程中遇到问题，可以在 GitHub Issues 页面提交反馈。

## 联系方式

GitHub 仓库地址：https://github.com/fkeu82/nvgt-android

## 总结

NVGT Android 汉化版让音频游戏开发触手可及。无论你是开发者还是玩家，都能在这款应用中体验到完整的功能和优秀的无障碍支持。
