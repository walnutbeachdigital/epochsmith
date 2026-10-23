package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

/**
 * An axe that, with steam, fells a whole tree at once (see SteamToolEvents).
 */
public class PneumaticAxeItem extends AxeItem implements SteamPowered {

    public PneumaticAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.pneumatic_axe"));
        SteamPowered.addTooltip(stack, lines);
    }
}
