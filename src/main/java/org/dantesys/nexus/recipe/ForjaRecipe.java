package org.dantesys.nexus.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.List;

public record ForjaRecipe(List<Ingredient> ingredients, int mana, int lava, int time,
                          ItemStackTemplate result) implements Recipe<ForjaRecipeInput> {
    public static final int INPUT_COUNT = 5;

    @Override public boolean matches(ForjaRecipeInput input, Level level) {
        if (input.size() != INPUT_COUNT || ingredients.isEmpty() || ingredients.size() > INPUT_COUNT) return false;
        for (int i = 0; i < ingredients.size(); i++) if (!ingredients.get(i).test(input.getItem(i))) return false;
        for (int i = ingredients.size(); i < INPUT_COUNT; i++) if (!input.getItem(i).isEmpty()) return false;
        return true;
    }
    @Override public ItemStack assemble(ForjaRecipeInput input) {
        return result.create();
    }
    @Override public RecipeSerializer<? extends Recipe<ForjaRecipeInput>> getSerializer() { return ModRecipes.FORJA_SERIALIZER.get(); }
    @Override public RecipeType<? extends Recipe<ForjaRecipeInput>> getType() { return ModRecipes.FORJA_TYPE.get(); }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.create(ingredients); }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }

    public static final class Serializer {
        private static final MapCodec<ForjaRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC.listOf(1, INPUT_COUNT).fieldOf("ingredients").forGetter(ForjaRecipe::ingredients),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("mana").forGetter(ForjaRecipe::mana),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("lava").forGetter(ForjaRecipe::lava),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("time").forGetter(ForjaRecipe::time),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(ForjaRecipe::result)
        ).apply(i, ForjaRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ForjaRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(INPUT_COUNT)), ForjaRecipe::ingredients,
                ByteBufCodecs.VAR_INT, ForjaRecipe::mana, ByteBufCodecs.VAR_INT, ForjaRecipe::lava,
                ByteBufCodecs.VAR_INT, ForjaRecipe::time, ItemStackTemplate.STREAM_CODEC, ForjaRecipe::result,
                ForjaRecipe::new);
        private Serializer() {}
        public static RecipeSerializer<ForjaRecipe> create() { return new RecipeSerializer<>(CODEC, STREAM_CODEC); }
    }
}
