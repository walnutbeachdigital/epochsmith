package com.walnutbeachdigital.epochsmith.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Mechanical era's crafting station.
 *  - Right-click with planks, logs or wrought iron to lay them on the bench (up to 16).
 *  - Right-click with an empty hand to work them, or connect rotation power to any side.
 *  - Sneak + empty hand takes back what hasn't been worked.
 */
public class JoinersBenchBlock extends BaseEntityBlock {

    public static final MapCodec<JoinersBenchBlock> CODEC = simpleCodec(JoinersBenchBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public JoinersBenchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JoinersBenchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.JOINERS_BENCH.get(), JoinersBenchBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!JoinersBenchBlockEntity.canWork(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof JoinersBenchBlockEntity bench) {
            if (!EraManager.hasReached(player, Era.MECHANICAL)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.bench.unknown"), true);
            } else if (bench.place(stack)) {
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
            } else {
                player.displayClientMessage(Component.translatable("message.epochsmith.bench.busy"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof JoinersBenchBlockEntity bench)) {
            return InteractionResult.SUCCESS;
        }
        if (bench.getWorkpiece().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.epochsmith.bench.empty"), true);
        } else if (player.isShiftKeyDown()) {
            ItemStack taken = bench.takeAll();
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
        } else if (!EraManager.hasReached(player, Era.MECHANICAL)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.bench.unknown"), true);
        } else {
            bench.effects();
            bench.work(JoinersBenchBlockEntity.WORK_PER_CLICK);
            player.causeFoodExhaustion(0.05F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof JoinersBenchBlockEntity bench) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bench.takeAll());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
