package com.example.vrmcreature.api.event;

import com.example.vrmcreature.entity.VrmMob;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

/**
 * VRM 生物 AI 行为钩子（服务端）。
 *
 * 在实体按模型配置注册默认行为 AI（静止/巡逻/跟随/攻击）之前触发。
 * 监听者可：
 *  - 不取消：默认 AI 照常注册；
 *  - {@link #setCanceled(boolean)}：跳过默认 AI，自行通过
 *    {@link #getMob()}.goalSelector / targetSelector 注册完全自定义的 AI。
 *
 * {@link #getBehavior()} 为模型配置中的行为模式（0 静止 / 1 巡逻 / 2 跟随 / 3 主动攻击），
 * {@link #isHostile()} 为该模型的阵营是否敌对。
 */
public class VrmBehaviorEvent extends Event implements ICancellableEvent {

    private final VrmMob mob;
    private final int behavior;
    private final boolean hostile;

    public VrmBehaviorEvent(VrmMob mob, int behavior, boolean hostile) {
        this.mob = mob;
        this.behavior = behavior;
        this.hostile = hostile;
    }

    /** 即将注册 AI 的实体 */
    public VrmMob getMob() {
        return mob;
    }

    /** 模型配置的行为模式：0 静止 1 巡逻 2 跟随 3 主动攻击 */
    public int getBehavior() {
        return behavior;
    }

    /** 该模型阵营是否敌对（faction == 0） */
    public boolean isHostile() {
        return hostile;
    }
}
