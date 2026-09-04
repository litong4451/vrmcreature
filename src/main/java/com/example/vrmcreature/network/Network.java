package com.example.vrmcreature.network;

import com.example.vrmcreature.VrmCreature;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class Network {
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(VrmCreature.MODID).versioned("1");
        registrar.playToServer(VrmSavePacket.TYPE, VrmSavePacket.CODEC, VrmSavePacket::handle);
        registrar.playToServer(VrmSpawnPacket.TYPE, VrmSpawnPacket.CODEC, VrmSpawnPacket::handle);
        registrar.playToServer(VrmBiomePacket.TYPE, VrmBiomePacket.CODEC, VrmBiomePacket::handle);
    }

    /** 客户端发送数据包到服务端 */
    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
