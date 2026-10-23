package com.walnutbeachdigital.epochsmith.item;

import com.walnutbeachdigital.epochsmith.client.ClientHooks;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Right-click to open the Chronicle of Invention.
 */
public class ChronicleItem extends Item {

    public ChronicleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Screens only exist on the client (the player's game), never on the server
        if (level.isClientSide()) {
            ClientHooks.openChronicle();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
