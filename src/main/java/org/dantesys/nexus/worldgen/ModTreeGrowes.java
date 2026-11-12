package org.dantesys.nexus.worldgen;

import net.minecraft.world.level.block.grower.TreeGrower;
import org.dantesys.nexus.Nexus;

import java.util.Optional;

public class ModTreeGrowes {
    public static final TreeGrower IGNEA = new TreeGrower(Nexus.MODID + ":ignea",
            Optional.empty(), Optional.of(ModConfiguratedFeatures.IGNEA_KEY), Optional.empty());
    public static final TreeGrower TEMPESTADE = new TreeGrower(Nexus.MODID + ":tempestade",
            Optional.empty(), Optional.of(ModConfiguratedFeatures.TEMPESTADE_KEY), Optional.empty());
    public static final TreeGrower SOMBRIA = new TreeGrower(Nexus.MODID + ":sombria",
            Optional.empty(), Optional.of(ModConfiguratedFeatures.SOMBRIA_KEY), Optional.empty());
    public static final TreeGrower LUZ = new TreeGrower(Nexus.MODID + ":luz",
            Optional.empty(), Optional.of(ModConfiguratedFeatures.LUZ_KEY), Optional.empty());
    public static final TreeGrower METAL = new TreeGrower(Nexus.MODID + ":metal",
            Optional.empty(), Optional.of(ModConfiguratedFeatures.METAL_KEY), Optional.empty());
    public static final TreeGrower VIDA = new TreeGrower(Nexus.MODID + ":vida",
            Optional.empty(), Optional.of(ModConfiguratedFeatures.VIDA_KEY), Optional.empty());
}
