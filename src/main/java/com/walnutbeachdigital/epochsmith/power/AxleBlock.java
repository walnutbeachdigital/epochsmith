package com.walnutbeachdigital.epochsmith.power;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries rotation in a straight line. Placed pointing along the face you click, like a log.
 */
public class AxleBlock extends KineticBlock {

    public static final MapCodec<AxleBlock> CODEC = simpleCodec(AxleBlock::new);

    public AxleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }
}
