package com.walnutbeachdigital.epochsmith.item;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The exosuit's power pack. While it's charged and you wear the full exosuit,
 * you get Jump Boost, no fall damage, and short flight (double-tap jump, like creative mode).
 * Flying uses charge; the rest is free.
 */
public class ExosuitChestplateItem extends ArmorItem implements ElectricPowered {

    public ExosuitChestplateItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.exosuit_chestplate"));
        ElectricPowered.addTooltip(stack, lines);
    }
}
