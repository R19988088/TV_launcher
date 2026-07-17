# Android TV Launcher 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 构建最低支持 Android 6 的 Apple TV 风格默认桌面，以原生 View 实现六列大圆角卡片、低延迟焦点动效、应用管理和固定签名的最小 release APK。

**架构：** 单进程 Java Android 应用，以 `LauncherActivity` 和系统 `GridView` 承载桌面，以 `SettingsActivity` 管理应用和显示参数。纯 Java 小组件负责排序、显示模式选择、配置和缓存键，平台层通过 `PackageManager`、有界后台加载器和硬件属性动画接入。

**技术栈：** Java 17、Android View/XML、API 23-36、AGP 9.3.0、Gradle 9.6.1、JUnit 4、R8、GitHub Actions。

---

## 文件结构

- `settings.gradle`：仓库与模块声明。
- `build.gradle`：AGP 插件版本。
- `gradle.properties`：R8、AndroidX 和构建性能设置。
- `gradlew`、`gradlew.bat`、`gradle/wrapper/*`：固定 Gradle 9.6.1。
- `app/build.gradle`：API、release 收缩、签名和测试配置。
- `app/proguard-rules.pro`：仅保留 manifest 入口，允许 R8 最大化收缩。
- `app/src/main/AndroidManifest.xml`：HOME、包变更广播和电视能力声明。
- `app/src/main/java/com/r19988088/tvlauncher/model/AppEntry.java`：桌面应用值对象。
- `app/src/main/java/com/r19988088/tvlauncher/model/ReorderSession.java`：移动模式的交换、提交和取消。
- `app/src/main/java/com/r19988088/tvlauncher/data/LauncherPreferences.java`：有序组件和显示设置持久化。
- `app/src/main/java/com/r19988088/tvlauncher/data/AppRepository.java`：枚举、去重和核对可启动应用。
- `app/src/main/java/com/r19988088/tvlauncher/image/BannerCacheKey.java`：图片缓存失效键。
- `app/src/main/java/com/r19988088/tvlauncher/image/BannerLoader.java`：后台加载 Banner 与方形图标降级卡片。
- `app/src/main/java/com/r19988088/tvlauncher/display/DisplayModeSelector.java`：纯 Java 同分辨率最高刷新率选择。
- `app/src/main/java/com/r19988088/tvlauncher/display/DisplayModeController.java`：窗口模式请求与失败回退。
- `app/src/main/java/com/r19988088/tvlauncher/ui/LauncherGridView.java`：焦点项最后绘制和方向移动。
- `app/src/main/java/com/r19988088/tvlauncher/ui/AppCardView.java`：卡片圆角、名称和焦点属性动画。
- `app/src/main/java/com/r19988088/tvlauncher/ui/AppGridAdapter.java`：网格复用与异步图片绑定。
- `app/src/main/java/com/r19988088/tvlauncher/LauncherActivity.java`：HOME 生命周期、按键、启动、移动和隐藏。
- `app/src/main/java/com/r19988088/tvlauncher/SettingsActivity.java`：遥控器友好的应用与显示设置。
- `app/src/main/java/com/r19988088/tvlauncher/PackageChangedReceiver.java`：安装、卸载和升级后的轻量失效通知。
- `app/src/main/res/layout/*`：桌面、卡片、设置及操作菜单布局。
- `app/src/main/res/drawable-nodpi/default_wallpaper.webp`：许可清晰的静态默认风景壁纸。
- `app/src/main/res/drawable/*`、`values/*`：形状、颜色、尺寸、文案和主题。
- `app/src/test/java/...`：排序、显示模式、缓存键和配置编解码测试。
- `scripts/profile_launcher.sh`：设备端帧时间和内存采集。
- `.github/workflows/release.yml`：固定签名的 release 测试、lint、构建和 GitHub Release 直传。
- `README.md`：安装、默认桌面、设置、签名和发布说明。

