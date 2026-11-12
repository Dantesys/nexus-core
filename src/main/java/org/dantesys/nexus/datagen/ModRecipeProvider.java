package org.dantesys.nexus.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.tags.ModTags;

import net.minecraft.world.item.crafting.Recipe;

/** Na 26.3 as receitas são geradas como entradas de registro (GatherDataRegistryEntriesEvent#recipe). */
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput, Nexus.MODID);
    }

    @Override
    protected void buildRecipes() {
        steelEquipmentRecipes();

            shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.STEEL_BLOCK.get())
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                    .define('A', ModItems.STEEL.get())
                    .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModItems.STEEL))
                    .group("steel")
                .save(output);

            shapeless(RecipeCategory.MISC, ModItems.STEEL.get(), 9)
                    .requires(ModBlocks.STEEL_BLOCK)
                    .unlockedBy(getHasName(ModBlocks.STEEL_BLOCK.get()), has(ModBlocks.STEEL_BLOCK))
                    .group("steel")
                .save(output);

        shaped(RecipeCategory.DECORATIONS, ModBlocks.BOLA_CRISTAL.get())
                .pattern("VVV")
                .pattern("VBV")
                .pattern("OAO")
                .define('V', Items.GLASS)
                .define('B', Items.AMETHYST_SHARD)
                .define('O', Items.OBSIDIAN)
                .define('A', Items.AMETHYST_BLOCK)
                .unlockedBy(getHasName(Items.AMETHYST_BLOCK), has(Items.AMETHYST_BLOCK))
                .save(output);

        shaped(RecipeCategory.DECORATIONS, ModBlocks.INFUSOR_ARCANO_BASICO.get())
                .pattern("BVB")
                .pattern("GIG")
                .pattern("IOI")
                .define('V', Items.GLASS)
                .define('B', Items.AMETHYST_SHARD)
                .define('G', Items.GOLD_NUGGET)
                .define('I', Items.IRON_INGOT)
                .define('O', Items.OBSIDIAN)
                .unlockedBy(getHasName(Items.AMETHYST_BLOCK), has(Items.AMETHYST_BLOCK))
                .save(output);

        shaped(RecipeCategory.MISC, ModBlocks.COLETOR_ARCANO_BASICO.get())
                .pattern("VVV")
                .pattern("ONO")
                .pattern("IFI")
                .define('V', Items.GLASS)
                .define('N', ModItems.NUCLEO)
                .define('O', Items.OBSIDIAN)
                    .define('I', ModItems.STEEL)
                    .define('F', ModBlocks.STEEL_BLOCK)
                .unlockedBy(getHasName(ModItems.NUCLEO), has(ModItems.NUCLEO))
                .save(output);

        // Núcleo: sem esmeraldas elementais no core.
        InfusorRecipeBuilder.infusing(
                Ingredient.of(Items.AMETHYST_SHARD),
                Ingredient.of(Items.DIAMOND),
                Ingredient.of(Items.AMETHYST_SHARD),
                Ingredient.of(ModItems.STEEL),
                200,
                new net.minecraft.world.item.ItemStackTemplate(
                        ModItems.NUCLEO.get(), 1)
        ).save(
                output,
                ResourceKey.create(
                        Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(
                                Nexus.MODID, "infusor/nucleo")
                )
        );
        InfusorRecipeBuilder.infusing(
                Items.IRON_INGOT,
                Items.COAL,
                Items.IRON_INGOT,
                Items.COAL,
                50,
                ModItems.STEEL,
                2
        ).save(
                output,
                ResourceKey.create(
                        Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(
                        Nexus.MODID, "infusor/steel")
                )
        );
        InfusorRecipeBuilder.infusing(
                Items.LAVA_BUCKET, Items.LAVA_BUCKET, ModItems.NUCLEO, ModItems.NUCLEO,
                200, ModItems.NUCLEO_FORJA, 1
        ).save(output, ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(Nexus.MODID, "infusor/nucleo_forja")));
        InfusorRecipeBuilder.infusing(
                Items.OBSIDIAN, ModBlocks.STEEL_BLOCK, Items.BUCKET, Items.BUCKET,
                100, ModBlocks.TANQUE_FORJA, 1
        ).save(output, ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(Nexus.MODID, "infusor/tanque_forja")));
        InfusorRecipeBuilder.infusing(
                Items.OBSIDIAN, Items.OBSIDIAN, ModBlocks.STEEL_BLOCK, ModBlocks.STEEL_BLOCK,
                300, ModBlocks.CONTROLADOR_FORJA, 1
        ).save(output, ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(Nexus.MODID, "infusor/controlador_forja")));
        InfusorRecipeBuilder.infusing(
                Ingredient.of(ModItems.NUCLEO),
                Ingredient.of(ModItems.NUCLEO),
                Ingredient.of(items.getOrThrow(ModTags.Items.CRISTAIS_RANK_F)),
                Ingredient.of(items.getOrThrow(ModTags.Items.CRISTAIS_RANK_F)),
                1000, ModItems.ARCANO_EVOLUIDOR_INCOMUM, 1
        ).save(output, ResourceKey.create(Registries.RECIPE,
                Identifier.fromNamespaceAndPath(Nexus.MODID, "infusor/arcano_evoluidor_incomum")));
    }

    private void steelEquipmentRecipes() {
        shaped(RecipeCategory.COMBAT, ModItems.STEEL_SWORD.get())
                .pattern("S")
                .pattern("S")
                .pattern("T")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .define('T', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.STEEL_PICKAXE.get())
                .pattern("SSS")
                .pattern(" T ")
                .pattern(" T ")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .define('T', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.STEEL_AXE.get())
                .pattern("SS")
                .pattern("ST")
                .pattern(" T")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .define('T', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.STEEL_SHOVEL.get())
                .pattern("S")
                .pattern("T")
                .pattern("T")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .define('T', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);
        shaped(RecipeCategory.TOOLS, ModItems.STEEL_HOE.get())
                .pattern("SS")
                .pattern(" T")
                .pattern(" T")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .define('T', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.STEEL_SPEAR.get())
                .pattern("  S")
                .pattern(" T ")
                .pattern("T  ")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .define('T', Items.STICK)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);

        shaped(RecipeCategory.COMBAT, ModItems.STEEL_HELMET.get())
                .pattern("SSS")
                .pattern("S S")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.STEEL_CHESTPLATE.get())
                .pattern("S S")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.STEEL_LEGGINGS.get())
                .pattern("SSS")
                .pattern("S S")
                .pattern("S S")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);
        shaped(RecipeCategory.COMBAT, ModItems.STEEL_BOOTS.get())
                .pattern("S S")
                .pattern("S S")
                .define('S', ModTags.Items.STEEL_INGOTS)
                .unlockedBy(getHasName(ModItems.STEEL.get()), has(ModTags.Items.STEEL_INGOTS))
                .save(output);

        ForjaRecipeBuilder.forging(
                Ingredient.of(Items.OBSIDIAN),
                Ingredient.of(ModItems.STEEL),
                Ingredient.of(ModItems.STEEL),
                Ingredient.of(ModItems.NUCLEO),
                500,   // mana
                1500,  // lava em mB
                200,   // 10 segundos
                new ItemStackTemplate(ModItems.MATRIZ_SINTETICA.get())
        ).save(output);

    }




}
