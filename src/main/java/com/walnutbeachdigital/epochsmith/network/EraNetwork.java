package com.walnutbeachdigital.epochsmith.network;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.client.ClientEraData;
import com.walnutbeachdigital.epochsmith.era.Era;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Registers Epochsmith's network messages and sends them.
 */
@EventBusSubscriber(modid = Epochsmith.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class EraNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(EraSyncPayload.TYPE, EraSyncPayload.STREAM_CODEC, EraNetwork::handleSync);
    }

    /** Runs on the player's game when the message arrives. */
    private static void handleSync(EraSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientEraData.setEra(payload.era()));
    }

    /** Tells this player's game what era they're in. */
    public static void sendTo(ServerPlayer player, Era era) {
        PacketDistributor.sendToPlayer(player, new EraSyncPayload(era));
    }

    private EraNetwork() {
    }
}
