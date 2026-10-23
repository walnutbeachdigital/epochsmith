package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Items that hold electric charge (electric drill, chainsaw, coil chestplate).
 * Recharged by right-clicking a Battery Bank, at 250 energy per point of charge.
 */
public interface ElectricPowered {

    int MAX_CHARGE = 400;
    int ENERGY_PER_CHARGE = 250;

    static int getCharge(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CHARGE.get(), 0);
    }

    static void setCharge(ItemStack stack, int charge) {
        stack.set(ModDataComponents.CHARGE.get(), Mth.clamp(charge, 0, MAX_CHARGE));
    }

    static boolean use(ItemStack stack, int amount) {
        int charge = getCharge(stack);
        if (charge < amount) {
            return false;
        }
        setCharge(stack, charge - amount);
        return true;
    }

    static void addTooltip(ItemStack stack, List<Component> lines) {
        lines.add(Component.translatable("tooltip.epochsmith.charge", getCharge(stack), MAX_CHARGE)
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.epochsmith.charge_recharge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
