package com.walnutbeachdigital.epochsmith.electric;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Lets other mods see Epochsmith's energy as standard Forge Energy (FE):
 * their cables can pull from dynamos, and charge or drain batteries.
 */
@EventBusSubscriber(modid = Epochsmith.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.DYNAMO.get(),
                (dynamo, side) -> dynamo.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.BATTERY.get(),
                (battery, side) -> battery.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.SOLAR_PANEL.get(),
                (panel, side) -> panel.getEnergy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.FUSION_REACTOR.get(),
                (reactor, side) -> reactor.getEnergy());
        // Hoppers and item pipes can feed (and empty) the sorting cabinet
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SORTING_CABINET.get(),
                (cabinet, side) -> cabinet.getHandler());
        // Hoppers and pipes feed the induction furnace and pull its results
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.INDUCTION_FURNACE.get(),
                (furnace, side) -> furnace.getAutomation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.DATA_VAULT.get(),
                (vault, side) -> vault.getAutomation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ASSEMBLER.get(),
                (assembler, side) -> assembler.getAutomation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PLASMA_SMELTER.get(),
                (smelter, side) -> smelter.getAutomation());
    }

    private ModCapabilities() {
    }
}
