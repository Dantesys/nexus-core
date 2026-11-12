package org.dantesys.nexus.datagen;

import net.minecraft.core.HolderLookup;
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

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ModTags.Items.ESMERALDAS)
                .add(ModItems.ESMERALDA_TEMPESTADE.get())
                .add(ModItems.ESMERALDA_SOMBRIA.get())
                .add(ModItems.ESMERALDA_VIDA.get())
                .add(ModItems.ESMERALDA_METAL.get())
                .add(ModItems.ESMERALDA_IGNEA.get())
                .add(ModItems.ESMERALDA_LUZ.get());
        tag(ModTags.Items.NUCLEO).add(ModItems.NUCLEO.get());
        tag(ModTags.Items.MATRIZ_RELIQUIA)
                .add(ModItems.MATRIZ_PRIMORDIAL.get(), ModItems.MATRIZ_SINTETICA.get());
        tag(ModTags.Items.MATRIZ_PRIMORDIAL)
                .add(ModItems.MATRIZ_PRIMORDIAL.get());
        tag(ModTags.Items.MATRIZ_SINTETICA)
                .add(ModItems.MATRIZ_SINTETICA.get());
        tag(ModTags.Items.CRISTAIS_RANK_F)
                .add(ModItems.CRISTAL_MANA_F.get());
        tag(ModTags.Items.CRISTAIS_RANK_E)
                .add(ModItems.CRISTAL_MANA_E.get());
        tag(ModTags.Items.CRISTAIS_RANK_D)
                .add(ModItems.CRISTAL_MANA_D.get());
        tag(ModTags.Items.CRISTAIS_RANK_C)
                .add(ModItems.CRISTAL_MANA_C.get());
        tag(ModTags.Items.CRISTAIS_RANK_B)
                .add(ModItems.CRISTAL_MANA_B.get());
        tag(ModTags.Items.CRISTAIS_RANK_A)
                .add(ModItems.CRISTAL_MANA_A.get());
        tag(ModTags.Items.CRISTAIS_RANK_S)
                .add(ModItems.CRISTAL_MANA_S.get());
        tag(ModTags.Items.STEEL_INGOTS).add(ModItems.STEEL.get());
        tag(ModTags.Items.STEEL_STORAGE_BLOCKS).add(ModBlocks.STEEL_BLOCK.get().asItem());

        tag(ItemTags.SWORDS).add(ModItems.STEEL_SWORD.get());
        tag(ItemTags.PICKAXES).add(ModItems.STEEL_PICKAXE.get());
        tag(ItemTags.AXES).add(ModItems.STEEL_AXE.get());
        tag(ItemTags.SHOVELS).add(ModItems.STEEL_SHOVEL.get());
        tag(ItemTags.HOES).add(ModItems.STEEL_HOE.get());
        tag(ItemTags.SPEARS).add(ModItems.STEEL_SPEAR.get());
        tag(ItemTags.HEAD_ARMOR).add(ModItems.STEEL_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.STEEL_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.STEEL_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.STEEL_BOOTS.get());

        tag(ItemTags.MELEE_WEAPON_ENCHANTABLE)
                .add(ModItems.STEEL_SWORD.get())
                .add(ModItems.STEEL_AXE.get())
                .add(ModItems.STEEL_SPEAR.get());
        tag(ItemTags.WEAPON_ENCHANTABLE)
                .add(ModItems.STEEL_SWORD.get())
                .add(ModItems.STEEL_SPEAR.get());
        tag(ItemTags.LUNGE_ENCHANTABLE).add(ModItems.STEEL_SPEAR.get());
        tag(ItemTags.SWEEPING_ENCHANTABLE).add(ModItems.STEEL_SWORD.get());
        tag(ItemTags.FIRE_ASPECT_ENCHANTABLE).add(ModItems.STEEL_SWORD.get());
        tag(ItemTags.SHARP_WEAPON_ENCHANTABLE)
                .add(ModItems.STEEL_SWORD.get())
                .add(ModItems.STEEL_AXE.get());
        tag(ItemTags.MINING_ENCHANTABLE)
                .add(ModItems.STEEL_PICKAXE.get())
                .add(ModItems.STEEL_AXE.get())
                .add(ModItems.STEEL_SHOVEL.get())
                .add(ModItems.STEEL_HOE.get());
        tag(ItemTags.MINING_LOOT_ENCHANTABLE)
                .add(ModItems.STEEL_PICKAXE.get())
                .add(ModItems.STEEL_AXE.get())
                .add(ModItems.STEEL_SHOVEL.get())
                .add(ModItems.STEEL_HOE.get());
        tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(ModItems.STEEL_HELMET.get());
        tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(ModItems.STEEL_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR_ENCHANTABLE).add(ModItems.STEEL_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR_ENCHANTABLE).add(ModItems.STEEL_BOOTS.get());
        tag(ItemTags.ARMOR_ENCHANTABLE)
                .add(ModItems.STEEL_HELMET.get())
                .add(ModItems.STEEL_CHESTPLATE.get())
                .add(ModItems.STEEL_LEGGINGS.get())
                .add(ModItems.STEEL_BOOTS.get());
        tag(ItemTags.EQUIPPABLE_ENCHANTABLE)
                .add(ModItems.STEEL_HELMET.get())
                .add(ModItems.STEEL_CHESTPLATE.get())
                .add(ModItems.STEEL_LEGGINGS.get())
                .add(ModItems.STEEL_BOOTS.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE)
                .add(ModItems.STEEL_SWORD.get())
                .add(ModItems.STEEL_PICKAXE.get())
                .add(ModItems.STEEL_AXE.get())
                .add(ModItems.STEEL_SHOVEL.get())
                .add(ModItems.STEEL_HOE.get())
                .add(ModItems.STEEL_SPEAR.get())
                .add(ModItems.STEEL_HELMET.get())
                .add(ModItems.STEEL_CHESTPLATE.get())
                .add(ModItems.STEEL_LEGGINGS.get())
                .add(ModItems.STEEL_BOOTS.get());
        tag(ItemTags.VANISHING_ENCHANTABLE)
                .add(ModItems.STEEL_SWORD.get())
                .add(ModItems.STEEL_PICKAXE.get())
                .add(ModItems.STEEL_AXE.get())
                .add(ModItems.STEEL_SHOVEL.get())
                .add(ModItems.STEEL_HOE.get())
                .add(ModItems.STEEL_SPEAR.get())
                .add(ModItems.STEEL_HELMET.get())
                .add(ModItems.STEEL_CHESTPLATE.get())
                .add(ModItems.STEEL_LEGGINGS.get())
                .add(ModItems.STEEL_BOOTS.get());
        tag(ItemTags.PLANKS)
                .add(ModBlocks.IGNEA_PLANKS.asItem())
                .add(ModBlocks.LUZ_PLANKS.asItem())
                .add(ModBlocks.METAL_PLANKS.asItem())
                .add(ModBlocks.SOMBRIA_PLANKS.asItem())
                .add(ModBlocks.TEMPESTADE_PLANKS.asItem())
                .add(ModBlocks.VIDA_PLANKS.asItem());
        tag(ItemTags.LOGS)
                .add(ModBlocks.IGNEA_LOG.asItem(), ModBlocks.STRIPPED_IGNEA_LOG.asItem())
                .add(ModBlocks.LUZ_LOG.asItem(), ModBlocks.STRIPPED_LUZ_LOG.asItem())
                .add(ModBlocks.METAL_LOG.asItem(), ModBlocks.STRIPPED_METAL_LOG.asItem())
                .add(ModBlocks.SOMBRIA_LOG.asItem(), ModBlocks.STRIPPED_SOMBRIA_LOG.asItem())
                .add(ModBlocks.TEMPESTADE_LOG.asItem(), ModBlocks.STRIPPED_TEMPESTADE_LOG.asItem())
                .add(ModBlocks.VIDA_LOG.asItem(), ModBlocks.STRIPPED_VIDA_LOG.asItem());
        tag(ItemTags.WOODEN_STAIRS)
                .add(ModBlocks.IGNEA_STAIRS.asItem(), ModBlocks.LUZ_STAIRS.asItem(), ModBlocks.METAL_STAIRS.asItem())
                .add(ModBlocks.SOMBRIA_STAIRS.asItem(), ModBlocks.TEMPESTADE_STAIRS.asItem(), ModBlocks.VIDA_STAIRS.asItem());
        tag(ItemTags.WOODEN_SLABS)
                .add(ModBlocks.IGNEA_SLAB.asItem(), ModBlocks.LUZ_SLAB.asItem(), ModBlocks.METAL_SLAB.asItem())
                .add(ModBlocks.SOMBRIA_SLAB.asItem(), ModBlocks.TEMPESTADE_SLAB.asItem(), ModBlocks.VIDA_SLAB.asItem());
        tag(ItemTags.WOODEN_BUTTONS)
                .add(ModBlocks.IGNEA_BUTTON.asItem(), ModBlocks.LUZ_BUTTON.asItem(), ModBlocks.METAL_BUTTON.asItem())
                .add(ModBlocks.SOMBRIA_BUTTON.asItem(), ModBlocks.TEMPESTADE_BUTTON.asItem(), ModBlocks.VIDA_BUTTON.asItem());
        tag(ItemTags.WOODEN_PRESSURE_PLATES)
                .add(ModBlocks.IGNEA_PRESSURE_PLATE.asItem(), ModBlocks.LUZ_PRESSURE_PLATE.asItem(), ModBlocks.METAL_PRESSURE_PLATE.asItem())
                .add(ModBlocks.SOMBRIA_PRESSURE_PLATE.asItem(), ModBlocks.TEMPESTADE_PRESSURE_PLATE.asItem(), ModBlocks.VIDA_PRESSURE_PLATE.asItem());
        tag(ItemTags.WOODEN_DOORS)
                .add(ModBlocks.IGNEA_DOOR.asItem(), ModBlocks.LUZ_DOOR.asItem(), ModBlocks.METAL_DOOR.asItem())
                .add(ModBlocks.SOMBRIA_DOOR.asItem(), ModBlocks.TEMPESTADE_DOOR.asItem(), ModBlocks.VIDA_DOOR.asItem());
        tag(ItemTags.WOODEN_TRAPDOORS)
                .add(ModBlocks.IGNEA_TRAPDOOR.asItem(), ModBlocks.LUZ_TRAPDOOR.asItem(), ModBlocks.METAL_TRAPDOOR.asItem())
                .add(ModBlocks.SOMBRIA_TRAPDOOR.asItem(), ModBlocks.TEMPESTADE_TRAPDOOR.asItem(), ModBlocks.VIDA_TRAPDOOR.asItem());
        tag(ItemTags.WOODEN_FENCES)
                .add(ModBlocks.IGNEA_FENCE.asItem(), ModBlocks.LUZ_FENCE.asItem(), ModBlocks.METAL_FENCE.asItem())
                .add(ModBlocks.SOMBRIA_FENCE.asItem(), ModBlocks.TEMPESTADE_FENCE.asItem(), ModBlocks.VIDA_FENCE.asItem());
        tag(ItemTags.FENCE_GATES)
                .add(ModBlocks.IGNEA_FENCE_GATE.asItem(), ModBlocks.LUZ_FENCE_GATE.asItem(), ModBlocks.METAL_FENCE_GATE.asItem())
                .add(ModBlocks.SOMBRIA_FENCE_GATE.asItem(), ModBlocks.TEMPESTADE_FENCE_GATE.asItem(), ModBlocks.VIDA_FENCE_GATE.asItem());
        tag(ItemTags.LEAVES)
                .add(ModBlocks.IGNEA_LEAVES.asItem(), ModBlocks.LUZ_LEAVES.asItem(), ModBlocks.METAL_LEAVES.asItem())
                .add(ModBlocks.SOMBRIA_LEAVES.asItem(), ModBlocks.TEMPESTADE_LEAVES.asItem(), ModBlocks.VIDA_LEAVES.asItem());
        tag(ItemTags.SAPLINGS)
                .add(ModBlocks.IGNEA_SAPLING.asItem(), ModBlocks.LUZ_SAPLING.asItem(), ModBlocks.METAL_SAPLING.asItem())
                .add(ModBlocks.SOMBRIA_SAPLING.asItem(), ModBlocks.TEMPESTADE_SAPLING.asItem(), ModBlocks.VIDA_SAPLING.asItem());
    }
}
