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
                .add(ModBlocks.INFUSOR_ARCANO_BASICO.key())
                .add(ModBlocks.BOLA_CRISTAL.key());

        tag(ModTags.Blocks.STEEL_STORAGE_BLOCKS).add(ModBlocks.STEEL_BLOCK.key());

    }
}
