package com.walnutbeachdigital.epochsmith.era;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import com.walnutbeachdigital.epochsmith.network.EraNetwork;

/**
 * Game events that drive era progression.
 */
@EventBusSubscriber(modid = Epochsmith.MODID)
public final class EraEvents {

    /** Picking up flint for the first time starts the Primitive era. */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getOriginalStack().is(Items.FLINT)) {
            EraManager.advanceTo(player, Era.PRIMITIVE);
        }
    }

    /** When a player joins, tell their game which era they're in. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EraNetwork.sendTo(player, EraManager.getEra(player));
        }
    }

    /** Same after respawning, so the Chronicle stays correct. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EraNetwork.sendTo(player, EraManager.getEra(player));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EraCommand.register(event.getDispatcher());
    }

    private EraEvents() {
    }
}