### 任务 1：建立可重复的 release 工程

**文件：**
- 创建：`settings.gradle`
- 创建：`build.gradle`
- 创建：`gradle.properties`
- 创建：`gradle/wrapper/gradle-wrapper.properties`
- 创建：`gradle/wrapper/gradle-wrapper.jar`
- 创建：`gradlew`
- 创建：`gradlew.bat`
- 创建：`app/build.gradle`
- 创建：`app/proguard-rules.pro`
- 创建：`app/src/main/AndroidManifest.xml`
- 创建：`app/src/main/res/values/strings.xml`
- 创建：`app/src/main/res/values/themes.xml`

- [ ] **步骤 1：创建最小 Gradle 工程配置**

```groovy
// app/build.gradle 的关键约束
plugins { id 'com.android.application' }

android {
    namespace 'com.r19988088.tvlauncher'
    compileSdk 36

    defaultConfig {
        applicationId 'com.r19988088.tvlauncher'
        minSdk 23
        targetSdk 36
        versionCode 1
        versionName '0.1.0'
        testInstrumentationRunner 'android.app.Instrumentation'
    }

    buildTypes {
        release {
            debuggable false
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

- [ ] **步骤 2：声明 HOME 入口和电视特性**

```xml
<activity android:name=".LauncherActivity" android:screenOrientation="landscape" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
    </intent-filter>
</activity>
```

- [ ] **步骤 3：生成并验证 Gradle wrapper**

运行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./gradlew --version
```

预期：输出 `Gradle 9.6.1` 和 Java 17，不下载或构建 debug APK。

- [ ] **步骤 4：提交工程骨架**

```bash
git add settings.gradle build.gradle gradle.properties gradle gradlew gradlew.bat app
git commit -m "build: bootstrap release Android project"
```

### 任务 2：用 TDD 实现有序桌面状态

**文件：**
- 创建：`app/src/test/java/com/r19988088/tvlauncher/model/ReorderSessionTest.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/model/AppEntry.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/model/ReorderSession.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/data/LauncherPreferences.java`
- 创建：`app/src/test/java/com/r19988088/tvlauncher/data/LauncherPreferencesCodecTest.java`

- [ ] **步骤 1：先写移动提交与取消的失败测试**

```java
@Test public void cancelRestoresOriginalOrder() {
    ReorderSession session = new ReorderSession(List.of("a/A", "b/B", "c/C"), 1);
    session.swapWith(2);
    assertEquals(List.of("a/A", "c/C", "b/B"), session.current());
    assertEquals(List.of("a/A", "b/B", "c/C"), session.cancel());
}

@Test public void commitKeepsMovedOrder() {
    ReorderSession session = new ReorderSession(List.of("a/A", "b/B"), 0);
    session.swapWith(1);
    assertEquals(List.of("b/B", "a/A"), session.commit());
}
```

- [ ] **步骤 2：运行测试并确认因类不存在而失败**

运行：`./gradlew :app:testReleaseUnitTest --tests '*ReorderSessionTest'`

预期：FAIL，编译错误指向 `ReorderSession` 尚不存在。

- [ ] **步骤 3：实现最小交换会话**

```java
public final class ReorderSession {
    private final List<String> original;
    private final List<String> current;
    private int selected;

    public ReorderSession(List<String> ids, int selected) {
        this.original = Collections.unmodifiableList(new ArrayList<>(ids));
        this.current = new ArrayList<>(ids);
        this.selected = selected;
    }

    public void swapWith(int target) {
        Collections.swap(current, selected, target);
        selected = target;
    }

    public List<String> current() {
        return Collections.unmodifiableList(new ArrayList<>(current));
    }
    public List<String> commit() { return current(); }
    public List<String> cancel() { return original; }
}
```

- [ ] **步骤 4：先写配置 JSON 的失败测试，再实现平台无关 Codec**

