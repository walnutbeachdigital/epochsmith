package com.walnutbeachdigital.epochsmith.block;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A flat stone for knapping. Right-click it while holding flint:
 * every third strike breaks off one flint blade (using up one flint).
 */
public class KnappingStoneBlock extends Block {

    public static final MapCodec<KnappingStoneBlock> CODEC = simpleCodec(KnappingStoneBlock::new);

    /** How many strikes the current flint has taken (0 to 2). */
    public static final IntegerProperty STRIKES = IntegerProperty.create("strikes", 0, 2);

    /** Half a block tall, like a slab. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public KnappingStoneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STRIKES, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STRIKES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // All the real work happens on the server; the client just swings the arm
        if (!level.isClientSide()) {
            if (!EraManager.hasReached(player, Era.PRIMITIVE)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.knapping.unknown"), true);
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }

            int strikes = state.getValue(STRIKES);
            level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS,
                    1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            ((ServerLevel) level).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FLINT)),
                    pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5, 6, 0.15, 0.05, 0.15, 0.05);

            if (strikes < 2) {
                level.setBlock(pos, state.setValue(STRIKES, strikes + 1), Block.UPDATE_ALL);
            } else {
                level.setBlock(pos, state.setValue(STRIKES, 0), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8F, 1.4F);
                Block.popResource(level, pos.above(), new ItemStack(ModItems.FLINT_BLADE.get()));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
