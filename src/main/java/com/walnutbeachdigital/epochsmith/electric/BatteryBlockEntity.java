package com.walnutbeachdigital.epochsmith.electric;

import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
import java.util.List;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores 200,000 energy. Charges itself from generators (dynamos, solar panels) on its wires (up to 1,024 per tick),
 * and machines draw from it when the dynamos can't keep up. Shows its charge on its sides.
 */
public class BatteryBlockEntity extends BlockEntity implements EnergySource, StatusDescriber {

    public static final int CAPACITY = 200_000;
    public static final int MAX_RATE = 1024;
    private static final int REFRESH_INTERVAL = 20;

    private final EpochEnergyStorage energy = new EpochEnergyStorage(CAPACITY, MAX_RATE, MAX_RATE, this::setChanged);
    private List<BlockPos> sources = List.of();

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state);
    }

    public EpochEnergyStorage getEnergy() {
        return energy;
    }

    @Override
    public int drawEnergy(int max) {
        return energy.take(Math.min(max, MAX_RATE));
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.battery.status", energy.getEnergyStored(), CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity battery) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            battery.sources = ElectricNetwork.findSources(level, pos);
        }
        int room = Math.min(MAX_RATE, CAPACITY - battery.energy.getEnergyStored());
        if (room > 0) {
            battery.energy.generate(ElectricNetwork.draw(level, battery.sources, room, true));
        }
        int charge = Math.round(4.0F * battery.energy.getEnergyStored() / CAPACITY);
        if (state.getValue(BatteryBlock.CHARGE) != charge) {
            level.setBlock(pos, state.setValue(BatteryBlock.CHARGE, charge), Block.UPDATE_CLIENTS);
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