测试要求空值回到默认 `columns=6`、卡片比例档位 `100`、图标档位 `60`，有效 JSON 必须保持组件顺序。Codec 使用 Android 平台内置 `org.json`，`LauncherPreferences` 只负责 SharedPreferences I/O。

- [ ] **步骤 5：运行核心状态测试**

运行：`./gradlew :app:testReleaseUnitTest --tests '*ReorderSessionTest' --tests '*LauncherPreferencesCodecTest'`

预期：全部 PASS。

- [ ] **步骤 6：提交桌面状态**

```bash
git add app/src/main/java/com/r19988088/tvlauncher/model app/src/main/java/com/r19988088/tvlauncher/data app/src/test
git commit -m "feat: add persistent ordered launcher state"
```

### 任务 3：用 TDD 实现 120 Hz 显示模式选择

**文件：**
- 创建：`app/src/test/java/com/r19988088/tvlauncher/display/DisplayModeSelectorTest.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/display/DisplayModeSelector.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/display/DisplayModeController.java`

- [ ] **步骤 1：先写同分辨率和上限行为的失败测试**

```java
@Test public void choosesHighestRefreshAtCurrentResolutionUpTo120() {
    List<Mode> modes = List.of(
        new Mode(1, 3840, 2160, 60f),
        new Mode(2, 3840, 2160, 120f),
        new Mode(3, 1920, 1080, 144f));
    assertEquals(2, DisplayModeSelector.choose(3840, 2160, modes).id());
}

@Test public void ignoresHigherRefreshAtDifferentResolution() {
    List<Mode> modes = List.of(
        new Mode(1, 3840, 2160, 60f),
        new Mode(2, 1920, 1080, 120f));
    assertEquals(1, DisplayModeSelector.choose(3840, 2160, modes).id());
}
```

- [ ] **步骤 2：运行测试确认缺失实现导致失败**

运行：`./gradlew :app:testReleaseUnitTest --tests '*DisplayModeSelectorTest'`

预期：FAIL，`DisplayModeSelector` 不存在。

- [ ] **步骤 3：实现纯 Java 选择器和平台适配器**

选择器只接受宽、高完全相同且 `refreshRate <= 120.01f` 的模式，并用刷新率降序选第一个。`DisplayModeController` 在 API 23+ 设置 `Window.setPreferredDisplayModeId()`；没有候选、候选等于当前模式或发生运行时异常时直接保持系统模式。

- [ ] **步骤 4：运行测试并提交**

运行：`./gradlew :app:testReleaseUnitTest --tests '*DisplayModeSelectorTest'`

预期：PASS。

```bash
git add app/src/main/java/com/r19988088/tvlauncher/display app/src/test/java/com/r19988088/tvlauncher/display
git commit -m "feat: prefer highest same-resolution refresh rate"
```

### 任务 4：实现应用发现和高效 Banner 管线

**文件：**
- 创建：`app/src/test/java/com/r19988088/tvlauncher/image/BannerCacheKeyTest.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/data/AppRepository.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/image/BannerCacheKey.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/image/BannerLoader.java`

- [ ] **步骤 1：先写缓存失效键的失败测试**

```java
@Test public void keyChangesForAppUpdateAndVisualSize() {
    BannerCacheKey base = new BannerCacheKey("pkg/.Main", 10L, 264, 148, 60);
    assertNotEquals(base, new BannerCacheKey("pkg/.Main", 11L, 264, 148, 60));
    assertNotEquals(base, new BannerCacheKey("pkg/.Main", 10L, 300, 168, 60));
    assertNotEquals(base, new BannerCacheKey("pkg/.Main", 10L, 264, 148, 70));
}
```

- [ ] **步骤 2：运行测试确认红灯，再实现不可变缓存键**

运行：`./gradlew :app:testReleaseUnitTest --tests '*BannerCacheKeyTest'`

预期：先 FAIL；实现 `equals`、`hashCode` 和稳定文件名后 PASS。

