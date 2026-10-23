package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Riveted steam boots: give Speed while they hold steam (see PoweredToolEvents).
 */
public class SteamBootsItem extends ArmorItem implements SteamPowered {

    public SteamBootsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.BOOTS, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.steam_boots"));
        SteamPowered.addTooltip(stack, lines);
    }
}
