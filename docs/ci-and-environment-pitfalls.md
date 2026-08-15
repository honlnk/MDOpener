# CI 构建与开发环境问题清单

> 来源：2026-08-15 ~ 08-16 修复四项文件关联 bug 并发布 v1.0.3 过程中的实际踩坑记录。
>
> 分两部分：**待办**（未解决，有空排查）与 **已解决**（原因和解法备查，避免重蹈）。

---

## 🟡 待办（有空再排查）

### 1. gh CLI 的 OAuth token 缺 `workflow` 权限

**现象**：HTTPS 方式推送包含 `.github/workflows/` 改动的提交时，GitHub 拒收：

```
refusing to allow an OAuth App to create or update workflow
`.github/workflows/build.yml` without `workflow` scope
```

**当前绕行**：改用 SSH 走 443 端口推送（见第 2 条），不影响使用。

**修法**（一次性，需浏览器交互）：

```bash
gh auth refresh -h github.com -s workflow
```

### 2. 本机网络封锁 SSH 22 端口

**现象**：`git fetch/push` 走 github.com:22 直接断连，报错 IP 落在 `198.18.x.x`（Fake-IP / 代理网段典型特征）。`ssh.github.com:443` 可正常认证。

**当前绕行**：推送时显式用 `ssh://git@ssh.github.com:443/honlnk/MDOpener.git`。

**一劳永逸**（给 `~/.ssh/config` 追加，之后 origin 原样可用）：

```
Host github.com
  HostName ssh.github.com
  Port 443
  User git
```

### 3. CI 里 Actions 版本弃用告警

**现象**：构建日志有两条 warning，不影响产物：

- `actions/setup-java@v4` 已停更，建议升 `@v5`
- `checkout@v4 / setup-java@v4 / upload-artifact@v4 / setup-gradle@v4` 基于 Node 20，已被强制跑在 Node 24

**修法**：`.github/workflows/build.yml` 里把各 action 版本号 +1，观察一次构建即可。

### 4. 本机无构建环境（JDK / Android SDK / gradlew 均缺失）

**现象**：仓库里没有 `gradlew` 脚本（只有 `gradle/wrapper/gradle-wrapper.properties`），本机也没装 JDK、Android SDK 和 gradle，任何本地编译都做不了，只能靠 CI 验证。

**如需本地构建**：安装 JDK 17 + Android SDK（compileSdk 34），然后任选：

```bash
# 方案 A：装任意 gradle 8.9 后在项目根目录生成 wrapper 并提交
gradle wrapper --gradle-version 8.9

# 方案 B：不生成 wrapper，直接用系统 gradle 构建（CI 目前就是这种用法）
gradle assembleRelease
```

**临时判断代码正确性的最低成本手段**：`xmllint --noout` 校验 XML、push 小分支看 CI。

### 5. （可选）发版自动化

当前发版是手动三步：改版本号 → 合 main 触发 CI → `gh release create` 挂 APK。

想省掉最后一步，可给 workflow 增加 `on: push: tags: ['v*']` 触发 + `softprops/action-gh-release` 步骤，打 tag 即自动建 Release 并上传 `app-release.apk`。做的时候记得复用现有 secrets 签名步骤。

---

## ✅ 已解决（备查）

### 发布签名固定化（v1.0.3 起生效）

**问题**：release 原用 `signingConfigs.debug` 签名，CI 临时机器每次现生成 debug keystore，导致每个构建产物签名都不同，覆盖安装报「软件包与现有软件包存在冲突」。

**方案**：

- `app/build.gradle`：`KEYSTORE_FILE` 环境变量存在时用 release 签名配置（PKCS12），否则回退 debug（本地构建行为不变）
- `.github/workflows/build.yml`：`Decode signing keystore` 步骤从 `ANDROID_KEYSTORE_BASE64` 解码后注入
- Secrets 三件套：`ANDROID_KEYSTORE_BASE64` / `ANDROID_KEYSTORE_PASSWORD` / `ANDROID_KEY_ALIAS`

**⚠️ 密钥保管**（丢失则永远无法覆盖安装，只能卸载重装）：

- 本地：`~/.keystores/MDOpener/`（keystore + 私钥 + `KEYINFO.txt` 密码，权限 600）
- GitHub Secrets 内有一份，可重新导出
- **建议再做一份冷备份（网盘 / 移动硬盘）**

### HTTPS 推送的凭证

SSH 被封时的替代通道：`gh auth setup-git` 让 gh 作为 git 的 HTTPS 凭证助手（局限：见待办 #1，推不了 workflow 文件）。

---

## 发版流程速查（当前手动版）

```bash
# 1. 改 app/build.gradle 的 versionCode / versionName，提交
# 2. 合并到 main 并推送（触发 CI）
git push ssh://git@ssh.github.com:443/honlnk/MDOpener.git dev:dev
git push ssh://git@ssh.github.com:443/honlnk/MDOpener.git dev:main
# 3. 等构建 & 下载校验
gh run watch --repo honlnk/MDOpener <run-id> --exit-status
gh run download <run-id> --repo honlnk/MDOpener --name md-opener-apk --dir ~/Downloads/MDOpener-apk
# 4. 打 tag + 发 Release（附件直接传 APK，不会被打包成 zip）
git tag -a v<x.y.z> -m "..." && git push ssh://git@ssh.github.com:443/honlnk/MDOpener.git v<x.y.z>
gh release create v<x.y.z> <apk路径> --repo honlnk/MDOpener --title "MD Opener v<x.y.z>" --notes "..."
```

---

*状态：待办 5 项（1 高频 / 2 低频 / 2 可选），均不阻塞正常发版。*
