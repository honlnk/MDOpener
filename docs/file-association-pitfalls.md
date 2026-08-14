# 文件关联已知坑点与待办

> 来源：2026-08-13 对中英文社区的调研（StackOverflow 经典问答、Gadgetbridge 等开源项目的 manifest 实践、微信/QQ 文件导入踩坑实录），结合本项目代码逐项核对得出。
>
> 本文档记录 4 个待办项（2 高 / 1 中 / 1 低），以及已确认避开和接受的坑，供后续开发参考。**已于 2026-08-15 全部修复**（见提交 6645478 / f7999fa / 90669f5）。

---

## 🔴 高优先级

### 1. pathPattern 多点路径失配

**现象**：Android `PatternMatcher` 中 `.*\\.md` 的 `.*` **不是正则意义上的贪婪匹配**。文件路径在扩展名之前若包含其他点号，匹配直接失败，系统不会唤起本应用。

**典型触发路径**：

```
/storage/emulated/0/Download/v1.5/读书笔记.md           ← 目录名带点
/storage/emulated/0/obsidian-v1.2/note.md               ← 版本号目录
content://com.tencent.mm.external.fileprovider/.../a.md  ← 微信/QQ FileProvider 路径
```

**现状**：`app/src/main/AndroidManifest.xml:29-38` 每种扩展名只写了一条 `.*\.xxx` 模式，会中招。

**修法**（Gadgetbridge 等项目的通行做法）：按路径中可能出现的点号数量枚举多条 pattern：

```xml
<data android:scheme="content" android:host="*" android:mimeType="*/*" android:pathPattern=".*\\.md" />
<data android:scheme="content" android:host="*" android:mimeType="*/*" android:pathPattern=".*\\..*\\.md" />
<data android:scheme="content" android:host="*" android:mimeType="*/*" android:pathPattern=".*\\..*\\..*\\.md" />
<data android:scheme="content" android:host="*" android:mimeType="*/*" android:pathPattern=".*\\..*\\..*\\..*\\.md" />
```

`.markdown` / `.mdown` / `file://` scheme 同理，需要同倍数展开。

**预计工作量**：只改 manifest，约 10 分钟。

---

### 2. GBK 编码文件乱码

**现象**：`app/src/main/java/com/honlnk/md_opener/app/core/UriHelpers.kt:12` 使用 `bufferedReader()`，即系统默认 UTF-8。国内用户从 Windows（中文系统记事本默认 ANSI/GBK）传来的 `.md` 文件打开会满屏乱码。这是文本查看器类工具被吐槽最多的问题。

**修法**：

- 读取原始字节流；
- 先嗅探 BOM（UTF-8/UTF-16）；
- 无 BOM 则按 UTF-8 严格解码，失败则回退 GBK/GB18030；
- 可选：引入 juniversalchardet 做编码探测（会增加体积，权衡）。

**预计工作量**：改 `UriReader.read()`，约半小时。

---

## 🟡 中优先级

### 3. 缺少 CATEGORY_BROWSABLE

**现象**：从浏览器下载 `.md` 后直接点开这一场景，部分浏览器/下载器发起 intent 时只查询带 `BROWSABLE` 的 Activity。当前 manifest 只声明了 `CATEGORY_DEFAULT`，可能漏掉该入口。

**修法**：在 VIEW 相关的 intent-filter 中加一行：

```xml
<category android:name="android.intent.category.BROWSABLE" />
```

**预计工作量**：一行。

---

## 🟢 低优先级

### 4. 三个小改进点（视需求取舍）

| 事项 | 说明 | 取舍建议 |
|---|---|---|
| `launchMode="singleTask"` | 当前 MainActivity 为 standard，连续打开不同 md 会叠加多个 Activity 实例。查看器语义下 singleTask + `onNewIntent`（已有实现）更合适 | 建议加 |
| `ACTION_SEND` 未处理 `EXTRA_TEXT` | `MainActivity.kt:36-42` 只读 `EXTRA_STREAM`。部分 App（如便签类）分享的是纯文本 `EXTRA_TEXT`，这类分享进不来 | 可将 EXTRA_TEXT 内容作为内存文档直接展示 |
| `application/octet-stream` 兜底的选择器污染 | 注册该 MIME 后，打开任意未知二进制文件时本应用都会出现在「打开方式」列表，且可能被用户误设为默认 | 覆盖率与打扰度的权衡，可保留；介意则移除 |

---

## 附：调研中已确认避开的坑（无需处理）

| 社区坑点 | 本项目实现 |
|---|---|
| 把 `content://` 当文件路径、`Uri.getPath()` 转 `File` 导致崩溃 | `UriHelpers.kt:11` 用 `ContentResolver.openInputStream()`，正确 |
| `content://` URI 不带扩展名（如 MediaStore `.../file/12345`）导致 pathPattern 失效 | manifest 有纯 MIME 兜底 filter（第 40-47 行） |
| 只注册 `file://` 漏掉 `content://` | 双 scheme 均已注册 |
| Android 12+ 未声明 `android:exported` 导致安装失败 | 已声明 `exported="true"` |
| LAUNCHER 与 VIEW filter 混放同一 Activity | 未造成实际问题（桌面图标正常，onNewIntent 已处理） |
| 临时 URI 权限时效 | 打开时立即读入内存，正常路径无影响；进程被杀后恢复再分享可能失败，概率低，接受 |

---

*状态：4 项待办已于 2026-08-15 全部修复。*
