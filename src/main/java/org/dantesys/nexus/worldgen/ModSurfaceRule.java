package org.dantesys.nexus.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;

import static net.minecraft.world.level.levelgen.SurfaceRules.*;
import static net.minecraft.world.level.levelgen.SurfaceRules.sequence;

public class ModSurfaceRule {
    private static final SurfaceRules.RuleSource DIRT = makeStateRule(Blocks.DIRT);
    private static final SurfaceRules.RuleSource GRASS_BLOCK = makeStateRule(Blocks.GRASS_BLOCK);
    private static final SurfaceRules.RuleSource SAND = makeStateRule(Blocks.SAND);
    private static final SurfaceRules.RuleSource GRAVEL = makeStateRule(Blocks.GRAVEL);
    private static final SurfaceRules.RuleSource CLAY = makeStateRule(Blocks.CLAY);
    private static final SurfaceRules.RuleSource STONE = makeStateRule(Blocks.STONE);
    private static final SurfaceRules.RuleSource ANDESITE = makeStateRule(Blocks.ANDESITE);
    private static final SurfaceRules.RuleSource COARSE_DIRT = makeStateRule(Blocks.COARSE_DIRT);
    private static final SurfaceRules.RuleSource MAGMA_BLOCK = makeStateRule(Blocks.MAGMA_BLOCK);

    private static SurfaceRules.RuleSource makeStateRule(Block block)
    {
        return SurfaceRules.state(block.defaultBlockState());
    }
    public static SurfaceRules.RuleSource defaltuRule(ResourceKey<Biome> biome) {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(isBiome(biome), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(waterBlockCheck(0, 0), GRASS_BLOCK),
                                SurfaceRules.ifTrue(noiseCondition(Noises.CLAY_BANDS_OFFSET, 0.45D), CLAY),
                                SurfaceRules.ifTrue(noiseCondition(Noises.GRAVEL, 0.35D), GRAVEL),
                                SAND
                        )),
                        SurfaceRules.ifTrue(UNDER_FLOOR, DIRT))
                ));
    }

    public static SurfaceRules.RuleSource igneoRule() {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(isBiome(ModBiomes.IGNEO), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(not(waterBlockCheck(0, 0)), MAGMA_BLOCK),
                                SurfaceRules.ifTrue(waterBlockCheck(0, 0), SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(noiseCondition(Noises.SURFACE, 0.48D), STONE),
                                        SurfaceRules.ifTrue(noiseCondition(Noises.GRAVEL, 0.38D), ANDESITE),
                                        SurfaceRules.ifTrue(noiseCondition(Noises.SURFACE_SECONDARY, 0.62D), COARSE_DIRT),
                                        GRASS_BLOCK
                                )),
                                SurfaceRules.ifTrue(noiseCondition(Noises.CLAY_BANDS_OFFSET, 0.45D), CLAY),
                                SurfaceRules.ifTrue(noiseCondition(Noises.GRAVEL, 0.35D), GRAVEL),
                                SAND
                        )),
                        SurfaceRules.ifTrue(UNDER_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(noiseCondition(Noises.SURFACE, 0.5D), STONE),
                                DIRT
                        ))
                ))
        );
    }

    public static SurfaceRules.RuleSource metalRule() {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(isBiome(ModBiomes.METAL), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(waterBlockCheck(0, 0), SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(noiseCondition(Noises.SURFACE, 0.35D), STONE),
                                        SurfaceRules.ifTrue(noiseCondition(Noises.GRAVEL, 0.40D), ANDESITE),
                                        SurfaceRules.ifTrue(noiseCondition(Noises.SURFACE_SECONDARY, 0.55D), GRAVEL),
                                        GRASS_BLOCK
                                )),
                                SurfaceRules.ifTrue(noiseCondition(Noises.CLAY_BANDS_OFFSET, 0.45D), CLAY),
                                SurfaceRules.ifTrue(noiseCondition(Noises.GRAVEL, 0.35D), GRAVEL),
                                SAND
                        )),
                        SurfaceRules.ifTrue(UNDER_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(noiseCondition(Noises.SURFACE, 0.35D), STONE),
                                SurfaceRules.ifTrue(noiseCondition(Noises.GRAVEL, 0.45D), ANDESITE),
                                DIRT
                        ))
                ))
        );
    }
    // Utility to register them in TerraBlender region
    public static SurfaceRules.RuleSource registerAll() {
        return sequence(
                igneoRule(),
                metalRule(),
                defaltuRule(ModBiomes.SOMBRIA),
                defaltuRule(ModBiomes.LUZ),
                defaltuRule(ModBiomes.VIDA),
                defaltuRule(ModBiomes.TEMPESTADE)
        );
    }
}
