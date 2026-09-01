package com.example.vrmcreature;

import com.example.vrmcreature.config.VrmCreatureConfig;
import com.example.vrmcreature.entity.ModEntities;
import com.example.vrmcreature.network.Network;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(VrmCreature.MODID)
public class VrmCreature {
    public static final String MODID = "vrmcreature";

    public VrmCreature(IEventBus modEventBus) {
        // 注册实体
        ModEntities.ENTITIES.register(modEventBus);
        // 注册网络通道
        modEventBus.addListener(Network::register);
        // 注册配置文件（config/vrmcreature-common.toml）
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VrmCreatureConfig.SPEC);
    }
}
