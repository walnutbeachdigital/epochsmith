package com.walnutbeachdigital.epochsmith.network;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.client.ClientHooks;
import com.walnutbeachdigital.epochsmith.information.DataVaultBlockEntity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The data vault terminal's messages: the vault's contents going to the player,
 * and the player's "take this out" requests coming back.
 */
@EventBusSubscriber(modid = Epochsmith.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class VaultNetwork {

    /** How close a player must be to use a vault's terminal (in blocks). */
    private static final double REACH = 8.0;

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(VaultContentsPayload.TYPE, VaultContentsPayload.STREAM_CODEC, VaultNetwork::handleContents)
                .playToServer(VaultWithdrawPayload.TYPE, VaultWithdrawPayload.STREAM_CODEC, VaultNetwork::handleWithdraw);
    }

    public static void sendContents(ServerPlayer player, DataVaultBlockEntity vault, boolean open) {
        List<ItemStack> kinds = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (DataVaultBlockEntity.Entry entry : vault.getEntries()) {
            kinds.add(entry.kind.copy());
            counts.add(entry.count);
        }
        PacketDistributor.sendToPlayer(player, new VaultContentsPayload(vault.getBlockPos(), kinds, counts, open));
    }

    /** Runs on the player's game. */
    private static void handleContents(VaultContentsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.showDataVault(payload));
    }

    /** Runs on the server. Checks the request is fair before handing anything over. */
    private static void handleWithdraw(VaultWithdrawPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > REACH * REACH
                    || !(player.level().getBlockEntity(payload.pos()) instanceof DataVaultBlockEntity vault)
                    || !vault.isPowered()) {
                return;
            }
            int amount = payload.fullStack() ? payload.kind().getMaxStackSize() : 1;
            ItemStack taken = vault.remove(payload.kind(), amount);
            if (!taken.isEmpty() && !player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            sendContents(player, vault, false);
        });
    }

    private VaultNetwork() {
    }
}
