package com.walnutbeachdigital.epochsmith.future;

import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineGui;
import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
import java.util.List;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.block.WorkpieceHolder;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Future era's materials machine. Builds things atom by atom:
 *  - 1 block of coal -> 4 graphene
 *  - 1 circuit board -> 4 nanobots
 * Each item uses 4,000 energy at up to 200 per tick (one every second on full power).
 */
public class NanofabricatorBlockEntity extends BlockEntity implements WorkpieceHolder, StatusDescriber, MachineGui {

    public static final int ENERGY_PER_ITEM = 4000;
    public static final int ENERGY_PER_TICK = 200;
    public static final int MAX_ITEMS = 64;
    private static final int REFRESH_INTERVAL = 20;

    public static final MachineLayout LAYOUT = new MachineLayout(3).theme("future")
            .input(56, 35).output(116, 35).arrow(79, 34, 0, 1)
            .gaugeOf(152, 17, 2, ENERGY_PER_TICK, MachineLayout.ENERGY, "gui.epochsmith.power");

    private final MachineItems items = new MachineItems(this::changed, (slot, stack) -> !resultFor(stack).isEmpty(),
            MachineItems.Role.INPUT, MachineItems.Role.OUTPUT);
    private int lastDrawn = 0;
    private int progress = 0;
    private List<BlockPos> sources = List.of();

    public NanofabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NANOFABRICATOR.get(), pos, state);
    }

    public static ItemStack resultFor(ItemStack stack) {
        if (stack.is(Items.COAL_BLOCK)) {
            return new ItemStack(ModItems.GRAPHENE.get(), 4);
        }
        if (stack.is(ModItems.CIRCUIT_BOARD.get())) {
            return new ItemStack(ModItems.NANOBOTS.get(), 4);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getWorkpiece() {
        return items.getStackInSlot(0);
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
            case 1 -> ENERGY_PER_ITEM;
            case 2 -> lastDrawn;
            default -> 0;
        };
    }

    public Component describe() {
        return Component.translatable(getBlockState().getValue(BlockStateProperties.LIT)
                ? "message.epochsmith.nanofabricator.working" : "message.epochsmith.nanofabricator.idle", getWorkpiece().getCount());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NanofabricatorBlockEntity fab) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            fab.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean working = false;
        ItemStack input = fab.getWorkpiece();
        fab.lastDrawn = 0;
        if (!input.isEmpty() && fab.items.fitsOutput(resultFor(input))) {
            int drawn = ElectricNetwork.draw(level, fab.sources, ENERGY_PER_TICK, false);
            fab.lastDrawn = drawn;
            working = drawn > 0;
            fab.progress += drawn;
            if (fab.progress >= ENERGY_PER_ITEM) {
                fab.progress -= ENERGY_PER_ITEM;
                fab.items.addOutput(resultFor(input));
                fab.items.extractItem(0, 1, false);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.4F);
                fab.changed();
            } else {
                fab.setChanged();
            }
        }
        if (state.getValue(BlockStateProperties.LIT) != working) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, working), Block.UPDATE_ALL);
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
