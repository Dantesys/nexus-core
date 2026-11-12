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
                .add(ModBlocks.STEEL_BLOCK.get())
                .add(ModBlocks.TANQUE_FORJA.get())
                .add(ModBlocks.CONTROLADOR_FORJA.get())
                .add(ModBlocks.COLETOR_ARCANO_BASICO.get())
                .add(ModBlocks.INFUSOR_ARCANO_BASICO.get());

        tag(ModTags.Blocks.STEEL_STORAGE_BLOCKS).add(ModBlocks.STEEL_BLOCK.get());

        tag(BlockTags.LOGS)
                .add(ModBlocks.IGNEA_LOG.get())
                .add(ModBlocks.STRIPPED_IGNEA_LOG.get())
                .add(ModBlocks.LUZ_LOG.get())
                .add(ModBlocks.STRIPPED_LUZ_LOG.get())
                .add(ModBlocks.METAL_LOG.get())
                .add(ModBlocks.STRIPPED_METAL_LOG.get())
                .add(ModBlocks.SOMBRIA_LOG.get())
                .add(ModBlocks.STRIPPED_SOMBRIA_LOG.get())
                .add(ModBlocks.TEMPESTADE_LOG.get())
                .add(ModBlocks.STRIPPED_TEMPESTADE_LOG.get())
                .add(ModBlocks.VIDA_LOG.get())
                .add(ModBlocks.STRIPPED_VIDA_LOG.get());
        tag(BlockTags.SLABS)
                .add(ModBlocks.IGNEA_SLAB.get())
                .add(ModBlocks.LUZ_SLAB.get())
                .add(ModBlocks.METAL_SLAB.get())
                .add(ModBlocks.SOMBRIA_SLAB.get())
                .add(ModBlocks.TEMPESTADE_SLAB.get())
                .add(ModBlocks.VIDA_SLAB.get());
        tag(BlockTags.STAIRS)
                .add(ModBlocks.IGNEA_STAIRS.get())
                .add(ModBlocks.LUZ_STAIRS.get())
                .add(ModBlocks.METAL_STAIRS.get())
                .add(ModBlocks.SOMBRIA_STAIRS.get())
                .add(ModBlocks.TEMPESTADE_STAIRS.get())
                .add(ModBlocks.VIDA_STAIRS.get());
        tag(BlockTags.PLANKS)
                .add(ModBlocks.IGNEA_PLANKS.get())
                .add(ModBlocks.LUZ_PLANKS.get())
                .add(ModBlocks.METAL_PLANKS.get())
                .add(ModBlocks.SOMBRIA_PLANKS.get())
                .add(ModBlocks.TEMPESTADE_PLANKS.get())
                .add(ModBlocks.VIDA_PLANKS.get());
        tag(BlockTags.PRESSURE_PLATES)
                .add(ModBlocks.IGNEA_PRESSURE_PLATE.get())
                .add(ModBlocks.LUZ_PRESSURE_PLATE.get())
                .add(ModBlocks.METAL_PRESSURE_PLATE.get())
                .add(ModBlocks.SOMBRIA_PRESSURE_PLATE.get())
                .add(ModBlocks.TEMPESTADE_PRESSURE_PLATE.get())
                .add(ModBlocks.VIDA_PRESSURE_PLATE.get());
        tag(BlockTags.DOORS)
                .add(ModBlocks.IGNEA_DOOR.get())
                .add(ModBlocks.LUZ_DOOR.get())
                .add(ModBlocks.METAL_DOOR.get())
                .add(ModBlocks.SOMBRIA_DOOR.get())
                .add(ModBlocks.TEMPESTADE_DOOR.get())
                .add(ModBlocks.VIDA_DOOR.get());
        tag(BlockTags.TRAPDOORS)
                .add(ModBlocks.IGNEA_TRAPDOOR.get())
                .add(ModBlocks.LUZ_TRAPDOOR.get())
                .add(ModBlocks.METAL_TRAPDOOR.get())
                .add(ModBlocks.SOMBRIA_TRAPDOOR.get())
                .add(ModBlocks.TEMPESTADE_TRAPDOOR.get())
                .add(ModBlocks.VIDA_TRAPDOOR.get());
        tag(BlockTags.BUTTONS)
                .add(ModBlocks.IGNEA_BUTTON.get())
                .add(ModBlocks.LUZ_BUTTON.get())
                .add(ModBlocks.METAL_BUTTON.get())
                .add(ModBlocks.SOMBRIA_BUTTON.get())
                .add(ModBlocks.TEMPESTADE_BUTTON.get())
                .add(ModBlocks.VIDA_BUTTON.get());
        tag(BlockTags.FENCES)
                .add(ModBlocks.IGNEA_FENCE.get())
                .add(ModBlocks.LUZ_FENCE.get())
                .add(ModBlocks.METAL_FENCE.get())
                .add(ModBlocks.SOMBRIA_FENCE.get())
                .add(ModBlocks.TEMPESTADE_FENCE.get())
                .add(ModBlocks.VIDA_FENCE.get());
        tag(BlockTags.FENCE_GATES)
                .add(ModBlocks.IGNEA_FENCE_GATE.get())
                .add(ModBlocks.LUZ_FENCE_GATE.get())
                .add(ModBlocks.METAL_FENCE_GATE.get())
                .add(ModBlocks.SOMBRIA_FENCE_GATE.get())
                .add(ModBlocks.TEMPESTADE_FENCE_GATE.get())
                .add(ModBlocks.VIDA_FENCE_GATE.get());
        tag(BlockTags.LEAVES)
                .add(ModBlocks.IGNEA_LEAVES.get())
                .add(ModBlocks.LUZ_LEAVES.get())
                .add(ModBlocks.METAL_LEAVES.get())
                .add(ModBlocks.SOMBRIA_LEAVES.get())
                .add(ModBlocks.TEMPESTADE_LEAVES.get())
                .add(ModBlocks.VIDA_LEAVES.get());
        tag(BlockTags.SAPLINGS)
                .add(ModBlocks.IGNEA_SAPLING.get())
                .add(ModBlocks.LUZ_SAPLING.get())
                .add(ModBlocks.METAL_SAPLING.get())
                .add(ModBlocks.SOMBRIA_SAPLING.get())
                .add(ModBlocks.TEMPESTADE_SAPLING.get())
                .add(ModBlocks.VIDA_SAPLING.get());
    }
}
