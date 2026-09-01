package com.example.vrmcreature;

import com.example.vrmcreature.client.VrmConfigScreen;
import com.example.vrmcreature.entity.ModEntities;
import com.example.vrmcreature.entity.client.VrmMobRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * 客户端事件（MOD bus）：注册实体渲染器、注册打开设置界面的快捷键。
 * 打开设置不再需要输入命令，按快捷键即可。
 * 默认不绑定任何按键，避免冲突；请到 设置 → 控制 → VRM 生物 中自行指定按键。
 */
@EventBusSubscriber(modid = VrmCreature.MODID, bus = EventBusSubscriber.Bus.MOD, value = net.neoforged.fml.common.Mod.EventBusSubscriber.Side.CLIENT)
public class ClientEvents {

    /** 打开 VRM 生物设置界面的快捷键，默认未绑定（可到控制设置中指定，避免按键冲突） */
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.vrmcreature.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.vrmcreature"
    );

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VRM_MOB.get(), VrmMobRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }

    /** 游戏 tick 轮询快捷键（GAME bus, CLIENT 侧） */
    @EventBusSubscriber(modid = VrmCreature.MODID, bus = EventBusSubscriber.Bus.GAME, value = net.neoforged.fml.common.Mod.EventBusSubscriber.Side.CLIENT)
    public static class KeyHandler {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            while (OPEN_CONFIG.consumeClick()) {
                mc.setScreen(new VrmConfigScreen());
            }
        }
    }
}
