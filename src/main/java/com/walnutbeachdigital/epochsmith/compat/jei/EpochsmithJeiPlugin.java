package com.walnutbeachdigital.epochsmith.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.walnutbeachdigital.epochsmith.Epochsmith;
import com.walnutbeachdigital.epochsmith.block.ModBlocks;
import com.walnutbeachdigital.epochsmith.electric.ArcFurnaceBlockEntity;
import com.walnutbeachdigital.epochsmith.future.PlasmaSmelterBlockEntity;
import com.walnutbeachdigital.epochsmith.item.ModItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

/**
 * JEI support: a page for every Epochsmith machine and in-world process, plus info pages for blocks
 * that don't have recipes of their own. Crafting-table and furnace recipes appear in JEI automatically.
 * JEI only loads this class when JEI is installed.
 */
@JeiPlugin
public class EpochsmithJeiPlugin implements IModPlugin {

    /** Every Epochsmith JEI page: its id, the machine(s) that do it, and how to fill it. */
    private record Page(RecipeType<MachineRecipe> type, Supplier<? extends ItemLike> icon, List<Supplier<? extends ItemLike>> machines) {
    }

    private static final List<Page> PAGES = new ArrayList<>();

    private static RecipeType<MachineRecipe> page(String id, Supplier<? extends ItemLike> icon, List<Supplier<? extends ItemLike>> machines) {
        RecipeType<MachineRecipe> type = RecipeType.create(Epochsmith.MODID, id, MachineRecipe.class);
        PAGES.add(new Page(type, icon, machines));
        return type;
    }

    private static RecipeType<MachineRecipe> page(String id, Supplier<? extends ItemLike> machine) {
        return page(id, machine, List.of(machine));
    }

    static final RecipeType<MachineRecipe> KNAPPING = page("knapping", ModBlocks.KNAPPING_STONE_ITEM);
    static final RecipeType<MachineRecipe> PIT_KILN = page("pit_kiln", ModBlocks.PIT_KILN_ITEM);
    static final RecipeType<MachineRecipe> BLOOMERY = page("bloomery", ModBlocks.BLOOMERY_ITEM);
    static final RecipeType<MachineRecipe> STONE_ANVIL = page("stone_anvil", ModBlocks.STONE_ANVIL_ITEM);
    static final RecipeType<MachineRecipe> MILLSTONE = page("millstone", ModBlocks.MILLSTONE_ITEM);
    static final RecipeType<MachineRecipe> JOINERS_BENCH = page("joiners_bench", ModBlocks.JOINERS_BENCH_ITEM);
    static final RecipeType<MachineRecipe> FORGE = page("forge", ModBlocks.FORGE_ITEM);
    static final RecipeType<MachineRecipe> COKE_OVEN = page("coke_oven", ModBlocks.COKE_OVEN_ITEM);
    static final RecipeType<MachineRecipe> LATHE = page("lathe", ModBlocks.LATHE_ITEM);
    static final RecipeType<MachineRecipe> BLAST_FURNACE = page("steam_blast_furnace", ModBlocks.STEAM_BLAST_FURNACE_ITEM);
    static final RecipeType<MachineRecipe> ARC_FURNACE = page("arc_furnace", ModBlocks.ARC_FURNACE_ITEM,
            List.of(ModBlocks.ARC_FURNACE_ITEM, ModBlocks.INDUCTION_FURNACE_ITEM));
    static final RecipeType<MachineRecipe> FABRICATOR = page("circuit_fabricator", ModBlocks.FABRICATOR_ITEM);
    static final RecipeType<MachineRecipe> NANOFABRICATOR = page("nanofabricator", ModBlocks.NANOFABRICATOR_ITEM);
    static final RecipeType<MachineRecipe> PLASMA_SMELTER = page("plasma_smelter", ModBlocks.PLASMA_SMELTER_ITEM);
    static final RecipeType<MachineRecipe> FUSION = page("fusion_reactor", ModBlocks.FUSION_REACTOR_ITEM);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (Page page : PAGES) {
            String id = page.type().getUid().getPath();
            registration.addRecipeCategories(new MachineCategory(page.type(),
                    Component.translatable("jei.epochsmith.category." + id), stack(page.icon().get()), gui));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Page page : PAGES) {
            for (Supplier<? extends ItemLike> machine : page.machines()) {
                registration.addRecipeCatalyst(stack(machine.get()), page.type());
            }
        }
        registration.addRecipeCatalyst(new ItemStack(ModItems.SMITHS_HAMMER.get()), STONE_ANVIL);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(KNAPPING, List.of(
                MachineRecipe.of(new ItemStack(Items.FLINT), new ItemStack(ModItems.FLINT_BLADE.get()), note("knapping"))));

