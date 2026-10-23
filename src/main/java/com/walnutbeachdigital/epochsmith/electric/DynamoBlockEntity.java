package com.walnutbeachdigital.epochsmith.electric;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.power.MechanicalPower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns rotation into electricity: 8 energy per tick for every point of turning force
 * (a steam engine at force 16 makes 128 per tick). Holds a small buffer of 4,000.
 */
public class DynamoBlockEntity extends BlockEntity implements EnergySource {

    public static final int ENERGY_PER_FORCE = 8;
    private static final int CHECK_INTERVAL = 10;

    private final EpochEnergyStorage energy = new EpochEnergyStorage(4000, 0, 4000, this::setChanged);
    private int force = 0;

    public DynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DYNAMO.get(), pos, state);
    }

    public EpochEnergyStorage getEnergy() {
        return energy;
    }

    @Override
    public int drawEnergy(int max) {
        return energy.take(max);
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.dynamo.status", force, force * ENERGY_PER_FORCE,
                energy.getEnergyStored(), energy.getMaxEnergyStored());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DynamoBlockEntity dynamo) {
        if (level.getGameTime() % CHECK_INTERVAL == 0) {
            dynamo.force = MechanicalPower.forceAt(level, pos);
            boolean active = dynamo.force > 0;
            if (state.getValue(DynamoBlock.ACTIVE) != active) {
                level.setBlock(pos, state.setValue(DynamoBlock.ACTIVE, active), Block.UPDATE_ALL);
            }
        }
        if (dynamo.force > 0) {
            dynamo.energy.generate(dynamo.force * ENERGY_PER_FORCE);
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
