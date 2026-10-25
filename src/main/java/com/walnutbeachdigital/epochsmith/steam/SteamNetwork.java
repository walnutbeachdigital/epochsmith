package com.walnutbeachdigital.epochsmith.steam;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.walnutbeachdigital.epochsmith.multiblock.MultiblockPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Follows steam pipes from a machine to find the boilers and steam users on the same network.
 */
public final class SteamNetwork {

    /** Turning force each working boiler supplies, shared between all engines on its pipes. */
    public static final int FORCE_PER_BOILER = 8;
    public static final int MAX_FORCE = 16;
    /** Most blocks searched, so huge pipe networks can't slow the game down. */
    private static final int MAX_NODES = 256;

    public record Result(int workingBoilers, int engines) {
    }

    /** Explores everything reachable from a machine through pipes. The machine itself counts as an engine. */
    public static Result explore(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        enqueueNeighbours(level, start, seen, queue);

        int boilers = 0;
        int engines = 1;
        while (!queue.isEmpty() && seen.size() < MAX_NODES) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SteamPipeBlock
                    || (state.getBlock() instanceof SteamCylinderBlock && MultiblockPartBlock.isFormed(state))) {
                enqueueNeighbours(level, pos, seen, queue);
            } else if (state.getBlock() instanceof BoilerBlock) {
                if (level.getBlockEntity(pos) instanceof BoilerBlockEntity boiler && boiler.isProducing()) {
                    boilers++;
                }
            } else if (state.getBlock() instanceof SteamEngineBlock) {
                engines++;
            }
        }
        return new Result(boilers, engines);
    }

    /** The turning force one engine gets from its network. */
    public static int engineForce(Level level, BlockPos engine) {
        Result result = explore(level, engine);
        if (result.workingBoilers() == 0) {
            return 0;
        }
        return Math.min(MAX_FORCE, Math.max(1, FORCE_PER_BOILER * result.workingBoilers() / result.engines()));
    }

    private static void enqueueNeighbours(Level level, BlockPos pos, Set<BlockPos> seen, Deque<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (!seen.contains(next) && level.getBlockState(next).getBlock() instanceof SteamConnectable) {
                seen.add(next);
                queue.add(next);
            }
        }
    }

    private SteamNetwork() {
    }
}
