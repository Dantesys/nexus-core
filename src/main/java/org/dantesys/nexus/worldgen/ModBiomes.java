package org.dantesys.nexus.worldgen;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.dantesys.nexus.Nexus;

public class ModBiomes {
    public static final ResourceKey<Biome> IGNEO = registerKey("fiery_valley");
    public static final ResourceKey<Biome> VIDA = registerKey("living_forest");
    public static final ResourceKey<Biome> SOMBRIA = registerKey("forest_of_shadows");
    public static final ResourceKey<Biome> LUZ = registerKey("illuminated_valley");
    public static final ResourceKey<Biome> METAL = registerKey("metallic_field");
    public static final ResourceKey<Biome> TEMPESTADE = registerKey("stormy_hill");

    public static void bootstrap(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<WorldCarver> carvers = ctx.lookup(Registries.CARVER);
        BiomeGenerationSettings.Builder igneaGen = new BiomeGenerationSettings.Builder(placed, carvers);
        igneaGen.addFeature(GenerationStep.Decoration.LAKES, ModPlacedFeatures.IGNEA_LAVA_LAKE_PLACED_KEY);
        igneaGen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.IGNEA_ROCK_PLACED_KEY);
        igneaGen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.IGNEA_MAGMA_CLUSTER_PLACED_KEY);
        igneaGen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.IGNEA_MINI_VOLCANO_PLACED_KEY);
        igneaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.IGNEA_PLACED_KEY);
        igneaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_NORMAL);
        igneaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_DEAD_BUSH);
        MobSpawnSettings igneaSpawns = createIgneaSpawns();
        ctx.register(IGNEO, createBiome(
                new BiomeSpecialEffects.Builder()
                        .waterColor(0x5D4038)
                        .foliageColorOverride(0xD86024)
                        .dryFoliageColorOverride(0xA33A1E)
                        .grassColorOverride(0x5B2A22)
                        .build(),
                igneaGen.build(),
                igneaSpawns,
                2.0f,
                0.0f,
                false
        ));
        BiomeGenerationSettings.Builder vidaGen = new BiomeGenerationSettings.Builder(placed, carvers);
        vidaGen.addFeature(GenerationStep.Decoration.LAKES, ModPlacedFeatures.VIDA_LAKE_PLACED_KEY);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.VIDA_PLACED_KEY);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_NORMAL);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_TALL_GRASS);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_MEADOW);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FLOWER_FOREST);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_RIVER);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.KELP_COLD);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_WARM);
        vidaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEA_PICKLE);
        MobSpawnSettings vidaSpawns = createVidaSpawns();
        ctx.register(VIDA, createBiome(
                new BiomeSpecialEffects.Builder()
                        .waterColor(0x48DDF2)
                        .foliageColorOverride(0x4FE83B)
                        .grassColorOverride(0x63F04A)
                        .build(),
                vidaGen.build(),
                vidaSpawns,
                0.85f,
                0.9f,
                true
        ));
        BiomeGenerationSettings.Builder sombriaGen = new BiomeGenerationSettings.Builder(placed, carvers);
        sombriaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.SOMBRIA_PLACED_KEY);
        sombriaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_NORMAL);
        sombriaGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_TALL_GRASS);
        MobSpawnSettings sombriaSpawns = createSombriaSpawns();
        ctx.register(SOMBRIA, createBiome(
                new BiomeSpecialEffects.Builder()
                        .waterColor(0x15102A)
                        .foliageColorOverride(0x281B42)
                        .dryFoliageColorOverride(0x171023)
                        .grassColorOverride(0x202238)
                        .build(),
                sombriaGen.build(),
                sombriaSpawns,
                0.5f,
                0.6f,
                true
        ));
        BiomeGenerationSettings.Builder luzGen = new BiomeGenerationSettings.Builder(placed, carvers);
        luzGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.LUZ_PLACED_KEY);
        luzGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_NORMAL);
        luzGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_TALL_GRASS);
        luzGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_MEADOW);
        luzGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FLOWER_FOREST);
        luzGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_PLAINS);
        MobSpawnSettings luzSpawns = createLuzSpawns();
        ctx.register(LUZ, createBiome(
                new BiomeSpecialEffects.Builder()
                        .waterColor(0x7FE7F2)
                        .foliageColorOverride(0xF1D979)
                        .dryFoliageColorOverride(0xD8C96D)
                        .grassColorOverride(0xB9D96A)
                        .build(),
                luzGen.build(),
                luzSpawns,
                0.8f,
                0.4f,
                true
        ));
        BiomeGenerationSettings.Builder tempestadeGen = new BiomeGenerationSettings.Builder(placed, carvers);
        tempestadeGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.TEMPESTADE_PLACED_KEY);
        tempestadeGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_NORMAL);
        tempestadeGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_TALL_GRASS);
        MobSpawnSettings tempestadeSpawns = createTempestadeSpawns();
        ctx.register(TEMPESTADE, createBiome(
                new BiomeSpecialEffects.Builder()
                        .waterColor(0x227FD4)
                        .foliageColorOverride(0x2F9BD8)
                        .dryFoliageColorOverride(0x4D7F95)
                        .grassColorOverride(0x3F8D8D)
                        .build(),
                tempestadeGen.build(),
                tempestadeSpawns,
                0.7f,
                0.9f,
                true
        ));
        BiomeSpecialEffects metalEffects = new BiomeSpecialEffects.Builder()
                .waterColor(0x6F9FB5)
                .foliageColorOverride(0x8EA6B4)
                .dryFoliageColorOverride(0x748894)
                .grassColorOverride(0x8A9A8B)
                .build();
        MobSpawnSettings metalSpawns = createMetalSpawns();
        BiomeGenerationSettings.Builder metalGen = new BiomeGenerationSettings.Builder(placed,carvers);
        metalGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.METAL_PLACED_KEY);
        metalGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_NORMAL);
        metalGen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_TALL_GRASS);
        metalGen.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ModPlacedFeatures.METAL_PILAR_PLACED_KEY);
        Biome metal = new Biome.BiomeBuilder()
                .specialEffects(metalEffects)
                .mobSpawnSettings(metalSpawns)
                .generationSettings(metalGen.build())
                .temperature(0.4f)
                .downfall(0.2f)
                .hasPrecipitation(false)
                .build();
        ctx.register(METAL, metal);
    }

    private static Biome createBiome(BiomeSpecialEffects effects, BiomeGenerationSettings generationSettings, float temperature, float downfall, boolean hasPrecipitation) {
        return createBiome(effects, generationSettings, MobSpawnSettings.EMPTY, temperature, downfall, hasPrecipitation);
    }

    private static Biome createBiome(BiomeSpecialEffects effects, BiomeGenerationSettings generationSettings,
                                     MobSpawnSettings spawnSettings, float temperature, float downfall,
                                     boolean hasPrecipitation) {
        return new Biome.BiomeBuilder()
                .specialEffects(effects)
                .mobSpawnSettings(spawnSettings)
                .generationSettings(generationSettings)
                .temperature(temperature)
                .downfall(downfall)
                .hasPrecipitation(hasPrecipitation)
                .build();
    }

    private static MobSpawnSettings createSombriaSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();

        addSpawn(spawns, MobCategory.MONSTER, 100, EntityTypes.ZOMBIE, 2, 4);
        addSpawn(spawns, MobCategory.MONSTER, 100, EntityTypes.SKELETON, 2, 4);
        addSpawn(spawns, MobCategory.MONSTER, 80, EntityTypes.SPIDER, 1, 4);
        addSpawn(spawns, MobCategory.MONSTER, 70, EntityTypes.CREEPER, 1, 3);
        addSpawn(spawns, MobCategory.MONSTER, 30, EntityTypes.ENDERMAN, 1, 2);
        addSpawn(spawns, MobCategory.MONSTER, 12, EntityTypes.WITCH, 1, 1);

        return spawns.build();
    }

    private static MobSpawnSettings createIgneaSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();

        addSpawn(spawns, MobCategory.MONSTER, 90, EntityTypes.MAGMA_CUBE, 1, 4);
        addSpawn(spawns, MobCategory.MONSTER, 3, EntityTypes.BLAZE, 1, 1);

        return spawns.build();
    }

    private static MobSpawnSettings createLuzSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();

        addPeacefulLandSpawns(spawns, 4);
        addSpawn(spawns, MobCategory.CREATURE, 2, EntityTypes.HORSE, 2, 3);
        addSpawn(spawns, MobCategory.CREATURE, 1, EntityTypes.DONKEY, 1, 2);
        addSpawn(spawns, MobCategory.CREATURE, 1, EntityTypes.PARROT, 1, 2);

        return spawns.build();
    }

    private static MobSpawnSettings createTempestadeSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();

        addPeacefulLandSpawns(spawns, 20);
        addSpawn(spawns, MobCategory.MONSTER, 2, EntityTypes.BREEZE, 1, 1);

        return spawns.build();
    }

    private static MobSpawnSettings createMetalSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();

        addPeacefulLandSpawns(spawns, 20);

        return spawns.build();
    }

    private static MobSpawnSettings createVidaSpawns() {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();

        // Fauna terrestre estritamente pacifica.
        addPeacefulLandSpawns(spawns, 10);
        addSpawn(spawns, MobCategory.CREATURE, 5, EntityTypes.HORSE, 2, 4);
        addSpawn(spawns, MobCategory.CREATURE, 2, EntityTypes.DONKEY, 1, 2);
        addSpawn(spawns, MobCategory.CREATURE, 4, EntityTypes.PARROT, 1, 2);

        // Fauna pacifica para rios, lagos e porcoes oceanicas do bioma.
        addSpawn(spawns, MobCategory.WATER_AMBIENT, 15, EntityTypes.COD, 3, 6);
        addSpawn(spawns, MobCategory.WATER_AMBIENT, 15, EntityTypes.SALMON, 3, 6);
        addSpawn(spawns, MobCategory.WATER_AMBIENT, 8, EntityTypes.TROPICAL_FISH, 3, 6);
        addSpawn(spawns, MobCategory.WATER_CREATURE, 10, EntityTypes.SQUID, 1, 4);
        addSpawn(spawns, MobCategory.WATER_CREATURE, 2, EntityTypes.DOLPHIN, 1, 2);
        addSpawn(spawns, MobCategory.AXOLOTLS, 4, EntityTypes.AXOLOTL, 1, 2);
        addSpawn(spawns, MobCategory.UNDERGROUND_WATER_CREATURE, 5, EntityTypes.GLOW_SQUID, 1, 3);

        return spawns.build();
    }

    private static void addPeacefulLandSpawns(MobSpawnSettings.Builder spawns, int baseWeight) {
        addSpawn(spawns, MobCategory.CREATURE, baseWeight + 2, EntityTypes.SHEEP, 2, 4);
        addSpawn(spawns, MobCategory.CREATURE, baseWeight, EntityTypes.PIG, 2, 4);
        addSpawn(spawns, MobCategory.CREATURE, baseWeight, EntityTypes.CHICKEN, 2, 4);
        addSpawn(spawns, MobCategory.CREATURE, baseWeight, EntityTypes.COW, 2, 4);
        addSpawn(spawns, MobCategory.CREATURE, Math.max(1, baseWeight - 2), EntityTypes.RABBIT, 2, 3);
    }

    private static void addSpawn(MobSpawnSettings.Builder spawns, MobCategory category, int weight,
                                 EntityType<?> entityType, int minCount, int maxCount) {
        // Na 26.3 a categoria vem do próprio tipo de entidade.
        spawns.addSpawn(entityType, weight, minCount, maxCount);
    }

    private static ResourceKey<Biome> registerKey(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(Nexus.MODID, name));
    }
}
