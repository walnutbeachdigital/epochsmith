package com.walnutbeachdigital.epochsmith.era;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.walnutbeachdigital.epochsmith.item.ModItems;
import com.walnutbeachdigital.epochsmith.network.EraNetwork;

/**
 * The one place to read or change a player's era.
 */
public final class EraManager {

    /** The era this player has reached. */
    public static Era getEra(Player player) {
        return player.getData(EraAttachments.ERA);
    }

    /** True if this player has reached the given era (or a later one). */
    public static boolean hasReached(Player player, Era era) {
        return getEra(player).isAtLeast(era);
    }

    /**
     * Moves the player forward to the given era. Does nothing if they're
     * already in that era or past it, so it's safe to call often.
     */
    public static void advanceTo(ServerPlayer player, Era era) {
        if (hasReached(player, era)) {
            return;
        }
        setEra(player, era, true);
        // Entering the Primitive era for the first time hands the player their Chronicle
        if (era == Era.PRIMITIVE) {
            giveChronicle(player);
        }
    }

    /**
     * Sets the player's era directly, forward or back. Used by the test command.
     * If announce is true, the player sees the "new era" title and hears a sound.
     */
    public static void setEra(ServerPlayer player, Era era, boolean announce) {
        player.setData(EraAttachments.ERA, era);
        EraNetwork.sendTo(player, era);
        if (announce && era != Era.NONE) {
            announce(player, era);
        }
    }

    private static void giveChronicle(ServerPlayer player) {
        ItemStack book = new ItemStack(ModItems.CHRONICLE.get());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }

    private static void announce(ServerPlayer player, Era era) {
        // Fade in 0.5s, stay 3s, fade out 1s (Minecraft counts in ticks: 20 per second)
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("era.epochsmith.entered")));
        player.connection.send(new ClientboundSetTitleTextPacket(era.displayName()));
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private EraManager() {
    }
}
