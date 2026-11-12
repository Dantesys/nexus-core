package org.dantesys.nexus.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.dantesys.nexus.Nexus;

public class ModTreeGrowes {
    public static final TreeGrower IGNEA = criar("ignea", ModConfiguratedFeatures.IGNEA_KEY);
    public static final TreeGrower TEMPESTADE = criar("tempestade", ModConfiguratedFeatures.TEMPESTADE_KEY);
    public static final TreeGrower SOMBRIA = criar("sombria", ModConfiguratedFeatures.SOMBRIA_KEY);
    public static final TreeGrower LUZ = criar("luz", ModConfiguratedFeatures.LUZ_KEY);
    public static final TreeGrower METAL = criar("metal", ModConfiguratedFeatures.METAL_KEY);
    public static final TreeGrower VIDA = criar("vida", ModConfiguratedFeatures.VIDA_KEY);

    // Uma árvore normal, sem versão 2x2 nem variante com flores por perto.
    private static TreeGrower criar(String nome, ResourceKey<Feature> arvore) {
        return new TreeGrower(Nexus.MODID + ":" + nome, WeightedList.of(arvore), WeightedList.of(), WeightedList.of(), arvore);
    }
}