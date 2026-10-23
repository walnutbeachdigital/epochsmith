package com.walnutbeachdigital.epochsmith.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
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
 * The Mechanical era's smelter. Needs rotation power (force 4+) to work its bellows.
 *  - Right-click with coal or charcoal to add heat.
 *  - Right-click with raw metal or wrought iron to load it (up to 16). Results pop out on top.
 *  - Empty hand shows its status; sneak + empty hand takes back unsmelted items.
 */
public class ForgeBlock extends BaseEntityBlock {

    public static final MapCodec<ForgeBlock> CODEC = simpleCodec(ForgeBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ForgeBlock(Properties properties) {
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
        return new ForgeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.FORGE.get(), ForgeBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean fuel = ForgeBlockEntity.isFuel(stack);
        if (!fuel && !ForgeBlockEntity.canSmelt(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ForgeBlockEntity forge)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!EraManager.hasReached(player, Era.MECHANICAL)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.forge.unknown"), true);
        } else if (fuel) {
            int used = forge.addFuel(stack.getCount());
            if (used == 0) {
                player.displayClientMessage(Component.translatable("message.epochsmith.forge.hot"), true);
            } else {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(used);
                }
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.2F);
                player.displayClientMessage(forge.describe(), true);
            }
        } else if (forge.insert(stack)) {
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
            player.displayClientMessage(forge.describe(), true);
        } else {
            player.displayClientMessage(Component.translatable("message.epochsmith.forge.busy"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ForgeBlockEntity forge) {
            if (player.isShiftKeyDown()) {
                ItemStack taken = forge.takeInput();
                if (!taken.isEmpty() && !player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            } else {
                player.displayClientMessage(forge.describe(), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ForgeBlockEntity forge) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), forge.takeInput());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Roaring flames on the hearth and sparks while working. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
        double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 0.78, z, 0.0, 0.03, 0.0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, pos.getY() + 0.8, z, 0.0, 0.0, 0.0);
        }
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.0, z, 0.0, 0.05, 0.0);
        if (random.nextInt(8) == 0) {
            level.playLocalSound(pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.2F, false);
        }
    }
}
