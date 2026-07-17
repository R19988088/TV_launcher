# 焦点导航与深灰背景实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 消除小米盒子上的容器焦点丢失，并让不同列数及少量图标始终对称排布，同时使用小尺寸深灰朦胧默认背景。

**架构：** 用纯 Java `GridFocusNavigator` 计算方向键目标，`LauncherActivity` 消费四向键并显式把焦点交给真实卡片。`GridMetrics` 同时接收设置列数和条目数，保持卡片轨道尺寸不变，只对稀疏单行重新计算对称 padding；默认背景换成 160x90 位图。

**技术栈：** Android Java、原生 GridView、JUnit 4、GitHub Actions。

---

## 文件结构

- 创建 `app/src/main/java/com/r19988088/tvlauncher/ui/GridFocusNavigator.java`：纯 Java 四向焦点目标计算。
- 创建 `app/src/test/java/com/r19988088/tvlauncher/ui/GridFocusNavigatorTest.java`：焦点边界和不完整末行回归测试。
- 修改 `app/src/main/java/com/r19988088/tvlauncher/ui/GridMetrics.java`：完整行和稀疏单行的对称几何。
- 修改 `app/src/test/java/com/r19988088/tvlauncher/ui/GridMetricsTest.java`：5 列及 3 图标居中测试。
- 修改 `app/src/main/java/com/r19988088/tvlauncher/LauncherActivity.java`：方向键接管、实际列数和焦点恢复。
- 替换 `app/src/main/res/drawable-nodpi/default_wallpaper.webp`：160x90 深灰朦胧背景。
- 修改 `app/build.gradle`：发布版本更新为 0.1.3。

### 任务 1：建立焦点与网格红灯测试

- [ ] **步骤 1：创建焦点导航失败测试**

测试 `GridFocusNavigator.move(current, itemCount, columns, direction)`：第一项向左保持 0、末项向右保持末项、第二项向左到 0、不完整末行向下保持、向上回到对应列。

- [ ] **步骤 2：扩展网格几何失败测试**

将 `GridMetrics.calculate` 签名扩展为 `(width, height, configuredColumns, itemCount, cardScale, density)`，断言：

```java
GridMetrics five = GridMetrics.calculate(1920, 1080, 5, 5, 100, 2f);
assertEquals(five.horizontalPadding(),
        (1920 - five.columnWidth() * 5 - five.horizontalSpacing() * 4) / 2);
assertEquals(5, five.displayColumns());

GridMetrics sparse = GridMetrics.calculate(1920, 1080, 6, 3, 100, 2f);
assertEquals(3, sparse.displayColumns());
assertEquals(531, sparse.horizontalPadding());
assertEquals(254, sparse.columnWidth());
```

- [ ] **步骤 3：推送红灯并验证 Actions 正确失败**

运行：提交并推送 `codex/focus-navigation-gray-bg`，观察 `gh run watch <run-id> --exit-status`。

预期：`GridFocusNavigator` 不存在且 `GridMetrics.calculate` 签名不匹配；不得出现签名配置或工具链错误。

### 任务 2：实现确定性方向导航

- [ ] **步骤 1：实现最小纯 Java 导航器**

`GridFocusNavigator` 定义 `LEFT/RIGHT/UP/DOWN` 常量。`move` 只在目标为真实条目且左右不跨行时返回新索引，否则返回当前索引。

- [ ] **步骤 2：在 LauncherActivity 消费四向键**

在移动模式分支之后、普通按键交给 `super` 之前拦截方向键：所有 ACTION_UP 均消费；ACTION_DOWN 调用 `GridFocusNavigator.move`，然后 `setSelection(target)` 并对 `visibleCardAt(target)` 调用 `requestFocus()`。当前索引优先用 `getPositionForView(findFocus())`，无卡片焦点时回退到 selected position 和 0。

- [ ] **步骤 3：保持中心键和长按行为不变**

方向键之外继续走现有 `dispatchKeyEvent`，不拦截 DPAD_CENTER、ENTER、MENU、SETTINGS 和 BACK。

### 任务 3：实现对称网格几何

- [ ] **步骤 1：扩展 GridMetrics**

完整配置列数先计算 4% 目标外边距、2.5% 横间距和固定轨道宽。`displayColumns = min(configuredColumns, itemCount)`；最终 padding 使用：

```java
int groupWidth = columnWidth * displayColumns
        + horizontalSpacing * (displayColumns - 1);
int horizontalPadding = Math.max(0, (viewportWidth - groupWidth) / 2);
```

并新增 `displayColumns()` getter。

- [ ] **步骤 2：应用实际展示列数**

`LauncherActivity.configureGrid()` 传入 `entries.size()`，调用 `gridView.setNumColumns(metrics.displayColumns())`，并使用 `GridView.NO_STRETCH`，防止系统二次拉伸破坏左右对称。

- [ ] **步骤 3：导航仍使用实际展示列数**

方向目标计算使用 `min(settings.columns(), entries.size())`，保证三图标居中时左右移动顺序为 0、1、2，没有空单元。

### 任务 4：替换默认背景并发布

- [ ] **步骤 1：生成 160x90 深灰朦胧背景**

使用本机图像工具生成低对比度深灰噪声/柔化位图并覆盖 `default_wallpaper.webp`；图像不得包含可辨识景物，文件保持数 KB 量级。

- [ ] **步骤 2：避免默认背景重复缩放**

默认资源已经是 160x90，`decodeDefaultWallpaper()` 直接解码；自定义壁纸继续使用现有采样和 `softenWallpaper()`。

- [ ] **步骤 3：版本更新**

在 `app/build.gradle` 设置 `versionCode 4`、`versionName '0.1.3'`。

- [ ] **步骤 4：推送绿灯并等待 Actions**

运行：`gh run watch <run-id> --exit-status`。

预期：单元测试、lint、R8、签名 Release 构建全部成功。

- [ ] **步骤 5：发布并安装实测**

合并到 `main`，创建 `v0.1.3`，等待 Release APK；下载后执行 `adb install -r TVLauncher-0.1.3.apk`。

- [ ] **步骤 6：盒子验证**

连续执行左、右、上、下并用 UI Automator 确认焦点始终位于真实卡片；验证三卡片左右边距相等、菜单设置返回正常、日志无 FATAL/ANR；使用 `dumpsys gfxinfo` 检查卡顿帧。
