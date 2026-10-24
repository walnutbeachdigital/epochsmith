package com.walnutbeachdigital.epochsmith.future;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.electric.ElectricConnectable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Works like copper wire, but also links through the air to every other wireless node
 * within 32 blocks. Put one next to your generators and one next to a far-off machine.
 */
public class WirelessNodeBlock extends BaseEntityBlock implements ElectricConnectable {

    public static final MapCodec<WirelessNodeBlock> CODEC = simpleCodec(WirelessNodeBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

    public WirelessNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessNodeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("message.epochsmith.wireless_node.status",
                    WirelessNodes.near(level, pos).size(), WirelessNodes.RANGE), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
