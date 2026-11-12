package org.dantesys.nexus.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> IGNEA_PLACED_KEY = registerKey("ignea_placed");
    public static final ResourceKey<PlacedFeature> TEMPESTADE_PLACED_KEY = registerKey("tempestade_placed");
    public static final ResourceKey<PlacedFeature> SOMBRIA_PLACED_KEY = registerKey("sombria_placed");
    public static final ResourceKey<PlacedFeature> LUZ_PLACED_KEY = registerKey("luz_placed");
    public static final ResourceKey<PlacedFeature> METAL_PLACED_KEY = registerKey("metal_placed");
    public static final ResourceKey<PlacedFeature> METAL_PILAR_PLACED_KEY = registerKey("metal_pilar_placed");
    public static final ResourceKey<PlacedFeature> VIDA_PLACED_KEY = registerKey("vida_placed");
    public static final ResourceKey<PlacedFeature> VIDA_LAKE_PLACED_KEY = registerKey("vida_lake_placed");
    public static final ResourceKey<PlacedFeature> IGNEA_LAVA_LAKE_PLACED_KEY = registerKey("ignea_lava_lake_placed");
    public static final ResourceKey<PlacedFeature> IGNEA_MAGMA_CLUSTER_PLACED_KEY = registerKey("ignea_magma_cluster_placed");
    public static final ResourceKey<PlacedFeature> IGNEA_ROCK_PLACED_KEY = registerKey("ignea_rock_placed");
    public static final ResourceKey<PlacedFeature> IGNEA_MINI_VOLCANO_PLACED_KEY = registerKey("ignea_mini_volcano_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        registerSparseTree(context, IGNEA_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguratedFeatures.IGNEA_KEY),
                ModBlocks.IGNEA_SAPLING.get());
        registerMediumTree(context, TEMPESTADE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguratedFeatures.TEMPESTADE_KEY),
                ModBlocks.TEMPESTADE_SAPLING.get());
        registerTree(context, SOMBRIA_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguratedFeatures.SOMBRIA_KEY),
                ModBlocks.SOMBRIA_SAPLING.get());
        registerMediumTree(context, LUZ_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguratedFeatures.LUZ_KEY),
                ModBlocks.LUZ_SAPLING.get());
        registerSparseTree(context, METAL_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguratedFeatures.METAL_KEY),
                ModBlocks.METAL_SAPLING.get());
        registerTree(context, VIDA_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguratedFeatures.VIDA_KEY),
                ModBlocks.VIDA_SAPLING.get());
        registerLake(context, configuredFeatures.getOrThrow(ModConfiguratedFeatures.VIDA_LAKE_KEY));
        registerLavaLake(context, configuredFeatures.getOrThrow(ModConfiguratedFeatures.IGNEA_LAVA_LAKE_KEY));
        registerMagmaCluster(context, configuredFeatures.getOrThrow(ModConfiguratedFeatures.IGNEA_MAGMA_CLUSTER_KEY));
        registerRock(context, configuredFeatures.getOrThrow(ModConfiguratedFeatures.IGNEA_ROCK_KEY));
        registerMiniVolcano(context, configuredFeatures.getOrThrow(ModConfiguratedFeatures.IGNEA_MINI_VOLCANO_KEY));
        registerStone(context, configuredFeatures.getOrThrow(ModConfiguratedFeatures.METAL_PILAR_KEY));
    }
    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(Nexus.MODID, name));
    }
    private static void registerStone(BootstrapContext<PlacedFeature> ctx,
                                      Holder<ConfiguredFeature<?, ?>> configuration){
        List<PlacementModifier> modifiers = List.of(
                CountPlacement.of(1),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                SurfaceWaterDepthFilter.forMaxDepth(0),
                BiomeFilter.biome()
        );

        ctx.register(ModPlacedFeatures.METAL_PILAR_PLACED_KEY, new PlacedFeature(configuration, modifiers));
    }

    private static void registerLavaLake(BootstrapContext<PlacedFeature> ctx,
                                         Holder<ConfiguredFeature<?, ?>> configuration) {
        List<PlacementModifier> modifiers = List.of(
                RarityFilter.onAverageOnceEvery(10),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        );

        ctx.register(ModPlacedFeatures.IGNEA_LAVA_LAKE_PLACED_KEY, new PlacedFeature(configuration, modifiers));
    }

    private static void registerMagmaCluster(BootstrapContext<PlacedFeature> ctx,
                                             Holder<ConfiguredFeature<?, ?>> configuration) {
        List<PlacementModifier> modifiers = List.of(
                CountPlacement.of(UniformInt.of(1, 2)),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                SurfaceWaterDepthFilter.forMaxDepth(0),
                BiomeFilter.biome()
        );

        ctx.register(ModPlacedFeatures.IGNEA_MAGMA_CLUSTER_PLACED_KEY, new PlacedFeature(configuration, modifiers));
    }

    private static void registerRock(BootstrapContext<PlacedFeature> ctx,
                                     Holder<ConfiguredFeature<?, ?>> configuration) {
        List<PlacementModifier> modifiers = List.of(
                CountPlacement.of(2),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                SurfaceWaterDepthFilter.forMaxDepth(0),
                BiomeFilter.biome()
        );

        ctx.register(ModPlacedFeatures.IGNEA_ROCK_PLACED_KEY, new PlacedFeature(configuration, modifiers));
    }

    private static void registerMiniVolcano(BootstrapContext<PlacedFeature> ctx,
                                            Holder<ConfiguredFeature<?, ?>> configuration) {
        List<PlacementModifier> modifiers = List.of(
                RarityFilter.onAverageOnceEvery(48),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                SurfaceWaterDepthFilter.forMaxDepth(0),
                BiomeFilter.biome()
        );

        ctx.register(ModPlacedFeatures.IGNEA_MINI_VOLCANO_PLACED_KEY, new PlacedFeature(configuration, modifiers));
    }

    private static void registerLake(BootstrapContext<PlacedFeature> ctx,
                                     Holder<ConfiguredFeature<?, ?>> configuration) {
        List<PlacementModifier> modifiers = List.of(
                RarityFilter.onAverageOnceEvery(18),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome()
        );

        ctx.register(ModPlacedFeatures.VIDA_LAKE_PLACED_KEY, new PlacedFeature(configuration, modifiers));
    }

    private static void registerTree(
            BootstrapContext<PlacedFeature> ctx,
            ResourceKey<PlacedFeature> key,
            Holder<ConfiguredFeature<?, ?>> configuration,
            net.minecraft.world.level.block.Block sapling) {

        List<PlacementModifier> modifiers = List.of(
                CountPlacement.of(4),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                BiomeFilter.biome(),
                PlacementUtils.countExtra(2, 0.2f, 1),
                PlacementUtils.filteredByBlockSurvival(sapling),
                BiomeFilter.biome(),
                SurfaceWaterDepthFilter.forMaxDepth(0)
        );

        ctx.register(key, new PlacedFeature(configuration, modifiers));
    }

    private static void registerSparseTree(
            BootstrapContext<PlacedFeature> ctx,
            ResourceKey<PlacedFeature> key,
            Holder<ConfiguredFeature<?, ?>> configuration,
            net.minecraft.world.level.block.Block sapling) {

        List<PlacementModifier> modifiers = List.of(
                CountPlacement.of(1),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                PlacementUtils.countExtra(0, 0.25f, 1),
                PlacementUtils.filteredByBlockSurvival(sapling),
                SurfaceWaterDepthFilter.forMaxDepth(0),
                BiomeFilter.biome()
        );

        ctx.register(key, new PlacedFeature(configuration, modifiers));
    }

    private static void registerMediumTree(
            BootstrapContext<PlacedFeature> ctx,
            ResourceKey<PlacedFeature> key,
            Holder<ConfiguredFeature<?, ?>> configuration,
            net.minecraft.world.level.block.Block sapling) {

        List<PlacementModifier> modifiers = List.of(
                CountPlacement.of(2),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
                PlacementUtils.countExtra(1, 0.25f, 1),
                PlacementUtils.filteredByBlockSurvival(sapling),
                SurfaceWaterDepthFilter.forMaxDepth(0),
                BiomeFilter.biome()
        );

        ctx.register(key, new PlacedFeature(configuration, modifiers));
    }
}
