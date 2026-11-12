package org.dantesys.nexus.worldgen.features;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;

/** Tipos de feature próprios (desde a 26.3 o registro guarda o codec de cada tipo). */
public class ModFeatures {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURES = DeferredRegister.create(Registries.FEATURE_TYPE, Nexus.MODID);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<VidaTreeFeature>> VIDA_TREE =
            FEATURES.register("vida_tree", () -> VidaTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<IgneaMiniVolcanoFeature>> IGNEA_MINI_VOLCANO =
            FEATURES.register("ignea_mini_volcano", () -> IgneaMiniVolcanoFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<IgneaMagmaClusterFeature>> IGNEA_MAGMA_CLUSTER =
            FEATURES.register("ignea_magma_cluster", () -> IgneaMagmaClusterFeature.CODEC);

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
