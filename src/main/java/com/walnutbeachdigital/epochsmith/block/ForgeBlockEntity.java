package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.item.ModItems;
import com.walnutbeachdigital.epochsmith.power.MechanicalPower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Smelts one item at a time while its bellows are turned (force 4 or more) and it has heat.
 *  - Raw iron, copper, gold or tin: 1 ingot, plus a 50% chance of a second. Costs 1 heat.
 *  - Wrought iron ingot: 1 crucible steel ingot. Costs 2 heat.
 * Each coal or charcoal adds 8 heat (64 max).
 */
public class ForgeBlockEntity extends BlockEntity {

    public static final int MIN_FORCE = 4;
    public static final int MAX_ITEMS = 16;
    public static final int HEAT_PER_FUEL = 8;
    public static final int MAX_HEAT = 64;
    private static final int POWER_CHECK_INTERVAL = 10;

    private ItemStack input = ItemStack.EMPTY;
    private int heat = 0;
    private int progress = 0;
    private int force = 0;

    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGE.get(), pos, state);
    }

    // ---- Forge recipes ----

    public static ItemStack resultFor(ItemStack stack) {
        if (stack.is(Items.RAW_IRON)) {
            return new ItemStack(Items.IRON_INGOT);
        }
        if (stack.is(Items.RAW_COPPER)) {
            return new ItemStack(Items.COPPER_INGOT);
        }
        if (stack.is(Items.RAW_GOLD)) {
            return new ItemStack(Items.GOLD_INGOT);
        }
        if (stack.is(ModItems.RAW_TIN.get())) {
            return new ItemStack(ModItems.TIN_INGOT.get());
        }
        if (stack.is(ModItems.WROUGHT_IRON_INGOT.get())) {
            return new ItemStack(ModItems.STEEL_INGOT.get());
        }
        return ItemStack.EMPTY;
    }

    private static boolean isSteel(ItemStack stack) {
        return stack.is(ModItems.WROUGHT_IRON_INGOT.get());
    }

    /** Steel takes twice as long and twice the heat. */
    private static int workFor(ItemStack stack) {
        return isSteel(stack) ? 640 : 320;
    }

    private static int heatFor(ItemStack stack) {
        return isSteel(stack) ? 2 : 1;
    }

    public static boolean canSmelt(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    // ---- Loading ----

    public boolean insert(ItemStack stack) {
        if (input.isEmpty()) {
            input = stack.split(Math.min(stack.getCount(), MAX_ITEMS));
            progress = 0;
            setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(input, stack) && input.getCount() < MAX_ITEMS) {
            int moved = Math.min(stack.getCount(), MAX_ITEMS - input.getCount());
            input.grow(moved);
            stack.shrink(moved);
            setChanged();
            return true;
        }
        return false;
    }

    /** Adds as much fuel as fits. Returns how many items were used. */
    public int addFuel(int count) {
        int used = Math.min(count, (MAX_HEAT - heat) / HEAT_PER_FUEL);
        heat += used * HEAT_PER_FUEL;
        setChanged();
        return used;
    }

    public ItemStack takeInput() {
        ItemStack taken = input;
        input = ItemStack.EMPTY;
        progress = 0;
        setChanged();
        return taken;
    }

    public Component describe() {
        return Component.translatable("message.epochsmith.forge.status", input.getCount(), heat, force);
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgeBlockEntity forge) {
        if (level.getGameTime() % POWER_CHECK_INTERVAL == 0) {
            forge.force = MechanicalPower.forceAt(level, pos);
        }
        boolean working = !forge.input.isEmpty() && forge.force >= MIN_FORCE && forge.heat >= heatFor(forge.input);
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
        }
        if (!working) {
            return;
        }
        forge.progress += forge.force;
        if (forge.progress >= workFor(forge.input)) {
            forge.progress = 0;
            forge.heat -= heatFor(forge.input);
            ItemStack result = resultFor(forge.input);
            if (!isSteel(forge.input) && level.random.nextBoolean()) {
                result.grow(1);
            }
            forge.input.shrink(1);
            Block.popResource(level, pos.above(), result);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
        }
        forge.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.saveOptional(registries));
        tag.putInt("Heat", heat);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        heat = tag.getInt("Heat");
        progress = tag.getInt("Progress");
    }
}
