package com.walnutbeachdigital.epochsmith.multiblock;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Putting multiblock machines together with the Smith's Hammer, and taking them apart again. */
public final class Multiblocks {

    /** How far a part can be from its controller in any direction (machines are up to 3x3x3, controller on a face). */
    private static final int REACH = 2;

    /**
     * The player struck {@code clicked} with a hammer: if it's a controller, or a part next to one,
     * check the structure and build the machine.
     */
    public static ItemInteractionResult strike(Level level, BlockPos clicked, Player player, ItemStack hammer, InteractionHand hand) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        BlockState clickedState = level.getBlockState(clicked);
        Iterable<BlockPos> candidates = clickedState.getBlock() instanceof MultiblockController
                ? java.util.List.of(clicked) : around(clicked);

        MultiblockShape.Problem best = null;
        for (BlockPos pos : candidates) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof MultiblockController controller) || state.getValue(MultiblockController.FORMED)) {
                continue;
            }
            for (Direction front : frontsToTry(state)) {
                MultiblockShape.Problem problem = controller.shape().check(level, pos, front);
                if (problem == null) {
                    form(level, pos.immutable(), state, front);
                    hammer.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                    level.playSound(null, clicked, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 0.9F);
                    player.displayClientMessage(Component.translatable("message.epochsmith.multiblock.formed",
                            state.getBlock().getName()), true);
                    return ItemInteractionResult.CONSUME;
                }
                if (best == null || problem.wrongBlocks() < best.wrongBlocks()) {
                    best = problem;
                }
            }
        }
        if (best == null) {
            if (clickedState.getBlock() instanceof MultiblockController) {
                player.displayClientMessage(Component.translatable("message.epochsmith.multiblock.already"), true);
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        level.playSound(null, clicked, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.8F, 1.2F);
        BlockPos p = best.pos();
        player.displayClientMessage(Component.translatable("message.epochsmith.multiblock.problem",
                best.wanted().getName(), p.getX(), p.getY(), p.getZ(), best.wrongBlocks()), true);
        return ItemInteractionResult.CONSUME;
    }

    /** The way the controller already faces first, then the other three. */
    private static Direction[] frontsToTry(BlockState state) {
        Direction first = state.getValue(MultiblockController.FACING);
        return new Direction[] {first, first.getClockWise(), first.getOpposite(), first.getCounterClockWise()};
    }

    private static void form(Level level, BlockPos origin, BlockState controllerState, Direction front) {
        MultiblockController controller = (MultiblockController) controllerState.getBlock();
        MultiblockShape shape = controller.shape();
        for (MultiblockShape.Cell cell : shape.parts().keySet()) {
            BlockPos pos = shape.toWorld(origin, front, cell);
            BlockState part = level.getBlockState(pos)
                    .setValue(MultiblockPartBlock.FORM, cell.equals(shape.core()) ? MultiblockPartBlock.Form.CORE : MultiblockPartBlock.Form.HIDDEN)
                    .setValue(MultiblockPartBlock.FACING, front)
                    .setValue(MultiblockPartBlock.ACTIVE, false);
            level.setBlock(pos, part, Block.UPDATE_ALL);
        }
        level.setBlock(origin, controller.withFormed(controllerState, front, true), Block.UPDATE_ALL);
    }

    /** Turns every part back into a plain block. {@code controllerState} is the controller as it was when built. */
    public static void unform(Level level, BlockPos origin, BlockState controllerState) {
        if (!(controllerState.getBlock() instanceof MultiblockController controller) || !controllerState.getValue(MultiblockController.FORMED)) {
            return;
        }
        Direction front = controllerState.getValue(MultiblockController.FACING);
        for (Map.Entry<MultiblockShape.Cell, Supplier<? extends Block>> entry : controller.shape().parts().entrySet()) {
            BlockPos pos = controller.shape().toWorld(origin, front, entry.getKey());
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MultiblockPartBlock && MultiblockPartBlock.isFormed(state)) {
                level.setBlock(pos, state.setValue(MultiblockPartBlock.FORM, MultiblockPartBlock.Form.NONE)
                        .setValue(MultiblockPartBlock.ACTIVE, false), Block.UPDATE_ALL);
            }
        }
        BlockState now = level.getBlockState(origin);
        if (now.getBlock() == controllerState.getBlock() && now.getValue(MultiblockController.FORMED)) {
            level.setBlock(origin, controller.withFormed(now, front, false), Block.UPDATE_ALL);
        }
    }

    /** A built part was broken or replaced: take its machine apart. */
    static void partRemoved(Level level, BlockPos pos) {
        for (BlockPos p : around(pos)) {
            BlockState state = level.getBlockState(p);
            if (MultiblockController.isFormed(state)
                    && ((MultiblockController) state.getBlock()).shape().contains(p, state.getValue(MultiblockController.FACING), pos)) {
                unform(level, p.immutable(), state);
            }
        }
    }

    /** The controller of the built machine that {@code part} belongs to, or null. */
    @Nullable
    public static BlockPos findController(Level level, BlockPos part) {
        for (BlockPos p : around(part)) {
            BlockState state = level.getBlockState(p);
            if (MultiblockController.isFormed(state)
                    && ((MultiblockController) state.getBlock()).shape().contains(p, state.getValue(MultiblockController.FACING), part)) {
                return p.immutable();
            }
        }
        return null;
    }

    /** Lights up (or dims) the big model of a built machine. */
    public static void setActive(Level level, BlockPos origin, BlockState controllerState, boolean active) {
        if (!MultiblockController.isFormed(controllerState)) {
            return;
        }
        MultiblockController controller = (MultiblockController) controllerState.getBlock();
        BlockPos core = controller.shape().corePos(origin, controllerState.getValue(MultiblockController.FACING));
        BlockState state = level.getBlockState(core);
        if (state.getBlock() instanceof MultiblockPartBlock && state.getValue(MultiblockPartBlock.ACTIVE) != active) {
            level.setBlock(core, state.setValue(MultiblockPartBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    /** Where the core of a built machine is (for particles), or the controller itself if it isn't built. */
    public static BlockPos corePos(BlockPos origin, BlockState controllerState) {
        if (!MultiblockController.isFormed(controllerState)) {
            return origin;
        }
        return ((MultiblockController) controllerState.getBlock()).shape()
                .corePos(origin, controllerState.getValue(MultiblockController.FACING));
    }

    private static Iterable<BlockPos> around(BlockPos pos) {
        return BlockPos.betweenClosed(pos.offset(-REACH, -REACH, -REACH), pos.offset(REACH, REACH, REACH));
    }

    private Multiblocks() {
    }
}
