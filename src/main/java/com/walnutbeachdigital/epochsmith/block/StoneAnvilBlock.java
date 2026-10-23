package com.walnutbeachdigital.epochsmith.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.item.SmithsHammerItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Bronze and Iron era's crafting station.
 *  1. Right-click with bronze ingots or iron blooms to lay them on the anvil (up to 16).
 *  2. Right-click with a Smith's Hammer to strike. Each finished piece pops off:
 *     bronze ingot -> bronze plate (4 strikes), iron bloom -> wrought iron ingot (6 strikes).
 *  3. Right-click with an empty hand to take unworked pieces back.
 */
public class StoneAnvilBlock extends BaseEntityBlock {

    public static final MapCodec<StoneAnvilBlock> CODEC = simpleCodec(StoneAnvilBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public StoneAnvilBlock(Properties properties) {
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

    /** Same placement as the vanilla anvil: the long side runs across your view. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
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
        return new StoneAnvilBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        boolean isHammer = stack.getItem() instanceof SmithsHammerItem;
        if (!isHammer && !StoneAnvilBlockEntity.canWork(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof StoneAnvilBlockEntity anvil)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!EraManager.hasReached(player, Era.BRONZE_IRON)) {
            player.displayClientMessage(Component.translatable("message.epochsmith.anvil.unknown"), true);
        } else if (isHammer) {
            strike(stack, level, pos, player, hand, anvil);
        } else if (anvil.place(stack)) {
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
        } else {
            player.displayClientMessage(Component.translatable("message.epochsmith.anvil.busy"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void strike(ItemStack hammer, Level level, BlockPos pos, Player player,
                               InteractionHand hand, StoneAnvilBlockEntity anvil) {
        if (anvil.getWorkpiece().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.epochsmith.anvil.empty"), true);
            return;
        }
        // Sparks and a ring on every blow
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.35F, 1.1F + level.random.nextFloat() * 0.3F);
        ((ServerLevel) level).sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 8, 0.15, 0.02, 0.15, 0.15);
        hammer.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));

        ItemStack finished = anvil.strike();
        if (!finished.isEmpty()) {
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
            Block.popResource(level, pos.above(), finished);
        }
    }

    /** Empty hand: take back the pieces you haven't worked yet. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StoneAnvilBlockEntity anvil
                && !anvil.getWorkpiece().isEmpty()) {
            ItemStack taken = anvil.takeAll();
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            return InteractionResult.CONSUME;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StoneAnvilBlockEntity anvil) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), anvil.takeAll());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
