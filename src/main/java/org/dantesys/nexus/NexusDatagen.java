package org.dantesys.nexus;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import org.dantesys.nexus.datagen.ModBlockTagsProvider;
import org.dantesys.nexus.datagen.ModBlocksLootTableProvider;
import org.dantesys.nexus.datagen.ModItemTagsProvider;
import org.dantesys.nexus.datagen.ModModelProvider;
import org.dantesys.nexus.datagen.ModRecipeProvider;
import org.dantesys.nexus.enchantment.ModEnchantments;

@EventBusSubscriber(modid = Nexus.MODID)
public class NexusDatagen {
    /**
     * Desde a 26.3, encantamentos, receitas e loot são "entradas de registro":
     * o NeoForge junta tudo aqui e grava os JSON no runData.
     */
    @SubscribeEvent
    public static void gatherRegistries(GatherDataRegistryEntriesEvent event) {
        event.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap)
                .recipe(ModRecipeProvider::new)
                .lootTable(new LootTableProvider.SubProviderEntry(ModBlocksLootTableProvider::new, LootContextParamSets.BLOCK));
    }

    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        event.createProvider(ModModelProvider::new);
        event.createProvider((output, lookup) -> new ModBlockTagsProvider(output, lookup));
        event.createProvider((output, lookup) -> new ModItemTagsProvider(output, lookup));
    }
}
