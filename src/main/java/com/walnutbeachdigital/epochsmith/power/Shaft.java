package com.walnutbeachdigital.epochsmith.power;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Any block that turns around an axis and passes rotation along it:
 * axles, and every power source (water wheel, windmill, hand crank).
 */
public interface Shaft {

    /** The line this block spins around (and connects along). */
    Direction.Axis getAxis(BlockState state);
}
