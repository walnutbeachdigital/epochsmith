package com.walnutbeachdigital.epochsmith.multiblock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The blueprint of a multiblock machine: which block goes where.
 *
 * Written as layers from the bottom up. Each layer is a list of rows from the front of the machine to the back,
 * and each row reads left to right as you stand in front of the machine looking at it.
 * One character marks the controller (the block you strike with the hammer), and one cell is the "core":
 * the part that draws the whole machine's big 3D model once it's built.
 */
public final class MultiblockShape {

    /** A spot in the blueprint: how far right, up and back from the front-left-bottom corner. */
    public record Cell(int right, int up, int back) {
    }

    /** The first block that's wrong, for telling the player what to fix. */
    public record Problem(BlockPos pos, Block wanted, int wrongBlocks) {
    }

    private final Map<Cell, Supplier<? extends Block>> cells = new LinkedHashMap<>();
    private final Cell controller;
    private final Cell core;

    public MultiblockShape(String[][] layers, Map<Character, Supplier<? extends Block>> key, char controllerChar, Cell core) {
        Cell found = null;
        for (int up = 0; up < layers.length; up++) {
            for (int back = 0; back < layers[up].length; back++) {
                String row = layers[up][back];
                for (int right = 0; right < row.length(); right++) {
                    char ch = row.charAt(right);
                    Cell cell = new Cell(right, up, back);
                    if (ch == controllerChar) {
                        found = cell;
                    } else if (key.containsKey(ch)) {
                        cells.put(cell, key.get(ch));
                    } else {
                        throw new IllegalArgumentException("Unknown blueprint character '" + ch + "'");
                    }
                }
            }
        }
        if (found == null || !cells.containsKey(core)) {
            throw new IllegalArgumentException("A blueprint needs a controller and a core part");
        }
        this.controller = found;
        this.core = core;
    }

    /** Every part (not the controller) and the block it must be. */
    public Map<Cell, Supplier<? extends Block>> parts() {
        return cells;
    }

    public Cell core() {
        return core;
    }

    /** How many blocks make up the machine, controller included. */
    public int size() {
        return cells.size() + 1;
    }

    /** Where a blueprint cell is in the world, for a controller at {@code origin} whose front faces {@code front}. */
    public BlockPos toWorld(BlockPos origin, Direction front, Cell cell) {
        Direction right = front.getCounterClockWise();
        Direction back = front.getOpposite();
        return origin.immutable().relative(right, cell.right() - controller.right())
                .relative(Direction.UP, cell.up() - controller.up())
                .relative(back, cell.back() - controller.back());
    }

    public BlockPos corePos(BlockPos origin, Direction front) {
        return toWorld(origin, front, core);
    }

    /** True if {@code pos} is one of this machine's parts. */
    public boolean contains(BlockPos origin, Direction front, BlockPos pos) {
        for (Cell cell : cells.keySet()) {
            if (toWorld(origin, front, cell).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /** Checks the blocks around a controller. Returns null when everything is in place. */
    @Nullable
    public Problem check(Level level, BlockPos origin, Direction front) {
        BlockPos firstPos = null;
        Block firstBlock = null;
        int wrong = 0;
        for (Map.Entry<Cell, Supplier<? extends Block>> entry : cells.entrySet()) {
            BlockPos pos = toWorld(origin, front, entry.getKey());
            BlockState state = level.getBlockState(pos);
            Block wanted = entry.getValue().get();
            boolean ok = state.is(wanted)
                    && !(state.getBlock() instanceof MultiblockPartBlock && MultiblockPartBlock.isFormed(state));
            if (!ok) {
                wrong++;
                if (firstPos == null) {
                    firstPos = pos;
                    firstBlock = wanted;
                }
            }
        }
        return wrong == 0 ? null : new Problem(firstPos, firstBlock, wrong);
    }
}
