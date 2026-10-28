# 攻略依据与实现说明

> 如果 `.md` 在预览器里返回 HTTP 405，直接打开同目录的 [GUIDES.html](GUIDES.html) 或 [GUIDES.docx](GUIDES.docx)。

这份文档记录大肥鱼助手参考了哪些社区攻略、哪些结论被写进了代码，以及哪些地方是“上下文修正”，不是硬抄 tier list。

> 社区梯度是主观的，且更偏向高进阶/熟练玩家。助手把社区梯度当作**基础分**，再按当前血量、金币、幕数、牌组厚度和流派做修正。新手局不应该因为某张牌是 C 级就永远不拿，也不应该因为 S 级就无视费用和当前牌组。

## 1. 卡牌梯度：SpireSpy

参考页面：

- [Ironclad Card Tier List](https://maybelatergames.co.uk/tools/slaythespire/tierlist/cards/ironclad/)
- [Silent Card Tier List](https://maybelatergames.co.uk/tools/slaythespire/tierlist/cards/silent/)
- [Defect Card Tier List](https://maybelatergames.co.uk/tools/slaythespire/tierlist/cards/defect/)
- [Watcher Card Tier List](https://maybelatergames.co.uk/tools/slaythespire/tierlist/cards/watcher/)
- [Colorless Card Tier List](https://maybelatergames.co.uk/tools/slaythespire/tierlist/cards/colorless/)

SpireSpy 使用 `S/A/B/C/D/F` 梯度，部分卡还有 `+/-`。本项目把它映射成 2–10 分，并和原有分数按 **65% 社区 / 35% 原有** 混合。这样既吸收社区共识，又不会让单张 tier list 的极端评价完全支配新手局。

例子（数值会随版本微调）：

| 卡 | 社区梯度 | 混合后基础分 |
|---|---|---|
| Feed | S | 9.2 |
| Immolate | S | 9.2 |
| Adrenaline | S | 9.5 |
| Wraith Form | S | 9.5 |
| Seek | S | 9.5 |
| Bash | C | 7.0 |
| Biased Cognition | A | 8.8 |
| Blasphemy | S | 8.8 |

Bash 这类牌在总梯度里不高，但第一层前几场战斗很强，所以选牌逻辑会额外给“第 1 幕前 6 层 + 攻击牌”加成；它不是无脑按 C 级丢掉。

### 流派识别

助手从卡牌标签统计当前牌组的主流派，并按角色使用不同标签：

- **铁甲战士**：力量（`str`）、消耗（`exhaust`）、格挡/防战（`block`）、自伤（`selfdmg`）
- **静默猎手**：毒（`poison`）、小刀（`shiv`）、弃牌（`discard`）、敏捷/防御（`block`）
- **故障机器人**：集中/冰球（`focus`）、充能球（`orb`）、0 费/爪击（`zero`）、能力（`power`）
- **观者**：姿态（`stance`）、保留（`retain`）、占卜（`scry`）、神性（`mantra`）

选牌时：

- 和当前主流派同标签的牌 +0.45；
- 毒流里硬拿小刀牌、刀流里硬拿毒牌会扣分；
- 集中冰体系里硬拿 0 费爪击会扣分；
- 重复牌、牌组过厚、第 3 幕普通攻击都会额外扣分。

## 2. 第一层路线：电玩帮路线攻略

参考：[《杀戮尖塔》新手攻略——路线篇](https://www.vgover.com/news/198867)

攻略的关键结论：

- 第一层是**发育层**，可以相对激进，以血量换资源。
- 第一层前 3 场普通怪来自**弱怪池**，战损低，是补过渡牌、金币、药水的最佳时机。
- 路线应尽量**多拿营火和精英**；精英给遗物，能提高卡组上限。
- 一般优先“**先营火升级，再打精英**”，而不是先精英后营火。
- 血线只要不死，营火优先升级；只有低于约 30%、下一个又是 Boss/强怪/精英且卡组弱时才考虑回血。
- 问号出怪基础 10%，连续没出怪会提高下一次出怪概率；不要连续赌太多问号。
- 小怪药水掉率基础 40%，没掉会提高下一次掉率；三瓶药时可以先用掉冗余药水。

代码里的对应调整：

- 第 1 幕前 6 层普通怪 +2.2，牌组少于 12 张再 +0.6；
- 第 1 幕营火 3.8–4.6，Boss 前营火再 +3；
- 精英不再一律负分：第 4 层以后、血量 >72%、牌组至少 15 张/6 攻击/5 防御时转为正分；早期或血线低仍然重罚；
- 事件早期权重降低，商店保留中等权重（能删牌/买关键牌）；
- 路线说明会写出“前 3 场弱怪 → 营火升级 → 精英/商店”。

### 第二幕生存检查

第二幕不是“多打怪”就够，场面会突然变成多段伤害、群体怪和高血量精英。代码会检查：

- 格挡牌是否 ≥7（第二幕低于 7 会提示补防御）
- 是否有 AOE（史莱姆王/收藏家/地精头子/蛇女/三奴隶）
- 是否有格挡引擎或大格挡（Barricade + Body Slam、Power Through、Impervious、Flame Barrier、Feel No Pain）
- 牌组是否已经 20 张以上还继续拿普通牌
- 精英是否在血量/格挡不足时被推荐

最近一局 Ironclad 的典型问题：第一层只走了 3 场普通怪、0 精英，第二幕 4 张防御 + Shrug It Off 就上 Gremlin Leader/Champs；25 层 Barricade 没拿、21 层 Power Through+ 没拿。现在这些关键牌会在对应条件下大幅加分，而不只是看社区总梯度。

## 3. 商店：Wiki 价格 + 社区决策

参考：[The Merchant - Slay the Spire Wiki](https://slaythespire.wiki.gg/wiki/The_Merchant)

Wiki 的关键价格：

- 有色普通卡 45–55，罕见 68–83，稀有 135–165；
- 无色罕见 81–99，稀有 162–198；
- 普通遗物 143–158，罕见 238–263，稀有 285–315，商店遗物 143–158；
- 删牌第一次 75，每次商店删牌 +25；Smiling Mask 固定 50；Membership Card 半价。

助手会：

- 用**实际价格**和当前金币过滤；
- 把卡牌质量、遗物等级、删牌需求分别换算成优先级；
- 如果最高优先级不够（<55）或买不起，明确推荐“跳过攒钱”；
- 删牌优先诅咒/状态/基础打击防御；没有废牌时不会无脑删核心；
- 商店面板会标出推荐购买的卡，并显示买后剩余金币。

遗物另外参考 [SpireSpy Relic Tier List](https://maybelatergames.co.uk/tools/slaythespire/tierlist/relics/ironclad/)：145 个遗物匹配到 `S/A/B/C/D` 梯度，商店会显示 `玩家 Tier`，并按实际价格修正。

## 4. 敌人特点与应对

参考 [LUDO.guide 第一层敌人/精英攻略](https://www.ludo.guide/guide/slay-the-spire/walkthrough/act-1-the-exordium/early-encounters-and-elites) 和 Wiki 的怪物页，战斗面板会逐只说明：

- 当前意图：伤害/段数、防御、加增益、上负面、蓄力、睡觉/眩晕、逃跑；
- 已知被动：仪式、激怒、蜷缩、荆棘、金属化、人工制品、无实体、每打一张牌掉血等；
- 常见行动循环/下回合倾向：咔咔先仪式、乐嘉睡三回合、时间老头 12 张牌上限等；
- 应对：别拿技能喂乐嘉、先杀咔咔、哨卫集中杀一个、心脏少打低价值牌。

不认识的名字会用社区外号，例如“大颚虫/咬咬虫”“乐嘉/睡美人”“时间老头/蜗牛”“甜甜圈/方片”。

## 5. 所有事件与 Neow

事件选项来自事件自己的 `roomEventText`（Neow 就是这种），不能只读静态 `RoomEventDialog.optionList`。现在：

- Neow 开局祝福会显示每个祝福的评分、说明和推荐；
- 所有问号事件、神龛、图书馆、蛇、牧师等都会显示选项；
- 没有专门数据的 Mod 事件，按“删牌/升级/金币/最大生命/诅咒/战斗/离开”等关键词做通用评分；
- 即使事件只有一个“继续”选项，也不会导致面板消失。

## 6. 流派阶段：过渡牌、核心牌与转型

参考 [SlayTheSpire.gg Strategy Guide](https://slaythespire.gg/guides/strategy)、[LUDO.guide 卡组精炼](https://www.ludo.guide/guide/slay-the-spire/the-silent/deck-refinement-and-scaling) 和 [JEU.VIDEO 新手卡组指南](https://jeu.video/en/guide/slay-spire-beginner-deck)：

- **不要从第一层就锁死流派**。前期先拿能立即用的伤害、格挡、AOE、抽牌，保证能打过第一幕精英和 Boss；
- **过渡牌**是“现在就能用”的牌：Strike Plus（单卡附带效果）、Block Plus、AOE、低费抽牌、能补斩杀的前台伤害；
- **核心牌**是“需要配合才强”的牌：Catalyst、Barricade、Corruption、Limit Break、Accuracy、Electrodynamics、Rushdown 等；
- 核心不是不能早拿，而是要有 **至少 2 个现有配合点**（例如 Catalyst 前已有毒牌，Barricade 前已有格挡和 Body Slam，Corruption 前已有消耗/ Dark Embrace）；
- 第一幕中后段开始找方向；第二幕围绕已有方向补 **scaling（伤害成长/防御成长）+ 抽牌/能量**；第三幕只补短板，不再随便换方向；
- **转型**只在“新方向已有 2 个以上配合点，且当前方向只有 0-2 张核心”时考虑；否则会把牌组拉成杂派；
- 牌组 18-20 张后开始严格：普通过渡牌少拿，优先删基础打击/防御、升级核心、补关键遗物；
- 不要为了“流派好看”硬拿裸核心；没有配合的 Barricade/Catalyst/Corruption 很容易卡手。

### 四个角色的方向与配合点

| 角色 | 方向 | 核心例子 | 配合点 |
|---|---|---|---|
| 铁甲 | 力量 | Limit Break、Demon Form、Inflame | 力量牌、Heavy Blade、多段攻击 |
| 铁甲 | 消耗 | Corruption、Dark Embrace、Feel No Pain | 消耗牌、技能牌、Status |
| 铁甲 | 格挡 | Barricade、Juggernaut、Body Slam | 格挡牌、Entrench、Shrug It Off |
| 猎手 | 毒 | Catalyst、Noxious Fumes、Envenom | 毒牌、Corpse Explosion |
| 猎手 | 小刀 | Accuracy、Wraith Form、Tools of the Trade | 小刀生成、Shiv、Wrist Blade |
| 猎手 | 弃牌 | Tools of the Trade、Tactician、Reflex | 弃牌牌、Acrobatics、Hovering Kite |
| 机器 | 集中/冰球 | Defragment、Consume、Capacitor、Loop | 充能球、冰球、Focus |
| 机器 | 充能球 | Electrodynamics、Biased Cognition、Echo Form | 充能球生成、集中 |
| 观者 | 姿态 | Rushdown、Mental Fortress、Tantrum | 平静/愤怒切换、退出平静 |
| 观者 | 保留/占卜 | Scrawl、Vault、Talk to the Hand | 保留、占卜、抽牌 |

代码里的阶段判断是：`打基础 → 找方向 → 定核心 → 补强 → 收尾`。它会显示当前方向、基础缺口和“下一步该抓什么”，而不是只给一张牌打高分。

## 7. 遗物：Boss 三选一与牌组联动

参考 [GlyphShuffle Boss Relic Tier List](https://glyphshuffle.com/blog/slay-the-spire-boss-relic-tier-list)、[SpireSpy Relic Tier List](https://maybelatergames.co.uk/tools/slaythespire/tierlist/relics/ironclad/) 和 Wiki 的遗物页：

- Boss 遗物按“效果 + 代价 + 当前牌组/血量”比较，不只看社区 Tier；
- 咖啡滴滤器/诅咒钥匙/融合锤等能量遗物，会在牌组高费牌多时加分，在需要休息/锻造时提示代价；
- 金字塔看手里有没有过牌/保留价值；蛇眼看高费牌数量、0 费牌数量；
- 潘多拉/星盘/空笼看基础打击防御数量；黑血看血量；
- 袖剑看小刀、风筝看弃牌、圣水看观者、紫莲看姿态、核电池看故障；
- 商店遗物会把“和当前流派是否契合”加进优先级。

战斗里会读取遗物状态，例如笔尖计数器、苦无/手里剑计数、死灵书、化学 X、腐化树枝、干瘪之手、鸟面瓮、钨钢棒等，并在推荐牌时说明配合。

## 8. Boss/精英预判与针对性抓牌

参考 [High Ground Gaming Bosses Guide](https://www.highgroundgaming.com/slay-the-spire-bosses-guide/)、[LUDO.guide 第一层敌人/精英](https://www.ludo.guide/guide/slay-the-spire/walkthrough/act-1-the-exordium/early-encounters-and-elites) 和 Wiki：

- 地图界面直接显示本幕 Boss（史莱姆王/守护者/六火/铜人/收藏家/冠军/时间老头/觉醒者/甜甜圈方片/心脏）及其机制和准备方向；
- 列出本幕可能的精英（乐嘉、睡美人、三哨卫、地精头子、刺书、三奴隶、大头、复仇女神、蛇女），并按当前牌组给出“能打/危险”判断；
- 路线评分会把“精英是否可打”加进去：能打才推荐精英，不能打就绕；
- 选牌会按当前 Boss/精英加针对性分：
  - 史莱姆王/收藏家/地精头子/蛇女 → AOE 加分
  - 守护者/六火/刺书/心脏 → 格挡加分
  - 乐嘉 → 攻击牌加分，技能牌扣分
  - 三哨卫 → AOE/集中伤害加分
  - 时间老头 → 高影响力牌加分，0 费小牌扣分
  - 觉醒者 → 能力牌扣分
- 面板会写“针对本幕：为什么这张现在更值”。

## 9. 药水顺序与留牌

参考 [LUDO.guide 药水使用策略](https://www.ludo.guide/guide/slay-the-spire/items-equipment/potions-tactical-boosts/potion-usage-strategies)：

- **Flex Potion（类固醇/临时力量）**：只持续到回合结束，必须先确认这回合能打出攻击；优先在多段攻击/大攻击前喝，先打攻击再喝会浪费。
- **Strength Potion（永久力量）**：早喝早赚，但最好在多段/大攻击前；手里没有能打出的攻击时不急着喝。
- **Duplication Potion**：下一张牌打两次，先想好复制哪张，别先出小牌。
- **Energy Potion**：先确认喝完后能打出关键牌；0 能量且手里有高费攻击时，顺序是“先喝能量，再打攻击”。
- **Speed/Dexterity Potion**：临时敏捷要先喝再打格挡牌；永久敏捷可以早喝。
- **防御/回血药**：敌人意图会打穿时立刻喝；否则留给精英/Boss。
- **留牌**：有 Runic Pyramid 时不要为了打空能量把手牌全丢；有保留牌时留到关键回合；有笔尖/苦无/手里剑时，先算计数再决定攻击顺序。

## 10. 药水：社区药水梯度 + 战况

参考：[Potion Tier List & Pick Guidelines](https://slaythespire.info/en/potion-tier-list-pick-guidelines-all-characters/)

助手不是简单按“稀有度”喝药，而是看：

- 这回合 incoming 是否 >= 当前血量 + 格挡（救命药水现在喝）；
- 能量是否为 0 且手里有攻击（能量药水现在喝）；
- 血量是否 <45%（回血药水现在喝）；
- 是否精英/Boss（进攻药水可以抢节奏）；
- 仙女瓶是否应该留着自动复活；
- 水果汁是否应该现在喝永久加最大生命；
- 烟雾弹是否应该留作紧急逃跑；
- 普通战斗是否应该省药给精英/Boss。

## 11. 卡牌选择方向

社区常见操作（放顶、放底、弃牌、消耗、拿回手牌、复制、变化、升级）在代码里按提示文本识别：

- **放牌堆顶**：下一抽必抽，选高影响力/关键牌；
- **放牌堆底**：把诅咒/状态/废牌压下去；
- **弃牌**：优先诅咒/状态/基础打击防御，其次低价值牌；
- **消耗**：优先废牌；有 Dark Embrace/Feel No Pain 时消耗也能变收益；
- **拿回手牌/复制**：选核心牌；
- **变化**：优先变诅咒/状态/基础牌；
- **升级**：按伤害/格挡/费用降低的增量排序。

## 12. 已知局限

- SpireSpy 梯度是社区主观评价，且不同版本/难度会有差异；
- 部分卡牌名称在中文/英文/旧版名称之间不一致，当前 323 张卡中约 284 张能直接匹配到社区梯度，其余使用原有分数/通用估值；
- 战斗求解是启发式，不是穷举所有出牌顺序；充能/抽牌/药水联动只做一层预判；
- 商店遗物、事件选项、Boss 遗物仍需要更多社区数据；
- 第一层精英建议会在血量和牌组达标时才转正，不保证新手一定能打过。

欢迎提交 PR 补充卡牌别名、遗物梯度、事件数据和实战 log。
