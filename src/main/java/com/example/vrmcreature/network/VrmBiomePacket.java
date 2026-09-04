package com.example.vrmcreature.network;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.config.VrmCreatureConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端「设置群系白名单」请求包。
 * biomeIds 为允许自然刷新的群系 ID 列表；空列表表示全部群系。
 */
public record VrmBiomePacket(List<String> biomeIds) implements CustomPacketPayload {

    public static final Type<VrmBiomePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VrmCreature.MODID, "vrm_biome"));

    public static final StreamCodec<ByteBuf, VrmBiomePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), VrmBiomePacket::biomeIds,
            VrmBiomePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VrmBiomePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow().isServerbound()) {
                VrmCreatureConfig.setSpawnBiomes(packet.biomeIds);
            }
        });
    }
}
