package org.dantesys.nexus.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.tags.ModTags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Nexus.MODID);
    }

    // Na 26.3 as tags recebem ResourceKey em vez do item.
    private static ResourceKey<Item> k(ItemLike item) {
        return BuiltInRegistries.ITEM.getResourceKey(item.asItem()).orElseThrow();
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ModTags.Items.ESMERALDAS)
                .add(k(ModItems.ESMERALDA_TEMPESTADE))
                .add(k(ModItems.ESMERALDA_SOMBRIA))
                .add(k(ModItems.ESMERALDA_VIDA))
                .add(k(ModItems.ESMERALDA_METAL))
                .add(k(ModItems.ESMERALDA_IGNEA))
                .add(k(ModItems.ESMERALDA_LUZ));
        tag(ModTags.Items.NUCLEO).add(k(ModItems.NUCLEO));
        tag(ModTags.Items.MATRIZ_RELIQUIA)
                .add(k(ModItems.MATRIZ_PRIMORDIAL), k(ModItems.MATRIZ_SINTETICA));
        tag(ModTags.Items.MATRIZ_PRIMORDIAL)
                .add(k(ModItems.MATRIZ_PRIMORDIAL));
        tag(ModTags.Items.MATRIZ_SINTETICA)
                .add(k(ModItems.MATRIZ_SINTETICA));
        tag(ModTags.Items.CRISTAIS_RANK_F)
                .add(k(ModItems.CRISTAL_MANA_F));
        tag(ModTags.Items.CRISTAIS_RANK_E)
                .add(k(ModItems.CRISTAL_MANA_E));
        tag(ModTags.Items.CRISTAIS_RANK_D)
                .add(k(ModItems.CRISTAL_MANA_D));
        tag(ModTags.Items.CRISTAIS_RANK_C)
                .add(k(ModItems.CRISTAL_MANA_C));
        tag(ModTags.Items.CRISTAIS_RANK_B)
                .add(k(ModItems.CRISTAL_MANA_B));
        tag(ModTags.Items.CRISTAIS_RANK_A)
                .add(k(ModItems.CRISTAL_MANA_A));
        tag(ModTags.Items.CRISTAIS_RANK_S)
                .add(k(ModItems.CRISTAL_MANA_S));
        tag(ModTags.Items.STEEL_INGOTS).add(k(ModItems.STEEL));
        tag(ModTags.Items.STEEL_STORAGE_BLOCKS).add(k(ModBlocks.STEEL_BLOCK));

        tag(ItemTags.SWORDS).add(k(ModItems.STEEL_SWORD));
        tag(ItemTags.PICKAXES).add(k(ModItems.STEEL_PICKAXE));
        tag(ItemTags.AXES).add(k(ModItems.STEEL_AXE));
        tag(ItemTags.SHOVELS).add(k(ModItems.STEEL_SHOVEL));
        tag(ItemTags.HOES).add(k(ModItems.STEEL_HOE));
        tag(ItemTags.SPEARS).add(k(ModItems.STEEL_SPEAR));
        tag(ItemTags.HEAD_ARMOR).add(k(ModItems.STEEL_HELMET));
        tag(ItemTags.CHEST_ARMOR).add(k(ModItems.STEEL_CHESTPLATE));
        tag(ItemTags.LEG_ARMOR).add(k(ModItems.STEEL_LEGGINGS));
        tag(ItemTags.FOOT_ARMOR).add(k(ModItems.STEEL_BOOTS));

        tag(ItemTags.MELEE_WEAPON_ENCHANTABLE)
                .add(k(ModItems.STEEL_SWORD))
                .add(k(ModItems.STEEL_AXE))
                .add(k(ModItems.STEEL_SPEAR));
        tag(ItemTags.WEAPON_ENCHANTABLE)
                .add(k(ModItems.STEEL_SWORD))
                .add(k(ModItems.STEEL_SPEAR));
        tag(ItemTags.LUNGE_ENCHANTABLE).add(k(ModItems.STEEL_SPEAR));
        tag(ItemTags.SWEEPING_ENCHANTABLE).add(k(ModItems.STEEL_SWORD));
        tag(ItemTags.FIRE_ASPECT_ENCHANTABLE).add(k(ModItems.STEEL_SWORD));
        tag(ItemTags.SHARP_WEAPON_ENCHANTABLE)
                .add(k(ModItems.STEEL_SWORD))
                .add(k(ModItems.STEEL_AXE));
        tag(ItemTags.MINING_ENCHANTABLE)
                .add(k(ModItems.STEEL_PICKAXE))
                .add(k(ModItems.STEEL_AXE))
                .add(k(ModItems.STEEL_SHOVEL))
                .add(k(ModItems.STEEL_HOE));
        tag(ItemTags.MINING_LOOT_ENCHANTABLE)
                .add(k(ModItems.STEEL_PICKAXE))
                .add(k(ModItems.STEEL_AXE))
                .add(k(ModItems.STEEL_SHOVEL))
                .add(k(ModItems.STEEL_HOE));
        tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(k(ModItems.STEEL_HELMET));
        tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(k(ModItems.STEEL_CHESTPLATE));
        tag(ItemTags.LEG_ARMOR_ENCHANTABLE).add(k(ModItems.STEEL_LEGGINGS));
        tag(ItemTags.FOOT_ARMOR_ENCHANTABLE).add(k(ModItems.STEEL_BOOTS));
        tag(ItemTags.ARMOR_ENCHANTABLE)
                .add(k(ModItems.STEEL_HELMET))
                .add(k(ModItems.STEEL_CHESTPLATE))
                .add(k(ModItems.STEEL_LEGGINGS))
                .add(k(ModItems.STEEL_BOOTS));
        tag(ItemTags.EQUIPPABLE_ENCHANTABLE)
                .add(k(ModItems.STEEL_HELMET))
                .add(k(ModItems.STEEL_CHESTPLATE))
                .add(k(ModItems.STEEL_LEGGINGS))
                .add(k(ModItems.STEEL_BOOTS));
        tag(ItemTags.DURABILITY_ENCHANTABLE)
                .add(k(ModItems.STEEL_SWORD))
                .add(k(ModItems.STEEL_PICKAXE))
                .add(k(ModItems.STEEL_AXE))
                .add(k(ModItems.STEEL_SHOVEL))
                .add(k(ModItems.STEEL_HOE))
                .add(k(ModItems.STEEL_SPEAR))
                .add(k(ModItems.STEEL_HELMET))
                .add(k(ModItems.STEEL_CHESTPLATE))
                .add(k(ModItems.STEEL_LEGGINGS))
                .add(k(ModItems.STEEL_BOOTS));
        tag(ItemTags.VANISHING_ENCHANTABLE)
                .add(k(ModItems.STEEL_SWORD))
                .add(k(ModItems.STEEL_PICKAXE))
                .add(k(ModItems.STEEL_AXE))
                .add(k(ModItems.STEEL_SHOVEL))
                .add(k(ModItems.STEEL_HOE))
                .add(k(ModItems.STEEL_SPEAR))
                .add(k(ModItems.STEEL_HELMET))
                .add(k(ModItems.STEEL_CHESTPLATE))
                .add(k(ModItems.STEEL_LEGGINGS))
                .add(k(ModItems.STEEL_BOOTS));
        tag(ItemTags.PLANKS)
                .add(k(ModBlocks.IGNEA_PLANKS))
                .add(k(ModBlocks.LUZ_PLANKS))
                .add(k(ModBlocks.METAL_PLANKS))
                .add(k(ModBlocks.SOMBRIA_PLANKS))
                .add(k(ModBlocks.TEMPESTADE_PLANKS))
                .add(k(ModBlocks.VIDA_PLANKS));
        tag(ItemTags.LOGS)
                .add(k(ModBlocks.IGNEA_LOG), k(ModBlocks.STRIPPED_IGNEA_LOG))
                .add(k(ModBlocks.LUZ_LOG), k(ModBlocks.STRIPPED_LUZ_LOG))
                .add(k(ModBlocks.METAL_LOG), k(ModBlocks.STRIPPED_METAL_LOG))
                .add(k(ModBlocks.SOMBRIA_LOG), k(ModBlocks.STRIPPED_SOMBRIA_LOG))
                .add(k(ModBlocks.TEMPESTADE_LOG), k(ModBlocks.STRIPPED_TEMPESTADE_LOG))
                .add(k(ModBlocks.VIDA_LOG), k(ModBlocks.STRIPPED_VIDA_LOG));
        tag(ItemTags.WOODEN_STAIRS)
                .add(k(ModBlocks.IGNEA_STAIRS), k(ModBlocks.LUZ_STAIRS), k(ModBlocks.METAL_STAIRS))
                .add(k(ModBlocks.SOMBRIA_STAIRS), k(ModBlocks.TEMPESTADE_STAIRS), k(ModBlocks.VIDA_STAIRS));
        tag(ItemTags.WOODEN_SLABS)
                .add(k(ModBlocks.IGNEA_SLAB), k(ModBlocks.LUZ_SLAB), k(ModBlocks.METAL_SLAB))
                .add(k(ModBlocks.SOMBRIA_SLAB), k(ModBlocks.TEMPESTADE_SLAB), k(ModBlocks.VIDA_SLAB));
        tag(ItemTags.WOODEN_BUTTONS)
                .add(k(ModBlocks.IGNEA_BUTTON), k(ModBlocks.LUZ_BUTTON), k(ModBlocks.METAL_BUTTON))
                .add(k(ModBlocks.SOMBRIA_BUTTON), k(ModBlocks.TEMPESTADE_BUTTON), k(ModBlocks.VIDA_BUTTON));
        tag(ItemTags.WOODEN_PRESSURE_PLATES)
                .add(k(ModBlocks.IGNEA_PRESSURE_PLATE), k(ModBlocks.LUZ_PRESSURE_PLATE), k(ModBlocks.METAL_PRESSURE_PLATE))
                .add(k(ModBlocks.SOMBRIA_PRESSURE_PLATE), k(ModBlocks.TEMPESTADE_PRESSURE_PLATE), k(ModBlocks.VIDA_PRESSURE_PLATE));
        tag(ItemTags.WOODEN_DOORS)
                .add(k(ModBlocks.IGNEA_DOOR), k(ModBlocks.LUZ_DOOR), k(ModBlocks.METAL_DOOR))
                .add(k(ModBlocks.SOMBRIA_DOOR), k(ModBlocks.TEMPESTADE_DOOR), k(ModBlocks.VIDA_DOOR));
        tag(ItemTags.WOODEN_TRAPDOORS)
                .add(k(ModBlocks.IGNEA_TRAPDOOR), k(ModBlocks.LUZ_TRAPDOOR), k(ModBlocks.METAL_TRAPDOOR))
                .add(k(ModBlocks.SOMBRIA_TRAPDOOR), k(ModBlocks.TEMPESTADE_TRAPDOOR), k(ModBlocks.VIDA_TRAPDOOR));
        tag(ItemTags.WOODEN_FENCES)
                .add(k(ModBlocks.IGNEA_FENCE), k(ModBlocks.LUZ_FENCE), k(ModBlocks.METAL_FENCE))
                .add(k(ModBlocks.SOMBRIA_FENCE), k(ModBlocks.TEMPESTADE_FENCE), k(ModBlocks.VIDA_FENCE));
        tag(ItemTags.FENCE_GATES)
                .add(k(ModBlocks.IGNEA_FENCE_GATE), k(ModBlocks.LUZ_FENCE_GATE), k(ModBlocks.METAL_FENCE_GATE))
                .add(k(ModBlocks.SOMBRIA_FENCE_GATE), k(ModBlocks.TEMPESTADE_FENCE_GATE), k(ModBlocks.VIDA_FENCE_GATE));
        tag(ItemTags.LEAVES)
                .add(k(ModBlocks.IGNEA_LEAVES), k(ModBlocks.LUZ_LEAVES), k(ModBlocks.METAL_LEAVES))
                .add(k(ModBlocks.SOMBRIA_LEAVES), k(ModBlocks.TEMPESTADE_LEAVES), k(ModBlocks.VIDA_LEAVES));
        tag(ItemTags.SAPLINGS)
                .add(k(ModBlocks.IGNEA_SAPLING), k(ModBlocks.LUZ_SAPLING), k(ModBlocks.METAL_SAPLING))
                .add(k(ModBlocks.SOMBRIA_SAPLING), k(ModBlocks.TEMPESTADE_SAPLING), k(ModBlocks.VIDA_SAPLING));
    }
}
