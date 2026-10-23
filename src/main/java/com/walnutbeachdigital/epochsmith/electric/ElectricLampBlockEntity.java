package com.walnutbeachdigital.epochsmith.electric;

import java.util.List;
import java.util.UUID;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
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

/**
 * Uses 2 energy per tick to shine at full brightness. The first time a lamp you placed
 * lights up, you enter the Information era (the Electrical era's signature invention).
 */
public class ElectricLampBlockEntity extends BlockEntity {

    public static final int ENERGY_PER_TICK = 2;
    private static final int REFRESH_INTERVAL = 20;

    private List<BlockPos> sources = List.of();
    private UUID owner = null;

    public ElectricLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_LAMP.get(), pos, state);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ElectricLampBlockEntity lamp) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            lamp.sources = ElectricNetwork.findSources(level, pos);
        }
        boolean lit = ElectricNetwork.draw(level, lamp.sources, ENERGY_PER_TICK, false) >= ENERGY_PER_TICK;
        if (state.getValue(BlockStateProperties.LIT) != lit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_ALL);
        }
        if (lit && level.getGameTime() % REFRESH_INTERVAL == 0 && lamp.owner != null
                && level.getPlayerByUUID(lamp.owner) instanceof ServerPlayer player) {
            EraManager.advanceTo(player, Era.INFORMATION);
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
