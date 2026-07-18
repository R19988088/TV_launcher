# 手动列表选择与实时浮动设置设计

## 目标

- 删除桌面卡片对子 View 系统焦点的依赖，使用唯一 `activePosition` 驱动选择、动画、启动和长按。
- 菜单键在桌面内打开浮动设置侧栏，桌面保持可见，所有布局调整实时预览并立即保存。
- 新增顶部空位 `0～5 行`，默认 `2 行`，每行按一张应用卡片的完整行高计算。
- 激活卡片使用轻微 overshoot 弹性放大，离开卡片平滑缩回；不引入动画依赖。

## 手动选择模型

`LauncherActivity` 持有唯一 `activePosition`。`GridView` 自身保持系统焦点，`AppCardView` 不再可聚焦，也不再用 `OnFocusChangeListener` 决定视觉状态。

方向键调用 `GridFocusNavigator` 计算目标，随后同步执行：

1. 旧卡片 `setActive(false, true)`；
2. 更新 `activePosition` 和 `GridView.setSelection()`；
3. 新卡片 `setActive(true, true)`；
4. 若目标尚未布局，下一帧对新出现的 View 应用激活态。

Adapter 保存 active position，任何新建或复用的卡片都在 `bind()` 时读取该值，因此滚动和复用不会丢失激活态。卡片列表只包含真实应用，不创建空占位。

确认键抬起时启动 active entry。确认键按住超过系统长按阈值时打开“移动/屏蔽”操作，抬起后不再启动应用。菜单和返回键保持原语义。

## 弹性动画

激活时图片从当前比例动画到 `1.10`，使用原生 `OvershootInterpolator`，张力保持克制；阴影和名称同步淡入。离开时使用现有平滑 PathInterpolator 回到 `1.0`，名称淡出。动画只作用于 ImageView 的 transform/elevation，不触发布局。

## 浮动设置

设置侧栏直接属于 `activity_launcher.xml`，位于桌面右侧，宽度约屏幕的 34%，背景为半透明深灰面板；不是新 Activity、Dialog 或嵌套卡片。打开时面板从右侧淡入/平移，关闭时反向退出。

面板包含：

- 每行数量 `4～8`；
- 卡片尺寸 `80%～120%`；
- 图标尺寸 `40%～80%`；
- 顶部空位 `0～5 行`，默认 2；
- 选择静态壁纸；
- 可滚动应用勾选列表。

设置控件获得遥控器焦点时，桌面卡片仅保留 active 视觉，不处理方向键。每次 `− / +` 或应用勾选后直接更新 `LauncherState`、保存偏好，并调用桌面现有布局/条目更新路径，面板不关闭。

关闭面板后，焦点回到 GridView，active position 按 component id 恢复。

## 顶部空位计算

`LauncherSettings` 新增 `topBlankRows`，范围 0～5，默认 2；codec 缺少字段时使用默认值，保持旧版本配置兼容。

`GridMetrics` 使用：

```text
rowHeight = itemTopInset + cardHeight + labelGap + labelHeight + itemBottomInset
topPadding = topBlankRows * (rowHeight + verticalSpacing)
```

若该值会导致首行完全超出视口，则限制到仍能显示一整行卡片的最大 padding，避免再次产生屏外可选条目。

## 代码边界

- `LauncherSettings` / `LauncherStateCodec`：顶部空位持久化与兼容。
- `GridMetrics`：按卡片完整行高计算顶部 padding。
- `AppCardView`：显式 active 视觉和弹性动画。
- `AppGridAdapter`：active position 与复用绑定。
- `LauncherActivity`：手动导航、确认/长按、浮层生命周期、实时预览和应用发现。
- `SettingsActivity`：从 Manifest 移除；实现迁入浮层后删除，避免保留两套设置状态。

## 性能

- 不增加 RecyclerView、SpringAnimation 或第三方依赖。
- 方向键只更新两张可见卡片，不调用全列表 `notifyDataSetChanged()`。
- 布局设置变化才重新绑定可见网格；普通移动只使用 transform 动画。
- 保持 Android 6 / API 23 下限和现有 Banner 缓存。

## 验证

- 单元测试覆盖 active position 边界、顶部空位默认/范围/旧配置迁移、不同卡片尺寸的 top padding 上限。
- Actions 执行 unit test、lint、R8 和签名 Release 构建。
- Xiaomi TV Box S 验证 `1→2→1` 每次单击切换，旧卡同步缩回；连续左右、上下无空状态。
- 设置浮层中逐项调整并截图确认桌面实时变化，关闭后 active 卡片保持。
- `gfxinfo` 检查方向动画卡顿帧，logcat 无 FATAL/ANR。
