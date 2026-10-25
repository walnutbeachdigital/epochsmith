package com.walnutbeachdigital.epochsmith.block;

import java.util.UUID;

import com.walnutbeachdigital.epochsmith.era.Era;
import com.walnutbeachdigital.epochsmith.era.EraManager;
import com.walnutbeachdigital.epochsmith.power.MechanicalPower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * Runs while turned (force 2+). The first time it runs, whoever placed it enters the Industrial era.
 * While running it can read the sky (time, moon, weather) and gives a comparator signal
 * that follows the time of day.
 */
public class GearTrainBlockEntity extends BlockEntity {

    public static final int MIN_FORCE = 2;
    private static final int CHECK_INTERVAL = 10;
    private static final String[] MOON_PHASES = {
            "full", "waning_gibbous", "last_quarter", "waning_crescent",
            "new", "waxing_crescent", "first_quarter", "waxing_gibbous"
    };

    private UUID owner = null;
    private int lastSignal = 0;

    public GearTrainBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEAR_TRAIN.get(), pos, state);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    /** 0 to 15, following the time of day (0 at sunrise). Only while running. */
    public int signal() {
        if (level == null || !getBlockState().getValue(GearTrainBlock.RUNNING)) {
            return 0;
        }
        return (int) ((level.getDayTime() % 24000L) * 16L / 24000L);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GearTrainBlockEntity train) {
        if (level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        boolean running = MechanicalPower.forceAt(level, pos) >= MIN_FORCE;
        if (state.getValue(GearTrainBlock.RUNNING) != running) {
            state = state.setValue(GearTrainBlock.RUNNING, running);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }
        if (!running) {
            return;
        }
        // The mechanism working is the Mechanical era's signature invention
        if (train.owner != null && level.getPlayerByUUID(train.owner) instanceof ServerPlayer player) {
            EraManager.advanceTo(player, Era.INDUSTRIAL);
        }
        int signal = train.signal();
        if (signal != train.lastSignal) {
            train.lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        if (level.getGameTime() % 40 == 0) {
            level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.25F, 0.6F);
        }
    }

    /** Sends the player a reading of the sky. */
    public void readOut(Player player) {
        if (level == null) {
            return;
        }
        long dayTime = level.getDayTime();
        long day = dayTime / 24000L + 1;
        int phase = level.getMoonPhase();
        int nightsToFull = (8 - phase) % 8;

        player.displayClientMessage(Component.translatable("message.epochsmith.gear_train.header"), false);
        player.displayClientMessage(Component.translatable("message.epochsmith.gear_train.day",
                day, Component.translatable("message.epochsmith.gear_train.time." + timeOfDay(dayTime % 24000L))), false);
        player.displayClientMessage(Component.translatable("message.epochsmith.gear_train.moon",
                Component.translatable("message.epochsmith.gear_train.moon." + MOON_PHASES[phase]),
                nightsToFull == 0
                        ? Component.translatable("message.epochsmith.gear_train.full_tonight")
                        : nightsToFull == 1 ? Component.translatable("message.epochsmith.gear_train.full_tomorrow")
                        : Component.translatable("message.epochsmith.gear_train.full_in", nightsToFull)), false);
        player.displayClientMessage(weather(), false);
    }

    private static String timeOfDay(long t) {
        if (t < 1000) return "dawn";
        if (t < 6000) return "morning";
        if (t < 11000) return "afternoon";
        if (t < 13000) return "dusk";
        if (t < 23000) return "night";
        return "dawn";
    }

    /** A forecast from the world's own weather timers. */
    private Component weather() {
        if (!(level.getLevelData() instanceof ServerLevelData data)) {
            return Component.translatable("message.epochsmith.gear_train.weather.unknown");
        }
        if (data.getClearWeatherTime() > 0) {
            return Component.translatable("message.epochsmith.gear_train.weather.clear_for", minutes(data.getClearWeatherTime()));
        }
        if (level.isRaining()) {
            return Component.translatable(level.isThundering()
                    ? "message.epochsmith.gear_train.weather.storm_ends"
                    : "message.epochsmith.gear_train.weather.rain_ends", minutes(data.getRainTime()));
        }
        return Component.translatable("message.epochsmith.gear_train.weather.rain_in", minutes(data.getRainTime()));
    }

    /** Ticks to whole minutes (at least 1), rounded to keep the forecast honest-sounding. */
    private static int minutes(int ticks) {
        return Math.max(1, Math.round(ticks / 1200.0F));
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
