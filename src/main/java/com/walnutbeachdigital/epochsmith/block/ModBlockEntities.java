package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.Epochsmith;

import com.walnutbeachdigital.epochsmith.power.KineticBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block entities: blocks that remember things (contents, timers) or tick.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Epochsmith.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PitKilnBlockEntity>> PIT_KILN =
            BLOCK_ENTITIES.register("pit_kiln",
                    () -> BlockEntityType.Builder.of(PitKilnBlockEntity::new, ModBlocks.PIT_KILN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WovenBasketBlockEntity>> WOVEN_BASKET =
            BLOCK_ENTITIES.register("woven_basket",
                    () -> BlockEntityType.Builder.of(WovenBasketBlockEntity::new, ModBlocks.WOVEN_BASKET.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloomeryBlockEntity>> BLOOMERY =
            BLOCK_ENTITIES.register("bloomery",
                    () -> BlockEntityType.Builder.of(BloomeryBlockEntity::new, ModBlocks.BLOOMERY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoneAnvilBlockEntity>> STONE_ANVIL =
            BLOCK_ENTITIES.register("stone_anvil",
                    () -> BlockEntityType.Builder.of(StoneAnvilBlockEntity::new, ModBlocks.STONE_ANVIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AmphoraBlockEntity>> AMPHORA =
            BLOCK_ENTITIES.register("amphora",
                    () -> BlockEntityType.Builder.of(AmphoraBlockEntity::new, ModBlocks.AMPHORA.get()).build(null));

    /** Shared by every spinning block: axles, hand cranks, water wheels and windmills. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KineticBlockEntity>> KINETIC =
            BLOCK_ENTITIES.register("kinetic",
                    () -> BlockEntityType.Builder.of(KineticBlockEntity::new,
                            ModBlocks.AXLE.get(), ModBlocks.HAND_CRANK.get(),
                            ModBlocks.WATER_WHEEL.get(), ModBlocks.WINDMILL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MillstoneBlockEntity>> MILLSTONE =
            BLOCK_ENTITIES.register("millstone",
                    () -> BlockEntityType.Builder.of(MillstoneBlockEntity::new, ModBlocks.MILLSTONE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JoinersBenchBlockEntity>> JOINERS_BENCH =
            BLOCK_ENTITIES.register("joiners_bench",
                    () -> BlockEntityType.Builder.of(JoinersBenchBlockEntity::new, ModBlocks.JOINERS_BENCH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeBlockEntity>> FORGE =
            BLOCK_ENTITIES.register("forge",
                    () -> BlockEntityType.Builder.of(ForgeBlockEntity::new, ModBlocks.FORGE.get()).build(null));

    private ModBlockEntities() {
    }
}
