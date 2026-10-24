package com.walnutbeachdigital.epochsmith.information;

import com.walnutbeachdigital.epochsmith.block.StatusDescriber;
import java.util.List;
import java.util.UUID;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The Information era's signature invention. Wire it to power and it boots up,
 * drawing 64 energy per tick for 10 seconds. Once online it needs only 8 per tick
 * to stay on, and the player who placed it reaches the Future era.
 */
public class ComputerCoreBlockEntity extends BlockEntity implements StatusDescriber {

    public static final int BOOT_ENERGY = 12800;
    public static final int BOOT_PER_TICK = 64;
    public static final int IDLE_PER_TICK = 8;
    private static final int REFRESH_INTERVAL = 20;

    private List<BlockPos> sources = List.of();
    private UUID owner = null;
    private int booted = 0;

    public ComputerCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPUTER_CORE.get(), pos, state);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    public boolean isOnline() {
        return booted >= BOOT_ENERGY;
    }

    public Component describe() {
        if (isOnline()) {
            return Component.translatable(getBlockState().getValue(BlockStateProperties.LIT)
                    ? "message.epochsmith.computer_core.online" : "message.epochsmith.computer_core.offline");
        }
        return Component.translatable("message.epochsmith.computer_core.booting", booted * 100 / BOOT_ENERGY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ComputerCoreBlockEntity core) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            core.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean running;
        if (!core.isOnline()) {
            int drawn = ElectricNetwork.draw(level, core.sources, BOOT_PER_TICK, false);
            core.booted = Math.min(BOOT_ENERGY, core.booted + drawn);
            running = false;
            if (drawn > 0) {
                core.setChanged();
                if (core.isOnline()) {
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
                    running = true;
                } else if (level.getGameTime() % 10 == 0) {
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.4F,
                            0.5F + core.booted * 1.5F / BOOT_ENERGY);
                }
            }
        } else {
            running = ElectricNetwork.draw(level, core.sources, IDLE_PER_TICK, false) >= IDLE_PER_TICK;
        }
        if (state.getValue(BlockStateProperties.LIT) != running) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, running), Block.UPDATE_ALL);
        }
        if (running && level.getGameTime() % REFRESH_INTERVAL == 0 && core.owner != null
                && level.getPlayerByUUID(core.owner) instanceof ServerPlayer player) {
            EraManager.advanceTo(player, Era.FUTURE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("Booted", booted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        booted = tag.getInt("Booted");
    }
}
