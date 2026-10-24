package com.walnutbeachdigital.epochsmith.future;

import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
import java.util.List;
import java.util.UUID;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;
import com.walnutbeachdigital.epochsmith.information.ItemVault;
import com.walnutbeachdigital.epochsmith.information.VaultStorage;
import com.walnutbeachdigital.epochsmith.item.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A quantum storage cell. Its items live in a quantum network (saved with the world), not in the block,
 * so every cell with the same link shares one inventory, anywhere. Breaking a cell loses nothing:
 * the dropped cell keeps its link. The terminal needs 32 energy per tick.
 */
public class QuantumCellBlockEntity extends BlockEntity implements ItemVault, StatusDescriber {

    public static final int ENERGY_PER_TICK = 32;
    private static final int REFRESH_INTERVAL = 20;

    private UUID link = null;
    private List<BlockPos> sources = List.of();

    /** Hoppers talk to whichever network the cell is linked to at that moment. */
    private final IItemHandler automation = new IItemHandler() {
        private IItemHandler target() {
            return getStorage().getAutomation();
        }

        @Override
        public int getSlots() {
            return target().getSlots();
        }

        @Override
        public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
            return target().getStackInSlot(slot);
        }

        @Override
        public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
            return target().insertItem(slot, stack, simulate);
        }

        @Override
        public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
            return target().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
            return true;
        }
    };

    public QuantumCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_CELL.get(), pos, state);
    }

    public IItemHandler getAutomation() {
        return automation;
    }

    /** This cell's link, creating a brand-new network the first time it's needed. */
    public UUID getLink() {
        if (link == null) {
            link = UUID.randomUUID();
            setChanged();
        }
        return link;
    }

    @Override
    public VaultStorage getStorage() {
        if (!(level instanceof ServerLevel server)) {
            // Clients never touch storage directly; they see it through the terminal
            return new VaultStorage(0, 0, () -> { });
        }
        return QuantumNetworks.get(server.getServer()).storage(getLink());
    }

    @Override
    public boolean isPowered() {
        return getBlockState().getValue(BlockStateProperties.LIT);
    }

    /** The first 8 characters of the link, so players can tell networks apart. */
    public String shortLink() {
        return getLink().toString().substring(0, 8);
    }

    public Component describe() {
        VaultStorage storage = getStorage();
        return Component.translatable("message.epochsmith.quantum_cell.status", shortLink(), storage.getTotal(),
                storage.getCapacity(), storage.getEntries().size(), storage.getMaxTypes());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, QuantumCellBlockEntity cell) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            cell.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean powered = ElectricNetwork.draw(level, cell.sources, ENERGY_PER_TICK, false) >= ENERGY_PER_TICK;
        if (state.getValue(BlockStateProperties.LIT) != powered) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, powered), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (link != null) {
            tag.putUUID("Link", link);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        link = tag.hasUUID("Link") ? tag.getUUID("Link") : null;
    }

    // The link travels with the item when the cell is broken, and is applied when it's placed

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.QUANTUM_LINK.get(), getLink());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        UUID fromItem = input.get(ModDataComponents.QUANTUM_LINK.get());
        if (fromItem != null) {
            link = fromItem;
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Link");
    }
}