- [ ] **步骤 3：实现应用枚举和去重**

`AppRepository` 分别查询 `CATEGORY_LEANBACK_LAUNCHER` 和 `CATEGORY_LAUNCHER`，以扁平化 `ComponentName` 去重。只返回可导出且可启动的 Activity；按本地化标签排序用于设置列表，但桌面顺序完全由偏好列表控制。

- [ ] **步骤 4：实现有界图片加载器**

`BannerLoader` 使用最多两个后台线程，按 Activity Banner、Application Banner、居中方形图标的顺序生成目标尺寸 Bitmap。使用 `LruCache` 且 1 GB 档位上限 12 MB；磁盘派生缓存仅保存桌面应用，过期结果通过绑定代次和组件键丢弃。

- [ ] **步骤 5：运行测试和 lint**

运行：`./gradlew :app:testReleaseUnitTest :app:lintRelease`

预期：测试 PASS，lint 无 fatal。

- [ ] **步骤 6：提交图片管线**

```bash
git add app/src/main/java/com/r19988088/tvlauncher/data/AppRepository.java app/src/main/java/com/r19988088/tvlauncher/image app/src/test/java/com/r19988088/tvlauncher/image
git commit -m "feat: load APK banners with bounded caching"
```

### 任务 5：实现六列桌面、焦点动效和移动隐藏

**文件：**
- 创建：`app/src/main/java/com/r19988088/tvlauncher/ui/LauncherGridView.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/ui/AppCardView.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/ui/AppGridAdapter.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/LauncherActivity.java`
- 创建：`app/src/main/res/layout/activity_launcher.xml`
- 创建：`app/src/main/res/layout/item_app_card.xml`
- 创建：`app/src/main/res/layout/dialog_app_actions.xml`
- 创建：`app/src/main/res/drawable/card_background.xml`
- 创建：`app/src/main/res/drawable/card_placeholder.xml`
- 创建：`app/src/main/res/values/colors.xml`
- 创建：`app/src/main/res/values/dimens.xml`
- 修改：`app/src/main/res/values/strings.xml`

- [ ] **步骤 1：先写焦点视觉规格的纯 Java 失败测试**

测试 `CardVisualState.focused()` 返回 `scale=1.12`、`translationY=-6dp`、`elevation=24dp`、`labelAlpha=1`，`unfocused()` 返回 `scale=1`、`elevation=1dp`、`labelAlpha=0`。

- [ ] **步骤 2：运行红灯并实现最小视觉状态值对象**

运行：`./gradlew :app:testReleaseUnitTest --tests '*CardVisualStateTest'`

预期：先 FAIL，添加值对象后 PASS。

- [ ] **步骤 3：实现大圆角卡片和两卡片属性动画**

`AppCardView` 根据实测高度把圆角设为高度的 12%，只动画 `scaleX`、`scaleY`、`translationY`、`elevation` 和名称 alpha。动画时长 180 ms；新焦点事件先取消当前 animator，从现值继续。

- [ ] **步骤 4：实现网格与空桌面**

默认 6 列，图标区保留上方留白，`clipChildren=false`、`clipToPadding=false`。空列表只显示中央“按菜单键添加应用”，非空时隐藏提示并请求第一项焦点。非焦点名称 alpha 固定为 0。

- [ ] **步骤 5：实现启动、长按、移动和隐藏**

短按调用明确组件 Intent；长按确认键和鼠标长按打开“移动/隐藏”。移动模式用 `ReorderSession` 交换邻接位置，确认提交，返回取消；隐藏只从桌面列表删除。

- [ ] **步骤 6：运行 release 单元测试和 lint**

运行：`./gradlew :app:testReleaseUnitTest :app:lintRelease`

预期：全部 PASS，无 fatal lint。

- [ ] **步骤 7：提交桌面 UI**

```bash
git add app/src/main/java/com/r19988088/tvlauncher app/src/main/res
git commit -m "feat: add high-performance TV launcher grid"
```

