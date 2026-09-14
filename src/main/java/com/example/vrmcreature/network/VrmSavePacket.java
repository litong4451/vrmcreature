package com.example.vrmcreature.network;

import com.example.vrmcreature.VrmCreature;
import com.example.vrmcreature.config.VrmModelConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端「创建/保存某模型属性」请求包。
 * 写入该模型对应 JSON：阵营/属性/行为/动作，并置 locked=true（锁定后不可再改）。
 * modelName: 模型名（不含扩展名）
 * faction: 0=敌方 1=中立 2=我方
 * behavior: 0 静止 1 巡逻 2 跟随玩家 3 主动攻击
 * animPref: 0 待机 1 走路 2 攻击
 */
public record VrmSavePacket(String modelName, int faction, double health, double damage, double speed,
                            int behavior, int animPref) implements CustomPacketPayload {

    public static final Type<VrmSavePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(VrmCreature.MODID, "vrm_save"));

    public static final StreamCodec<ByteBuf, VrmSavePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, VrmSavePacket::modelName,
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
            if (!ctx.flow().isServerbound()) {
                return;
            }
            VrmModelConfig.Data d = VrmModelConfig.load(packet.modelName);
            if (d.locked) {
                return; // 已锁定，禁止修改
            }
            d.faction = packet.faction;
            d.health = packet.health;
            d.damage = packet.damage;
            d.speed = packet.speed;
            d.behavior = packet.behavior;
            d.animPref = packet.animPref;
            d.locked = true;
            VrmModelConfig.save(packet.modelName, d);
        });
    }
}
