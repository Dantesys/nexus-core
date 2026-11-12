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

        logToPlank(ModBlocks.IGNEA_LOG.get(), ModBlocks.IGNEA_PLANKS.get());
        logToPlank(ModBlocks.STRIPPED_IGNEA_LOG.get(), ModBlocks.IGNEA_PLANKS.get());
        woodSetRecipes(
                ModBlocks.IGNEA_PLANKS.get(),
                ModBlocks.IGNEA_STAIRS.get(),
                ModBlocks.IGNEA_SLAB.get(),
                ModBlocks.IGNEA_BUTTON.get(),
                ModBlocks.IGNEA_PRESSURE_PLATE.get(),
                ModBlocks.IGNEA_FENCE.get(),
                ModBlocks.IGNEA_FENCE_GATE.get(),
                ModBlocks.IGNEA_DOOR.get(),
                ModBlocks.IGNEA_TRAPDOOR.get(),
                "ignea"
        );
        logToPlank(ModBlocks.LUZ_LOG.get(), ModBlocks.LUZ_PLANKS.get());
        logToPlank(ModBlocks.STRIPPED_LUZ_LOG.get(), ModBlocks.LUZ_PLANKS.get());
        woodSetRecipes(
                ModBlocks.LUZ_PLANKS.get(),
                ModBlocks.LUZ_STAIRS.get(),
                ModBlocks.LUZ_SLAB.get(),
                ModBlocks.LUZ_BUTTON.get(),
                ModBlocks.LUZ_PRESSURE_PLATE.get(),
                ModBlocks.LUZ_FENCE.get(),
                ModBlocks.LUZ_FENCE_GATE.get(),
                ModBlocks.LUZ_DOOR.get(),
                ModBlocks.LUZ_TRAPDOOR.get(),
                "luz"
        );
        logToPlank(ModBlocks.METAL_LOG.get(), ModBlocks.METAL_PLANKS.get());
        logToPlank(ModBlocks.STRIPPED_METAL_LOG.get(), ModBlocks.METAL_PLANKS.get());
        woodSetRecipes(
                ModBlocks.METAL_PLANKS.get(),
                ModBlocks.METAL_STAIRS.get(),
                ModBlocks.METAL_SLAB.get(),
                ModBlocks.METAL_BUTTON.get(),
                ModBlocks.METAL_PRESSURE_PLATE.get(),
                ModBlocks.METAL_FENCE.get(),
                ModBlocks.METAL_FENCE_GATE.get(),
                ModBlocks.METAL_DOOR.get(),
                ModBlocks.METAL_TRAPDOOR.get(),
                "metal"
        );
        logToPlank(ModBlocks.SOMBRIA_LOG.get(), ModBlocks.SOMBRIA_PLANKS.get());
        logToPlank(ModBlocks.STRIPPED_SOMBRIA_LOG.get(), ModBlocks.SOMBRIA_PLANKS.get());
        woodSetRecipes(
                ModBlocks.SOMBRIA_PLANKS.get(),
                ModBlocks.SOMBRIA_STAIRS.get(),
                ModBlocks.SOMBRIA_SLAB.get(),
                ModBlocks.SOMBRIA_BUTTON.get(),
                ModBlocks.SOMBRIA_PRESSURE_PLATE.get(),
                ModBlocks.SOMBRIA_FENCE.get(),
                ModBlocks.SOMBRIA_FENCE_GATE.get(),
                ModBlocks.SOMBRIA_DOOR.get(),
                ModBlocks.SOMBRIA_TRAPDOOR.get(),
                "sombria"
        );
        logToPlank(ModBlocks.TEMPESTADE_LOG.get(), ModBlocks.TEMPESTADE_PLANKS.get());
        logToPlank(ModBlocks.STRIPPED_TEMPESTADE_LOG.get(), ModBlocks.TEMPESTADE_PLANKS.get());
        woodSetRecipes(
                ModBlocks.TEMPESTADE_PLANKS.get(),
                ModBlocks.TEMPESTADE_STAIRS.get(),
                ModBlocks.TEMPESTADE_SLAB.get(),
                ModBlocks.TEMPESTADE_BUTTON.get(),
                ModBlocks.TEMPESTADE_PRESSURE_PLATE.get(),
                ModBlocks.TEMPESTADE_FENCE.get(),
                ModBlocks.TEMPESTADE_FENCE_GATE.get(),
                ModBlocks.TEMPESTADE_DOOR.get(),
                ModBlocks.TEMPESTADE_TRAPDOOR.get(),
                "tempestade"
        );
        logToPlank(ModBlocks.VIDA_LOG.get(), ModBlocks.VIDA_PLANKS.get());
        logToPlank(ModBlocks.STRIPPED_VIDA_LOG.get(), ModBlocks.VIDA_PLANKS.get());
        woodSetRecipes(
                ModBlocks.VIDA_PLANKS.get(),
                ModBlocks.VIDA_STAIRS.get(),
                ModBlocks.VIDA_SLAB.get(),
                ModBlocks.VIDA_BUTTON.get(),
                ModBlocks.VIDA_PRESSURE_PLATE.get(),
                ModBlocks.VIDA_FENCE.get(),
                ModBlocks.VIDA_FENCE_GATE.get(),
                ModBlocks.VIDA_DOOR.get(),
                ModBlocks.VIDA_TRAPDOOR.get(),
                "vida"
        );
        elementalEmeraldInfusorRecipes();
        Ingredient elementalEmerald = Ingredient.of(
                items.getOrThrow(ModTags.Items.ESMERALDAS));
        InfusorRecipeBuilder.infusing(
                elementalEmerald,
                elementalEmerald,
                elementalEmerald,
                elementalEmerald,
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

    private void elementalEmeraldInfusorRecipes() {
        elementalEmeraldInfusorRecipe(
                ModItems.FRAGMENTO_IGNEA.get(),
                ModItems.ESMERALDA_IGNEA.get(),
                "esmeralda_ignea"
        );
        elementalEmeraldInfusorRecipe(
                ModItems.FRAGMENTO_VIDA.get(),
                ModItems.ESMERALDA_VIDA.get(),
                "esmeralda_vida"
        );
        elementalEmeraldInfusorRecipe(
                ModItems.FRAGMENTO_TEMPESTADE.get(),
                ModItems.ESMERALDA_TEMPESTADE.get(),
                "esmeralda_tempestade"
        );
        elementalEmeraldInfusorRecipe(
                ModItems.FRAGMENTO_METAL.get(),
                ModItems.ESMERALDA_METAL.get(),
                "esmeralda_metal"
        );
        elementalEmeraldInfusorRecipe(
                ModItems.FRAGMENTO_LUZ.get(),
                ModItems.ESMERALDA_LUZ.get(),
                "esmeralda_luz"
        );
        elementalEmeraldInfusorRecipe(
                ModItems.FRAGMENTO_SOMBRIA.get(),
                ModItems.ESMERALDA_SOMBRIA.get(),
                "esmeralda_sombria"
        );
    }

    private void elementalEmeraldInfusorRecipe(
            ItemLike fragment,
            ItemLike result,
            String recipeName
    ) {
        InfusorRecipeBuilder.infusing(
                Items.EMERALD,
                fragment,
                Items.EMERALD,
                fragment,
                100,
                result,
                2
        ).save(
                output,
                ResourceKey.create(
                        Registries.RECIPE,
                        Identifier.fromNamespaceAndPath(
                                Nexus.MODID, "infusor/" + recipeName)
                )
        );
    }

    private void logToPlank(ItemLike log, ItemLike plank) {
        shapeless(RecipeCategory.BUILDING_BLOCKS, plank, 4)
                .requires(log)
                .unlockedBy(getHasName(log), has(log))
                .save(output, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Nexus.MODID, getItemName(plank) + "_from_" + getItemName(log))));
    }

    private void woodSetRecipes(
            ItemLike planks,
            ItemLike stairs,
            ItemLike slab,
            ItemLike button,
            ItemLike pressurePlate,
            ItemLike fence,
            ItemLike fenceGate,
            ItemLike door,
            ItemLike trapdoor,
            String group
    ) {
        stairBuilder(stairs, Ingredient.of(planks))
                .unlockedBy(getHasName(planks), has(planks))
                .group(group)
                .save(output);

        slab(RecipeCategory.BUILDING_BLOCKS, slab, planks);

        buttonBuilder(button, Ingredient.of(planks))
                .unlockedBy(getHasName(planks), has(planks))
                .group(group)
                .save(output);

        pressurePlate(pressurePlate, planks);

        fenceBuilder(fence, Ingredient.of(planks))
                .unlockedBy(getHasName(planks), has(planks))
                .group(group)
                .save(output);

        fenceGateBuilder(fenceGate, Ingredient.of(planks))
                .unlockedBy(getHasName(planks), has(planks))
                .group(group)
                .save(output);

        doorBuilder(door, Ingredient.of(planks))
                .unlockedBy(getHasName(planks), has(planks))
                .group(group)
                .save(output);

        trapdoorBuilder(trapdoor, Ingredient.of(planks))
                .unlockedBy(getHasName(planks), has(planks))
                .group(group)
                .save(output);
    }
}
