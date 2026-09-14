package com.example.vrmcreature.api.event;

import com.example.vrmcreature.config.VrmModelConfig;
import com.example.vrmcreature.entity.VrmMob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.Event;

/**
 * VRM 生物生成钩子（服务端）。
 *
 * 在实体完成模型配置（属性、行为 AI）之后触发，此时实体已生成于世界中。
 * 监听者可通过 {@link #getMob()} 二次调整实体：修改属性、附加状态效果、替换 AI 等。
 */
public class VrmMobSpawnEvent extends Event {

    private final VrmMob mob;
    private final String modelName;
    private final VrmModelConfig.Data data;
    private final MobSpawnType spawnType;

    public VrmMobSpawnEvent(VrmMob mob, String modelName, VrmModelConfig.Data data, MobSpawnType spawnType) {
        this.mob = mob;
        this.modelName = modelName;
        this.data = data;
        this.spawnType = spawnType;
    }

    /** 已应用配置的实体 */
    public VrmMob getMob() {
        return mob;
    }

    /** 本次使用的模型名 */
    public String getModelName() {
        return modelName;
    }

    /** 本次应用到的模型配置（可读，直接修改字段不会自动同步到实体，需通过 getMob() 设置） */
    public VrmModelConfig.Data getConfig() {
        return data;
    }

    /** 本次生成的触发方式 */
    public MobSpawnType getSpawnType() {
        return spawnType;
    }
}
