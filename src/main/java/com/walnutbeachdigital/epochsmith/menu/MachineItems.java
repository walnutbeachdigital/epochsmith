package com.walnutbeachdigital.epochsmith.menu;

import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A machine's item slots. Each slot has a role: things go INTO input and fuel slots, results come OUT of
 * output slots, and a pattern slot holds one item that tells the machine what to make (it isn't used up).
 *
 * The machine itself (and its GUI) can use every slot freely. Hoppers and pipes get a stricter view
 * through {@link #automation()}: they can only fill input and fuel slots and only empty output slots.
 */
public class MachineItems extends ItemStackHandler {

    public enum Role { INPUT, FUEL, OUTPUT, PATTERN }

    private final Role[] roles;
    private final BiPredicate<Integer, ItemStack> accepts;
    private final Runnable onChange;
    private final IItemHandler automation = new Automation();
    private final int[] limits;

    /**
     * @param accepts which items each input, fuel or pattern slot takes (slot number, item)
     */
    public MachineItems(Runnable onChange, BiPredicate<Integer, ItemStack> accepts, Role... roles) {
        super(roles.length);
        this.roles = roles;
        this.accepts = accepts;
        this.onChange = onChange;
        this.limits = new int[roles.length];
        java.util.Arrays.fill(limits, 64);
    }

    /** Caps how many items one slot holds (e.g. a kiln that fires 16 at a time). */
    public MachineItems limit(int slot, int max) {
        limits[slot] = max;
        return this;
    }

    public Role role(int slot) {
        return roles[slot];
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return roles[slot] == Role.OUTPUT || accepts.test(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return roles[slot] == Role.PATTERN ? 1 : limits[slot];
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }

    /** True if this whole stack would fit in the output slots. */
    public boolean fitsOutput(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < getSlots() && !rest.isEmpty(); i++) {
            if (roles[i] == Role.OUTPUT) {
                rest = insertItem(i, rest, true);
            }
        }
        return rest.isEmpty();
    }

    /** Puts a result into the output slots. Returns whatever didn't fit. */
    public ItemStack addOutput(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < getSlots() && !rest.isEmpty(); i++) {
            if (roles[i] == Role.OUTPUT) {
                rest = insertItem(i, rest, false);
            }
        }
        return rest;
    }

    /** The view hoppers and pipes get. */
    public IItemHandler automation() {
        return automation;
    }

    public void dropAll(Level level, BlockPos pos) {
        for (int i = 0; i < getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), getStackInSlot(i).copy());
            setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private class Automation implements IItemHandler {
        @Override
        public int getSlots() {
            return MachineItems.this.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return MachineItems.this.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            Role role = roles[slot];
            if (role == Role.OUTPUT || role == Role.PATTERN) {
                return stack;
            }
            return MachineItems.this.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return roles[slot] == Role.OUTPUT ? MachineItems.this.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return MachineItems.this.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            Role role = roles[slot];
            return role != Role.OUTPUT && role != Role.PATTERN && MachineItems.this.isItemValid(slot, stack);
        }
    }
}
