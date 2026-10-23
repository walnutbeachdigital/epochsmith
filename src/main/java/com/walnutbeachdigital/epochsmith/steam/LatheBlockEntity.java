package com.walnutbeachdigital.epochsmith.steam;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.block.WorkpieceHolder;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns precision parts while it has steam (a working boiler on its pipe network).
 *  wrought iron ingot -> 4 rivets (3 seconds)
 *  copper ingot -> 1 pressure gauge (6 seconds)
 *  steel ingot -> 1 steam piston (8 seconds)
 */
public class LatheBlockEntity extends BlockEntity implements WorkpieceHolder {

    public static final int MAX_PIECES = 16;
    private static final int CHECK_INTERVAL = 10;

    private ItemStack workpiece = ItemStack.EMPTY;
    private int progress = 0;
    private boolean steam = false;

    public LatheBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LATHE.get(), pos, state);
    }

    // ---- Lathe recipes ----

    public static ItemStack resultFor(ItemStack input) {
        if (input.is(ModItems.WROUGHT_IRON_INGOT.get())) {
            return new ItemStack(ModItems.RIVETS.get(), 4);
        }
        if (input.is(Items.COPPER_INGOT)) {
            return new ItemStack(ModItems.PRESSURE_GAUGE.get());
        }
        if (input.is(ModItems.STEEL_INGOT.get())) {
            return new ItemStack(ModItems.STEAM_PISTON.get());
        }
        return ItemStack.EMPTY;
    }

    /** Ticks of steam needed per piece. */
    public static int workFor(ItemStack input) {
        if (input.is(ModItems.STEEL_INGOT.get())) {
            return 160;
        }
        return input.is(Items.COPPER_INGOT) ? 120 : 60;
    }

    public static boolean canWork(ItemStack stack) {
        return !resultFor(stack).isEmpty();
    }

    // ---- Workpiece ----

    @Override
    public ItemStack getWorkpiece() {
        return workpiece;
    }

    public boolean hasSteam() {
        return steam;
    }

    public boolean place(ItemStack stack) {
        if (workpiece.isEmpty()) {
            workpiece = stack.split(Math.min(stack.getCount(), MAX_PIECES));
            progress = 0;
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
        progress = 0;
        changed();
        return taken;
    }

    public Component describe() {
        return Component.translatable(steam ? "message.epochsmith.lathe.status" : "message.epochsmith.lathe.no_steam",
                workpiece.getCount());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LatheBlockEntity lathe) {
        if (level.getGameTime() % CHECK_INTERVAL == 0) {
            lathe.steam = SteamNetwork.explore(level, pos).workingBoilers() > 0;
        }
        boolean working = lathe.steam && !lathe.workpiece.isEmpty();
        if (state.getValue(LatheBlock.ACTIVE) != working) {
            level.setBlock(pos, state.setValue(LatheBlock.ACTIVE, working), Block.UPDATE_ALL);
        }
        if (!working) {
            return;
        }
        lathe.progress++;
        if (level.getGameTime() % 8 == 0 && level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    3, 0.2, 0.02, 0.2, 0.1);
        }
        if (level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.25F, 1.6F);
        }
        if (lathe.progress >= workFor(lathe.workpiece)) {
            lathe.progress = 0;
            Block.popResource(level, pos.above(), resultFor(lathe.workpiece));
            lathe.workpiece.shrink(1);
            level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.7F, 1.3F);
            lathe.changed();
        } else {
            lathe.setChanged();
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Workpiece", workpiece.saveOptional(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
        progress = tag.getInt("Progress");
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
