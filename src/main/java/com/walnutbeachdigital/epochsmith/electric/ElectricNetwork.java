package com.walnutbeachdigital.epochsmith.electric;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Follows copper wire from a machine to find every generator (dynamo, solar panel) and battery
 * it can draw from. Generators come first in the list, so machines use fresh power before stored power.
 */
public final class ElectricNetwork {

    private static final int MAX_NODES = 512;

    public static List<BlockPos> findSources(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> generators = new ArrayList<>();
        List<BlockPos> batteries = new ArrayList<>();
        seen.add(start);
        enqueueNeighbours(level, start, seen, queue);
        while (!queue.isEmpty() && seen.size() < MAX_NODES) {
            BlockPos pos = queue.poll();
            if (level.getBlockState(pos).getBlock() instanceof CopperWireBlock) {
                enqueueNeighbours(level, pos, seen, queue);
            } else if (level.getBlockEntity(pos) instanceof BatteryBlockEntity) {
                batteries.add(pos);
            } else if (level.getBlockEntity(pos) instanceof EnergySource) {
                generators.add(pos);
            }
        }
        generators.addAll(batteries);
        return generators;
    }

    /**
     * Draws energy from a list of sources (as found above), in order, until 'amount' is met.
     * Returns how much was drawn.
     */
    public static int draw(Level level, List<BlockPos> sources, int amount, boolean generatorsOnly) {
        int drawn = 0;
        for (BlockPos pos : sources) {
            if (drawn >= amount) {
                break;
            }
            if (!(level.getBlockEntity(pos) instanceof EnergySource source)) {
                continue;
            }
            if (generatorsOnly && source instanceof BatteryBlockEntity) {
                continue;
            }
            drawn += source.drawEnergy(amount - drawn);
        }
        return drawn;
    }

    private static void enqueueNeighbours(Level level, BlockPos pos, Set<BlockPos> seen, Deque<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (!seen.contains(next) && level.getBlockState(next).getBlock() instanceof ElectricConnectable) {
                seen.add(next);
                queue.add(next);
            }
        }
    }

    private ElectricNetwork() {
    }
}
