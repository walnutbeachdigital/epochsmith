package com.walnutbeachdigital.epochsmith.power;

import net.minecraft.world.level.block.Block;

/**
 * Passes rotation between all six sides, so axles can turn corners and power can split.
 * Has no moving parts of its own to draw; MechanicalPower treats it as a junction.
 */
public class GearboxBlock extends Block {

    public GearboxBlock(Properties properties) {
        super(properties);
    }
}
