# 杀戮尖塔大肥鱼助手 / Big Fish Advisor

[![GitHub release](https://img.shields.io/github/v/release/cccppy/BigFishAdvisor)](https://github.com/cccppy/BigFishAdvisor/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

一个给《杀戮尖塔》新手的游戏内助手 Mod：可爱的「大肥鱼」会在你爬塔时冒泡，告诉你下一层走哪条路、这张牌要不要拿、这回合先打哪张，并解释原因。

**下载：[最新 Release](https://github.com/cccppy/BigFishAdvisor/releases/latest)**

> 重要：这是**启发式教练**，不是穷举求解器。它会根据当前血量、金币、牌组、敌人意图和公开社区思路给出推荐，但《杀戮尖塔》没有绝对唯一的最优解。请把建议当成「高手的思考过程」，而不是外挂式自动打牌。

## 功能

- **地图路线**：为当前幕计算一条推荐路线，用金色路径和编号标出，并说明为什么这样走（营火、商店、精英、血量、金币）。
- **选牌建议**：给三张奖励牌评分和排名，标出首选/备选/不推荐，并解释牌组缺什么、为什么这张牌适合；同时给出“拿还是跳过”的判断。
- **事件/问号（全部）**：开局 Neow 祝福、所有问号事件、神龛、图书馆、蛇等都会给出推荐项、每个选项的完整文本、评分、说明和理由；推荐选项用金框标出。未收录的 Mod 事件也会按选项文本给通用建议。
- **营火建议**：休息、锻造、挖掘、举铁、删牌、红钥匙中推荐一个；选锻造时会直接推荐升级哪张牌，并在选牌界面用金框标出；选删牌时会推荐删哪张，并在选牌界面用红框标出。已经升级过的牌不会再被推荐升级。
- **删牌建议**：营火抽烟/事件/商店删牌时，优先推荐诅咒、状态、基础打击/防御和重复牌，并解释原因。
- **商店决策**：进商店会直接说“买牌 / 买遗物 / 删牌 / 跳过”，并给出价格、剩余金币和理由。
- **药水建议**：面板会提示哪些药水现在喝、哪些留给精英/Boss；危险血量/0 能量/斩杀时会明确说“现在用”。紧凑气泡会把“先喝哪瓶 + 再打哪张”写在同一句里，不用展开也能看到。
- **选牌方向**：放牌堆顶/底、弃牌、消耗、拿回手牌、复制、变化、升级时，会高亮该选的牌并解释“下一抽/弃掉/消耗”的收益。
- **战斗建议**：高亮本回合推荐打出的手牌，标出推荐目标；解释斩杀线、格挡效率、易伤/虚弱、能力牌节奏和能量利用。
- **女仆口吻**：会轻轻叫“主人”，用白话解释“为什么打这张、对面这回合要干嘛”，不再用账本式条目堆砌。
- **逐怪说明**：每只敌人当前意图、被动、常见行动循环和应对都会分开写；不认识的名字会用社区外号（如乐嘉、时间老头、甜甜圈）。
- **流派阶段**：按“打基础 → 找方向 → 定核心 → 补强 → 收尾”判断现在该抓过渡牌还是核心牌；没有 2 个配合点时不建议裸拿核心，已经成型后才开始严格排斥跨体系牌。
- **流派取舍**：主流派 5 张以上后，跨体系普通牌会扣分；牌组 18 张以上时，跨体系首选会直接建议跳过，避免拿成杂派。
- **遗物联动**：战斗会说明当前遗物提供的能量/属性/计数（笔尖、苦无、手里剑等）和卡牌配合；Boss 遗物三选一会逐个比较效果、代价和牌组适配；商店遗物会按流派契合度加权。
- **Boss/精英预判**：地图直接显示本幕 Boss、可能精英和当前牌组能不能打；选牌会针对 Boss/精英加克制分（AOE、格挡、攻击、反能力等）。
- **第二幕检查**：格挡少于 7 张、缺 AOE、牌组过厚时会明确提醒；Barricade + Body Slam、Power Through、Impervious、Flame Barrier、Feel No Pain 等关键防御牌会按当前牌组大幅加权，而不是只按社区总梯度。
- **操作顺序**：力量/敏捷/复制/能量药水会结合“这回合能打出的牌”给出先喝还是等；金字塔/保留牌会提醒留牌；笔尖/苦无/手里剑会计数。
- **每一步都有说明**：战斗显示敌人意图/格挡/预计掉血/能量和备选；选牌列出三张牌的 Tier、评分和理由；路线显示完整路径与逐段拆解；事件列出每个选项的说明和评分；商店列出货架推荐；营火列出所有选项和升级备选。
- **绝不省略**：面板按内容自动增高；一整屏仍放不下时出现 `1/2`、`<<` 和 `>>` 翻页按钮，所有文字都能翻到，不会用省略号代替内容。
- **大肥鱼气泡**：所有提示都由大肥鱼说；支持的卡牌名会同时显示中文和英文，方便对照。
- **可拖动 / 可折叠**：拖动标题或气泡任意移动；默认只显示一句推荐，点 `+` 或标题展开完整理由，点 `−` 收起；位置会保存。
- **随时开关**：`F8` 隐藏/显示整个助手；配置文件也会保存位置和展开状态。有的键盘需要点击fn+f8。
- **无额外依赖**：只需要 Java 8、ModTheSpire 和本 Mod；不依赖 BaseMod/StSLib。

## 安装（Windows，发布包）

发布包 `BigFishAdvisor-1.1.0.zip` 里已经带好 ModTheSpire 3.6.3（MIT 协议）。

1. 解压到一个**非中文、非 Program Files** 的普通目录，例如 `D:\BigFishAdvisor`。
2. 右键 `install.ps1` → **使用 PowerShell 运行**。如果系统禁止脚本，打开 PowerShell 执行：
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\install.ps1
   ```
3. 安装脚本会自动寻找 Steam 版《杀戮尖塔》，把 ModTheSpire 和 Mod 放进游戏目录，并把本 Mod 勾选进 MTS 启动列表。
4. 安装完成后，在 Steam 里选择 **“Play with Mods”（带 Mod 游玩）**。它会直接打开本地的 ModTheSpire 选择器，不会再提示“没有安装 ModTheSpire”。
5. 确认勾选 **大肥鱼助手 Big Fish Advisor**，点 **Play**。
6. 进游戏后按 `F8` 开关；地图、选牌、事件、战斗时自动出现气泡。

安装脚本会把 Steam 用来启动“Play with Mods”的 `mts-launcher.jar` 换成一个小型本地启动器：它不再只查 Steam 创意工坊，而是直接启动游戏目录里的 `ModTheSpire.jar`。原文件备份为 `mts-launcher-original.jar`。

> 普通 **Play** 仍然是原版游戏；要带 Mod 请用 **Play with Mods**，或双击桌面快捷方式 / `MTS.cmd`。
> 如果 Steam 更新/验证文件后失效，重新运行一次 `install.ps1`。
> 想恢复原版启动：运行 `uninstall.ps1`。

如果自动定位失败，显式指定游戏目录：
```powershell
.\install.ps1 -GameDir "D:\steam\steamapps\common\SlayTheSpire"
```

卸载：运行 `uninstall.ps1`（只删本 Mod，不动 ModTheSpire 和其它 Mod）。

## 手动安装

1. 安装 **Java 8**（游戏自带 `jre` 也可以）。
2. 把 `BigFishAdvisor.jar` 放进游戏的 `mods` 文件夹。
3. 把 `ModTheSpire.jar`、`MTS.cmd` 放进游戏根目录。
4. 把 `MtsLocalLauncher.jar` 复制为 `mts-launcher.jar`（先备份原文件）。
5. 在 Steam 选择 **Play with Mods**，或在游戏目录双击 `MTS.cmd`。

这样 Steam 的 **Play with Mods** 会打开本地 ModTheSpire 选择器，而不是去创意工坊找 ModTheSpire。

## 已知限制

- 这是**启发式教练**，不保证每一步都是数学最优；高进阶、特殊遗物/事件、Mod 卡牌可能判断偏差。
- 目前只对原版四个角色的卡牌做了较细的评分；Mod 卡牌会走通用规则。
- 事件建议是通用规则 + 常见事件表；随机选项、Mod 事件可能只能给保守评价。
- 不会自动打牌、自动选牌或跳过动画。
- 推荐数字是估算值，实际以游戏内卡牌/意图为准。
- 如果启动时弹出 `0xc0000142`，这是 Windows 的 `javaw.exe` 启动失败，通常是启动器工作目录不对、Java 进程残留或安全软件拦截；请确认使用本包的 `MTS.cmd`（已自动 `cd` 到游戏目录并设置 1GB 内存）。仍失败时双击同目录的 `MTS-debug.cmd`，它会保留控制台并显示真实错误；也可以重启电脑后再试。

## 操作

| 操作 | 效果 |
|---|---|
| 拖动标题 / 紧凑气泡 | 把大肥鱼移到任意位置（位置会保存） |
| 拖动空闲/隐藏时的小鱼 | 小鱼也能单独移动，位置独立保存 |
| 点击紧凑气泡 / `+` | 展开完整理由 |
| 点击展开面板的 `−` | 收起到一句推荐 |
| `F8` | 隐藏 / 显示整个助手 |
| 鼠标悬停奖励牌 | 查看这张牌的单独理由和排名 |

## 它怎么判断

- **路线**：对地图做动态规划，枚举到 Boss 的合法路径；按血量、金币、牌组、幕数、精英风险、营火/商店位置打分。第一层按社区路线攻略改成“前 3 场弱怪 → 营火升级 → 精英/商店”的发育节奏。
- **选牌**：基础分来自 **SpireSpy 社区卡牌梯度**，与本项目原有分数按 65% / 35% 混合；再按当前牌组的攻击/防御/群体/抽牌/流派标签、重复牌、牌组厚度、幕数和角色流派调整。选牌面板会显示 `玩家 Tier X`（SpireSpy）以及本局修正后的分数。
- **流派阶段**：先判断牌组处于“基础/找方向/定核心/补强/收尾”哪一步。前期优先能立即用的伤害、格挡、AOE、抽牌；有 2 个以上配合点后才推荐核心；方向成型后才严格排斥跨体系牌。核心和配合点按角色分别识别：铁甲的力量/消耗/格挡/自伤，猎手的毒/小刀/弃牌/防御，机器的集中/充能球/0 费/能力，观者的姿态/保留/占卜/神性。
- **流派**：铁甲战士看力量/消耗/格挡/自伤；静默猎手看毒/小刀/弃牌/敏捷；故障机器人看集中/充能球/0 费；观者看姿态/保留/占卜/神性。每个角色的选牌、商店和卡牌选择都会参考当前主流派，避免毒流里硬塞小刀牌。
- **商店**：参考 Wiki 的商店价格与删牌价格；比较卡牌、遗物、删牌和跳过，金币不够或性价比低时会明确说“跳过攒钱”。
- **战斗**：估算每张可打牌的伤害/格挡、敌人本回合总伤害、斩杀线、易伤/虚弱收益、能力牌节奏和能量效率；用可解释的权重排序。
- **药水**：按当前血量、敌人意图、能量、是否精英/Boss，区分“现在喝”和“留给精英/Boss”；仙女瓶、烟雾弹、水果汁等有单独处理。
- **选牌方向**：识别放牌堆顶/底、弃牌、消耗、拿回手牌、复制、变化、升级等选择，并按“下一抽收益/弃废牌/保留核心”给建议。

更完整的社区依据、价格数据和设计取舍见 [GUIDES.md](GUIDES.md)。如果 `.md` 预览返回 HTTP 405，直接看 [GUIDES.html](GUIDES.html) 或 [GUIDES.docx](GUIDES.docx)。

所有这些权重都在 `src/bigfish/advisor/` 里，欢迎 PR 改进。

## 从源码构建

需要 JDK 8、游戏的 `desktop-1.0.jar` 和 `ModTheSpire.jar`。

```powershell
# Windows
.\build.ps1 -GameDir "D:\steam\steamapps\common\SlayTheSpire" -MtsJar ".\tools\ModTheSpire.jar"
```

```bash
# Linux / macOS
./build.sh /path/to/SlayTheSpire /path/to/ModTheSpire.jar
```

产物在 `dist/BigFishAdvisor.jar`。

## 在 VS Code 里开发

1. 用 VS Code 打开 **项目根目录**（不是 `D:\DSH-Workspace`）：
   ```powershell
   code D:\DSH-Workspace\BigFishAdvisor
   ```
2. 安装推荐扩展：VS Code 会提示安装 **Extension Pack for Java**、**PowerShell**、**Python**。至少需要 Java 扩展来补全/跳转。
3. 如果你没有 Java 17/21，只装了项目自带的 JDK 8：Java 扩展本身可能提示需要新 JDK 来启动语言服务器，按提示装一个即可；**编译任务仍然固定用 JDK 8**，不会改变 Mod 的 Java 8 兼容性。
4. `Ctrl+Shift+B` 运行 **Build BigFishAdvisor**，产物是 `dist/BigFishAdvisor.jar`。
5. 需要装进游戏时，运行任务 **Install into Slay the Spire**，然后完全退出游戏，用 Steam 的 **Play with Mods** 重开。
6. 想调试游戏内代码：把游戏目录的 `MTS-debug.cmd` 改成带 JDWP 的启动命令：
   ```bat
   "jre\bin\java.exe" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Xmx1g -jar "ModTheSpire.jar"
   ```
   运行它，然后在 VS Code 的 **Run and Debug** 选择 **Attach to Slay the Spire (JDWP 5005)**。
7. 游戏日志在 `D:\steam\steamapps\common\SlayTheSpire\sendToDevs\logs\SlayTheSpire.log`，改完先看日志有没有异常。

### 常见改动位置

| 想改什么 | 文件 |
|---|---|
| 卡牌评分/说明 | `resources/bigfish/cards.json`、`CardDatabase.java` |
| 选牌/跳过 | `CardRewardAdvisor.java` |
| 战斗出牌/目标 | `CombatAdvisor.java`、`EnemyAdvisor.java` |
| 路线/Boss/精英 | `MapAdvisor.java`、`ActAdvisor.java` |
| 事件/Neow | `EventAdvisor.java` |
| 药水顺序 | `PotionAdvisor.java` |
| 遗物/Boss 遗物 | `RelicAdvisor.java`、`RelicDatabase.java` |
| 面板/交互/拖动 | `Overlay.java`、`Ui.java` |
| 配置开关 | `Config.java` |

改完中文文案后，务必运行 **Validate cards/font/names**；如果新增了字体里没有的汉字，需要重新生成 `resources/bigfish/font.ttf` 和 `charset.txt`，否则游戏里会显示成方块：

```powershell
# 需要 pip install fonttools；--font 指向带完整汉字的 TTF
python scripts/build_font.py --font D:\DSH-Workspace\assets_src\font1.ttf
```

字体源文件不在仓库里（避免把完整字体重复打包）；换字体时用任意你有权再分发的 TTF，并保留 `OFL.txt` 等授权文件。

本地仓库已经关联 GitHub：`origin` 指向 `https://github.com/cccppy/BigFishAdvisor`。提交前先跑构建和资源校验，避免把 `dist/`、`release/`、`tools/` 提交进去（`.gitignore` 已排除）。

## 开源协议

代码以 **MIT** 发布，见 [LICENSE](LICENSE)。

`resources/bigfish/avatar.png`、`avatar_small.png` 是角色形象素材，**不包含在 MIT 代码授权内**；公开发布前请确认你拥有该形象的使用/再分发授权，或替换成自己的原创素材。卡牌名/数值来自《杀戮尖塔》，版权归 Mega Crit 所有；本 Mod 不包含游戏本体资源。

## 致谢

- [ModTheSpire](https://github.com/kiooeht/ModTheSpire) 提供加载与 patch API。
- 《杀戮尖塔》社区（中文/英文攻略、卡牌评价、路线讨论）提供了策略灵感；具体权重和文案是本项目独立实现的。
- 字体：[ZCOOL KuaiLe](https://github.com/google/fonts/tree/main/ofl/zcoolkuaile)，SIL Open Font License 1.1。
