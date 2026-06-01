# NVGT Android 汉化版构建问题清单

## 一、项目概述

**项目名称**: NVGT Android 汉化版
**GitHub 仓库**: https://github.com/fkeu82/nvgt-android
**原项目**: https://github.com/samtupy/nvgt
**项目描述**: 将 NVGT (NonVisual Gaming Toolkit) 音频游戏开发引擎移植到 Android 平台

---

## 二、我已完成的工作

### 1. 仓库创建
- ✅ 克隆原项目 NVGT 源代码
- ✅ 创建 GitHub 仓库 https://github.com/fkeu82/nvgt-android
- ✅ 推送所有源代码（包括完整 C++ 源码、Android NDK 配置等）
- ✅ 添加 GitHub Actions 工作流自动化构建

### 2. GitHub Actions 工作流
- ✅ 创建 `.github/workflows/android.yml`（第一个版本）
- ✅ 修复分支名称（从 main 改为 master）
- ✅ 简化构建流程（移除 submodule 依赖）
- ✅ 使用阿里云镜像源加速下载
- ✅ 添加 gradlew 执行权限

### 3. 最新工作流配置
- 文件路径: `.github/workflows/android.yml`
- 使用 macOS-14 构建环境
- 自动下载 NVGT 官方预编译依赖包 (droidev.zip)
- 自动生成 Android 签名密钥
- 构建完成后自动发布到 GitHub Releases

---

## 三、当前遇到的问题

### ❌ 构建失败
**失败步骤**: "Build Android APK"（第8步）
**失败时间**: 2026-06-01 20:12:30 (2分钟完成)
**失败任务 ID**: android_build (Job ID: 78849106311)

**可能的原因**:
1. Gradle 或 Android SDK 下载超时
2. NDK 构建工具链配置问题
3. 缺少某些系统依赖库
4. droidev.zip 下载失败或解压问题
5. gradlew 脚本权限问题

---

## 四、关键配置信息

### 1. 工作流文件内容
**文件**: `.github/workflows/android.yml`

```yaml
name: Android Build

on:
  workflow_dispatch:
  push:
    branches:
      - master

jobs:
  android_build:
    runs-on: macos-14
    steps:
    - uses: actions/checkout@v5

    - uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'
        cache: 'gradle'

    - uses: nttld/setup-ndk@v1
      id: setup-ndk
      with:
        ndk-version: r27
        link-to-sdk: true
        add-to-path: true

    - name: set ndk environ
      run: echo "ANDROID_NDK_HOME=${{steps.setup-ndk.outputs.ndk-path}}" >> $GITHUB_ENV

    - name: Download pre-built droidev dependencies
      run: |
        curl -L -o droidev.zip https://nvgt.dev/droidev.zip
        unzip -q droidev.zip
        ls -la

    - name: Generate keystore
      run: |
        cd jni
        keytool -genkey -keyalg RSA -keysize 2048 -v -keystore android.keystore -dname cn=NVGT-Android-CN -storepass Android1422207 -validity 10000 -alias app
        cd ..

    - name: Build Android APK
      run: |
        cd jni
        chmod +x gradlew
        ./gradlew assembleRunnerRelease --no-daemon -Pandroid.injected.signing.store.file=android.keystore -Pandroid.injected.signing.store.password=Android1422207 -Pandroid.injected.signing.key.alias=app -Pandroid.injected.signing.key.password=Android1422207 --stacktrace
        mv build/outputs/apk/runner/release/nvgt-runner-release.apk ../release/nvgt.apk
        cd ..
        ls -la release/

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: nvgt-android-apk
        path: release/nvgt.apk

  release:
    runs-on: ubuntu-latest
    needs: android_build
    if: github.event_name == 'push' && github.ref == 'refs/heads/master'
    permissions:
      contents: write
    steps:
    - uses: actions/checkout@v5
    - uses: actions/download-artifact@v4
      with:
        name: nvgt-android-apk
        path: artifacts
    - name: set version
      run: |
        VERSION=$(cat version)
        echo "VERSION=$VERSION" >> $GITHUB_ENV
    - name: create release
      uses: softprops/action-gh-release@v2
      with:
        files: release/nvgt_android_${{ env.VERSION }}.apk
        tag_name: android-${{ env.VERSION }}
```

### 2. Android 构建配置
**目录**: `jni/`
**关键文件**:
- `Android.mk` - NDK 构建配置（定义编译目标、库依赖）
- `build.gradle` - Gradle 项目配置
- `gradlew` - Gradle Wrapper 脚本（已添加执行权限）

### 3. 依赖下载
- **下载地址**: https://nvgt.dev/droidev.zip
- **大小**: 约 160MB
- **内容**: 预编译的 Android 库文件（SDL3、miniaudio、Poco、Angelscript 等）

### 4. 构建目标
- **最低 Android 版本**: API 28 (Android 9)
- **目标架构**: ARM64-v8a
- **签名**: 自动生成测试签名（密码: Android1422207）

---

## 五、排查建议

### 1. 查看详细日志
请访问以下链接查看构建失败的具体错误信息：
- 失败的 Job: https://github.com/fkeu82/nvgt-android/actions/runs/26754042282/job/78849106311
- 完整日志下载: https://github.com/fkeu82/nvgt-android/actions/runs/26754042282/logs

### 2. 可能需要检查的地方
1. **Gradle 下载**: 检查是否需要配置 Gradle 镜像（如阿里云）
2. **Android SDK**: 确认是否需要额外安装 platform-tools
3. **NDK**: r27 版本是否兼容
4. **系统依赖**: macOS 是否缺少某些编译工具
5. **网络问题**: GitHub Actions 的 macOS runner 是否可以访问 nvgt.dev

### 3. 参考官方文档
- NVGT 官方 Android 构建指南: https://github.com/samtupy/nvgt/blob/main/doc/src/advanced/Building%20NVGT%20for%20Android.md
- 原项目官方 CI 配置: https://github.com/samtupy/nvgt/blob/main/.github/workflows/release.yml

---

## 六、GitHub Actions 运行历史

| #  | 时间 | 状态 | 备注 |
|----|------|------|------|
| 1  | 19:45 | ❌ 失败 | Dependabot 自动检查（可忽略） |
| 2  | 20:05 | ❌ 失败 | Android Build 失败 |
| 3  | 20:10 | ❌ 失败 | Android Build 失败 |
| 4  | 刚才  | 🔄 运行中 | 手动触发的新构建 |

---

## 七、快速修复建议

### 如果问题持续，可以尝试：

1. **使用 vcpkg 重新编译依赖**（替代预编译包）
   ```yaml
   - name: Build dependencies from source
     run: python3 vcpkg/build_dependencies.py --archive arm64-android
   ```

2. **增加 Gradle 超时时间**
   ```yaml
   - name: Build Android APK
     timeout-minutes: 30
     run: |
       cd jni
       ./gradlew assembleRunnerRelease --no-daemon ...
   ```

3. **简化构建命令**，移除 --stacktrace 减少输出
4. **添加更多调试步骤**，如 `ls -la` 查看文件结构

---

## 八、联系方式

**GitHub 仓库**: https://github.com/fkeu82/nvgt-android
**GitHub 用户**: fkeu82
**Access Token**: 已提供（有 repo 权限）

---

## 九、后续步骤

1. 修复构建问题
2. 验证 APK 可以正常安装
3. 测试所有功能是否正常（音频、TTS、网络等）
4. 发布到 Google Play 或其他分发渠道

---

**最后更新**: 2026-06-01 20:15 (GMT+8)
