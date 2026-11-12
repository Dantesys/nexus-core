package org.dantesys.nexus.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public record InfusorRecipe(List<Ingredient> inputItem, int inputMana, ItemStackTemplate output)
        implements Recipe<InfusorRecipeInput> {
    public static final int INPUT_COUNT = 4;

    @Override
    public boolean matches(InfusorRecipeInput input, Level level) {
        if (inputItem.size() != INPUT_COUNT || input.size() != INPUT_COUNT) {
            return false;
        }

        boolean[] used = new boolean[input.size()];
        for (Ingredient ingredient : inputItem) {
            boolean matched = false;
            for (int slot = 0; slot < input.size(); slot++) {
                if (!used[slot] && ingredient.test(input.getItem(slot))) {
                    used[slot] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }

        for (int slot = 0; slot < input.size(); slot++) {
            if (input.getItem(slot).isEmpty() || !used[slot]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(InfusorRecipeInput input) {
        return output.create();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    public int getManaCost() {
        return inputMana;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<InfusorRecipeInput>> getSerializer() {
        return ModRecipes.INFUSOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<InfusorRecipeInput>> getType() {
        return ModRecipes.INFUSOR_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(inputItem);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer {
        private static final MapCodec<InfusorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.listOf(INPUT_COUNT, INPUT_COUNT)
                                .fieldOf("ingredients").forGetter(InfusorRecipe::inputItem),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("mana").forGetter(InfusorRecipe::inputMana),
                        ItemStackTemplate.CODEC.fieldOf("result").forGetter(InfusorRecipe::output)
                ).apply(instance, InfusorRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, InfusorRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(INPUT_COUNT)),
                        InfusorRecipe::inputItem,
                        ByteBufCodecs.VAR_INT,
                        InfusorRecipe::inputMana,
                        ItemStackTemplate.STREAM_CODEC,
                        InfusorRecipe::output,
                        InfusorRecipe::new
                );

        private Serializer() {
        }

        public static RecipeSerializer<InfusorRecipe> create() {
            return new RecipeSerializer<>(CODEC, STREAM_CODEC);
        }
    }
}
