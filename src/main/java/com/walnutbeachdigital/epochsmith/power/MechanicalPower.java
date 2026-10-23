package com.walnutbeachdigital.epochsmith.power;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Works out how much turning force reaches a block.
 *
 * Rotation travels in straight lines through shafts (axles and sources) that run
 * along the direction of travel. Every source on a line adds its force.
 */
public final class MechanicalPower {

    /** Most force a single machine or line can receive. */
    public static final int MAX_FORCE = 16;
    /** Longest line of shafts that carries power. */
    public static final int MAX_LENGTH = 32;

    /** Total force reaching a machine from all six sides. */
    public static int forceAt(Level level, BlockPos machine) {
        Set<BlockPos> seen = new HashSet<>();
        seen.add(machine);
        int total = 0;
        for (Direction direction : Direction.values()) {
            total += walk(level, machine.relative(direction), direction, seen);
        }
        return Math.min(total, MAX_FORCE);
    }

    /** Total force on the line a shaft belongs to (used to make axles spin). */
    public static int lineForce(Level level, BlockPos pos, Direction.Axis axis) {
        Set<BlockPos> seen = new HashSet<>();
        seen.add(pos);
        int total = ownForce(level, pos, level.getBlockState(pos));
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == axis) {
                total += walk(level, pos.relative(direction), direction, seen);
            }
        }
        return Math.min(total, MAX_FORCE);
    }

    /** Follows shafts in one direction, adding up every source along the way. */
    private static int walk(Level level, BlockPos pos, Direction direction, Set<BlockPos> seen) {
        int total = 0;
        for (int i = 0; i < MAX_LENGTH && seen.add(pos); i++) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof Shaft shaft) || shaft.getAxis(state) != direction.getAxis()) {
                break;
            }
            total += ownForce(level, pos, state);
            pos = pos.relative(direction);
        }
        return total;
    }

    private static int ownForce(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof RotationSource source ? source.getForce(level, pos, state) : 0;
    }

    private MechanicalPower() {
    }
}