### 任务 6：实现设置和静态壁纸

**文件：**
- 创建：`app/src/main/java/com/r19988088/tvlauncher/SettingsActivity.java`
- 创建：`app/src/main/res/layout/activity_settings.xml`
- 创建：`app/src/main/res/layout/item_setting_app.xml`
- 创建：`app/src/main/res/layout/item_setting_stepper.xml`
- 创建：`app/src/main/res/drawable-nodpi/default_wallpaper.webp`
- 修改：`app/src/main/AndroidManifest.xml`
- 修改：`app/src/main/res/values/strings.xml`

- [ ] **步骤 1：先写设置范围的失败测试**

测试列数被限制到 `4..8`，卡片尺寸限制到 `80..120`，居中图标尺寸限制到 `40..80`；非法值回到默认 `6/100/60`。

- [ ] **步骤 2：运行红灯并实现设置规范化**

运行：`./gradlew :app:testReleaseUnitTest --tests '*LauncherSettingsTest'`

预期：先 FAIL，最小实现后 PASS。

- [ ] **步骤 3：实现遥控器设置列表**

设置页列出全部可启动应用并用确认键切换添加状态；列数、卡片尺寸和图标尺寸使用离散步进控件。只在用户提交新值时保存，返回桌面后一次性重算网格。

- [ ] **步骤 4：添加许可清晰的静态风景壁纸和本地图片选择**

默认壁纸保存为最大 1920x1080 WebP。自定义壁纸通过 `ACTION_OPEN_DOCUMENT` 选择并持久化 URI 权限；解码失败立即回退默认壁纸。

- [ ] **步骤 5：运行完整 release 检查并提交**

运行：`./gradlew :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease`

预期：测试和 lint 通过；有 release 签名配置时生成一个 release APK。

```bash
git add app/src/main
git commit -m "feat: add launcher settings and wallpaper"
```

### 任务 7：补齐应用变更和性能证据采集

**文件：**
- 创建：`app/src/main/java/com/r19988088/tvlauncher/PackageChangedReceiver.java`
- 创建：`scripts/profile_launcher.sh`
- 修改：`app/src/main/AndroidManifest.xml`
- 创建：`README.md`

- [ ] **步骤 1：实现安装、卸载、升级后的增量核对**

Receiver 只记录包状态失效；桌面恢复时由 `AppRepository` 核对受影响组件。卸载移除条目，升级保留顺序并失效图片键，不启动 Service。

- [ ] **步骤 2：创建可复现的 ADB 性能脚本**

```bash
#!/usr/bin/env bash
set -euo pipefail
pkg=com.r19988088.tvlauncher
adb shell dumpsys gfxinfo "$pkg" reset
adb shell am force-stop "$pkg"
adb shell monkey -p "$pkg" 1 >/dev/null
adb shell dumpsys gfxinfo "$pkg" framestats > build/gfxinfo-framestats.txt
adb shell dumpsys meminfo "$pkg" > build/meminfo.txt
adb shell dumpsys display > build/display.txt
```

- [ ] **步骤 3：记录验收命令**

README 明确默认 HOME 设置、菜单键、长按移动/隐藏、签名不变要求、Xiaomi TV Box S 的 500/1000 次方向移动测试步骤，以及没有真实 120 Hz 显示模式时不得宣称 120 FPS。

- [ ] **步骤 4：运行 shell 语法、测试和 lint并提交**

运行：

```bash
bash -n scripts/profile_launcher.sh
./gradlew :app:testReleaseUnitTest :app:lintRelease
```

