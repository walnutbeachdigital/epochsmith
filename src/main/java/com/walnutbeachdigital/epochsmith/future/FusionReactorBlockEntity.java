package com.walnutbeachdigital.epochsmith.future;

import java.util.List;
import java.util.UUID;

import com.walnutbeachdigital.epochsmith.block.ModBlockEntities;
import com.walnutbeachdigital.epochsmith.electric.ElectricNetwork;
import com.walnutbeachdigital.epochsmith.electric.EnergySource;
import com.walnutbeachdigital.epochsmith.electric.EpochEnergyStorage;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Future era's signature invention, and the end goal of Epochsmith.
 *
 * Load it with plasma cells (up to 16). The first time it starts, it needs 50,000 energy
 * from the wire network to heat the plasma (ignition). Once lit, every plasma cell burns
 * for two minutes making 1,024 energy per tick, and it stays lit as long as it has fuel.
 * If it runs dry, it has to be ignited again.
 */
public class FusionReactorBlockEntity extends BlockEntity implements EnergySource {

    public static final int MAX_FUEL = 16;
    public static final int IGNITION_ENERGY = 50000;
    public static final int IGNITION_PER_TICK = 500;
    public static final int OUTPUT_PER_TICK = 1024;
    public static final int BURN_TIME = 2400;
    private static final int REFRESH_INTERVAL = 20;

    private final EpochEnergyStorage energy = new EpochEnergyStorage(1000000, 0, 1000000, this::setChanged);
    private int fuel = 0;
    private int burn = 0;
    private int ignition = 0;
    private boolean announced = false;
    private UUID owner = null;
    private List<BlockPos> sources = List.of();

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_REACTOR.get(), pos, state);
    }

    public EpochEnergyStorage getEnergy() {
        return energy;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    @Override
    public int drawEnergy(int max) {
        return energy.take(max);
    }

    public boolean isLit() {
        return burn > 0;
    }

    /** Adds plasma cells from the stack. Returns how many went in. */
    public int addFuel(ItemStack stack) {
        if (!stack.is(ModItems.PLASMA_CELL.get())) {
            return 0;
        }
        int added = Math.min(stack.getCount(), MAX_FUEL - fuel);
        if (added > 0) {
            fuel += added;
            stack.shrink(added);
            setChanged();
        }
        return added;
    }

    public ItemStack takeFuel() {
        ItemStack taken = new ItemStack(ModItems.PLASMA_CELL.get(), fuel);
        fuel = 0;
        setChanged();
        return taken;
    }

    public Component describe() {
        if (isLit()) {
            return Component.translatable("message.epochsmith.fusion.lit", OUTPUT_PER_TICK, fuel, burn / 20,
                    energy.getEnergyStored(), energy.getMaxEnergyStored());
        }
        if (fuel == 0) {
            return Component.translatable("message.epochsmith.fusion.no_fuel");
        }
        return Component.translatable("message.epochsmith.fusion.igniting", ignition * 100 / IGNITION_ENERGY, fuel);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FusionReactorBlockEntity reactor) {
        if (level.getGameTime() % REFRESH_INTERVAL == 0) {
            reactor.sources = ElectricNetwork.findSources(level, pos);
        }
        if (reactor.burn > 0) {
            reactor.burn--;
            reactor.energy.generate(OUTPUT_PER_TICK);
            if (reactor.burn == 0 && reactor.fuel > 0) {
                // Feed the next cell into the plasma without losing ignition
                reactor.fuel--;
                reactor.burn = BURN_TIME;
            }
            if (level.getGameTime() % 40 == 0) {
                level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.8F, 0.6F);
            }
            reactor.setChanged();
        } else if (reactor.fuel > 0) {
            // Ignition: heat the plasma with power from the network
            int drawn = ElectricNetwork.draw(level, reactor.sources, IGNITION_PER_TICK, false);
            if (drawn > 0) {
                reactor.ignition += drawn;
                if (level.getGameTime() % 10 == 0) {
                    level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F,
                            0.5F + reactor.ignition * 1.5F / IGNITION_ENERGY);
                }
                if (reactor.ignition >= IGNITION_ENERGY) {
                    reactor.ignition = 0;
                    reactor.fuel--;
                    reactor.burn = BURN_TIME;
                    level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 1.4F);
                    reactor.celebrate(level);
                }
                reactor.setChanged();
            }
        }
        boolean lit = reactor.burn > 0;
        if (state.getValue(BlockStateProperties.LIT) != lit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_ALL);
        }
    }

    /** The first ignition of a reactor shows its builder the finale. */
    private void celebrate(Level level) {
        if (announced || owner == null || !(level.getPlayerByUUID(owner) instanceof ServerPlayer player)) {
            return;
        }
        announced = true;
        player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 100, 40));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.epochsmith.fusion.finale_sub")));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("message.epochsmith.fusion.finale")));
        player.sendSystemMessage(Component.translatable("message.epochsmith.fusion.finale_chat"));
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Fuel", fuel);
        tag.putInt("Burn", burn);
        tag.putInt("Ignition", ignition);
        tag.putBoolean("Announced", announced);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
        fuel = tag.getInt("Fuel");
        burn = tag.getInt("Burn");
        ignition = tag.getInt("Ignition");
        announced = tag.getBoolean("Announced");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
