package com.walnutbeachdigital.epochsmith.steam;

import java.util.UUID;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Checks its steam supply twice a second and remembers the turning force it makes.
 * The first time it runs, whoever placed it enters the Electrical era.
 */
public class SteamEngineBlockEntity extends BlockEntity {

    private static final int CHECK_INTERVAL = 10;

    private int force = 0;
    private UUID owner = null;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    public int getForce() {
        return force;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity engine) {
        if (level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        engine.force = SteamNetwork.engineForce(level, pos);
        boolean running = engine.force > 0;
        if (state.getValue(SteamEngineBlock.RUNNING) != running) {
            level.setBlock(pos, state.setValue(SteamEngineBlock.RUNNING, running), Block.UPDATE_ALL);
        }
        if (!running) {
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.25F, 0.7F);
        }
        // The steam engine running is the Industrial era's signature invention
        if (engine.owner != null && level.getPlayerByUUID(engine.owner) instanceof ServerPlayer player) {
            EraManager.advanceTo(player, Era.ELECTRICAL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
