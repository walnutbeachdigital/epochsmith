package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.menu.MachineMenus;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * The Bronze and Iron era's smelter.
 *  1. Load copper ingots and tin ingots (for bronze) OR raw iron (for iron blooms).
 *  2. Light it with 2 charcoal. Only charcoal burns hot and clean enough.
 *  3. After 30 seconds: every 3 copper + 1 tin becomes 4 bronze; every raw iron becomes an iron bloom.
 *  4. Right-click with an empty hand to take out the metal (and any unused charge).
 * Making your first iron bloom unlocks the Mechanical era.
 */
public class BloomeryBlock extends BaseEntityBlock {

    public static final MapCodec<BloomeryBlock> CODEC = simpleCodec(BloomeryBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;


    public BloomeryBlock(Properties properties) {
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

    /** The front (with the opening) faces the player who placed it. */
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
        return new BloomeryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.BLOOMERY.get(), BloomeryBlockEntity::serverTick);
    }





    /** Right-click opens the machine's GUI (whatever you're holding). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return MachineMenus.open(state, level, pos, player, Era.BRONZE_IRON, "message.epochsmith.bloomery.unknown");
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Breaking the machine drops everything inside. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineGui gui) {
            MachineMenus.dropAll(level, pos, gui.getMenuItems());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Smoke from the top and flames at the front opening while lit. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.LARGE_SMOKE, cx, pos.getY() + 1.05, cz, 0.0, 0.07, 0.0);

        Direction front = state.getValue(FACING);
        double fx = cx + front.getStepX() * 0.52 + (random.nextDouble() - 0.5) * 0.3;
        double fz = cz + front.getStepZ() * 0.52 + (random.nextDouble() - 0.5) * 0.3;
        level.addParticle(ParticleTypes.FLAME, fx, pos.getY() + 0.25, fz, 0.0, 0.0, 0.0);
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
    }
}
