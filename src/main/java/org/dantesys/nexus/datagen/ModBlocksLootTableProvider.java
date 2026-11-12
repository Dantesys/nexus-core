package org.dantesys.nexus.datagen;

import net.minecraft.core.Holder;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.items.ModItems;

import java.util.Set;

public class ModBlocksLootTableProvider extends BlockLootSubProvider {
    public ModBlocksLootTableProvider(LootTableSubProvider.Context context) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.STEEL_BLOCK.get());
        dropSelf(ModBlocks.TANQUE_FORJA.get());
        dropSelf(ModBlocks.CONTROLADOR_FORJA.get());
        dropSelf(ModBlocks.BOLA_CRISTAL.get());
        dropSelf(ModBlocks.COLETOR_ARCANO_BASICO.get());
        dropSelf(ModBlocks.INFUSOR_ARCANO_BASICO.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
