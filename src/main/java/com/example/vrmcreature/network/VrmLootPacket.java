package com.example.vrmcreature.network;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.config.VrmModelConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端「保存某模型掉落物配置」请求包。
 * 写入该模型对应 JSON 的 loot；lootLines 格式：物品ID;数量;概率（如 "minecraft:diamond;1;0.3"）。
 */
public record VrmLootPacket(String modelName, List<String> lootLines) implements CustomPacketPayload {

    public static final Type<VrmLootPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VrmCreature.MODID, "vrm_loot"));

    public static final StreamCodec<ByteBuf, VrmLootPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, VrmLootPacket::modelName,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), VrmLootPacket::lootLines,
            VrmLootPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VrmLootPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.flow().isServerbound()) {
                return;
            }
            VrmModelConfig.Data d = VrmModelConfig.load(packet.modelName);
            d.loot.clear();
            d.loot.addAll(packet.lootLines);
            VrmModelConfig.save(packet.modelName, d);
        });
    }
}
