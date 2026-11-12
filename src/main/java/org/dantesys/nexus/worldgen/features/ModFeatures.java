package org.dantesys.nexus.worldgen.features;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Nexus.MODID);

    public static final DeferredHolder<Feature<?>, Feature<TreeConfiguration>> VIDA_TREE = FEATURES.register(
            "vida_tree",
            () -> new VidaTreeFeature(TreeConfiguration.CODEC)
    );
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> IGNEA_MINI_VOLCANO = FEATURES.register(
            "ignea_mini_volcano",
            () -> new IgneaMiniVolcanoFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> IGNEA_MAGMA_CLUSTER = FEATURES.register(
            "ignea_magma_cluster",
            () -> new IgneaMagmaClusterFeature(NoneFeatureConfiguration.CODEC)
    );

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
