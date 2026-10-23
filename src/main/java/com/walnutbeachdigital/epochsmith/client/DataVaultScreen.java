package com.walnutbeachdigital.epochsmith.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.walnutbeachdigital.epochsmith.information.DataVaultBlockEntity;
import com.walnutbeachdigital.epochsmith.network.VaultContentsPayload;
import com.walnutbeachdigital.epochsmith.network.VaultWithdrawPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The data vault terminal. Type to search, scroll to browse,
 * left-click an item to take a stack, right-click to take one.
 */
public class DataVaultScreen extends Screen {

    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int SLOT = 18;
    private static final int WIDTH = COLUMNS * SLOT + 14;
    private static final int HEIGHT = ROWS * SLOT + 50;
    private static final int PANEL = 0xF0101820;
    private static final int BORDER = 0xFF3A7BD5;
    private static final int SLOT_BG = 0xFF1C2A38;
    private static final int SLOT_HOVER = 0xFF2E4A66;
    private static final int TEXT = 0xFFCFE3FF;

    private final BlockPos pos;
    private List<ItemStack> kinds;
    private List<Integer> counts;
    private final List<Integer> shown = new ArrayList<>();
    private EditBox search;
    private int scrollRow = 0;
    private int left;
    private int top;

    public DataVaultScreen(VaultContentsPayload payload) {
        super(Component.translatable("screen.epochsmith.data_vault"));
        this.pos = payload.pos();
        this.kinds = payload.kinds();
        this.counts = payload.counts();
    }

    public BlockPos getPos() {
        return pos;
    }

    /** New contents from the server (after taking something out). */
    public void update(VaultContentsPayload payload) {
        this.kinds = payload.kinds();
        this.counts = payload.counts();
        refilter();
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        String previous = search == null ? "" : search.getValue();
        search = new EditBox(font, left + 7, top + 18, WIDTH - 14, 12, Component.translatable("screen.epochsmith.data_vault.search"));
        search.setHint(Component.translatable("screen.epochsmith.data_vault.search"));
        search.setValue(previous);
        search.setResponder(text -> {
            scrollRow = 0;
            refilter();
        });
        addRenderableWidget(search);
        setInitialFocus(search);
        refilter();
    }

    private void refilter() {
        shown.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < kinds.size(); i++) {
            if (query.isEmpty() || kinds.get(i).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                shown.add(i);
            }
        }
        // Most plentiful first
        shown.sort((a, b) -> Integer.compare(counts.get(b), counts.get(a)));
        scrollRow = Mth.clamp(scrollRow, 0, maxScroll());
    }

    private int maxScroll() {
        int rows = (shown.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    /** Which entry (index into kinds) is under the mouse, or -1. */
    private int entryAt(double mouseX, double mouseY) {
        int gridX = left + 7;
        int gridY = top + 34;
        if (mouseX < gridX || mouseY < gridY) {
            return -1;
        }
        int col = (int) (mouseX - gridX) / SLOT;
        int row = (int) (mouseY - gridY) / SLOT;
        if (col >= COLUMNS || row >= ROWS) {
            return -1;
        }
        int index = (scrollRow + row) * COLUMNS + col;
        return index < shown.size() ? shown.get(index) : -1;
    }

    private static String shortCount(int count) {
        if (count >= 10000) {
            return (count / 1000) + "k";
        }
        if (count >= 1000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        }
        return Integer.toString(count);
    }

    /** The panel is drawn as part of the background, so the search box sits on top of it. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, BORDER);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
        graphics.drawString(font, title, left + 7, top + 6, TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int hovered = entryAt(mouseX, mouseY);
        int gridX = left + 7;
        int gridY = top + 34;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int x = gridX + col * SLOT;
                int y = gridY + row * SLOT;
                int index = (scrollRow + row) * COLUMNS + col;
                int entry = index < shown.size() ? shown.get(index) : -1;
                graphics.fill(x, y, x + SLOT - 1, y + SLOT - 1, entry >= 0 && entry == hovered ? SLOT_HOVER : SLOT_BG);
                if (entry >= 0) {
                    ItemStack kind = kinds.get(entry);
                    graphics.renderItem(kind, x + 1, y + 1);
                    graphics.renderItemDecorations(font, kind, x + 1, y + 1, shortCount(counts.get(entry)));
                }
            }
        }

        int total = 0;
        for (int count : counts) {
            total += count;
        }
        Component status = Component.translatable("screen.epochsmith.data_vault.status",
                total, DataVaultBlockEntity.CAPACITY, kinds.size(), DataVaultBlockEntity.MAX_TYPES);
        graphics.drawString(font, status, left + 7, top + HEIGHT - 12, TEXT, false);

        if (hovered >= 0) {
            graphics.renderTooltip(font, kinds.get(hovered), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int entry = entryAt(mouseX, mouseY);
        if (entry >= 0 && (button == 0 || button == 1)) {
            PacketDistributor.sendToServer(new VaultWithdrawPayload(pos, kinds.get(entry).copyWithCount(1), button == 0));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll());
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
