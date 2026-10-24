package com.walnutbeachdigital.epochsmith.future;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.item.ModItems;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Future era's smelter. No wires needed: it burns plasma cells.
 *  - Right-click with plasma cells to fuel it, or with something smeltable to load it (four kinds at once).
 *  - Empty hand collects what's done; sneak + empty hand takes back inputs and fuel.
 *  - Hoppers and pipes can feed it (items and plasma cells) and empty it from any side.
 */
public class PlasmaSmelterBlock extends BaseEntityBlock {

    public static final MapCodec<PlasmaSmelterBlock> CODEC = simpleCodec(PlasmaSmelterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public PlasmaSmelterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
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
        return new PlasmaSmelterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.PLASMA_SMELTER.get(), PlasmaSmelterBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.PLASMA_CELL.get()) && PlasmaSmelterBlockEntity.resultFor(level, stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlasmaSmelterBlockEntity furnace) {
            if (!EraManager.hasReached(player, Era.FUTURE)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.plasma_smelter.unknown"), true);
            } else if (furnace.insert(stack)) {
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
                player.displayClientMessage(furnace.describe(), true);
            } else {
                player.displayClientMessage(Component.translatable("message.epochsmith.plasma_smelter.busy"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlasmaSmelterBlockEntity furnace) {
            java.util.List<ItemStack> taken = player.isShiftKeyDown() ? furnace.takeInputs() : furnace.takeOutputs();
            for (ItemStack stack : taken) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
            player.displayClientMessage(furnace.describe(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PlasmaSmelterBlockEntity furnace) {
            for (ItemStack stack : furnace.takeInputs()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
            for (ItemStack stack : furnace.takeOutputs()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Plasma flames while it smelts. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 1.02, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.05, 0.0);
        }
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 0.4F, 2.0F, false);
        }
    }
}
