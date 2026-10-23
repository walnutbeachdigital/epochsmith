package com.walnutbeachdigital.epochsmith.block;

import java.util.UUID;

import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Holds the bloomery's charge (copper and tin, or raw iron), the finished metal, and the firing timer.
 *
 * Bronze: every 3 copper + 1 tin becomes 4 bronze. Leftovers stay in the bloomery.
 * Iron:   every raw iron becomes 1 iron bloom. Copper/tin and iron can't share a charge.
 */
public class BloomeryBlockEntity extends BlockEntity {

    /** One firing takes 30 seconds. */
    public static final int FIRE_TIME = 600;
    public static final int MAX_COPPER = 12;
    public static final int MAX_TIN = 4;
    public static final int MAX_RAW_IRON = 8;

    private int copper = 0;
    private int tin = 0;
    private int rawIron = 0;
    private ItemStack output = ItemStack.EMPTY;
    private int progress = 0;
    private UUID igniter = null;

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOMERY.get(), pos, state);
    }

    // ---- Reading the charge ----

    public boolean hasOutput() {
        return !output.isEmpty();
    }

    /** Enough loaded to make at least one batch of bronze or one iron bloom. */
    public boolean hasValidCharge() {
        return rawIron > 0 || (copper >= 3 && tin >= 1);
    }

    public boolean hasCopperOrTin() {
        return copper > 0 || tin > 0;
    }

    public boolean hasIron() {
        return rawIron > 0;
    }

    /** A short summary for the action bar, e.g. "Bloomery: 6 copper, 2 tin". */
    public Component describe() {
        if (rawIron > 0) {
            return Component.translatable("message.epochsmith.bloomery.status_iron", rawIron);
        }
        return Component.translatable("message.epochsmith.bloomery.status_bronze", copper, tin);
    }

    // ---- Loading ----

    /** Loads as much copper as fits. Returns how many went in. */
    public int addCopper(int amount) {
        int moved = Math.min(amount, MAX_COPPER - copper);
        copper += moved;
        setChanged();
        return moved;
    }

    public int addTin(int amount) {
        int moved = Math.min(amount, MAX_TIN - tin);
        tin += moved;
        setChanged();
        return moved;
    }

    public int addRawIron(int amount) {
        int moved = Math.min(amount, MAX_RAW_IRON - rawIron);
        rawIron += moved;
        setChanged();
        return moved;
    }

    /** Hands the player everything inside: the finished metal and any unused charge. */
    public void emptyInto(Player player) {
        give(player, output);
        give(player, new ItemStack(Items.COPPER_INGOT, copper));
        give(player, new ItemStack(ModItems.TIN_INGOT.get(), tin));
        give(player, new ItemStack(Items.RAW_IRON, rawIron));
        output = ItemStack.EMPTY;
        copper = 0;
        tin = 0;
        rawIron = 0;
        setChanged();
    }

    /** Everything inside, for dropping when the block is broken. */
    public ItemStack[] contentsForDrop() {
        return new ItemStack[] {
                output.copy(),
                new ItemStack(Items.COPPER_INGOT, copper),
                new ItemStack(ModItems.TIN_INGOT.get(), tin),
                new ItemStack(Items.RAW_IRON, rawIron)
        };
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public void ignite(Player player) {
        progress = 0;
        igniter = player.getUUID();
        setChanged();
    }

    // ---- Ticking ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, BloomeryBlockEntity bloomery) {
        if (!state.getValue(BlockStateProperties.LIT)) {
            return;
        }
        bloomery.progress++;
        if (bloomery.progress >= FIRE_TIME) {
            bloomery.finishFiring(level, pos, state);
        }
    }

    private void finishFiring(Level level, BlockPos pos, BlockState state) {
        boolean madeIron = false;
        if (rawIron > 0) {
            output = new ItemStack(ModItems.IRON_BLOOM.get(), rawIron);
            rawIron = 0;
            madeIron = true;
        } else {
            int batches = Math.min(copper / 3, tin);
            output = new ItemStack(ModItems.BRONZE_INGOT.get(), batches * 4);
            copper -= batches * 3;
            tin -= batches;
        }
        progress = 0;
        level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.8F);

        // Smelting iron for the first time is the Bronze and Iron era's signature invention
        if (madeIron && igniter != null && level.getPlayerByUUID(igniter) instanceof ServerPlayer player) {
            EraManager.advanceTo(player, Era.MECHANICAL);
        }
        igniter = null;
        setChanged();
    }

    // ---- Saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Copper", copper);
        tag.putInt("Tin", tin);
        tag.putInt("RawIron", rawIron);
        tag.put("Output", output.saveOptional(registries));
        tag.putInt("Progress", progress);
        if (igniter != null) {
            tag.putUUID("Igniter", igniter);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        copper = tag.getInt("Copper");
        tin = tag.getInt("Tin");
        rawIron = tag.getInt("RawIron");
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        igniter = tag.hasUUID("Igniter") ? tag.getUUID("Igniter") : null;
    }
}
