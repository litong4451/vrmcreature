package com.example.vrmcreature.api.event;

import com.example.vrmcreature.entity.VrmMob;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

import java.util.List;

/**
 * VRM 生物死亡掉落钩子（服务端）。
 *
 * 在按模型配置执行默认掉落之前触发。监听者可：
 *  - 修改 {@link #getLoot()} 返回的列表（增删掉落行，行格式：物品ID;数量;概率，如 minecraft:diamond;1;0.3）；
 *  - {@link #setCanceled(boolean)} 完全取消默认掉落，自行实现掉落逻辑。
 */
public class VrmLootDropEvent extends Event implements ICancellableEvent {

    private final VrmMob mob;
    private final List<String> loot;

    public VrmLootDropEvent(VrmMob mob, List<String> loot) {
        this.mob = mob;
        this.loot = loot;
    }

    /** 死亡的实体 */
    public VrmMob getMob() {
        return mob;
    }

    /** 即将执行的掉落表（可增删条目） */
    public List<String> getLoot() {
        return loot;
    }
}
