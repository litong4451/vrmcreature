package com.example.vrmcreature.network;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.config.VrmCreatureConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端「首次创建保存」请求包。
 * faction: 0=敌方 1=中立 2=我方
 * behavior: 0 静止 1 巡逻 2 跟随玩家 3 主动攻击
 * animPref: 0 待机 1 走路 2 攻击
 */
public record VrmSavePacket(int faction, double health, double damage, double speed,
                            int behavior, int animPref) implements CustomPacketPayload {

    public static final Type<VrmSavePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VrmCreature.MODID, "vrm_save"));

    public static final StreamCodec<ByteBuf, VrmSavePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VrmSavePacket::faction,
            ByteBufCodecs.DOUBLE, VrmSavePacket::health,
            ByteBufCodecs.DOUBLE, VrmSavePacket::damage,
            ByteBufCodecs.DOUBLE, VrmSavePacket::speed,
            ByteBufCodecs.VAR_INT, VrmSavePacket::behavior,
            ByteBufCodecs.VAR_INT, VrmSavePacket::animPref,
            VrmSavePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VrmSavePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow().isServerbound() && !VrmCreatureConfig.isLocked()) {
                VrmCreatureConfig.saveCreation(
                        packet.faction, packet.health, packet.damage, packet.speed,
                        packet.behavior, packet.animPref);
            }
        });
    }
}
