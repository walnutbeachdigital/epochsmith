package com.walnutbeachdigital.epochsmith.steam;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.item.ModItems;

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
 * Smelts with a steam-driven air blast and coke.
 *  - Raw iron, copper, gold or tin: always 2 ingots.
 *  - Iron ingot: 1 crucible steel ingot (steel from ordinary iron, like the Bessemer process).
 * 2 seconds per item while it has steam; each item uses 1 heat, each coke adds 8.
 */
public class BlastFurnaceBlockEntity extends BlockEntity {

    public static final int WORK_PER_ITEM = 40;
    public static final int MAX_ITEMS = 16;
    public static final int HEAT_PER_COKE = 8;
    public static final int MAX_HEAT = 64;
    private static final int CHECK_INTERVAL = 10;

    private ItemStack input = ItemStack.EMPTY;
    private int heat = 0;
    private int progress = 0;
    private boolean steam = false;

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_FURNACE.get(), pos, state);
    }

    public static ItemStack resultFor(ItemStack stack) {
        if (stack.is(Items.RAW_IRON)) {
            return new ItemStack(Items.IRON_INGOT, 2);
        }
        if (stack.is(Items.RAW_COPPER)) {
            return new ItemStack(Items.COPPER_INGOT, 2);
        }
        if (stack.is(Items.RAW_GOLD)) {
            return new ItemStack(Items.GOLD_INGOT, 2);
        }
        if (stack.is(ModItems.RAW_TIN.get())) {
            return new ItemStack(ModItems.TIN_INGOT.get(), 2);
        }
        if (stack.is(Items.IRON_INGOT)) {
            return new ItemStack(ModItems.STEEL_INGOT.get());
        }
        return ItemStack.EMPTY;
    }

    public static boolean canSmelt(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

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

    /** Adds coke until full. Returns how many were used. */
    public int addCoke(int count) {
        int used = Math.min(count, (MAX_HEAT - heat) / HEAT_PER_COKE);
        heat += used * HEAT_PER_COKE;
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
        return Component.translatable("message.epochsmith.blast_furnace.status", input.getCount(), heat,
                Component.translatable(steam ? "message.epochsmith.blast_furnace.steam_on" : "message.epochsmith.blast_furnace.steam_off"));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlastFurnaceBlockEntity furnace) {
        if (level.getGameTime() % CHECK_INTERVAL == 0) {
            furnace.steam = SteamNetwork.explore(level, pos).workingBoilers() > 0;
        }
        boolean working = furnace.steam && furnace.heat > 0 && !furnace.input.isEmpty();
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
        }
        if (!working) {
            return;
        }
        furnace.progress++;
        if (furnace.progress >= WORK_PER_ITEM) {
            furnace.progress = 0;
            furnace.heat--;
            Block.popResource(level, pos.above(), resultFor(furnace.input));
            furnace.input.shrink(1);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.2F);
        }
        furnace.setChanged();
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
