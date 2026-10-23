package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The metal lying on the anvil and how many hammer strikes it has taken.
 * The workpiece is also sent to players' games so it can be drawn on top of the anvil.
 */
public class StoneAnvilBlockEntity extends BlockEntity implements WorkpieceHolder {

    /** Most pieces you can stack on the anvil at once. Each finished piece pops off on its own. */
    public static final int MAX_PIECES = 16;

    private ItemStack workpiece = ItemStack.EMPTY;
    private int strikes = 0;

    public StoneAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STONE_ANVIL.get(), pos, state);
    }

    // ---- Anvil recipes ----

    /** What hammering one of these items makes, or EMPTY if it can't be worked. */
    public static ItemStack resultFor(ItemStack input) {
        if (input.is(ModItems.IRON_BLOOM.get())) {
            return new ItemStack(ModItems.WROUGHT_IRON_INGOT.get());
        }
        if (input.is(ModItems.BRONZE_INGOT.get())) {
            return new ItemStack(ModItems.BRONZE_PLATE.get());
        }
        return ItemStack.EMPTY;
    }

    /** How many strikes one piece needs. Iron blooms take longer: they have slag to beat out. */
    public static int strikesFor(ItemStack input) {
        return input.is(ModItems.IRON_BLOOM.get()) ? 6 : 4;
    }

    public static boolean canWork(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    // ---- Workpiece ----

    @Override
    public ItemStack getWorkpiece() {
        return workpiece;
    }

    /** Puts as many pieces as fit onto the anvil. Returns true if any went on. */
    public boolean place(ItemStack stack) {
        if (workpiece.isEmpty()) {
            workpiece = stack.split(Math.min(stack.getCount(), MAX_PIECES));
            strikes = 0;
            changed();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(workpiece, stack) && workpiece.getCount() < MAX_PIECES) {
            int moved = Math.min(stack.getCount(), MAX_PIECES - workpiece.getCount());
            workpiece.grow(moved);
            stack.shrink(moved);
            changed();
            return true;
        }
        return false;
    }

    public ItemStack takeAll() {
        ItemStack taken = workpiece;
        workpiece = ItemStack.EMPTY;
        strikes = 0;
        changed();
        return taken;
    }

    /**
     * One hammer strike. Returns the finished piece when this strike completes one, otherwise EMPTY.
     */
    public ItemStack strike() {
        if (workpiece.isEmpty()) {
            return ItemStack.EMPTY;
        }
        strikes++;
        ItemStack finished = ItemStack.EMPTY;
        if (strikes >= strikesFor(workpiece)) {
            finished = resultFor(workpiece);
            workpiece.shrink(1);
            strikes = 0;
        }
        changed();
        return finished;
    }

    /** Saves and tells nearby players' games, so the drawn workpiece stays correct. */
    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Saving and syncing ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Workpiece", workpiece.saveOptional(registries));
        tag.putInt("Strikes", strikes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
        strikes = tag.getInt("Strikes");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
