package com.walnutbeachdigital.epochsmith.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * Digs anything a pickaxe or shovel can. With steam it mines 3x3 (see SteamToolEvents).
 */
public class SteamDrillItem extends DiggerItem implements SteamPowered {

    public SteamDrillItem(Tier tier, TagKey<Block> blocks, Properties properties) {
        super(tier, blocks, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.steam_drill"));
        SteamPowered.addTooltip(stack, lines);
    }
}
