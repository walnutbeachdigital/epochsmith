package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Items that store steam (steam drill, pneumatic axe, steam boots).
 * Recharged by right-clicking a working boiler.
 */
public interface SteamPowered {

    int MAX_STEAM = 256;

    static int getSteam(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.STEAM.get(), 0);
    }

    static void setSteam(ItemStack stack, int steam) {
        stack.set(ModDataComponents.STEAM.get(), Mth.clamp(steam, 0, MAX_STEAM));
    }

    /** Uses some steam. Returns false (and uses none) if there isn't enough. */
    static boolean use(ItemStack stack, int amount) {
        int steam = getSteam(stack);
        if (steam < amount) {
            return false;
        }
        setSteam(stack, steam - amount);
        return true;
    }

    static void addTooltip(ItemStack stack, List<Component> lines) {
        lines.add(Component.translatable("tooltip.epochsmith.steam", getSteam(stack), MAX_STEAM)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.epochsmith.steam_recharge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
