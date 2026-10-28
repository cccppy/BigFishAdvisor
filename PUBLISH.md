# 发布到 GitHub

已发布：<https://github.com/cccppy/BigFishAdvisor>  
Release：<https://github.com/cccppy/BigFishAdvisor/releases/tag/v1.1.0>

这个目录已经是一个可公开的开源仓库结构；`release/BigFishAdvisor-1.1.0-source.zip` 是干净源码包。

## 建议步骤

1. 在 GitHub 新建仓库 `BigFishAdvisor`，不要勾选自动生成 README。
2. 解压源码包，把里面的文件上传到仓库根目录：
   ```
   src/  resources/  scripts/  launcher/  .github/
   build.ps1  build.sh  install.ps1  uninstall.ps1  make-release.ps1
   README.md  LICENSE  OFL.txt  CONTRIBUTING.md  CHANGELOG.md
   THIRD_PARTY_NOTICES.md  ARTWORK_LICENSE.md  ModTheSpire-LICENSE.txt
   .gitignore
   ```
3. 打 tag `v1.0.0`，在 Releases 上传：
   - `release/BigFishAdvisor-1.0.0.zip`（玩家直接安装）
   - 可选：`release/BigFishAdvisor-1.0.0-source.zip`
4. 在 Release 说明中写清楚：
   - 需要正版《杀戮尖塔》；
   - 解压后运行 `install.ps1`，然后在 Steam 选择 **Play with Mods**；
   - 安装器替换 Workshop 专用的 `mts-launcher.jar`，改为启动本地 `ModTheSpire.jar`；
   - 原文件备份为 `mts-launcher-original.jar`，卸载会恢复；
   - `F8` 开关；
   - 这是启发式建议，不保证最优。

## 版本号

版本号的唯一来源是 `resources/ModTheSpire.json` 的 `version`。
升级时同时修改 `src/bigfish/advisor/BigFishAdvisor.java` 里的 `VERSION`，然后重新构建、打 tag。

## 不要上传

- `desktop-1.0.jar`（游戏本体，版权属于 Mega Crit）
- `tools/`（本地 JDK、ModTheSpire 缓存）
- `build/`、`dist/`、`release/` 的中间产物
- `logs/`、`mtsdata/`、`test/`
- 任何 Steam 账号、存档、日志中的个人信息