预期：shell 无输出，Gradle 检查全部通过。

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/r19988088/tvlauncher/PackageChangedReceiver.java scripts README.md
git commit -m "perf: add package refresh and device profiling"
```

### 任务 8：固定签名并发布可直接下载的最小 APK

**文件：**
- 修改：`.gitignore`
- 修改：`app/build.gradle`
- 创建：`.github/workflows/release.yml`
- 本地生成且忽略：`.release-signing/TVLauncher-release.jks`
- 本地生成且忽略：`.release-signing/keystore.properties`

- [ ] **步骤 1：生成唯一长期 release keystore 并在本地保留备份**

使用 `keytool -genkeypair` 生成 RSA 4096、有效期 100 年的 `tv_launcher` 别名。将 keystore 和属性文件放入已忽略的 `.release-signing/`，权限设为仅当前用户读取。

- [ ] **步骤 2：把同一份密钥写入仓库 Actions secrets**

设置 `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEY_ALIAS`、`ANDROID_KEYSTORE_PASSWORD` 和 `ANDROID_KEY_PASSWORD`。之后所有 release 只能使用这些 secrets；缺失时构建失败，不回退 debug 签名。

- [ ] **步骤 3：配置 release 签名与最小化**

`app/build.gradle` 从根目录 `keystore.properties` 读取密钥路径和密码，启用 v1/v2 签名、R8、资源收缩和 zipalign。release APK 文件名固定为 `TVLauncher-<version>.apk`。

- [ ] **步骤 4：创建只发布 APK 的 Action**

```yaml
on:
  push:
    tags: ['v*']
  workflow_dispatch:
    inputs:
      tag:
        required: true
        type: string

permissions:
  contents: write
```

工作流使用 JDK 17，重建 `keystore.properties`，运行 `testReleaseUnitTest lintRelease assembleRelease`，然后用 `gh release create` 或 `gh release upload --clobber` 把单个签名 release APK直接附加到 GitHub Release。不得调用 `upload-artifact`，不得构建或上传 debug APK，也不得打 ZIP。

- [ ] **步骤 5：本地验证签名、收缩和 APK 内容**

运行：

```bash
./gradlew clean :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
apksigner verify --verbose --print-certs app/build/outputs/apk/release/TVLauncher-0.1.0.apk
unzip -l app/build/outputs/apk/release/TVLauncher-0.1.0.apk | rg 'kotlin|lib/.*\.so' && exit 1 || true
```

预期：release 测试/lint/构建通过，v1/v2 签名有效，APK 不包含 Kotlin 运行库或 native `.so`。

- [ ] **步骤 6：提交 CI 与签名配置**

```bash
git add .gitignore app/build.gradle .github/workflows/release.yml
git commit -m "ci: publish signed release APK directly"
```

### 任务 9：最终验证、集成和首次发布

**文件：**
- 检查：全部已跟踪文件

- [ ] **步骤 1：执行干净 release 验证**

运行：

```bash
./gradlew clean :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
git status --short
```

预期：Gradle 全部成功；工作树无未跟踪源码或未提交改动。

- [ ] **步骤 2：检查 APK 体积和证书指纹**

运行：

```bash
ls -lh app/build/outputs/apk/release/TVLauncher-0.1.0.apk
apksigner verify --print-certs app/build/outputs/apk/release/TVLauncher-0.1.0.apk
```

记录 APK 字节数和 SHA-256 证书摘要，后续版本必须一致。

- [ ] **步骤 3：合并实现分支并推送仓库**

在主工作区将 `feature/android-tv-launcher` 快进合并到 `main`，设置远程为 `https://github.com/R19988088/TV_launcher.git` 并推送 `main`。

- [ ] **步骤 4：创建并推送首次版本标签**

```bash
git tag -a v0.1.0 -m "TV Launcher 0.1.0"
git push origin v0.1.0
```

- [ ] **步骤 5：等待 Action 完成并验证 Release 资产**

运行：

```bash
gh run watch --repo R19988088/TV_launcher --exit-status
gh release view v0.1.0 --repo R19988088/TV_launcher --json assets,url
```

预期：工作流成功；Release 只有一个可直接下载、扩展名为 `.apk` 的签名 release 资产。
