# Apple TV 网格与柔化壁纸实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框跟踪。

**目标：** 修复方角 Banner 和不可见网格位置，并以低内存方式实现接近参考图的六列四行布局与柔化壁纸。

**架构：** 新增纯 Java `GridMetrics` 负责可测试的屏幕几何；`LauncherActivity` 只把计算结果应用到现有 `GridView`。`BannerLoader` 在缓存生成阶段裁切圆角并升级缓存版本。壁纸沿用现有后台执行器，解码后缩成微型 Bitmap，由现有硬件加速 `ImageView` 放大。

**技术栈：** Java 17、Android View、JUnit 4、GitHub Actions、API 23-36。

**约束：** 不在本地运行 Gradle；红灯、绿灯、lint、R8 和签名验证全部由 GitHub Actions 完成。

---

### 任务 1：定义网格几何

**文件：**
- 创建：`app/src/test/java/com/r19988088/tvlauncher/ui/GridMetricsTest.java`
- 创建：`app/src/main/java/com/r19988088/tvlauncher/ui/GridMetrics.java`
- 修改：`app/src/main/java/com/r19988088/tvlauncher/LauncherActivity.java`
- 修改：`app/src/main/java/com/r19988088/tvlauncher/ui/AppGridAdapter.java`
- 修改：`app/src/main/java/com/r19988088/tvlauncher/ui/AppCardView.java`

- [ ] 先写 1920x1080、六列、density=2 的失败测试，断言边距 77px、间距 48px、轨道宽 254px、卡片高 142px、顶部 padding 72px、纵向间距 20px。
- [ ] 推送测试并确认 GitHub Actions 因 `GridMetrics` 不存在失败。
- [ ] 实现只做整数计算的不可变 `GridMetrics`。
- [ ] `LauncherActivity` 设置显式 column width、`STRETCH_SPACING`、padding 和 spacing。
- [ ] Adapter 把轨道宽与卡片宽分别传给 `AppCardView`；根 View 使用轨道宽，图片在轨道内居中。

### 任务 2：保证像素圆角与缓存失效

**文件：**
- 修改：`app/src/test/java/com/r19988088/tvlauncher/image/BannerCacheKeyTest.java`
- 修改：`app/src/main/java/com/r19988088/tvlauncher/image/BannerCacheKey.java`
- 修改：`app/src/main/java/com/r19988088/tvlauncher/image/BannerLoader.java`

- [ ] 先写缓存渲染版本变化会改变文件名的失败测试。
- [ ] 缓存原始字符串加入固定渲染版本。
- [ ] `BannerLoader.render()` 在绘制 Drawable 前用高度 12% 的圆角 `Path` 裁切 Bitmap Canvas。
- [ ] 保留 `ImageView` outline，确保焦点 elevation 阴影继续使用同一圆角。

### 任务 3：一次性柔化壁纸

**文件：**
- 修改：`app/src/main/res/layout/activity_launcher.xml`
- 修改：`app/src/main/java/com/r19988088/tvlauncher/LauncherActivity.java`

- [ ] 删除 XML 的完整默认壁纸同步 `src`。
- [ ] 默认和自定义壁纸统一在 `repositoryExecutor` 解码，解码长边不超过约 320px。
- [ ] 把解码结果双线性缩至长边 160px，回收中间 Bitmap；主线程只替换最终 Bitmap。
- [ ] 自定义壁纸失败时回退内置壁纸，生命周期代次失效时回收过期结果。

### 任务 4：发布和实机验收

**文件：**
- 修改：`app/build.gradle`

- [ ] 版本升至 `0.1.2`、`versionCode 3`。
- [ ] 推送分支并等待 GitHub Actions 完成 release 单测、lint、R8、签名验证。
- [ ] 合并 `main`、创建 `v0.1.2` 标签并确认 Release 只有一个 APK。
- [ ] `adb install -r` 覆盖安装，保留签名和桌面配置。
- [ ] 在盒子上验证 HOME、菜单设置往返、四行可见、方向键真实卡片导航和无崩溃日志。
- [ ] 采集 1920x1080 截图，与参考图检查边距、圆角和壁纸柔化。
