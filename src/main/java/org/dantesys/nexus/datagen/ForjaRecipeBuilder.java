package org.dantesys.nexus.datagen;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import org.dantesys.nexus.recipe.ForjaRecipe;
import org.dantesys.nexus.util.Ranks;
import org.jspecify.annotations.Nullable;
import java.util.List;

public final class ForjaRecipeBuilder implements RecipeBuilder {
    private final List<Ingredient> ingredients;
    private final int mana, lava, time;
    private final ItemStackTemplate result;
    private final RecipeUnlockAdvancementBuilder advancement = new RecipeUnlockAdvancementBuilder();
    private boolean hasCriterion;

    private ForjaRecipeBuilder(List<Ingredient> ingredients, int mana, int lava, int time,
                               ItemStackTemplate result) {
        if (mana < 0 || lava < 0 || time <= 0) throw new IllegalArgumentException("Invalid forge costs/time");
        this.ingredients = List.copyOf(ingredients); this.mana = mana; this.lava = lava;
        this.time = time; this.result = result;
    }
    public static ForjaRecipeBuilder forging(Ingredient material1, Ingredient material2,
            Ingredient material3, Ingredient material4, int mana, int lava, int time, ItemStackTemplate result) {
        return new ForjaRecipeBuilder(List.of(material1, material2, material3, material4), mana, lava, time, result);
    }
    @Override public ForjaRecipeBuilder unlockedBy(String name, Criterion<?> criterion) { advancement.unlockedBy(name, criterion); hasCriterion = true; return this; }
    @Override public ForjaRecipeBuilder group(@Nullable String group) { return this; }
    @Override public ResourceKey<Recipe<?>> defaultId() {
        Identifier id = BuiltInRegistries.ITEM.getKey(result.item().value());
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(id.getNamespace(), "forja/" + id.getPath()));
    }
    @Override public void save(RecipeOutput output, ResourceKey<Recipe<?>> key) {
        AdvancementHolder unlock = hasCriterion ? advancement.build(output, key, RecipeCategory.MISC) : null;
        output.accept(key, new ForjaRecipe(ingredients, mana, lava, time, result), unlock);
    }
}
