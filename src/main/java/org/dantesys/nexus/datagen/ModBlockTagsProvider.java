package org.dantesys.nexus.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.tags.ModTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Nexus.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.STEEL_BLOCK.key())
                .add(ModBlocks.TANQUE_FORJA.key())
                .add(ModBlocks.CONTROLADOR_FORJA.key())
                .add(ModBlocks.COLETOR_ARCANO_BASICO.key())
                .add(ModBlocks.INFUSOR_ARCANO_BASICO.key());

        tag(ModTags.Blocks.STEEL_STORAGE_BLOCKS).add(ModBlocks.STEEL_BLOCK.key());

        tag(BlockTags.LOGS)
                .add(ModBlocks.IGNEA_LOG.key())
                .add(ModBlocks.STRIPPED_IGNEA_LOG.key())
                .add(ModBlocks.LUZ_LOG.key())
                .add(ModBlocks.STRIPPED_LUZ_LOG.key())
                .add(ModBlocks.METAL_LOG.key())
                .add(ModBlocks.STRIPPED_METAL_LOG.key())
                .add(ModBlocks.SOMBRIA_LOG.key())
                .add(ModBlocks.STRIPPED_SOMBRIA_LOG.key())
                .add(ModBlocks.TEMPESTADE_LOG.key())
                .add(ModBlocks.STRIPPED_TEMPESTADE_LOG.key())
                .add(ModBlocks.VIDA_LOG.key())
                .add(ModBlocks.STRIPPED_VIDA_LOG.key());
        tag(BlockTags.SLABS)
                .add(ModBlocks.IGNEA_SLAB.key())
                .add(ModBlocks.LUZ_SLAB.key())
                .add(ModBlocks.METAL_SLAB.key())
                .add(ModBlocks.SOMBRIA_SLAB.key())
                .add(ModBlocks.TEMPESTADE_SLAB.key())
                .add(ModBlocks.VIDA_SLAB.key());
        tag(BlockTags.STAIRS)
                .add(ModBlocks.IGNEA_STAIRS.key())
                .add(ModBlocks.LUZ_STAIRS.key())
                .add(ModBlocks.METAL_STAIRS.key())
                .add(ModBlocks.SOMBRIA_STAIRS.key())
                .add(ModBlocks.TEMPESTADE_STAIRS.key())
                .add(ModBlocks.VIDA_STAIRS.key());
        tag(BlockTags.PLANKS)
                .add(ModBlocks.IGNEA_PLANKS.key())
                .add(ModBlocks.LUZ_PLANKS.key())
                .add(ModBlocks.METAL_PLANKS.key())
                .add(ModBlocks.SOMBRIA_PLANKS.key())
                .add(ModBlocks.TEMPESTADE_PLANKS.key())
                .add(ModBlocks.VIDA_PLANKS.key());
        tag(BlockTags.PRESSURE_PLATES)
                .add(ModBlocks.IGNEA_PRESSURE_PLATE.key())
                .add(ModBlocks.LUZ_PRESSURE_PLATE.key())
                .add(ModBlocks.METAL_PRESSURE_PLATE.key())
                .add(ModBlocks.SOMBRIA_PRESSURE_PLATE.key())
                .add(ModBlocks.TEMPESTADE_PRESSURE_PLATE.key())
                .add(ModBlocks.VIDA_PRESSURE_PLATE.key());
        tag(BlockTags.DOORS)
                .add(ModBlocks.IGNEA_DOOR.key())
                .add(ModBlocks.LUZ_DOOR.key())
                .add(ModBlocks.METAL_DOOR.key())
                .add(ModBlocks.SOMBRIA_DOOR.key())
                .add(ModBlocks.TEMPESTADE_DOOR.key())
                .add(ModBlocks.VIDA_DOOR.key());
        tag(BlockTags.TRAPDOORS)
                .add(ModBlocks.IGNEA_TRAPDOOR.key())
                .add(ModBlocks.LUZ_TRAPDOOR.key())
                .add(ModBlocks.METAL_TRAPDOOR.key())
                .add(ModBlocks.SOMBRIA_TRAPDOOR.key())
                .add(ModBlocks.TEMPESTADE_TRAPDOOR.key())
                .add(ModBlocks.VIDA_TRAPDOOR.key());
        tag(BlockTags.BUTTONS)
                .add(ModBlocks.IGNEA_BUTTON.key())
                .add(ModBlocks.LUZ_BUTTON.key())
                .add(ModBlocks.METAL_BUTTON.key())
                .add(ModBlocks.SOMBRIA_BUTTON.key())
                .add(ModBlocks.TEMPESTADE_BUTTON.key())
                .add(ModBlocks.VIDA_BUTTON.key());
        tag(BlockTags.FENCES)
                .add(ModBlocks.IGNEA_FENCE.key())
                .add(ModBlocks.LUZ_FENCE.key())
                .add(ModBlocks.METAL_FENCE.key())
                .add(ModBlocks.SOMBRIA_FENCE.key())
                .add(ModBlocks.TEMPESTADE_FENCE.key())
                .add(ModBlocks.VIDA_FENCE.key());
        tag(BlockTags.FENCE_GATES)
                .add(ModBlocks.IGNEA_FENCE_GATE.key())
                .add(ModBlocks.LUZ_FENCE_GATE.key())
                .add(ModBlocks.METAL_FENCE_GATE.key())
                .add(ModBlocks.SOMBRIA_FENCE_GATE.key())
                .add(ModBlocks.TEMPESTADE_FENCE_GATE.key())
                .add(ModBlocks.VIDA_FENCE_GATE.key());
        tag(BlockTags.LEAVES)
                .add(ModBlocks.IGNEA_LEAVES.key())
                .add(ModBlocks.LUZ_LEAVES.key())
                .add(ModBlocks.METAL_LEAVES.key())
                .add(ModBlocks.SOMBRIA_LEAVES.key())
                .add(ModBlocks.TEMPESTADE_LEAVES.key())
                .add(ModBlocks.VIDA_LEAVES.key());
        tag(BlockTags.SAPLINGS)
                .add(ModBlocks.IGNEA_SAPLING.key())
                .add(ModBlocks.LUZ_SAPLING.key())
                .add(ModBlocks.METAL_SAPLING.key())
                .add(ModBlocks.SOMBRIA_SAPLING.key())
                .add(ModBlocks.TEMPESTADE_SAPLING.key())
                .add(ModBlocks.VIDA_SAPLING.key());
    }
}
