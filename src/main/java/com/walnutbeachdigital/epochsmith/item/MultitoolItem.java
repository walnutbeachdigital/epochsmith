package com.walnutbeachdigital.epochsmith.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Pickaxe, axe, shovel and hoe in one. Sneak + right-click (in the air) switches modes:
 *  - Precise: mines one block at a time.
 *  - Area: mines 3x3 and fells whole trees while charged.
 */
public class MultitoolItem extends DiggerItem implements ElectricPowered, PoweredTools.AreaDrill, PoweredTools.TreeFeller {

    public static final int PRECISE = 0;
    public static final int AREA = 1;
    public static final int VEIN = 2;
    private static final String[] MODE_NAMES = {"precise", "area", "vein"};

    public MultitoolItem(Tier tier, TagKey<Block> blocks, Properties properties) {
        super(tier, blocks, properties);
    }

    /** How many modes sneak + right-click cycles through. */
    protected int modeCount() {
        return 2;
    }

    protected String descriptionKey() {
        return "tooltip.epochsmith.multitool";
    }

    public static int getMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TOOL_MODE.get(), PRECISE);
    }

    @Override
    public boolean drillsArea(ItemStack stack) {
        return getMode(stack) == AREA;
    }

    @Override
    public boolean fellsTrees(ItemStack stack) {
        return getMode(stack) == AREA;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide()) {
            int mode = (getMode(stack) + 1) % modeCount();
            stack.set(ModDataComponents.TOOL_MODE.get(), mode);
            player.displayClientMessage(Component.translatable("message.epochsmith.multitool." + MODE_NAMES[mode]), true);
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return ElectricPowered.getCharge(stack) > 0 ? speed * 1.6F : speed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.epochsmith.multitool." + MODE_NAMES[Math.min(getMode(stack), modeCount() - 1)])
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(descriptionKey()));
        ElectricPowered.addTooltip(stack, lines);
    }
}
