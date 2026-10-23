package com.walnutbeachdigital.epochsmith.power;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Turns when water touches it: force 2 in still water, 4 in flowing water.
 * The wheel faces you when placed and turns around a horizontal axis.
 */
public class WaterWheelBlock extends KineticBlock implements RotationSource {

    public static final MapCodec<WaterWheelBlock> CODEC = simpleCodec(WaterWheelBlock::new);

    public WaterWheelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Z));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeAlong(state.getValue(AXIS), 4, 12);
    }

    @Override
    public int getForce(Level level, BlockPos pos, BlockState state) {
        int force = 0;
        for (Direction direction : Direction.values()) {
            FluidState fluid = level.getFluidState(pos.relative(direction));
            if (fluid.is(FluidTags.WATER)) {
                force = Math.max(force, fluid.isSource() ? 2 : 4);
            }
        }
        return force;
    }
}
