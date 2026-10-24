package com.walnutbeachdigital.epochsmith.menu;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * A block entity with a machine GUI. The same layout is used on both sides; values are read on the
 * server and synced to the open screen automatically.
 */
public interface MachineGui {

    /** The machine's slots, in the same order as its layout's slots. */
    IItemHandlerModifiable getMenuItems();

    MachineLayout getLayout();

    /** The current value of one of the layout's numbered values (progress, heat, energy...). */
    int getMenuValue(int index);

    /** A player opened the GUI (server side), e.g. to credit them with what the machine makes. */
    default void onMenuOpened(Player player) {
    }

    /** A GUI button was pressed (server side). Return true if it did something. */
    default boolean onMenuButton(Player player, int id) {
        return false;
    }
}
