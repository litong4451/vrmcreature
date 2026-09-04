package com.example.vrmcreature;

import com.example.vrmcreature.config.VrmCreatureConfig;
import com.example.vrmcreature.entity.ModEntities;
import com.example.vrmcreature.entity.VrmMob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * 模组事件总线（MOD bus）：
 * 注册实体属性、自然刷新规则。
 */
@EventBusSubscriber(modid = VrmCreature.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvents {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.VRM_MOB.get(), VrmMob.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // 是否允许自然刷新，由配置 canSpawn 控制
        if (VrmCreatureConfig.CAN_SPAWN.get()) {
            event.register(
                    ModEntities.VRM_MOB.get(),
                    SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, spawnType, pos, random) -> {
                        // 群系过滤：仅允许配置的群系自然刷新（空列表 = 全部）
                        String biomeId = level.getBiome(pos).unwrapKey()
                                .map(k -> k.location().toString())
                                .orElse("");
                        return VrmCreatureConfig.isBiomeAllowed(biomeId);
                    },
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
}
