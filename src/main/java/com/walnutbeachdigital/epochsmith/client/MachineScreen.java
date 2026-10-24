package com.walnutbeachdigital.epochsmith.client;

import com.walnutbeachdigital.epochsmith.menu.MachineItems;
import com.walnutbeachdigital.epochsmith.menu.MachineLayout;
import com.walnutbeachdigital.epochsmith.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The screen for every Epochsmith machine, drawn from its layout in vanilla's style:
 * a grey panel, vanilla slots, the furnace arrow and flame, and gauges with tooltips.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation FLAME = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    private static final int PANEL = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int EDGE = 0xFF000000;
    private static final int TEXT = 0xFF404040;

    private final MachineLayout layout;
    private final ResourceLocation background;
    private final int textColor;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.layout = menu.getLayout();
        this.imageWidth = 176;
        this.imageHeight = layout.inventoryY + 82;
        this.inventoryLabelY = layout.inventoryY - 11;
        this.background = ResourceLocation.fromNamespaceAndPath("epochsmith", "textures/gui/era/" + layout.theme + ".png");
        this.textColor = switch (layout.theme) {
            case "primitive", "bronze_iron" -> 0xFFF0E0C0;
            case "mechanical" -> 0xFF3C2814;
            case "electrical" -> 0xFFF0C096;
            case "information" -> 0xFF40404A;
            case "future" -> 0xFFAAE6FF;
            default -> 0xFFDCE0E6;
        };
    }

    @Override
    protected void init() {
        super.init();
        for (MachineLayout.ButtonDef def : layout.buttons) {
            addRenderableWidget(Button.builder(Component.translatable(def.label()), button -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, def.id());
                }
            }).bounds(leftPos + def.x(), topPos + def.y(), def.width(), def.height()).build());
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // The era's panel, then its slots (output slots get a raised frame)
        g.blit(background, x, y, 0, 0, imageWidth, imageHeight);
        for (MachineLayout.SlotDef def : layout.slots) {
            if (def.role() == MachineItems.Role.OUTPUT) {
                g.blit(background, x + def.x() - 5, y + def.y() - 5, 176, 18, 26, 26);
            }
            g.blit(background, x + def.x() - 1, y + def.y() - 1, 176, 0, 18, 18);
            if (def.role() == MachineItems.Role.PATTERN) {
                g.fill(x + def.x(), y + def.y(), x + def.x() + 16, y + def.y() + 16, 0x3040A0FF);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                g.blit(background, x + 7 + col * 18, y + layout.inventoryY - 1 + row * 18, 176, 0, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            g.blit(background, x + 7 + col * 18, y + layout.inventoryY + 57, 176, 0, 18, 18);
        }

        // Progress arrows and flames (empty shapes from the era texture, filled ones from vanilla's furnace sprites)
        for (MachineLayout.Meter arrow : layout.arrows) {
            g.blit(background, x + arrow.x(), y + arrow.y(), 176, 44, 24, 17);
            int width = scaled(arrow, 24);
            if (width > 0) {
                g.blitSprite(ARROW, 24, 16, 0, 0, x + arrow.x(), y + arrow.y(), width, 16);
            }
        }
        for (MachineLayout.Meter flame : layout.flames) {
            g.blit(background, x + flame.x(), y + flame.y(), 176, 62, 14, 14);
            int height = scaled(flame, 14);
            if (height > 0) {
                g.blitSprite(FLAME, 14, 14, 0, 14 - height, x + flame.x(), y + flame.y() + 14 - height, 14, height);
            }
        }

        // Gauges: a sunken frame that fills from the bottom
        for (MachineLayout.Gauge gauge : layout.gauges) {
            int gx = x + gauge.x();
            int gy = y + gauge.y();
            g.fill(gx - 1, gy - 1, gx + gauge.width() + 1, gy + gauge.height() + 1, SHADOW);
            g.fill(gx, gy, gx + gauge.width() + 1, gy + gauge.height() + 1, LIGHT);
            g.fill(gx, gy, gx + gauge.width(), gy + gauge.height(), 0xFF1E1E1E);
            int max = gaugeMax(gauge);
            int filled = max <= 0 ? 0 : (int) Math.min(gauge.height(), (long) menu.getValue(gauge.value()) * gauge.height() / max);
            if (filled > 0) {
                g.fill(gx, gy + gauge.height() - filled, gx + gauge.width(), gy + gauge.height(), gauge.color());
                g.fill(gx, gy + gauge.height() - filled, gx + 1, gy + gauge.height(), 0x40FFFFFF);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, textColor, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, textColor, false);
        for (MachineLayout.Status status : layout.statuses) {
            Component text = Component.translatable(status.keyPrefix() + "." + menu.getValue(status.value()));
            g.drawString(font, text, status.x(), status.y(), textColor, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        for (MachineLayout.Gauge gauge : layout.gauges) {
            int gx = leftPos + gauge.x();
            int gy = topPos + gauge.y();
            if (mouseX >= gx - 1 && mouseX <= gx + gauge.width() && mouseY >= gy - 1 && mouseY <= gy + gauge.height()) {
                g.renderTooltip(font, Component.translatable("gui.epochsmith.gauge", Component.translatable(gauge.label()),
                        String.format("%,d", menu.getValue(gauge.value())), String.format("%,d", gaugeMax(gauge))), mouseX, mouseY);
            }
        }
    }

    private int gaugeMax(MachineLayout.Gauge gauge) {
        return gauge.max() >= 0 ? menu.getValue(gauge.max()) : gauge.fixedMax();
    }

    private int scaled(MachineLayout.Meter meter, int size) {
        int max = menu.getValue(meter.max());
        int value = menu.getValue(meter.value());
        return max <= 0 || value <= 0 ? 0 : Math.min(size, Math.max(1, value * size / max));
    }
}
