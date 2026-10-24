package com.walnutbeachdigital.epochsmith.item;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The powered exosuit's abilities, active while all four pieces are worn and the chestplate has charge:
 * Jump Boost II, no fall damage, and flight that uses 1 charge every half second in the air.
 */
@EventBusSubscriber(modid = Epochsmith.MODID)
public final class ExosuitEvents {

    /** Remembers that the exosuit (not creative mode or another mod) gave this player flight. */
    private static final String FLIGHT_TAG = "epochsmith_exosuit_flight";
    private static final int FLIGHT_COST_INTERVAL = 10;

    /** True if the player wears all four exosuit pieces. */
    public static boolean wearsFullSuit(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || !armor.getMaterial().is(ModArmorMaterials.EXOSUIT.getKey())) {
                return false;
            }
        }
        return true;
    }

    private static boolean suitPowered(Player player) {
        return wearsFullSuit(player) && ElectricPowered.getCharge(player.getItemBySlot(EquipmentSlot.CHEST)) > 0;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean powered = suitPowered(player);
        Abilities abilities = player.getAbilities();
        boolean granted = player.getPersistentData().getBoolean(FLIGHT_TAG);

        if (powered && !abilities.mayfly) {
            abilities.mayfly = true;
            player.getPersistentData().putBoolean(FLIGHT_TAG, true);
            player.onUpdateAbilities();
        } else if (!powered && granted) {
            abilities.mayfly = false;
            abilities.flying = false;
            player.getPersistentData().remove(FLIGHT_TAG);
            player.onUpdateAbilities();
            if (wearsFullSuit(player)) {
                player.displayClientMessage(Component.translatable("message.epochsmith.exosuit.empty"), true);
            }
        }

        if (!powered) {
            return;
        }
        if (abilities.flying && player.tickCount % FLIGHT_COST_INTERVAL == 0) {
            ElectricPowered.use(player.getItemBySlot(EquipmentSlot.CHEST), 1);
        }
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false, true));
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && suitPowered(player)) {
            event.setCanceled(true);
        }
    }

    private ExosuitEvents() {
    }
}
