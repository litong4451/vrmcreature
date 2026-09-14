package com.example.vrmcreature;

import com.example.vrmcreature.api.event.VrmModelPickEvent;
import com.example.vrmcreature.api.event.VrmMobSpawnEvent;
import com.example.vrmcreature.config.VrmModelConfig;
import com.example.vrmcreature.entity.ModEntities;
import com.example.vrmcreature.entity.VrmMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntitySpawnEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * 模组事件总线：
 *  - MOD bus：注册实体属性、自然刷新放置规则（按各模型配置判断）。
 *  - GAME bus：自然刷新生成实体时，按当前群系从各模型配置中按权重随机选一个模型。
 */
@EventBusSubscriber(modid = VrmCreature.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvents {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.VRM_MOB.get(), VrmMob.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // 自然刷新规则：仅当「任一已配置模型允许当前群系」时才可刷新
        event.register(
                ModEntities.VRM_MOB.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> {
                    // 群系过滤：任一模型允许该群系即可自然刷新
                    String biomeId = level.getBiome(pos).unwrapKey()
                            .map(k -> k.location().toString())
                            .orElse("");
                    return VrmModelConfig.isAnySpawnAllowed(biomeId);
                },
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** GAME bus：自然刷新生成实体时按权重选模型并应用该模型配置 */
    @EventBusSubscriber(modid = VrmCreature.MODID, bus = EventBusSubscriber.Bus.GAME)
    public static class SpawnHandler {
        @SubscribeEvent
        public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
            if (!(event.getEntity() instanceof VrmMob mob)) {
                return;
            }
            // 仅处理自然/结构等世界生成（排除玩家手动刷怪蛋等由其他逻辑处理的场景，
            // 手动生成走 VrmSpawnPacket，这里不覆盖）
            MobSpawnType type = event.getSpawnType();
            if (type != MobSpawnType.NATURAL && type != MobSpawnType.STRUCTURE
                    && type != MobSpawnType.REINFORCEMENT && type != MobSpawnType.PATROL) {
                return;
            }
            String biomeId = mob.level().getBiome(mob.blockPosition()).unwrapKey()
                    .map(k -> k.location().toString())
                    .orElse("");
            String model = VrmModelConfig.pickModelForBiome(biomeId, mob.getRandom());
            if (model == null) {
                return;
            }
            // 生成钩子 1：选中模型后触发，依赖方可替换模型或取消本次刷新
            VrmModelPickEvent pickEvent = new VrmModelPickEvent(mob, biomeId, type, model);
            if (NeoForge.EVENT_BUS.post(pickEvent)) {
                return;
            }
            String chosen = pickEvent.getModelName();
            mob.applyModelConfig(chosen);
            // 生成钩子 2：应用配置后触发，依赖方可二次调整实体属性/行为
            // （配置读取钩子 VrmConfigLoadEvent 已在 applyModelConfig 内触发过，这里用无钩子读取避免重复触发）
            NeoForge.EVENT_BUS.post(new VrmMobSpawnEvent(mob, chosen, VrmModelConfig.load(chosen), type));
        }

        /**
         * 兜底：任何其他途径生成的 VrmMob（刷怪蛋 / 命令 / 依赖方 API 直接实例化）
         * 若尚未被自然刷新链路分配模型（modelName 仍为默认 "model"），
         * 则自动应用默认模型配置并走完整钩子链路，避免生成出无配置的空白实体。
         * （自然刷新链路已在 FinalizeSpawn 中 setModelName，不会重复处理）
         */
        @SubscribeEvent
        public static void onEntitySpawn(EntitySpawnEvent event) {
            if (!(event.getEntity() instanceof VrmMob mob)) {
                return;
            }
            if ("model".equals(mob.getModelName()) && VrmModelConfig.isConfigured("model")) {
                mob.applyModelConfig("model");
            }
        }
    }
}
