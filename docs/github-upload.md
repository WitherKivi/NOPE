# NOPE 1.0.0 上传 GitHub：Windows 操作步骤

本次只准备本机交付文件，没有创建远程仓库或代你上传。源码目录是 `D:\nope\mobile-ime-offline`。教程以仓库名 `NOPE` 为例；命令中的 `YOUR_NAME`、姓名和邮箱需要换成自己的信息。

## 1. 认识交付文件

`dist/NOPE-输入法-1.0.0.apk` 是安装包；`dist/NOPE-输入法-1.0.0-source.zip` 是完整源码、构建脚本、词库、原生库、许可证和设计资料；`dist/NOPE-输入法-1.0.0-complete.zip` 是以上内容连同发布说明、核验报告和摘要的交付合集。源码不包含构建缓存、旧安装包、手机临时文件、学习数据或签名密钥。

GitHub 的 Code 页面存放解压后的源码文件。安装包、源码 ZIP 和 complete ZIP 放在 Releases 附件中。`.gitignore` 已排除整个 `dist/`、本机缓存、签名密钥和环境文件，`vendor/`、词库与许可证会保留。

## 2. 检查 Git

打开 PowerShell，运行：

```powershell
git --version
```

本机已有 Git，可直接继续。如果其他电脑提示找不到命令，从 [Git for Windows](https://gitforwindows.org/) 安装，保留 Git Credential Manager，安装后重新打开 PowerShell。

## 3. 在 GitHub 创建空仓库

登录 [GitHub](https://github.com/)，右上角 `+` → `New repository`，输入 `NOPE`，描述可填「Android 离线中日英输入法与 Code 键盘」。按自己的需要选择 Public 或 Private。创建时不要勾选自动添加 README、.gitignore 或许可证，因为源码已经包含这些文件。点击 `Create repository`，复制 HTTPS 地址，例如 `https://github.com/YOUR_NAME/NOPE.git`。

这些步骤对应 [GitHub 官方导入说明](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)。

## 4. 建立本机版本记录

在 PowerShell 中逐段执行。以下命令针对第一次上传、尚未初始化 Git 的当前工程。已有仓库时先运行 `git status` 和 `git remote -v` 查看状态，跳过初始化和重复添加远程地址。

```powershell
Set-Location 'D:\nope\mobile-ime-offline'
git init -b main
git config user.name '你的提交署名'
git config user.email '你的GitHub提交邮箱'
```

邮箱可使用 GitHub Settings → Emails 中的 noreply 提交邮箱。这里只配置这个仓库，不改其他项目。

接着暂存源码并查看文件列表：

```powershell
git add .
git status --short
```

列表应包含 `app/src/`、`docs/`、`scripts/`、`tests/`、`vendor/` 和根目录工程文件；不应包含 `dist/`、`target/`、`.gradle/`、`app/build/`、`.env`、`.keystore` 或 `.jks`。确认后提交：

```powershell
git commit -m 'Release NOPE 1.0.0'
```

源码内最大的词库约 70.3 MiB，小于 Git 的 100 MiB 单文件硬限制。首次推送出现“大于 50 MiB”的性能提示时，此词库仍可推送。本项目目前无需为了首次上传额外设置 Git LFS。网页直接上传的单文件上限为 25 MiB，因此不要用网页 Add file 上传整个源码。参见 [GitHub 大文件说明](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github)。

## 5. 上传源码

把下面地址中的 `YOUR_NAME` 换成自己的 GitHub 用户名；仓库名称也必须与刚创建的一致：

```powershell
git remote add origin https://github.com/YOUR_NAME/NOPE.git
git remote -v
git push -u origin main
```

首次推送可能由 Git Credential Manager 打开浏览器登录，按页面完成授权和双重验证。无需把 GitHub 密码或 token 写入任何工程文件。参见 [官方凭据说明](https://docs.github.com/en/get-started/git-basics/caching-your-github-credentials-in-git)。

推送完成后刷新 GitHub 仓库，应该能看到 README、源码目录与 LICENSE。若浏览器只有一个 ZIP 文件，说明上传了压缩包而非源码目录。

## 6. 创建 1.0.0 Release 并上传安装包

进入仓库右侧 `Releases` → `Draft a new release`：

1. 在 Tag 栏输入 `v1.0.0` 并选择创建新标签，Target 选 `main`。
2. Title 填 `NOPE 1.0.0`。
3. 将 `docs/release-notes-v1.0.0.md` 的正文复制到说明框。
4. 在附件区域上传 `dist/` 中的 1.0.0 APK、source.zip、SHA256SUMS.txt，以及 delivery.json / verification.json。需要方便整套下载时可同时上传 complete.zip。
5. 本版还缺重新连接手机后的最终界面验收，当前可勾选 `This is a pre-release`，或先点击 `Save draft` 保存草稿。手机验收完成后再发布或取消预发布标记；版本号仍是 1.0.0。
6. 决定公开发布时点击 `Publish release`。其他人可在 Releases → Assets 下载 APK。

附件上传完成前不要关闭页面。先保存草稿也可以继续补齐附件。操作依据 [GitHub Release 官方说明](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)。本地 APK 约 114 MiB，应作为 Release 附件，不要强制加入 Git 提交。

## 7. 以后更新

修改源码后，在同一目录运行：

```powershell
git status
git add .
git commit -m 'Describe the change'
git push
```

发布新 APK 时递增 `VERSION` 与 `app/build.gradle.kts` 中的 `versionCode`，重新测试、构建和核验，建立新的版本标签及 Release。已发布版本的附件保持可追溯。

## 8. 覆盖升级与签名

当前 APK 沿用之前的开发签名，可覆盖同一签名旧版。原签名文件保留在本机工程外，未进入源码和 GitHub 文件；请单独离线备份。以后构建覆盖升级包时设置 `NOPE_DEBUG_KEYSTORE` 指向原密钥。其他人克隆源码后生成的默认开发密钥通常不同，无法直接覆盖你已经安装的版本。

工程继续保留现有 GPL-3.0 LICENSE、上游完整源码、资源来源和第三方许可。上传时保留这些内容。

## 9. 常见提示

`remote origin already exists`：先运行 `git remote -v`，确认已有地址是否正确；需要更换时使用 `git remote set-url origin 正确地址`。

`rejected / fetch first`：通常是远端创建时生成了额外 README 或已有提交。不要直接强制推送。先确认远端内容；若只是刚创建的空项目，使用一个未初始化文件的新仓库最容易按本教程操作。

`file exceeds 100 MiB`：查看错误列出的文件。若是 `dist/*.apk` 或压缩包，表示被手动强制暂存或早于忽略规则进入提交历史。当前新仓库按默认 `git add .` 不会加入这些文件；修改历史前先保留本地文件并核对是哪次提交。

`Author identity unknown`：补充第 4 步的仓库署名与邮箱。

第一次上传包含离线词库，网络传输需要时间。源码文件多于 25 MiB 时，改用本教程的 Git 推送即可，不要为了上传而删除引擎资源。
