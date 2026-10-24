package com.walnutbeachdigital.epochsmith.steam;

import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
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
public class LatheBlockEntity extends BlockEntity implements WorkpieceHolder, StatusDescriber, MachineGui {

    public static final int MAX_PIECES = 16;
    private static final int CHECK_INTERVAL = 10;

    public static final MachineLayout LAYOUT = new MachineLayout(4)
            .input(56, 35).output(116, 35).arrow(79, 34, 0, 1)
            .status(8, 60, 2, "gui.epochsmith.steam_status");

    private final MachineItems items = new MachineItems(this::changed, (slot, stack) -> canWork(stack),
            MachineItems.Role.INPUT, MachineItems.Role.OUTPUT).limit(0, MAX_PIECES);
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
        return items.getStackInSlot(0);
    }

    public boolean hasSteam() {
        return steam;
    }

    public Component describe() {
        return Component.translatable(steam ? "message.epochsmith.lathe.status" : "message.epochsmith.lathe.no_steam",
                getWorkpiece().getCount());
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
            case 1 -> getWorkpiece().isEmpty() ? 1 : workFor(getWorkpiece());
            case 2 -> steam ? 1 : 0;
            default -> 0;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LatheBlockEntity lathe) {
        if (level.getGameTime() % CHECK_INTERVAL == 0) {
            lathe.steam = SteamNetwork.explore(level, pos).workingBoilers() > 0;
        }
        ItemStack piece = lathe.getWorkpiece();
        boolean working = lathe.steam && !piece.isEmpty() && lathe.items.fitsOutput(resultFor(piece));
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
        if (lathe.progress >= workFor(piece)) {
            lathe.progress = 0;
            lathe.items.addOutput(resultFor(piece));
            lathe.items.extractItem(0, 1, false);
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
        tag.put("Items", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
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