        registration.addRecipes(PIT_KILN, List.of(
                MachineRecipe.of(new ItemStack(Items.CLAY_BALL), new ItemStack(Items.BRICK), note("pit_kiln")),
                MachineRecipe.of(new ItemStack(ModItems.UNFIRED_AMPHORA.get()), stack(ModBlocks.AMPHORA_ITEM.get()), note("pit_kiln")),
                new MachineRecipe(List.of(tag(ItemTags.LOGS_THAT_BURN)), List.of(new ItemStack(Items.CHARCOAL)), note("pit_kiln"))));

        registration.addRecipes(BLOOMERY, List.of(
                new MachineRecipe(List.of(List.of(new ItemStack(Items.COPPER_INGOT, 3)), List.of(new ItemStack(ModItems.TIN_INGOT.get()))),
                        List.of(new ItemStack(ModItems.BRONZE_INGOT.get(), 4)), note("bloomery_bronze")),
                MachineRecipe.of(new ItemStack(Items.RAW_IRON), new ItemStack(ModItems.IRON_BLOOM.get()), note("bloomery_iron"))));

        registration.addRecipes(STONE_ANVIL, List.of(
                MachineRecipe.of(new ItemStack(ModItems.IRON_BLOOM.get()), new ItemStack(ModItems.WROUGHT_IRON_INGOT.get()),
                        Component.translatable("jei.epochsmith.note.stone_anvil", 6)),
                MachineRecipe.of(new ItemStack(ModItems.BRONZE_INGOT.get()), new ItemStack(ModItems.BRONZE_PLATE.get()),
                        Component.translatable("jei.epochsmith.note.stone_anvil", 4))));

        registration.addRecipes(MILLSTONE, List.of(
                MachineRecipe.of(new ItemStack(Items.COBBLESTONE), new ItemStack(Items.GRAVEL), note("millstone")),
                MachineRecipe.of(new ItemStack(Items.GRAVEL), new ItemStack(Items.SAND), note("millstone")),
                MachineRecipe.of(new ItemStack(Items.BONE), new ItemStack(Items.BONE_MEAL, 4), note("millstone")),
                MachineRecipe.of(new ItemStack(Items.SUGAR_CANE), new ItemStack(Items.SUGAR, 2), note("millstone"))));

        registration.addRecipes(JOINERS_BENCH, List.of(
                new MachineRecipe(List.of(tag(ItemTags.PLANKS)), List.of(new ItemStack(ModItems.WOODEN_GEAR.get(), 2)), note("joiners_bench")),
                new MachineRecipe(List.of(tag(ItemTags.LOGS)), List.of(stack(ModBlocks.AXLE_ITEM.get(), 6)), note("joiners_bench")),
                MachineRecipe.of(new ItemStack(ModItems.WROUGHT_IRON_INGOT.get()), new ItemStack(ModItems.IRON_GEAR.get(), 2), note("joiners_bench"))));

        registration.addRecipes(FORGE, List.of(
                MachineRecipe.of(new ItemStack(Items.RAW_IRON), new ItemStack(Items.IRON_INGOT), note("forge")),
                MachineRecipe.of(new ItemStack(Items.RAW_COPPER), new ItemStack(Items.COPPER_INGOT), note("forge")),
                MachineRecipe.of(new ItemStack(Items.RAW_GOLD), new ItemStack(Items.GOLD_INGOT), note("forge")),
                MachineRecipe.of(new ItemStack(ModItems.RAW_TIN.get()), new ItemStack(ModItems.TIN_INGOT.get()), note("forge")),
                MachineRecipe.of(new ItemStack(ModItems.WROUGHT_IRON_INGOT.get()), new ItemStack(ModItems.STEEL_INGOT.get()), note("forge_steel"))));

        registration.addRecipes(COKE_OVEN, List.of(
                MachineRecipe.of(new ItemStack(Items.COAL), new ItemStack(ModItems.COKE.get()), note("coke_oven"))));

        registration.addRecipes(LATHE, List.of(
                MachineRecipe.of(new ItemStack(ModItems.WROUGHT_IRON_INGOT.get()), new ItemStack(ModItems.RIVETS.get(), 4), note("lathe")),
                MachineRecipe.of(new ItemStack(Items.COPPER_INGOT), new ItemStack(ModItems.PRESSURE_GAUGE.get()), note("lathe")),
                MachineRecipe.of(new ItemStack(ModItems.STEEL_INGOT.get()), new ItemStack(ModItems.STEAM_PISTON.get()), note("lathe"))));

