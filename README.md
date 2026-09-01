---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 5b393a90ab977a878df81f78a13f4437_6ba7418da4c311f1abe1525400e6dd8f
    ReservedCode1: aVc9kZdP0daqK2MKbeXT4CAKq9GXkGeL4+IberKE1GIJv7CGGrek0YO2ycJUgGiT3KauJc3ur88WigR6G3YpRMm6/ZirzBNup9T6AT1EtroQ0TLDJQEsB47np/y9g1UBerHVb/j1tpIotNqpTW+Jefdv2gD5vgnwdg9JO4kRL7AIJiLOqp2hwMzFaqU=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 5b393a90ab977a878df81f78a13f4437_6ba7418da4c311f1abe1525400e6dd8f
    ReservedCode2: aVc9kZdP0daqK2MKbeXT4CAKq9GXkGeL4+IberKE1GIJv7CGGrek0YO2ycJUgGiT3KauJc3ur88WigR6G3YpRMm6/ZirzBNup9T6AT1EtroQ0TLDJQEsB47np/y9g1UBerHVb/j1tpIotNqpTW+Jefdv2gD5vgnwdg9JO4kRL7AIJiLOqp2hwMzFaqU=
---

# VRM Creature 使用说明（NeoForge 1.21.1）

## 一、功能概览
- 加载自定义 VRM 模型作为可生成生物
- 可配置：自然刷新开关 / 中立 / 我方 / 敌方
- 可配置：生命值、攻击力、移动速度、刷新权重、刷新数量

## 二、构建环境
- JDK 21（64 位）
- IntelliJ IDEA（Community 版即可）

## 三、导入与构建
1. IDEA 直接打开本目录（VRMCreature），等待 Gradle 同步完成（首次较慢）。
2. 运行 `runClient` 启动测试客户端。

## 四、放置 VRM 模型
将你的 `.vrm` 文件重命名为 `model.vrm`，放到：
```
src/main/resources/assets/vrmcreature/vrm/model.vrm
```

> 兼容说明：
> - 同时支持 **VRM 0.x（VRM 1.0）** 与 **VRM 2.0（VRMC_vrm）** 格式，
>   加载器会自动识别版本（读取扩展名 `VRM` / `VRMC_vrm`）。
> - 网格、骨骼蒙皮、动画片段均从模型内解析，无需额外准备贴图——
>   基础色贴图优先从模型内提取（VRM 0.x 的 MToon `materialProperties`、
>   VRM 2.0 的标准 `baseColorTexture` 均已兼容），自动注册为动态纹理。
> - 若模型内无内嵌贴图，才会回退使用
>   `src/main/resources/assets/vrmcreature/textures/vrm/model.png`（可选）。
> - 加载日志会打印版本信息，如 `version=VRM 2.0`。

## 五、配置（游戏内自动生成 config/vrmcreature-common.toml）
| 配置项 | 说明 |
|---|---|
| canSpawn | 是否允许自然刷新 |
| isNeutral | 中立阵营（不主动攻击，被打后反击） |
| isFriendly | 我方阵营（与玩家友好） |
| maxHealth | 最大生命值 |
| attackDamage | 攻击伤害 |
| movementSpeed | 移动速度 |
| spawnWeight | 自然刷新权重 |
| spawnMinGroup / spawnMaxGroup | 单次刷新数量范围 |

> 阵营判定优先级：isFriendly=true → 我方；isNeutral=true → 中立；
> 两者都 false → 敌方（主动攻击玩家）。

## 六、游戏内设置界面与「现在生成」
按快捷键打开设置界面。**默认不绑定任何按键**（避免与其他模组/操作冲突），请到 设置 → 控制 → VRM 生物 →「打开 VRM 生物设置」自行指定一个键：
```
（设置中自定义按键，例如 V）
```
界面功能：
- 切换阵营：敌方 / 中立 / 我方
- 调整属性：生命值、攻击力、速度
- 调整生成数量
- 点「现在生成」→ 弹出确认框「是否现在生成？」→ 确认后按当前配置立即生成，
  并把设置写入配置文件（影响后续自然刷新）

## 七、召唤测试
开发环境下可执行命令生成实体：
```
/summon vrmcreature:vrm_mob
```

## 八、打包发布
```
gradlew build
```
产物在 `build/libs/` 下。
*（内容由AI生成，仅供参考）*
