package com.walnutbeachdigital.epochsmith.electric;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;

import com.walnutbeachdigital.epochsmith.item.ElectricPowered;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Stores electricity for when the dynamos stop. The bars on its sides show how full it is.
 * Right-click to see the exact charge. Works with other mods' energy cables too.
 */
public class BatteryBlock extends BaseEntityBlock implements ElectricConnectable {

    public static final MapCodec<BatteryBlock> CODEC = simpleCodec(BatteryBlock::new);
    /** 0 (empty) to 4 (full): how many charge bars are lit. */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public BatteryBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.BATTERY.get(), BatteryBlockEntity::serverTick);
    }

    /** Right-click with an electric tool or coil chestplate to recharge it from the bank. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof ElectricPowered)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            int missing = ElectricPowered.MAX_CHARGE - ElectricPowered.getCharge(stack);
            int energy = battery.getEnergy().take(missing * ElectricPowered.ENERGY_PER_CHARGE);
            int added = energy / ElectricPowered.ENERGY_PER_CHARGE;
            if (added > 0) {
                ElectricPowered.setCharge(stack, ElectricPowered.getCharge(stack) + added);
                level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            player.displayClientMessage(Component.translatable("message.epochsmith.battery.charged",
                    ElectricPowered.getCharge(stack), ElectricPowered.MAX_CHARGE), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            player.displayClientMessage(battery.describe(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
