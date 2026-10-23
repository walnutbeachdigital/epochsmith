package com.walnutbeachdigital.epochsmith.steam;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

/**
 * Burns fuel to boil water into steam.
 *  - Water: a tank of 8 buckets. A water source block touching the boiler refills it on its own.
 *    Boiling uses one bucket every 10 seconds.
 *  - Fuel: coal or charcoal burn 80 seconds, coke 160 seconds. Fuel only burns while there's water.
 */
public class BoilerBlockEntity extends BlockEntity {

    public static final int MAX_WATER = 8000;
    public static final int BUCKET = 1000;
    private static final int WATER_PER_TICK = 5;
    private static final int REFILL_PER_SECOND = 400;
    public static final int MAX_BURN = 3200 * 8;

    private int water = 0;
    private int burnTicks = 0;

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER.get(), pos, state);
    }

    /** How long one item of this fuel burns, or 0 if it isn't boiler fuel. */
    public static int burnTimeFor(ItemStack stack) {
        if (stack.is(ModItems.COKE.get())) {
            return 3200;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return 1600;
        }
        return 0;
    }

    public boolean isProducing() {
        return burnTicks > 0 && water >= WATER_PER_TICK;
    }

    /** Adds fuel items until the firebox is full. Returns how many were used. */
    public int addFuel(ItemStack stack) {
        int each = burnTimeFor(stack);
        int used = 0;
        while (used < stack.getCount() && burnTicks + each <= MAX_BURN) {
            burnTicks += each;
            used++;
        }
        setChanged();
        return used;
    }

    public boolean addBucket() {
        if (water + BUCKET > MAX_WATER) {
            return false;
        }
        water += BUCKET;
        setChanged();
        return true;
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.boiler.status",
                water / 100 / 10.0F, burnTicks / 20, Component.translatable(isProducing()
                        ? "message.epochsmith.boiler.producing" : "message.epochsmith.boiler.idle"));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity boiler) {
        if (level.getGameTime() % 20 == 0 && boiler.water < MAX_WATER) {
            for (Direction direction : Direction.values()) {
                FluidState fluid = level.getFluidState(pos.relative(direction));
                if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
                    boiler.water = Math.min(MAX_WATER, boiler.water + REFILL_PER_SECOND);
                    break;
                }
            }
        }
        boolean producing = boiler.isProducing();
        if (producing) {
            boiler.burnTicks--;
            boiler.water -= WATER_PER_TICK;
        }
        if (state.getValue(BlockStateProperties.LIT) != producing) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, producing), Block.UPDATE_ALL);
        }
        boiler.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Water", water);
        tag.putInt("Burn", burnTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = tag.getInt("Water");
        burnTicks = tag.getInt("Burn");
    }
}
