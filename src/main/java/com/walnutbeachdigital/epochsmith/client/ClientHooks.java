package com.walnutbeachdigital.epochsmith.client;

import net.minecraft.client.Minecraft;

/**
 * Client-only actions, kept in their own class so the server never loads them.
 */
public final class ClientHooks {

    public static void openChronicle() {
        Minecraft.getInstance().setScreen(new ChronicleScreen());
    }

    private ClientHooks() {
    }
}
