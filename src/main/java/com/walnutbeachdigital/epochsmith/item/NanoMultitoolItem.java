package com.walnutbeachdigital.epochsmith.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * Every tool in one, built from nanomaterials. Mines like a pickaxe, axe, shovel and hoe,
 * hits like a sword, and right-clicking a block strips logs, makes paths or tills soil.
 * Sneak + right-click in the air cycles three modes:
 *  - Precise: one block at a time.
 *  - Area: 3x3 mining and whole-tree felling.
 *  - Vein: mines an entire ore vein (and fells trees) in one go.
 */
public class NanoMultitoolItem extends MultitoolItem implements PoweredTools.VeinMiner {

    public NanoMultitoolItem(Tier tier, TagKey<Block> blocks, Properties properties) {
        super(tier, blocks, properties);
    }

    @Override
    protected int modeCount() {
        return 3;
    }

    @Override
    protected String descriptionKey() {
        return "tooltip.epochsmith.nano_multitool";
    }

    @Override
    public boolean minesVeins(ItemStack stack) {
        return getMode(stack) == VEIN;
    }

    @Override
    public boolean fellsTrees(ItemStack stack) {
        return getMode(stack) != PRECISE;
    }

    /** Right-click a block: strip it like an axe, flatten it like a shovel, or till it like a hoe. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = Items.NETHERITE_AXE.useOn(context);
        if (!result.consumesAction()) {
            result = Items.NETHERITE_SHOVEL.useOn(context);
        }
        if (!result.consumesAction()) {
            result = Items.NETHERITE_HOE.useOn(context);
        }
        return result;
    }
}
