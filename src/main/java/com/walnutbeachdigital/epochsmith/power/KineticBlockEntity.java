package com.walnutbeachdigital.epochsmith.power;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared by every spinning block (axles, wheels, windmills, cranks).
 * Every half second it works out how fast it's turning and tells players' games,
 * which draw the block rotating at that speed.
 */
public class KineticBlockEntity extends BlockEntity {

    private static final int UPDATE_INTERVAL = 10;
    /** How many ticks of turning each crank of the handle adds, and the most it can store. */
    public static final int CRANK_TICKS_PER_USE = 60;
    public static final int MAX_CRANK_TICKS = 300;

    private int speed = 0;
    private int crankTicks = 0;

    public KineticBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KINETIC.get(), pos, state);
    }

    public int getSpeed() {
        return speed;
    }

    public boolean isCranked() {
        return crankTicks > 0;
    }

    public void crank() {
        crankTicks = Math.min(crankTicks + CRANK_TICKS_PER_USE, MAX_CRANK_TICKS);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KineticBlockEntity kinetic) {
        if (kinetic.crankTicks > 0) {
            kinetic.crankTicks--;
        }
        if (level.getGameTime() % UPDATE_INTERVAL != 0 || !(state.getBlock() instanceof Shaft shaft)) {
            return;
        }
        int newSpeed = MechanicalPower.lineForce(level, pos, shaft.getAxis(state));
        if (newSpeed != kinetic.speed) {
            kinetic.speed = newSpeed;
            kinetic.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Speed", speed);
        tag.putInt("CrankTicks", crankTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        speed = tag.getInt("Speed");
        crankTicks = tag.getInt("CrankTicks");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
