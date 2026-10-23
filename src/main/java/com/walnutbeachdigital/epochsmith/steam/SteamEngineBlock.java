package com.walnutbeachdigital.epochsmith.steam;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.power.RotationSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Industrial era's signature invention. Takes steam from boilers (through pipes, or touching)
 * and turns it into rotation power along its axle line, like a much stronger water wheel.
 * Its axle runs left-to-right as you face it when placing.
 */
public class SteamEngineBlock extends BaseEntityBlock implements RotationSource, SteamConnectable {

    public static final MapCodec<SteamEngineBlock> CODEC = simpleCodec(SteamEngineBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    public SteamEngineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(RUNNING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, RUNNING);
    }

    /** The output axle runs across your view, so the flywheel faces you. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    public Direction.Axis getAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public int getForce(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine ? engine.getForce() : 0;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamEngineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.STEAM_ENGINE.get(), SteamEngineBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine) {
            engine.setOwner(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine) {
            player.displayClientMessage(engine.getForce() > 0
                    ? Component.translatable("message.epochsmith.engine.running", engine.getForce())
                    : Component.translatable("message.epochsmith.engine.no_steam"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Puffs of steam from the cylinder while running. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(RUNNING) && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.35 + random.nextDouble() * 0.3,
                    pos.getY() + 0.95, pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.04, 0.0);
        }
    }
}
