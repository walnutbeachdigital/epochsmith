package com.walnutbeachdigital.epochsmith.block;

import com.walnutbeachdigital.epochsmith.Epochsmith;

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

    private ModBlockEntities() {
    }
}
