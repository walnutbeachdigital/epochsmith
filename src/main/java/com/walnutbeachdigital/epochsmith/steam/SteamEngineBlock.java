package com.walnutbeachdigital.epochsmith.steam;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.power.RotationSource;
import com.walnutbeachdigital.epochsmith.block.ModBlocks;
import com.walnutbeachdigital.epochsmith.item.SmithsHammerItem;
import com.walnutbeachdigital.epochsmith.multiblock.MultiblockController;
import com.walnutbeachdigital.epochsmith.multiblock.MultiblockShape;
import com.walnutbeachdigital.epochsmith.multiblock.Multiblocks;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

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
import net.minecraft.world.phys.BlockHitResult;

/**
 * Turns steam from boilers into rotation power. The Industrial era's signature invention.
 * A multiblock, 3 wide, 3 tall and 2 deep: this engine block sits in the middle of the front face with
 * Engine Casing all round it; behind it is a wall of Engine Casing with a row of three Steam Cylinders
 * through the middle. Strike it with a Smith's Hammer to build it, pipe steam into a cylinder,
 * and take rotation from the axle that comes out of the front.
 */
public class SteamEngineBlock extends BaseEntityBlock implements RotationSource, SteamConnectable, MultiblockController {

    public static final MapCodec<SteamEngineBlock> CODEC = simpleCodec(SteamEngineBlock::new);
    public static final DirectionProperty FACING = MultiblockController.FACING;
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    /** Layers bottom to top, rows front to back. E is this engine, c Engine Casing, S a Steam Cylinder. */
    public static final MultiblockShape SHAPE = new MultiblockShape(new String[][] {
            {"ccc", "ccc"},
            {"cEc", "SSS"},
            {"ccc", "ccc"}},
            Map.of('c', ModBlocks.ENGINE_CASING, 'S', ModBlocks.STEAM_CYLINDER), 'E', new MultiblockShape.Cell(1, 1, 1));

    public SteamEngineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, false)
                .setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public MultiblockShape shape() {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING, FORMED);
    }

    /** The output axle comes out of the front, toward you. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public Direction.Axis getAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public int getForce(Level level, BlockPos pos, BlockState state) {
        return state.getValue(FORMED) && level.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine ? engine.getForce() : 0;
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
            player.displayClientMessage(!state.getValue(FORMED) ? Component.translatable("message.epochsmith.engine.unbuilt")
                    : engine.getForce() > 0 ? Component.translatable("message.epochsmith.engine.running", engine.getForce())
                    : Component.translatable("message.epochsmith.engine.no_steam"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Strike with a Smith's Hammer to build the engine. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof SmithsHammerItem) {
            return Multiblocks.strike(level, pos, player, stack, hand);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Breaking the engine block takes the whole engine apart. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            Multiblocks.unform(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Puffs of steam from the exhaust stack while running. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(RUNNING) && state.getValue(FORMED) && random.nextInt(2) == 0) {
            BlockPos core = Multiblocks.corePos(pos, state);
            level.addParticle(ParticleTypes.CLOUD, core.getX() + 0.35 + random.nextDouble() * 0.3,
                    core.getY() + 2.05, core.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.06, 0.0);
        }
    }
}
