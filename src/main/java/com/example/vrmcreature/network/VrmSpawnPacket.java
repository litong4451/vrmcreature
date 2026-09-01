package com.example.vrmcreature.network;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.config.VrmCreatureConfig;
import com.example.vrmcreature.entity.ModEntities;
import com.example.vrmcreature.entity.VrmMob;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端「现在生成」请求包。
 * 属性一律读取已锁定的配置文件，只传数量。
 */
public record VrmSpawnPacket(int count) implements CustomPacketPayload {

    public static final Type<VrmSpawnPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VrmCreature.MODID, "vrm_spawn"));

    public static final StreamCodec<ByteBuf, VrmSpawnPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VrmSpawnPacket::count,
            VrmSpawnPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VrmSpawnPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow().isServerbound() && ctx.player() instanceof ServerPlayer player) {
                Level level = player.level();
                for (int i = 0; i < packet.count; i++) {
                    VrmMob mob = ModEntities.VRM_MOB.get().create(level);
                    if (mob != null) {
                        mob.moveTo(player.getX() + 0.5D, player.getY(), player.getZ() + 0.5D,
                                player.getYRot(), 0.0F);
                        mob.getAttribute(Attributes.MAX_HEALTH)
                                .setBaseValue(VrmCreatureConfig.MAX_HEALTH.get());
                        mob.setHealth((float) VrmCreatureConfig.MAX_HEALTH.get());
                        level.addFreshEntity(mob);
                    }
                }
            }
        });
    }
}
