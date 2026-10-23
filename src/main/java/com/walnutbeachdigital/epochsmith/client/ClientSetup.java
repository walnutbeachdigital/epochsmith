package com.walnutbeachdigital.epochsmith.client;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only setup: hooks up custom renderers. Never runs on a dedicated server.
 */
@EventBusSubscriber(modid = Epochsmith.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.STONE_ANVIL.get(), StoneAnvilRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.KINETIC.get(), KineticRenderer::new);
    }

    private ClientSetup() {
    }
}
