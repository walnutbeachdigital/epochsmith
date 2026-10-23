package com.walnutbeachdigital.epochsmith.block;

import java.util.UUID;

import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Holds the pit kiln's contents and runs the firing timer.
 */
public class PitKilnBlockEntity extends BlockEntity {

    /** One firing takes 20 seconds (20 ticks per second) and fires the whole batch at once. */
    public static final int FIRE_TIME = 400;
    /** Most items the kiln holds at once. */
    public static final int MAX_ITEMS = 16;

    private ItemStack contents = ItemStack.EMPTY;
    private int progress = 0;
    private UUID igniter = null;

    public PitKilnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIT_KILN.get(), pos, state);
    }

    // ---- What the kiln can fire ----

    /** The fired result for an item, or EMPTY if the kiln can't fire it. */
    public static ItemStack resultFor(ItemStack input) {
        if (input.is(Items.CLAY_BALL)) {
            return new ItemStack(Items.BRICK, input.getCount());
        }
        if (input.is(ModItems.UNFIRED_AMPHORA.get())) {
            return new ItemStack(ModBlocks.AMPHORA_ITEM.get(), input.getCount());
        }
        if (input.is(ItemTags.LOGS_THAT_BURN)) {
            return new ItemStack(Items.CHARCOAL, input.getCount());
        }
        return ItemStack.EMPTY;
    }

    public static boolean canFire(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    // ---- Contents ----

    public ItemStack getContents() {
        return contents;
    }

    public boolean hasFirableContents() {
        return canFire(contents);
    }

    /** Puts as much of the stack in as fits. Returns true if anything went in. */
    public boolean insert(ItemStack stack) {
        if (contents.isEmpty()) {
            contents = stack.split(Math.min(stack.getCount(), MAX_ITEMS));
            setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(contents, stack) && contents.getCount() < MAX_ITEMS) {
            int moved = Math.min(stack.getCount(), MAX_ITEMS - contents.getCount());
            contents.grow(moved);
            stack.shrink(moved);
            setChanged();
            return true;
        }
        return false;
    }

    /** Takes everything out. */
    public ItemStack takeAll() {
        ItemStack taken = contents;
        contents = ItemStack.EMPTY;
        setChanged();
        return taken;
    }

    /** Starts a firing. The player who lit it is credited when it finishes. */
    public void ignite(Player player) {
        progress = 0;
        igniter = player.getUUID();
        setChanged();
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, PitKilnBlockEntity kiln) {
        if (!state.getValue(BlockStateProperties.LIT)) {
            return;
        }
        kiln.progress++;
        if (kiln.progress >= FIRE_TIME) {
            kiln.finishFiring(level, pos, state);
        }
    }

    private void finishFiring(Level level, BlockPos pos, BlockState state) {
        contents = resultFor(contents);
        progress = 0;
        level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);

        // The first successful firing is the Primitive era's signature invention
        if (igniter != null && level.getPlayerByUUID(igniter) instanceof ServerPlayer player) {
            EraManager.advanceTo(player, Era.BRONZE_IRON);
        }
        igniter = null;
        setChanged();
    }

    // ---- Saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Contents", contents.saveOptional(registries));
        tag.putInt("Progress", progress);
        if (igniter != null) {
            tag.putUUID("Igniter", igniter);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents = ItemStack.parseOptional(registries, tag.getCompound("Contents"));
        progress = tag.getInt("Progress");
        igniter = tag.hasUUID("Igniter") ? tag.getUUID("Igniter") : null;
    }
}
