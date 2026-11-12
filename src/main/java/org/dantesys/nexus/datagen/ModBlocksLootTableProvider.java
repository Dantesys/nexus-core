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
        dropSelf(ModBlocks.IGNEA_LOG.get());
        dropSelf(ModBlocks.IGNEA_PLANKS.get());
        add(ModBlocks.IGNEA_DOOR.get(),
                block -> createDoorTable(ModBlocks.IGNEA_DOOR.get()));
        dropSelf(ModBlocks.IGNEA_TRAPDOOR.get());
        dropSelf(ModBlocks.IGNEA_BUTTON.get());
        dropSelf(ModBlocks.IGNEA_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.IGNEA_STAIRS.get());
        dropSelf(ModBlocks.IGNEA_SLAB.get());
        dropSelf(ModBlocks.IGNEA_FENCE.get());
        dropSelf(ModBlocks.IGNEA_FENCE_GATE.get());
        dropSelf(ModBlocks.STRIPPED_IGNEA_LOG.get());
        dropSelf(ModBlocks.IGNEA_SAPLING.get());
        dropSelf(ModBlocks.LUZ_LOG.get());
        dropSelf(ModBlocks.LUZ_PLANKS.get());
        add(ModBlocks.LUZ_DOOR.get(),
                block -> createDoorTable(ModBlocks.LUZ_DOOR.get()));
        dropSelf(ModBlocks.LUZ_TRAPDOOR.get());
        dropSelf(ModBlocks.LUZ_BUTTON.get());
        dropSelf(ModBlocks.LUZ_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.LUZ_STAIRS.get());
        dropSelf(ModBlocks.LUZ_SLAB.get());
        dropSelf(ModBlocks.LUZ_FENCE.get());
        dropSelf(ModBlocks.LUZ_FENCE_GATE.get());
        dropSelf(ModBlocks.STRIPPED_LUZ_LOG.get());
        dropSelf(ModBlocks.LUZ_SAPLING.get());
        dropSelf(ModBlocks.METAL_LOG.get());
        dropSelf(ModBlocks.METAL_PLANKS.get());
        add(ModBlocks.METAL_DOOR.get(),
                block -> createDoorTable(ModBlocks.METAL_DOOR.get()));
        dropSelf(ModBlocks.METAL_TRAPDOOR.get());
        dropSelf(ModBlocks.METAL_BUTTON.get());
        dropSelf(ModBlocks.METAL_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.METAL_STAIRS.get());
        dropSelf(ModBlocks.METAL_SLAB.get());
        dropSelf(ModBlocks.METAL_FENCE.get());
        dropSelf(ModBlocks.METAL_FENCE_GATE.get());
        dropSelf(ModBlocks.STRIPPED_METAL_LOG.get());
        dropSelf(ModBlocks.METAL_SAPLING.get());
        dropSelf(ModBlocks.SOMBRIA_LOG.get());
        dropSelf(ModBlocks.SOMBRIA_PLANKS.get());
        add(ModBlocks.SOMBRIA_DOOR.get(),
                block -> createDoorTable(ModBlocks.SOMBRIA_DOOR.get()));
        dropSelf(ModBlocks.SOMBRIA_TRAPDOOR.get());
        dropSelf(ModBlocks.SOMBRIA_BUTTON.get());
        dropSelf(ModBlocks.SOMBRIA_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.SOMBRIA_STAIRS.get());
        dropSelf(ModBlocks.SOMBRIA_SLAB.get());
        dropSelf(ModBlocks.SOMBRIA_FENCE.get());
        dropSelf(ModBlocks.SOMBRIA_FENCE_GATE.get());
        dropSelf(ModBlocks.STRIPPED_SOMBRIA_LOG.get());
        dropSelf(ModBlocks.SOMBRIA_SAPLING.get());
        dropSelf(ModBlocks.TEMPESTADE_LOG.get());
        dropSelf(ModBlocks.TEMPESTADE_PLANKS.get());
        add(ModBlocks.TEMPESTADE_DOOR.get(),
                block -> createDoorTable(ModBlocks.TEMPESTADE_DOOR.get()));
        dropSelf(ModBlocks.TEMPESTADE_TRAPDOOR.get());
        dropSelf(ModBlocks.TEMPESTADE_BUTTON.get());
        dropSelf(ModBlocks.TEMPESTADE_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.TEMPESTADE_STAIRS.get());
        dropSelf(ModBlocks.TEMPESTADE_SLAB.get());
        dropSelf(ModBlocks.TEMPESTADE_FENCE.get());
        dropSelf(ModBlocks.TEMPESTADE_FENCE_GATE.get());
        dropSelf(ModBlocks.STRIPPED_TEMPESTADE_LOG.get());
        dropSelf(ModBlocks.TEMPESTADE_SAPLING.get());
        dropSelf(ModBlocks.VIDA_LOG.get());
        dropSelf(ModBlocks.VIDA_PLANKS.get());
        add(ModBlocks.VIDA_DOOR.get(),
                block -> createDoorTable(ModBlocks.VIDA_DOOR.get()));
        dropSelf(ModBlocks.VIDA_TRAPDOOR.get());
        dropSelf(ModBlocks.VIDA_BUTTON.get());
        dropSelf(ModBlocks.VIDA_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.VIDA_STAIRS.get());
        dropSelf(ModBlocks.VIDA_SLAB.get());
        dropSelf(ModBlocks.VIDA_FENCE.get());
        dropSelf(ModBlocks.VIDA_FENCE_GATE.get());
        dropSelf(ModBlocks.STRIPPED_VIDA_LOG.get());
        dropSelf(ModBlocks.VIDA_SAPLING.get());
        add(ModBlocks.IGNEA_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.IGNEA_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES)
                        .withPool(LootPool.lootPool()
                                .setRolls(ContextIntProviders.exactly(1))
                                .add(LootItem.lootTableItem(ModItems.FRAGMENTO_IGNEA.get())
                                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                                )
                        ));
        add(ModBlocks.LUZ_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.LUZ_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES)
                        .withPool(LootPool.lootPool()
                                .setRolls(ContextIntProviders.exactly(1))
                                .add(LootItem.lootTableItem(ModItems.FRAGMENTO_LUZ.get())
                                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                                )
                        ));
        add(ModBlocks.METAL_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.METAL_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES)
                        .withPool(LootPool.lootPool()
                                .setRolls(ContextIntProviders.exactly(1))
                                .add(LootItem.lootTableItem(ModItems.FRAGMENTO_METAL.get())
                                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                                )
                        ));
        add(ModBlocks.SOMBRIA_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.SOMBRIA_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES)
                        .withPool(LootPool.lootPool()
                                .setRolls(ContextIntProviders.exactly(1))
                                .add(LootItem.lootTableItem(ModItems.FRAGMENTO_SOMBRIA.get())
                                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                                )
                        ));
        add(ModBlocks.TEMPESTADE_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.TEMPESTADE_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES)
                        .withPool(LootPool.lootPool()
                                .setRolls(ContextIntProviders.exactly(1))
                                .add(LootItem.lootTableItem(ModItems.FRAGMENTO_TEMPESTADE.get())
                                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                                )
                        ));
        add(ModBlocks.VIDA_LEAVES.get(), block ->
                createLeavesDrops(block, ModBlocks.VIDA_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES)
                        .withPool(LootPool.lootPool()
                                .setRolls(ContextIntProviders.exactly(1))
                                .add(LootItem.lootTableItem(ModItems.FRAGMENTO_VIDA.get())
                                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                                )
                        ));
        dropSelf(ModBlocks.BOLA_CRISTAL.get());
        dropSelf(ModBlocks.COLETOR_ARCANO_BASICO.get());
        dropSelf(ModBlocks.INFUSOR_ARCANO_BASICO.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
