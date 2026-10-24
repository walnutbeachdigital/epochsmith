package com.walnutbeachdigital.epochsmith.client;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.item.CompositeHelmetItem;
import com.walnutbeachdigital.epochsmith.item.ElectricPowered;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.Tags;

/**
 * The composite helmet's visor: coordinates, heading and the nearest ore within 8 blocks,
 * drawn in the top-left corner while the helmet is worn and charged.
 */
@EventBusSubscriber(modid = Epochsmith.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TacticalHud {

    private static final int SCAN_RADIUS = 8;
    private static final int SCAN_INTERVAL = 20;
    private static final int COLOR = 0xFF7FE0FF;
    private static final int ORE_COLOR = 0xFFFFD27F;

    private static long lastScan = -1;
    private static Block nearestOre = null;
    private static BlockPos nearestPos = null;

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT,
                ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "tactical_hud"), TacticalHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof CompositeHelmetItem) || ElectricPowered.getCharge(helmet) <= 0) {
            return;
        }
        BlockPos pos = player.blockPosition();
        int x = 4;
        int y = 4;
        graphics.drawString(mc.font, Component.translatable("hud.epochsmith.coords", pos.getX(), pos.getY(), pos.getZ()),
                x, y, COLOR, true);
        graphics.drawString(mc.font, Component.translatable("hud.epochsmith.heading",
                Component.translatable("hud.epochsmith.direction." + player.getDirection().getName())), x, y + 10, COLOR, true);

        long now = player.level().getGameTime();
        if (lastScan < 0 || now - lastScan >= SCAN_INTERVAL || now < lastScan) {
            lastScan = now;
            scan(player.level(), pos);
        }
        if (nearestOre != null) {
            int distance = Mth.floor(Math.sqrt(nearestPos.distSqr(pos)));
            int dy = nearestPos.getY() - pos.getY();
            Component vertical = Component.translatable(dy > 0 ? "hud.epochsmith.above" : dy < 0 ? "hud.epochsmith.below"
                    : "hud.epochsmith.level", Math.abs(dy));
            graphics.drawString(mc.font, Component.translatable("hud.epochsmith.ore", nearestOre.getName(), distance, vertical),
                    x, y + 20, ORE_COLOR, true);
        } else {
            graphics.drawString(mc.font, Component.translatable("hud.epochsmith.no_ore"), x, y + 20, COLOR, true);
        }
    }

    /** Finds the closest ore block around the player (runs once a second). */
    private static void scan(Level level, BlockPos center) {
        nearestOre = null;
        nearestPos = null;
        double best = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tags.Blocks.ORES)) {
                        double d = dx * dx + dy * dy + dz * dz;
                        if (d < best) {
                            best = d;
                            nearestOre = state.getBlock();
                            nearestPos = pos.immutable();
                        }
                    }
                }
            }
        }
    }

    private TacticalHud() {
    }
}
