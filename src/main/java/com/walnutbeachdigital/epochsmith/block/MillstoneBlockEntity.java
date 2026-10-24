package com.walnutbeachdigital.epochsmith.block;

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

/**
 * Grinds one item at a time while turned by rotation power. More force grinds faster:
 * at force 4 (a water wheel in flowing water) each item takes 2.5 seconds.
 */
public class MillstoneBlockEntity extends BlockEntity implements StatusDescriber {

    public static final int WORK_PER_ITEM = 200;
    public static final int MIN_FORCE = 2;
    public static final int MAX_ITEMS = 16;
    private static final int POWER_CHECK_INTERVAL = 10;

    private ItemStack input = ItemStack.EMPTY;
    private int progress = 0;
    private int force = 0;

    public MillstoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MILLSTONE.get(), pos, state);
    }

    // ---- Millstone recipes ----

    /** What grinding one of these makes, or EMPTY if it can't be ground. */
    public static ItemStack resultFor(ItemStack stack) {
        if (stack.is(Items.COBBLESTONE)) {
            return new ItemStack(Items.GRAVEL);
        }
        if (stack.is(Items.GRAVEL)) {
            return new ItemStack(Items.SAND);
        }
        if (stack.is(Items.BONE)) {
            return new ItemStack(Items.BONE_MEAL, 4);
        }
        if (stack.is(Items.SUGAR_CANE)) {
            return new ItemStack(Items.SUGAR, 2);
        }
        return ItemStack.EMPTY;
    }

    public static boolean canGrind(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    // ---- Contents ----

    public ItemStack getInput() {
        return input;
    }

    public int getForce() {
        return force;
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

    public ItemStack takeAll() {
        ItemStack taken = input;
        input = ItemStack.EMPTY;
        progress = 0;
        setChanged();
        return taken;
    }

    /** A short status line, e.g. "Millstone: 6 left, force 4". */
    public Component describe() {
        return Component.translatable("message.epochsmith.millstone.status", input.getCount(), force);
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, MillstoneBlockEntity mill) {
        if (level.getGameTime() % POWER_CHECK_INTERVAL == 0) {
            mill.force = MechanicalPower.forceAt(level, pos);
        }
        boolean working = !mill.input.isEmpty() && mill.force >= MIN_FORCE;
        if (state.getValue(MillstoneBlock.ACTIVE) != working) {
            level.setBlock(pos, state.setValue(MillstoneBlock.ACTIVE, working), Block.UPDATE_ALL);
        }
        if (!working) {
            return;
        }
        mill.progress += mill.force;
        if (level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.3F, 0.7F);
        }
        if (mill.progress >= WORK_PER_ITEM) {
            mill.progress = 0;
            Block.popResource(level, pos.above(), resultFor(mill.input));
            mill.input.shrink(1);
        }
        mill.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Input", input.saveOptional(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        progress = tag.getInt("Progress");
    }
}
