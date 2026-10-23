package com.walnutbeachdigital.epochsmith.information;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.EnergySource;
import com.walnutbeachdigital.epochsmith.electric.EpochEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Makes 16 energy per tick in open daylight, 8 while it rains, nothing at night.
 */
public class SolarPanelBlockEntity extends BlockEntity implements EnergySource {

    public static final int FULL_SUN = 16;
    private static final int CHECK_INTERVAL = 20;

    private final EpochEnergyStorage energy = new EpochEnergyStorage(2000, 0, 2000, this::setChanged);
    private int output = 0;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
    }

    public EpochEnergyStorage getEnergy() {
        return energy;
    }

    @Override
    public int drawEnergy(int max) {
        return energy.take(max);
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.solar.status", output, energy.getEnergyStored());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        if (level.getGameTime() % CHECK_INTERVAL == 0) {
            boolean sun = level.isDay() && level.canSeeSky(pos.above());
            panel.output = !sun ? 0 : level.isRaining() ? FULL_SUN / 2 : FULL_SUN;
        }
        if (panel.output > 0) {
            panel.energy.generate(panel.output);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
    }
}
