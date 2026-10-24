package com.walnutbeachdigital.epochsmith.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * A JEI page for one Epochsmith machine or in-world process: inputs on the left,
 * an arrow, outputs on the right, and a note underneath.
 */
public class MachineCategory implements IRecipeCategory<MachineRecipe> {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 46;
    private static final int SLOT = 18;
    private static final int OUTPUT_X = 4 * SLOT + 24;

    private final RecipeType<MachineRecipe> type;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable slot;

    public MachineCategory(RecipeType<MachineRecipe> type, Component title, ItemStack icon, IGuiHelper gui) {
        this.type = type;
        this.title = title;
        this.icon = gui.createDrawableItemStack(icon);
        this.slot = gui.getSlotDrawable();
    }

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.inputs().size() && i < 4; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * SLOT, 1).addItemStacks(recipe.inputs().get(i));
        }
        for (int i = 0; i < recipe.outputs().size() && i < 2; i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 1 + i * SLOT, 1).addItemStack(recipe.outputs().get(i));
        }
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        for (int i = 0; i < recipe.inputs().size() && i < 4; i++) {
            slot.draw(graphics, i * SLOT, 0);
        }
        for (int i = 0; i < recipe.outputs().size() && i < 2; i++) {
            slot.draw(graphics, OUTPUT_X + i * SLOT, 0);
        }
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, "→", OUTPUT_X - 14, 5, 0xFF555555, false);
        List<FormattedCharSequence> lines = font.split(recipe.note(), WIDTH);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            graphics.drawString(font, lines.get(i), 0, 24 + i * 10, 0xFF555555, false);
        }
    }
}
