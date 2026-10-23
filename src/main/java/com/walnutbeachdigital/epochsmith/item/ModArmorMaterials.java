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

    private ModArmorMaterials() {
    }
}
