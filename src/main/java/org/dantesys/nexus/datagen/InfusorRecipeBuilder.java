package org.dantesys.nexus.datagen;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import org.dantesys.nexus.recipe.InfusorRecipe;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Datagen builder for recipes processed by the Arcane Infuser.
 */
public final class InfusorRecipeBuilder implements RecipeBuilder {
    private final List<Ingredient> ingredients;
    private final int manaCost;
    private final ItemStackTemplate result;
    private final RecipeUnlockAdvancementBuilder advancementBuilder =
            new RecipeUnlockAdvancementBuilder();
    private boolean hasCriterion;

    private InfusorRecipeBuilder(List<Ingredient> ingredients, int manaCost, ItemStackTemplate result) {
        if (ingredients.size() != InfusorRecipe.INPUT_COUNT) {
            throw new IllegalArgumentException("Infusor recipes require exactly four ingredients");
        }
        if (manaCost <= 0) {
            throw new IllegalArgumentException("Infusor mana cost must be greater than zero");
        }
        this.ingredients = List.copyOf(ingredients);
        this.manaCost = manaCost;
        this.result = result;
    }

    public static InfusorRecipeBuilder infusing(
            Ingredient first,
            Ingredient second,
            Ingredient third,
            Ingredient fourth,
            int manaCost,
            ItemStackTemplate result
    ) {
        return new InfusorRecipeBuilder(
                List.of(first, second, third, fourth), manaCost, result);
    }

    public static InfusorRecipeBuilder infusing(
            Ingredient first,
            Ingredient second,
            Ingredient third,
            Ingredient fourth,
            int manaCost,
            ItemStack result
    ) {
        return infusing(
                first, second, third, fourth, manaCost,
                ItemStackTemplate.fromNonEmptyStack(result));
    }

    public static InfusorRecipeBuilder infusing(
            Ingredient first,
            Ingredient second,
            Ingredient third,
            Ingredient fourth,
            int manaCost,
            ItemLike result,
            int resultCount
    ) {
        return infusing(
                first, second, third, fourth, manaCost,
                new ItemStackTemplate(result.asItem(), resultCount));
    }

    public static InfusorRecipeBuilder infusing(
            ItemLike first,
            ItemLike second,
            ItemLike third,
            ItemLike fourth,
            int manaCost,
            ItemStackTemplate result
    ) {
        return infusing(
                Ingredient.of(first),
                Ingredient.of(second),
                Ingredient.of(third),
                Ingredient.of(fourth),
                manaCost,
                result);
    }

    public static InfusorRecipeBuilder infusing(
            ItemLike first,
            ItemLike second,
            ItemLike third,
            ItemLike fourth,
            int manaCost,
            ItemStack result
    ) {
        return infusing(
                first, second, third, fourth, manaCost,
                ItemStackTemplate.fromNonEmptyStack(result));
    }

    public static InfusorRecipeBuilder infusing(
            ItemLike first,
            ItemLike second,
            ItemLike third,
            ItemLike fourth,
            int manaCost,
            ItemLike result
    ) {
        return infusing(
                first, second, third, fourth, manaCost,
                new ItemStackTemplate(result.asItem()));
    }

    public static InfusorRecipeBuilder infusing(
            ItemLike first,
            ItemLike second,
            ItemLike third,
            ItemLike fourth,
            int manaCost,
            ItemLike result,
            int resultCount
    ) {
        return infusing(
                first, second, third, fourth, manaCost,
                new ItemStackTemplate(result.asItem(), resultCount));
    }

    @Override
    public InfusorRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        advancementBuilder.unlockedBy(name, criterion);
        hasCriterion = true;
        return this;
    }

    /**
     * Infusor recipes are special and do not use recipe-book groups.
     */
    @Override
    public InfusorRecipeBuilder group(@Nullable String group) {
        return this;
    }

    @Override
    public ResourceKey<Recipe<?>> defaultId() {
        Identifier resultId = BuiltInRegistries.ITEM.getKey(result.item().value());
        return ResourceKey.create(
                Registries.RECIPE,
                Identifier.fromNamespaceAndPath(
                        resultId.getNamespace(), "infusor/" + resultId.getPath()));
    }

    @Override
    public void save(RecipeOutput output, ResourceKey<Recipe<?>> key) {
        InfusorRecipe recipe = new InfusorRecipe(ingredients, manaCost, result);
        AdvancementHolder advancement = hasCriterion
                ? advancementBuilder.build(output, key, RecipeCategory.MISC)
                : null;
        output.accept(key, recipe, advancement);
    }
}
