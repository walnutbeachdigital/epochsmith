package com.walnutbeachdigital.epochsmith.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that makes turning force: water wheels, windmills, hand cranks.
 */
public interface RotationSource extends Shaft {

    /** How much turning force this source makes right now (0 = stopped). */
    int getForce(Level level, BlockPos pos, BlockState state);
}
