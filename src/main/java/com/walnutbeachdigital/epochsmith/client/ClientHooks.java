package com.walnutbeachdigital.epochsmith.client;

import com.walnutbeachdigital.epochsmith.network.VaultContentsPayload;

import net.minecraft.client.Minecraft;

/**
 * Client-only actions, kept in their own class so the server never loads them.
 */
public final class ClientHooks {

    public static void openChronicle() {
        Minecraft.getInstance().setScreen(new ChronicleScreen());
    }

    /** Opens the data vault terminal, or refreshes it if it's already open. */
    public static void showDataVault(VaultContentsPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DataVaultScreen screen && screen.getPos().equals(payload.pos())) {
            screen.update(payload);
        } else if (payload.open()) {
            mc.setScreen(new DataVaultScreen(payload));
        }
    }

    private ClientHooks() {
    }
}
