package com.walnutbeachdigital.epochsmith.power;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Muscle power. Each right-click adds 3 seconds of turning (up to 15 seconds), at force 2.
 */
public class HandCrankBlock extends KineticBlock implements RotationSource {

    public static final MapCodec<HandCrankBlock> CODEC = simpleCodec(HandCrankBlock::new);
    public static final int FORCE = 2;

    public HandCrankBlock(Properties properties) {
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeAlong(state.getValue(AXIS), 3, 13);
    }

    @Override
    public int getForce(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic && kinetic.isCranked() ? FORCE : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
            kinetic.crank();
            level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, 0.8F);
            player.causeFoodExhaustion(0.05F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
