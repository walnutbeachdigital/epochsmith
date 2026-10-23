package com.walnutbeachdigital.epochsmith.item;

import java.util.EnumMap;
import java.util.List;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Armor materials. Each era adds one.
 */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Epochsmith.MODID);

    /**
     * Hide: a little tougher than leather. Total armor 8 (leather 7).
     * Repaired with leather. Placeholder look: vanilla's leather armor texture, undyed.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HIDE = ARMOR_MATERIALS.register("hide",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 1);
                        defense.put(ArmorItem.Type.LEGGINGS, 3);
                        defense.put(ArmorItem.Type.CHESTPLATE, 3);
                        defense.put(ArmorItem.Type.HELMET, 1);
                        defense.put(ArmorItem.Type.BODY, 3);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("leather"))),
                    0.0F,
                    0.0F));

    /**
     * Bronze lamellar: as protective as iron (total 15) and easier to enchant (12, iron 9).
     * Repaired with bronze ingots. Uses Epochsmith's own armor texture.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BRONZE = ARMOR_MATERIALS.register("bronze",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 2);
                        defense.put(ArmorItem.Type.LEGGINGS, 5);
                        defense.put(ArmorItem.Type.CHESTPLATE, 6);
                        defense.put(ArmorItem.Type.HELMET, 2);
                        defense.put(ArmorItem.Type.BODY, 5);
                    }),
                    12,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.BRONZE_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "bronze"))),
                    0.0F,
                    0.0F));

    /**
     * Knight's plate (crucible steel): between iron and diamond. Total armor 17
     * (iron 15, diamond 20) with 1 toughness. Repaired with steel ingots.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> KNIGHT = ARMOR_MATERIALS.register("knight",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 2);
                        defense.put(ArmorItem.Type.LEGGINGS, 5);
                        defense.put(ArmorItem.Type.CHESTPLATE, 7);
                        defense.put(ArmorItem.Type.HELMET, 3);
                        defense.put(ArmorItem.Type.BODY, 7);
                    }),
                    10,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.STEEL_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "knight"))),
                    1.0F,
                    0.0F));

    /**
     * Riveted steam armor: diamond-level protection (total 20) with 2 toughness.
     * Repaired with steel ingots.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STEAM = ARMOR_MATERIALS.register("steam",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 3);
                        defense.put(ArmorItem.Type.LEGGINGS, 6);
                        defense.put(ArmorItem.Type.CHESTPLATE, 8);
                        defense.put(ArmorItem.Type.HELMET, 3);
                        defense.put(ArmorItem.Type.BODY, 8);
                    }),
                    10,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.STEEL_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Epochsmith.MODID, "steam"))),
                    2.0F,
                    0.0F));

    private ModArmorMaterials() {
    }
}
