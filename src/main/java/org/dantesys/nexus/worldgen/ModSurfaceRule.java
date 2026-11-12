package org.dantesys.nexus.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

import static net.minecraft.world.level.levelgen.material.MaterialRules.*;

/**
 * Regras de superfície dos biomas (na 26.3 viraram "material rules").
 * Os biomas agora são referenciados pelo registro, por isso tudo é montado a partir do RegistryAccess.
 */
public class ModSurfaceRule {
    private static final MaterialRule DIRT = makeStateRule(Blocks.DIRT);
    private static final MaterialRule GRASS_BLOCK = makeStateRule(Blocks.GRASS_BLOCK);
    private static final MaterialRule SAND = makeStateRule(Blocks.SAND);
    private static final MaterialRule GRAVEL = makeStateRule(Blocks.GRAVEL);
    private static final MaterialRule CLAY = makeStateRule(Blocks.CLAY);
    private static final MaterialRule STONE = makeStateRule(Blocks.STONE);
    private static final MaterialRule ANDESITE = makeStateRule(Blocks.ANDESITE);
    private static final MaterialRule COARSE_DIRT = makeStateRule(Blocks.COARSE_DIRT);
    private static final MaterialRule MAGMA_BLOCK = makeStateRule(Blocks.MAGMA_BLOCK);

    private static final MaterialCondition ON_FLOOR = stoneDepthCheck(0, false, CaveSurface.FLOOR);
    private static final MaterialCondition UNDER_FLOOR = stoneDepthCheck(0, true, CaveSurface.FLOOR);

    private static MaterialRule makeStateRule(Block block) {
        return state(block.defaultBlockState());
    }

    private static MaterialCondition ruido(ResourceKey<net.minecraft.world.level.levelgen.synth.NormalNoise> noise, double min) {
        return noiseCondition2d(noise, min);
    }

    public static MaterialRule defaltuRule(HolderGetter<Biome> biomes, ResourceKey<Biome> biome) {
        return sequence(
                ifTrue(isBiome(biomes, biome), sequence(
                        ifTrue(ON_FLOOR, sequence(
                                ifTrue(waterBlockCheck(0, 0), GRASS_BLOCK),
                                ifTrue(ruido(Noises.CLAY_BANDS_OFFSET, 0.45D), CLAY),
                                ifTrue(ruido(Noises.GRAVEL, 0.35D), GRAVEL),
                                SAND
                        )),
                        ifTrue(UNDER_FLOOR, DIRT))
                ));
    }

    public static MaterialRule igneoRule(HolderGetter<Biome> biomes) {
        return sequence(
                ifTrue(isBiome(biomes, ModBiomes.IGNEO), sequence(
                        ifTrue(ON_FLOOR, sequence(
                                ifTrue(not(waterBlockCheck(0, 0)), MAGMA_BLOCK),
                                ifTrue(waterBlockCheck(0, 0), sequence(
                                        ifTrue(ruido(Noises.SURFACE, 0.48D), STONE),
                                        ifTrue(ruido(Noises.GRAVEL, 0.38D), ANDESITE),
                                        ifTrue(ruido(Noises.SURFACE_SECONDARY, 0.62D), COARSE_DIRT),
                                        GRASS_BLOCK
                                )),
                                ifTrue(ruido(Noises.CLAY_BANDS_OFFSET, 0.45D), CLAY),
                                ifTrue(ruido(Noises.GRAVEL, 0.35D), GRAVEL),
                                SAND
                        )),
                        ifTrue(UNDER_FLOOR, sequence(
                                ifTrue(ruido(Noises.SURFACE, 0.5D), STONE),
                                DIRT
                        ))
                ))
        );
    }

    public static MaterialRule metalRule(HolderGetter<Biome> biomes) {
        return sequence(
                ifTrue(isBiome(biomes, ModBiomes.METAL), sequence(
                        ifTrue(ON_FLOOR, sequence(
                                ifTrue(waterBlockCheck(0, 0), sequence(
                                        ifTrue(ruido(Noises.SURFACE, 0.35D), STONE),
                                        ifTrue(ruido(Noises.GRAVEL, 0.40D), ANDESITE),
                                        ifTrue(ruido(Noises.SURFACE_SECONDARY, 0.55D), GRAVEL),
                                        GRASS_BLOCK
                                )),
                                ifTrue(ruido(Noises.CLAY_BANDS_OFFSET, 0.45D), CLAY),
                                ifTrue(ruido(Noises.GRAVEL, 0.35D), GRAVEL),
                                SAND
                        )),
                        ifTrue(UNDER_FLOOR, sequence(
                                ifTrue(ruido(Noises.SURFACE, 0.35D), STONE),
                                ifTrue(ruido(Noises.GRAVEL, 0.45D), ANDESITE),
                                DIRT
                        ))
                ))
        );
    }

    /** Usado pelo TerraBlender (MaterialRuleManager.RuleBuilder). */
    public static MaterialRule registerAll(RegistryAccess registries) {
        HolderGetter<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
        return sequence(
                igneoRule(biomes),
                metalRule(biomes),
                defaltuRule(biomes, ModBiomes.SOMBRIA),
                defaltuRule(biomes, ModBiomes.LUZ),
                defaltuRule(biomes, ModBiomes.VIDA),
                defaltuRule(biomes, ModBiomes.TEMPESTADE)
        );
    }
}
