package com.walnutbeachdigital.epochsmith.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The one menu every Epochsmith machine uses. It lays out the machine's slots from its
 * {@link MachineLayout}, adds the player's inventory underneath, and syncs the machine's values.
 */
public class MachineMenu extends AbstractContainerMenu {

    private final BlockPos pos;
    private final MachineLayout layout;
    private final ContainerData data;
    private final MachineGui machine;     // only on the server
    private final int machineSlots;

    /** Server side: wraps the real machine. */
    public MachineMenu(int id, Inventory inventory, BlockEntity blockEntity) {
        this(id, inventory, blockEntity.getBlockPos(), (MachineGui) blockEntity,
                ((MachineGui) blockEntity).getMenuItems(), new Values((MachineGui) blockEntity));
    }

    /** Client side: rebuilt from the position the server sends; slots and values fill in by syncing. */
    public MachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), null, null, null);
    }

    private MachineMenu(int id, Inventory inventory, BlockPos pos, MachineGui machine, IItemHandler items, ContainerData data) {
        super(ModMenus.MACHINE.get(), id);
        this.pos = pos;
        this.machine = machine;
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (!(be instanceof MachineGui gui)) {
            throw new IllegalStateException("No Epochsmith machine at " + pos);
        }
        this.layout = gui.getLayout();
        IItemHandler slots = items != null ? items : new ItemStackHandler(layout.slots.size());
        this.data = data != null ? data : new SimpleContainerData(layout.values * 2);
        for (int i = 0; i < layout.slots.size(); i++) {
            MachineLayout.SlotDef def = layout.slots.get(i);
            addSlot(new MachineSlot(slots, i, def.x(), def.y(), def.role()));
        }
        this.machineSlots = layout.slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, layout.inventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, layout.inventoryY + 58));
        }
        addDataSlots(this.data);
    }

    public MachineLayout getLayout() {
        return layout;
    }

    public BlockPos getPos() {
        return pos;
    }

    /** A machine value, rebuilt from the two 16-bit halves it's synced in. */
    public int getValue(int index) {
        return (data.get(index * 2) & 0xFFFF) | ((data.get(index * 2 + 1) & 0xFFFF) << 16);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return machine != null && machine.onMenuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0
                && player.level().getBlockEntity(pos) instanceof MachineGui;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int inventoryStart = machineSlots;
        int hotbarStart = machineSlots + 27;
        int end = machineSlots + 36;
        if (index < machineSlots) {
            // Machine -> player
            if (!moveItemStackTo(stack, inventoryStart, end, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player -> the first machine slot that takes it
            boolean moved = false;
            for (int i = 0; i < machineSlots && !stack.isEmpty(); i++) {
                Slot target = slots.get(i);
                if (target instanceof MachineSlot machineSlot && machineSlot.role != MachineItems.Role.OUTPUT
                        && target.mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
                boolean fromInventory = index < hotbarStart;
                if (!moveItemStackTo(stack, fromInventory ? hotbarStart : inventoryStart, fromInventory ? end : hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    /** A machine slot: output slots can't be filled by hand, pattern slots hold one item. */
    public static class MachineSlot extends SlotItemHandler {
        public final MachineItems.Role role;

        public MachineSlot(IItemHandler handler, int index, int x, int y, MachineItems.Role role) {
            super(handler, index, x, y);
            this.role = role;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return role != MachineItems.Role.OUTPUT && super.mayPlace(stack);
        }
    }

    /** Server-side values, each split into two 16-bit halves (Minecraft syncs menu data as 16-bit numbers). */
    private static final class Values implements ContainerData {
        private final MachineGui machine;

        Values(MachineGui machine) {
            this.machine = machine;
        }

        @Override
        public int get(int index) {
            int value = machine.getMenuValue(index / 2);
            return index % 2 == 0 ? value & 0xFFFF : (value >>> 16) & 0xFFFF;
        }

        @Override
        public void set(int index, int value) {
            // Values are read live from the machine; nothing to store
        }

        @Override
        public int getCount() {
            return machine.getLayout().values * 2;
        }
    }
}
