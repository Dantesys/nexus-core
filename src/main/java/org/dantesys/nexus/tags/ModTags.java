package org.dantesys.nexus.tags;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import org.dantesys.nexus.Nexus;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> STEEL_STORAGE_BLOCKS = createCommonTag("storage_blocks/steel");

        private static TagKey<Block> createCommonTag(String name) {
            return BlockTags.create(Identifier.fromNamespaceAndPath("c", name));
        }
    }

    public static class Items {
        public static final TagKey<Item> ESMERALDAS = createTag("esmeraldas");
        public static final TagKey<Item> NUCLEO = createTag("nucleo");
        public static final TagKey<Item> MATRIZ_RELIQUIA = createTag("matriz_reliquia");
        public static final TagKey<Item> MATRIZ_PRIMORDIAL = createTag("matriz_primordial");
        public static final TagKey<Item> MATRIZ_SINTETICA = createTag("matriz_sintetica");
        public static final TagKey<Item> CATALISADOR = createTag("catalisador");
        public static final TagKey<Item> CRISTAIS_RANK_F = createTag("cristais_rank_f");
        public static final TagKey<Item> CRISTAIS_RANK_E = createTag("cristais_rank_e");
        public static final TagKey<Item> CRISTAIS_RANK_D = createTag("cristais_rank_d");
        public static final TagKey<Item> CRISTAIS_RANK_C = createTag("cristais_rank_c");
        public static final TagKey<Item> CRISTAIS_RANK_B = createTag("cristais_rank_b");
        public static final TagKey<Item> CRISTAIS_RANK_A = createTag("cristais_rank_a");
        public static final TagKey<Item> CRISTAIS_RANK_S = createTag("cristais_rank_s");
        public static final TagKey<Item> STEEL_INGOTS = createCommonTag("ingots/steel");
        public static final TagKey<Item> STEEL_STORAGE_BLOCKS = createCommonTag("storage_blocks/steel");
        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(Identifier.fromNamespaceAndPath(Nexus.MODID, name));
        }

        private static TagKey<Item> createCommonTag(String name) {
            return ItemTags.create(Identifier.fromNamespaceAndPath("c", name));
        }
    }

    public static class Enchantments {
        public static final TagKey<Enchantment> NUCLEO_EXCLUSIVE = TagKey.create(
                net.minecraft.core.registries.Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath(Nexus.MODID, "exclusive_set/nucleo")
        );
    }
}
