package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
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
public class MillstoneBlockEntity extends BlockEntity implements StatusDescriber, MachineGui {

    public static final int WORK_PER_ITEM = 200;
    public static final int MIN_FORCE = 2;
    public static final int MAX_ITEMS = 16;
    private static final int POWER_CHECK_INTERVAL = 10;

    public static final MachineLayout LAYOUT = new MachineLayout(4).theme("mechanical")
            .input(56, 35).output(116, 35).arrow(79, 34, 0, 1)
            .gaugeOf(152, 17, 2, 16, MachineLayout.FORCE, "gui.epochsmith.force");

    private final MachineItems items = new MachineItems(this::setChanged, (slot, stack) -> canGrind(stack),
            MachineItems.Role.INPUT, MachineItems.Role.OUTPUT);
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
        return items.getStackInSlot(0);
    }

    public int getForce() {
        return force;
    }

    /** A short status line, e.g. "Millstone: 6 left, force 4". */
    public Component describe() {
        return Component.translatable("message.epochsmith.millstone.status", getInput().getCount(), force);
    }

    // ---- GUI ----

    @Override
    public MachineItems getMenuItems() {
        return items;
    }

    @Override
    public MachineLayout getLayout() {
        return LAYOUT;
    }

    @Override
    public int getMenuValue(int index) {
        return switch (index) {
            case 0 -> progress;
            case 1 -> WORK_PER_ITEM;
            case 2 -> force;
            default -> 0;
        };
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, MillstoneBlockEntity mill) {
        if (level.getGameTime() % POWER_CHECK_INTERVAL == 0) {
            mill.force = MechanicalPower.forceAt(level, pos);
        }
        boolean working = !mill.getInput().isEmpty() && mill.force >= MIN_FORCE
                && mill.items.fitsOutput(resultFor(mill.getInput()));
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
            mill.items.addOutput(resultFor(mill.getInput()));
            mill.items.extractItem(0, 1, false);
        }
        mill.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
        progress = tag.getInt("Progress");
    }
}
