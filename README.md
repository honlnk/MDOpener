# MD Opener

一个轻量、纯粹的 Android Markdown 查看器。装上之后，在任意文件管理器里点击 `.md` / `.markdown` / `.mdown` 文件，即可用本应用打开并查看渲染后的内容。

无联网权限、无广告、无后台行为——只做一件事：把 Markdown 漂亮地显示出来。

## 功能特性

- 📂 **系统级关联**：注册为 `.md / .markdown / .mdown` 文件的默认打开方式，支持 `content://` 与 `file://` 两种来源，对按扩展名 / 按 MIME 的文件管理器均做了兜底
- 🔗 **多入口**：支持从文件管理器「打开」（`ACTION_VIEW`）、接收「分享」（`ACTION_SEND`），以及从桌面图标启动查看「最近打开」
- 🎨 **主题切换**：跟随系统 / 浅色 / 深色
- 🔤 **可调字号**：12sp – 28sp
- 📏 **正文宽度**：320dp – 1100dp，适配手机与平板
- 📑 **目录（TOC）**：自动提取标题，点击快速跳转
- 🔍 **页内搜索**：实时高亮匹配，显示命中数量
- 🖼️ **相对图片解析**：支持 `![](./images/a.png)` 这类同级目录引用（file:// 与树形 DocumentsProvider 的 content:// 均可）
- 📄 **导出 PDF**：展开全部折叠内容，智能分页，矢量文字可选中
- 🕘 **最近文件**：自动记录打开历史，快速回到上次阅读处
- 🧩 **代码高亮**：GitHub 风格（浅 / 深双主题），基于 highlight.js

## 技术栈

- **Kotlin** + **Jetpack Compose**（Material 3）
- 渲染引擎：WebView + [marked](https://github.com/markedjs/marked) + [highlight.js](https://highlightjs.org/)
- 设置持久化：Jetpack DataStore (Preferences)
- 最低支持：Android 7.0（API 24）
- 目标 SDK：Android 14（API 34）

## 项目结构

```
app/src/main/
├── AndroidManifest.xml          # 文件关联 intent-filter
├── assets/                      # 前端渲染资源
│   ├── viewer.html              # 渲染模板
│   ├── marked.min.js
│   ├── highlight.min.js
│   ├── typewriter.css           # 正文排版
│   ├── hljs-github.css          # 代码高亮（浅色）
│   └── hljs-github-dark.css     # 代码高亮（深色）
└── java/com/honlnk/md_opener/app/
    ├── MainActivity.kt          # 入口，处理 VIEW/SEND Intent
    ├── MainViewModel.kt
    ├── MarkdownOpenerApp.kt
    ├── core/
    │   ├── MarkdownJsBridge.kt  # Kotlin ↔ JS 桥接
    │   ├── Store.kt             # DataStore 设置与最近文件
    │   ├── UriHelpers.kt        # Uri → 文件名 / 内容读取
    │   └── SiblingResolver.kt   # 相对图片路径解析
    ├── model/Models.kt
    └── ui/
        ├── AppRoot.kt           # 导航根
        ├── RecentScreen.kt      # 最近打开列表
        ├── ViewerScreen.kt      # 查看器（目录 / 搜索 / 导出 PDF）
        ├── SettingsScreen.kt    # 设置
        ├── components/MarkdownWebView.kt
        └── theme/Theme.kt
```

## 构建

> 项目已配置国内镜像（Gradle 走腾讯云、Maven 走阿里云），国内网络可直接同步。

```bash
./gradlew assembleRelease
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

要求：JDK 17+、Android SDK 34。

## 版本

当前版本 **1.0.2**（versionCode 3）。

## License

MIT
