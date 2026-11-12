package org.dantesys.nexus.blocks.blockentity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static org.dantesys.nexus.Nexus.MODID;
import static org.dantesys.nexus.blocks.ModBlocks.*;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BolaCristalBE>> BOLA_CRISTAL_BE =
            BLOCK_ENTITIES.register(
                    "bola_cristal",
                    () -> new BlockEntityType<>(BolaCristalBE::new, BOLA_CRISTAL.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ColetorArcanoBasicoBE>> COLETOR_ARCANO_BASICO_BE =
            BLOCK_ENTITIES.register(
                    "coletor_arcano_basico",
                    () -> new BlockEntityType<>(ColetorArcanoBasicoBE::new, COLETOR_ARCANO_BASICO.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusorArcanoBasicoBE>> INFUSOR_ARCANO_BASICO_BE =
            BLOCK_ENTITIES.register(
                    "infusor_arcano_basico",
                    () -> new BlockEntityType<>(InfusorArcanoBasicoBE::new, INFUSOR_ARCANO_BASICO.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TanqueForjaBE>> TANQUE_FORJA_BE =
            BLOCK_ENTITIES.register(
                    "tanque_forja",
                    () -> new BlockEntityType<>(TanqueForjaBE::new, TANQUE_FORJA.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControladorForjaBE>> CONTROLADOR_FORJA_BE =
            BLOCK_ENTITIES.register(
                    "controlador_forja",
                    () -> new BlockEntityType<>(ControladorForjaBE::new, CONTROLADOR_FORJA.get())
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
