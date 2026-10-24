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
        // Machines with GUIs: hoppers fill their input and fuel slots and empty their output slots
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PIT_KILN.get(),
                (kiln, side) -> kiln.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BLOOMERY.get(),
                (bloomery, side) -> bloomery.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MILLSTONE.get(),
                (mill, side) -> mill.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.FORGE.get(),
                (forge, side) -> forge.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COKE_OVEN.get(),
                (oven, side) -> oven.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.LATHE.get(),
                (lathe, side) -> lathe.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.BLAST_FURNACE.get(),
                (furnace, side) -> furnace.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ARC_FURNACE.get(),
                (furnace, side) -> furnace.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.FABRICATOR.get(),
                (fab, side) -> fab.getMenuItems().automation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.NANOFABRICATOR.get(),
                (fab, side) -> fab.getMenuItems().automation());
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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.QUANTUM_CELL.get(),
                (cell, side) -> cell.getAutomation());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MATTER_PRINTER.get(),
                (printer, side) -> printer.getAutomation());
    }

    private ModCapabilities() {
    }
}
