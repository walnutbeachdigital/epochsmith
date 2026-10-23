package com.walnutbeachdigital.epochsmith.client;

import com.walnutbeachdigital.epochsmith.era.Era;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * The Chronicle of Invention book screen.
 * Page 1 is the contents page; each page after that is one era's chapter.
 * Chapters for eras the player hasn't reached yet are shown as locked.
 */
public class ChronicleScreen extends Screen {

    // Uses vanilla's book background for now; we can swap in custom art later
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;
    private static final int BOOK_TOP = 2;
    private static final int TEXT_WIDTH = 114;
    private static final int TEXT_BOTTOM = BOOK_TOP + 150;
    private static final int INK = 0xFF000000;
    private static final int FADED_INK = 0xFF555555;

    private static final Era[] CHAPTERS = {
            Era.PRIMITIVE, Era.BRONZE_IRON, Era.MECHANICAL, Era.INDUSTRIAL,
            Era.ELECTRICAL, Era.INFORMATION, Era.FUTURE
    };

    private int page = 0;
    private PageButton forwardButton;
    private PageButton backButton;

    public ChronicleScreen() {
        super(Component.translatable("chronicle.epochsmith.title"));
    }

    @Override
    protected void init() {
        int left = (this.width - BOOK_WIDTH) / 2;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, 196, 200, 20).build());
        this.forwardButton = this.addRenderableWidget(new PageButton(left + 116, 159, true, button -> turnPage(1), true));
        this.backButton = this.addRenderableWidget(new PageButton(left + 43, 159, false, button -> turnPage(-1), true));
        updateButtons();
    }

    private int pageCount() {
        return CHAPTERS.length + 1;
    }

    private void turnPage(int direction) {
        this.page = Mth.clamp(this.page + direction, 0, pageCount() - 1);
        updateButtons();
    }

    private void updateButtons() {
        this.forwardButton.visible = this.page < pageCount() - 1;
        this.backButton.visible = this.page > 0;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        graphics.blit(BOOK_TEXTURE, (this.width - BOOK_WIDTH) / 2, BOOK_TOP, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - BOOK_WIDTH) / 2;
        int x = left + 36;

        Component indicator = Component.translatable("book.pageIndicator", this.page + 1, pageCount());
        graphics.drawString(this.font, indicator, left + 148 - this.font.width(indicator), BOOK_TOP + 16, INK, false);

        int y = BOOK_TOP + 30;
        if (this.page == 0) {
            drawContents(graphics, x, y);
        } else {
            drawChapter(graphics, CHAPTERS[this.page - 1], x, y);
        }
    }

    private void drawContents(GuiGraphics graphics, int x, int y) {
        Era current = ClientEraData.getEra();
        y = drawWrapped(graphics, this.title.copy().withStyle(ChatFormatting.BOLD), x, y, INK) + 8;
        y = drawWrapped(graphics, Component.translatable("chronicle.epochsmith.current_era"), x, y, FADED_INK);
        y = drawWrapped(graphics, current.displayName(), x, y, INK) + 8;
        drawWrapped(graphics, Component.translatable("chronicle.epochsmith.contents_hint"), x, y, FADED_INK);
    }

    private void drawChapter(GuiGraphics graphics, Era chapter, int x, int y) {
        boolean unlocked = ClientEraData.getEra().isAtLeast(chapter);
        String id = chapter.getSerializedName();

        if (unlocked) {
            y = drawWrapped(graphics, chapter.displayName().copy().withStyle(ChatFormatting.BOLD), x, y, INK) + 6;
            y = drawWrapped(graphics, Component.translatable("chronicle.epochsmith." + id + ".intro"), x, y, INK) + 8;
            y = drawWrapped(graphics, Component.translatable("chronicle.epochsmith.signature_label"), x, y, FADED_INK);
            drawWrapped(graphics, Component.translatable("chronicle.epochsmith." + id + ".signature"), x, y, INK);
        } else {
            y = drawWrapped(graphics, Component.literal("???").withStyle(ChatFormatting.BOLD), x, y, INK) + 6;
            Component hint;
            if (chapter == Era.PRIMITIVE) {
                hint = Component.translatable("chronicle.epochsmith.locked_first");
            } else {
                Era previous = Era.values()[chapter.ordinal() - 1];
                hint = Component.translatable("chronicle.epochsmith.locked",
                        Component.translatable("chronicle.epochsmith." + previous.getSerializedName() + ".signature"));
            }
            drawWrapped(graphics, hint, x, y, FADED_INK);
        }
    }

    /** Draws text wrapped to the page width. Returns the y position after the last line. */
    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int color) {
        for (FormattedCharSequence line : this.font.split(text, TEXT_WIDTH)) {
            if (y > TEXT_BOTTOM) {
                break;
            }
            graphics.drawString(this.font, line, x, y, color, false);
            y += this.font.lineHeight;
        }
        return y;
    }
}
