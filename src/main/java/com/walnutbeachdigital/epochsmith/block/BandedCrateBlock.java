package com.walnutbeachdigital.epochsmith.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Mechanical era's storage: a 27-slot iron-banded crate.
 * Place two crates side by side and opening either one shows both together (54 slots),
 * like a double chest. Breaking a crate spills its contents, as a chest does.
 */
public class BandedCrateBlock extends BaseEntityBlock {

    public static final MapCodec<BandedCrateBlock> CODEC = simpleCodec(BandedCrateBlock::new);

    public BandedCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BandedCrateBlockEntity(pos, state);
    }

    /** The first crate found beside this one (north, east, south, west), or null. */
    @Nullable
    private static BandedCrateBlockEntity neighbour(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof BandedCrateBlockEntity other) {
                return other;
            }
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BandedCrateBlockEntity crate)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        BandedCrateBlockEntity other = neighbour(level, pos);
        if (other == null) {
            player.openMenu(crate);
        } else {
            // Always put the crate with the lower position first, so both crates show the same layout
            boolean thisFirst = pos.asLong() < other.getBlockPos().asLong();
            CompoundContainer joined = thisFirst ? new CompoundContainer(crate, other) : new CompoundContainer(other, crate);
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> ChestMenu.sixRows(id, inventory, joined),
                    Component.translatable("container.epochsmith.banded_crate_large")));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BandedCrateBlockEntity crate) {
            Containers.dropContents(level, pos, crate);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
