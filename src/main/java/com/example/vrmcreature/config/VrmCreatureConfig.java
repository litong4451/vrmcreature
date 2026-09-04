package com.example.vrmcreature.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * 模组通用配置。
 * 流程：首次「创建」时可设置阵营/属性/行为/动作 → 保存后 locked=true 永久锁定，
 * 后续进入界面只读展示，仅能再次生成。
 */
public class VrmCreatureConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // 创建锁定标记
    public static final ModConfigSpec.BooleanValue LOCKED;
    // 自然刷新开关
    public static final ModConfigSpec.BooleanValue CAN_SPAWN;
    // 允许自然刷新的群系 ID 列表（空列表表示全部群系）
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SPAWN_BIOMES;
    // 阵营
    public static final ModConfigSpec.BooleanValue IS_NEUTRAL;
    public static final ModConfigSpec.BooleanValue IS_FRIENDLY;
    // 基础属性
    public static final ModConfigSpec.DoubleValue MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue ATTACK_DAMAGE;
    public static final ModConfigSpec.DoubleValue MOVEMENT_SPEED;
    public static final ModConfigSpec.IntValue SPAWN_WEIGHT;
    // 行为 AI：0 静止 1 巡逻 2 跟随玩家 3 主动攻击
    public static final ModConfigSpec.IntValue BEHAVIOR;
    // 动作偏好：0 待机 1 走路 2 攻击（运行时按状态自动切换动画）
    public static final ModConfigSpec.IntValue ANIM_PREF;

    static {
        BUILDER.push("vrm_mob");

        LOCKED = BUILDER
                .comment("是否已创建并锁定属性（锁定后不可再修改）", "true after first creation")
                .define("locked", false);

        CAN_SPAWN = BUILDER
                .comment("是否允许自然刷新", "Allow natural spawning")
                .define("canSpawn", true);

        SPAWN_BIOMES = BUILDER
                .comment("允许自然刷新的群系 ID 列表，例如 [\"minecraft:plains\",\"minecraft:forest\"]；空列表表示全部群系",
                        "Biome IDs allowed for natural spawning; empty = all biomes")
                .defineListAllowEmpty("spawnBiomes", List.of(), e -> e instanceof String);

        IS_NEUTRAL = BUILDER
                .comment("中立阵营（不主动攻击，被打后反击）")
                .define("isNeutral", false);

        IS_FRIENDLY = BUILDER
                .comment("我方阵营（与玩家友好）")
                .define("isFriendly", false);

        MAX_HEALTH = BUILDER
                .comment("最大生命值")
                .defineInRange("maxHealth", 20.0D, 1.0D, 1024.0D);

        ATTACK_DAMAGE = BUILDER
                .comment("攻击伤害")
                .defineInRange("attackDamage", 3.0D, 0.0D, 1024.0D);

        MOVEMENT_SPEED = BUILDER
                .comment("移动速度")
                .defineInRange("movementSpeed", 0.25D, 0.0D, 10.0D);

        SPAWN_WEIGHT = BUILDER
                .comment("自然刷新权重")
                .defineInRange("spawnWeight", 10, 0, 1000);

        BEHAVIOR = BUILDER
                .comment("行为模式：0 静止 1 巡逻 2 跟随玩家 3 主动攻击")
                .defineInRange("behavior", 1, 0, 3);

        ANIM_PREF = BUILDER
                .comment("动作偏好：0 待机 1 走路 2 攻击")
                .defineInRange("animPref", 0, 0, 2);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static boolean isLocked() {
        return LOCKED.get();
    }

    /** 读取当前阵营：0 敌方 1 中立 2 我方 */
    public static int getFaction() {
        if (IS_FRIENDLY.get()) return 2;
        if (IS_NEUTRAL.get()) return 1;
        return 0;
    }

    /** 当前群系是否允许自然刷新（空列表 = 全部群系） */
    public static boolean isBiomeAllowed(String biomeId) {
        List<? extends String> allowed = SPAWN_BIOMES.get();
        if (allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(biomeId);
    }

    /** 写入群系白名单（服务端） */
    public static void setSpawnBiomes(List<String> biomeIds) {
        SPAWN_BIOMES.set(biomeIds);
    }

    /** 首次创建保存，之后永久锁定 */
    public static void saveCreation(int faction, double health, double damage, double speed,
                                    int behavior, int animPref) {
        IS_NEUTRAL.set(faction == 1);
        IS_FRIENDLY.set(faction == 2);
        MAX_HEALTH.set(health);
        ATTACK_DAMAGE.set(damage);
        MOVEMENT_SPEED.set(speed);
        BEHAVIOR.set(behavior);
        ANIM_PREF.set(animPref);
        LOCKED.set(true);
    }
}
