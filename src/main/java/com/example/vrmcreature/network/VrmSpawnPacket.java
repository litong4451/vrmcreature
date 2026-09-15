package com.example.vrmcreature.network;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.config.VrmModelConfig;
import com.example.vrmcreature.entity.ModEntities;
import com.example.vrmcreature.entity.VrmMob;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端「现在生成」请求包（单模型）。
 * modelName 为本次要生成的模型名（不含扩展名），生成 count 只该模型的生物，
 * 属性与行为按该模型对应 JSON 配置生效（applyModelConfig）。
 */
public record VrmSpawnPacket(int count, String modelName) implements CustomPacketPayload {

    public static final Type<VrmSpawnPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VrmCreature.MODID, "vrm_spawn"));

    public static final StreamCodec<ByteBuf, VrmSpawnPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VrmSpawnPacket::count,
            ByteBufCodecs.STRING_UTF8, VrmSpawnPacket::modelName,
            VrmSpawnPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VrmSpawnPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow() == PacketFlow.SERVERBOUND && ctx.player() instanceof ServerPlayer player) {
                Level level = player.level();
                String modelName = (packet.modelName == null || packet.modelName.isEmpty())
                        ? "model" : packet.modelName;
                for (int i = 0; i < packet.count; i++) {
                    VrmMob mob = ModEntities.VRM_MOB.get().create(level);
                    if (mob != null) {
                        mob.moveTo(player.getX() + 0.5D, player.getY(), player.getZ() + 0.5D,
                                player.getYRot(), 0.0F);
                        // 应用该模型独立配置（属性 + 行为 + 模型名）
                        mob.applyModelConfig(modelName);
                        level.addFreshEntity(mob);
                    }
                }
            }
        });
    }
}