        registration.addRecipes(BLAST_FURNACE, List.of(
                MachineRecipe.of(new ItemStack(Items.RAW_IRON), new ItemStack(Items.IRON_INGOT, 2), note("blast_furnace")),
                MachineRecipe.of(new ItemStack(Items.RAW_COPPER), new ItemStack(Items.COPPER_INGOT, 2), note("blast_furnace")),
                MachineRecipe.of(new ItemStack(Items.RAW_GOLD), new ItemStack(Items.GOLD_INGOT, 2), note("blast_furnace")),
                MachineRecipe.of(new ItemStack(ModItems.RAW_TIN.get()), new ItemStack(ModItems.TIN_INGOT.get(), 2), note("blast_furnace")),
                MachineRecipe.of(new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.STEEL_INGOT.get()), note("blast_furnace"))));

        Level level = Minecraft.getInstance().level;
        List<MachineRecipe> arc = new ArrayList<>();
        for (Item item : List.of(Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD, ModItems.RAW_TIN.get(), Items.IRON_INGOT, Items.SAND)) {
            ItemStack result = level == null ? ItemStack.EMPTY : ArcFurnaceBlockEntity.resultFor(level, new ItemStack(item));
            if (!result.isEmpty()) {
                arc.add(MachineRecipe.of(new ItemStack(item), result, note("arc_furnace")));
            }
        }
        registration.addRecipes(ARC_FURNACE, arc);

        registration.addRecipes(FABRICATOR, List.of(
                MachineRecipe.of(new ItemStack(ModItems.SILICON.get()), new ItemStack(ModItems.TRANSISTOR.get(), 4), note("powered"))));

        registration.addRecipes(NANOFABRICATOR, List.of(
                MachineRecipe.of(new ItemStack(Items.COAL_BLOCK), new ItemStack(ModItems.GRAPHENE.get(), 4), note("powered")),
                MachineRecipe.of(new ItemStack(ModItems.CIRCUIT_BOARD.get()), new ItemStack(ModItems.NANOBOTS.get(), 4), note("powered"))));

        // Plasma smelter: every ore and raw ore the game knows, tripled (other mods' ores included)
        List<MachineRecipe> plasma = new ArrayList<>();
        if (level != null) {
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack input = new ItemStack(item);
                if (input.is(Tags.Items.RAW_MATERIALS) || input.is(Tags.Items.ORES)) {
                    ItemStack result = PlasmaSmelterBlockEntity.resultFor(level, input);
                    if (!result.isEmpty()) {
                        plasma.add(MachineRecipe.of(input, result, note("plasma_smelter")));
                    }
                }
            }
        }
        registration.addRecipes(PLASMA_SMELTER, plasma);

        registration.addRecipes(FUSION, List.of(new MachineRecipe(
                List.of(List.of(new ItemStack(ModItems.PLASMA_CELL.get()))), List.of(), note("fusion_reactor"))));

        // Info pages for things without recipes of their own
        info(registration, ModItems.PLANT_FIBER.get(), "plant_fiber");
        info(registration, ModItems.CHRONICLE.get(), "chronicle");
        info(registration, ModBlocks.MATTER_PRINTER_ITEM.get(), "matter_printer");
        info(registration, ModBlocks.ASSEMBLER_ITEM.get(), "automated_assembler");
        info(registration, ModBlocks.DATA_VAULT_ITEM.get(), "data_vault");
        info(registration, ModBlocks.QUANTUM_CELL_ITEM.get(), "quantum_cell");
        info(registration, ModBlocks.COMPUTER_CORE_ITEM.get(), "computer_core");
        info(registration, ModBlocks.WIRELESS_NODE_ITEM.get(), "wireless_node");
        info(registration, ModBlocks.HOVER_PAD_ITEM.get(), "hover_pad");
        info(registration, ModBlocks.SOLAR_PANEL_ITEM.get(), "solar_panel");
        info(registration, ModBlocks.STEAM_ENGINE_ITEM.get(), "steam_engine");
        info(registration, ModBlocks.DYNAMO_ITEM.get(), "dynamo");
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(stack(item), VanillaTypes.ITEM_STACK, Component.translatable("jei.epochsmith.info." + key));
    }

    private static Component note(String key) {
        return Component.translatable("jei.epochsmith.note." + key);
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }

    /** Every item in a tag, for a slot that accepts any of them. */
    private static List<ItemStack> tag(TagKey<Item> tag) {
        List<ItemStack> items = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> items.add(new ItemStack(holder.value())));
        return items;
    }
}
