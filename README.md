# 精简优化助手 (Android 原生版 · Shizuku)

基于 Kotlin + Jetpack Compose (Material 3) + Shizuku API 开发的原生安卓应用，无需电脑数据线、无需 Root，直接在联想 Y700、三星平板/手机及其他安卓设备上完成系统预装精简、误删一键救回与系统性能优化。

## ☁️ 如何云端编译 APK（无需安装 Android Studio）

1. 在 GitHub 上新建一个仓库（公开或私有均可）。
2. 将本项目的所有文件（**包含 `.github/workflows/build.yml`**）上传/推送到仓库的 `main` 分支。
3. 打开仓库顶部的 **Actions** 页面，等待 `Build Android APK` 任务运行完成（约 2~3 分钟）。
4. 点进构建记录，在底部 **Artifacts** 中下载 `AndroidManager-APK`，解压后即可获得 `app-debug.apk`。

## 📱 手机/平板端使用步骤

1. 安装 **Shizuku**（可在 GitHub 或酷安下载）。
2. 在设备设置中开启 **开发者选项 -> 无线调试**，打开 Shizuku 完成本地配对并启动服务。
3. 安装并打开 **精简优化助手**，点击顶部卡片的 **连接 / 授权 Shizuku** 并允许授权。
4. 授权成功后即可直接精简预装应用、一键恢复误删组件或应用系统性能优化。
