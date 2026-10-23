package com.walnutbeachdigital.epochsmith.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * The Primitive era's storage: a small 9-slot basket.
 * Like a shulker box, it keeps its contents when you break it and carry it away.
 */
public class WovenBasketBlock extends BaseEntityBlock {

    public static final MapCodec<WovenBasketBlock> CODEC = simpleCodec(WovenBasketBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public WovenBasketBlock(Properties properties) {
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
        return new WovenBasketBlockEntity(pos, state);
    }

    /** Right-click opens the basket. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WovenBasketBlockEntity basket) {
            player.openMenu(basket);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * In Creative mode blocks don't drop, so drop a filled basket by hand
     * (otherwise its contents would be lost). Survival drops are handled by the loot table.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative()
                && level.getBlockEntity(pos) instanceof WovenBasketBlockEntity basket && !basket.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(basket.collectComponents());
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
