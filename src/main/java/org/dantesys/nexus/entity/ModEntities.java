package org.dantesys.nexus.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;

/** Entidades visuais das habilidades das reliquias. Nenhuma e salva no mundo. */
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Nexus.MODID);

    /** Fisgy — Isca de Tubarao. */
    public static final DeferredHolder<EntityType<?>, EntityType<BocaTubarao>> BOCA_TUBARAO = ENTITIES.registerEntityType(
            "boca_tubarao", BocaTubarao::new, MobCategory.MISC,
            b -> b.sized(1.5F, 1.5F).clientTrackingRange(8).updateInterval(2).noSave().noSummon());

    /** Rifty — Black Hole. */
    public static final DeferredHolder<EntityType<?>, EntityType<BuracoNegro>> BURACO_NEGRO = ENTITIES.registerEntityType(
            "buraco_negro", BuracoNegro::new, MobCategory.MISC,
            b -> b.sized(1.0F, 1.0F).clientTrackingRange(16).updateInterval(20).noSave().noSummon());

    /** Rifty — rachadura da Dobra Dimensional e fenda radial do Dimensional Crash. */
    public static final DeferredHolder<EntityType<?>, EntityType<FendaDimensional>> FENDA = ENTITIES.registerEntityType(
            "fenda_dimensional", FendaDimensional::new, MobCategory.MISC,
            b -> b.sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(20).noSave().noSummon());

    /** Domix — Alcateia. */
    public static final DeferredHolder<EntityType<?>, EntityType<LoboEspectral>> LOBO_ESPECTRAL = ENTITIES.registerEntityType(
            "lobo_espectral", LoboEspectral::new, MobCategory.CREATURE,
            b -> b.sized(0.6F, 0.85F).eyeHeight(0.68F).clientTrackingRange(10).noSave().noSummon());

    /** Cultry — Palma da Lotus. */
    public static final DeferredHolder<EntityType<?>, EntityType<PalmaLotus>> PALMA_LOTUS = ENTITIES.registerEntityType(
            "palma_lotus", PalmaLotus::new, MobCategory.MISC,
            b -> b.sized(2.0F, 0.5F).clientTrackingRange(10).updateInterval(20).noSave().noSummon());

    /** Draconix — O Verdadeiro Dragao (montaria). */
    public static final DeferredHolder<EntityType<?>, EntityType<DragaoDraconix>> DRAGAO_DRACONIX = ENTITIES.registerEntityType(
            "dragao_draconix", DragaoDraconix::new, MobCategory.CREATURE,
            b -> b.sized(1.6F, 1.4F).eyeHeight(1.2F).fireImmune().clientTrackingRange(10).noSave().noSummon());

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(LOBO_ESPECTRAL.get(), Wolf.createAttributes().build());
        event.put(DRAGAO_DRACONIX.get(), DragaoDraconix.createAttributes().build());
    }
}
