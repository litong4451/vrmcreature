package com.example.vrmcreature;

import com.example.vrmcreature.config.VrmModelConfig;
import com.example.vrmcreature.entity.ModEntities;
import com.example.vrmcreature.network.Network;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;

@Mod(VrmCreature.MODID)
public class VrmCreature {
    public static final String MODID = "vrmcreature";

    public VrmCreature(IEventBus modEventBus) {
        // 注册实体
        ModEntities.ENTITIES.register(modEventBus);
        // 注册网络通道
        modEventBus.addListener(Network::register);
        // 服务端/客户端通用初始化：确保每模型配置目录存在
        modEventBus.addListener((FMLCommonSetupEvent e) -> VrmModelConfig.ensureConfigDir());
        // 专用服务端初始化：服务端启动即确保配置目录存在
        modEventBus.addListener((FMLDedicatedServerSetupEvent e) -> VrmModelConfig.ensureConfigDir());
    }
}
