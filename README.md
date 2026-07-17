# TV Launcher

面向 Android 电视和电视盒子的 Apple TV 风格默认桌面。最低支持 Android 6（API 23），默认采用六列横向大卡片，并以 Xiaomi TV Box S 的 4K 输出环境作为性能验证基线。

## 功能

- 可注册为系统默认 HOME Launcher。
- 首次启动保持空桌面，中央提示按菜单键进入设置。
- 直接读取 APK 的 Activity Banner 或 Application Banner。
- 应用没有横向 Banner 时，生成同尺寸横向卡片并将方形图标等比居中。
- 非焦点卡片只有极浅投影，不显示应用名称。
- 焦点卡片放大、上浮、显示名称并使用明显软投影。
- 默认每行 6 个应用，设置中可调整列数、卡片尺寸和中央图标尺寸。
- 遥控器长按确认键或鼠标长按可移动、隐藏应用。
- 支持选择本地静态壁纸。
- 保持当前分辨率，并请求同分辨率下最高刷新率，上限 120 Hz；失败时跟随系统模式。

## 安装与使用

从 [Releases](https://github.com/R19988088/TV_launcher/releases) 直接下载 `.apk`，安装后按 Home 键并选择 `TV Launcher` 作为默认桌面。

桌面操作：

- 菜单键：进入设置。
- 方向键：移动焦点。
- 确认键：启动应用。
- 长按确认键：选择移动或隐藏。
- 移动模式下确认键保存，返回键取消。

部分遥控器把菜单键映射为 Android 的设置键，启动器同时接受 `KEYCODE_MENU` 和 `KEYCODE_SETTINGS`。

## 性能约束

- UI 使用原生 Java 与 Android View，不包含 Compose、Flutter、WebView、Rust 或 native `.so`。
- 桌面静止时没有持续渲染循环。
- 焦点移动只动画离开和进入的两张卡片，不重新测量网格。
- APK Banner 解码、降级卡片生成和磁盘缓存均在最多两个后台线程运行。
- 内存 Banner 缓存上限为 12 MB，磁盘派生缓存上限为 64 MB。
- 默认壁纸为 1920×1080 WebP，4K 输出由 GPU 缩放，避免单张 4K ARGB Bitmap 占用约 32 MB Java 堆。

连接实际设备后运行：

```bash
./scripts/profile_launcher.sh
```

脚本执行 500 次方向移动并保存 `gfxinfo framestats`、`meminfo`、显示模式和 SurfaceFlinger 延迟数据。没有真实的同分辨率 120 Hz 显示模式时，不能把动画上限描述成已验证的 120 FPS。

## Release 构建

GitHub Actions 只运行 release 单元测试、release lint、R8/资源收缩、固定签名和 release APK 构建。工作流不构建或上传 debug APK，不调用 `upload-artifact`，也不生成 ZIP；版本标签会把一个签名 `.apk` 直接附加到 GitHub Release。

签名由仓库 secrets 中的以下固定值提供：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`

这些 secrets 缺失时 release 构建会失败，不会回退到 debug 签名。首次 release 后应记录证书 SHA-256，后续版本必须保持一致。

## 壁纸来源

默认山景照片来自 [Luca Bravo / Unsplash](https://unsplash.com/photos/O453M2Liufs)，按 Unsplash License 使用，已离线压缩到应用资源中，启动器运行时不联网。
