package com.walnutbeachdigital.epochsmith.block;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The strongbox's 54 slots and its owner.
 * It is a "worldly" container with no accessible faces, which is how hoppers and
 * other automation are kept out: only players opening it by hand can reach the items.
 */
public class StrongboxBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int SIZE = 54;
    private static final int[] NO_SLOTS = new int[0];

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    @Nullable
    private UUID owner = null;
    private String ownerName = "";
    private boolean locked = true;

    public StrongboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRONGBOX.get(), pos, state);
    }

    // ---- Ownership ----

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
    }

    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    /** Whether this player may open or break it. Creative players always can. */
    public boolean canUse(Player player) {
        return !locked || isOwner(player) || player.getAbilities().instabuild;
    }

    public boolean isLocked() {
        return locked;
    }

    public void toggleLock() {
        locked = !locked;
        setChanged();
    }

    public String getOwnerName() {
        return ownerName;
    }

    // ---- Container ----

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.epochsmith.strongbox");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.sixRows(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    // ---- Keep hoppers and pipes out ----

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    // ---- Saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("OwnerName", ownerName);
        tag.putBoolean("Locked", locked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        locked = !tag.contains("Locked") || tag.getBoolean("Locked");
    }
}
