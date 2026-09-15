package com.example.vrmcreature.api.event;

import com.example.vrmcreature.entity.VrmMob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

/**
 * 自然刷新选中模型钩子（服务端）。
 *
 * 在 VrmCreature 依据「群系 + 权重」选定模型之后、应用配置之前触发。
 * 监听方式：在你的模组中用 @SubscribeEvent 监听本事件，并注册到 NeoForge.EVENT_BUS
 * （或使用 @EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)）。
 *
 * 典型用法：
 *  - {@link #setModelName(String)} 替换将要使用的模型；
 *  - {@link #setCanceled(boolean)} 取消本次自然刷新（该生物将不会生成）。
 */
public class VrmModelPickEvent extends Event implements ICancellableEvent {

    private final VrmMob mob;
    private final String biomeId;
    private final MobSpawnType spawnType;
    private String modelName;

    public VrmModelPickEvent(VrmMob mob, String biomeId, MobSpawnType spawnType, String modelName) {
        this.mob = mob;
        this.biomeId = biomeId;
        this.spawnType = spawnType;
        this.modelName = modelName;
    }

    /** 即将生成的实体（此时尚未应用模型配置） */
    public VrmMob getMob() {
        return mob;
    }

    /** 当前所在群系的 ID（如 minecraft:plains） */
    public String getBiomeId() {
        return biomeId;
    }

    /** 本次生成的触发方式 */
    public MobSpawnType getSpawnType() {
        return spawnType;
    }

    /** 当前选中的模型名 */
    public String getModelName() {
        return modelName;
    }

    /** 替换将要使用的模型（忽略空值） */
    public void setModelName(String modelName) {
        if (modelName != null && !modelName.isEmpty()) {
            this.modelName = modelName;
        }
    }
}
