package com.walnutbeachdigital.epochsmith.block;

import net.minecraft.network.chat.Component;

/**
 * A machine that can describe what it's doing in one line (the message shown when you right-click it).
 * Jade shows the same line when you look at the machine.
 */
public interface StatusDescriber {

    Component describe();
}
