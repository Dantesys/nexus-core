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
    protected void addTags(HolderLookup.Provider registries) {;
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
    }
}
