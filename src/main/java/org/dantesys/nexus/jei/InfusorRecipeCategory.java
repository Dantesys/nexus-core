package org.dantesys.nexus.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.recipe.InfusorRecipe;

public final class InfusorRecipeCategory extends AbstractRecipeCategory<InfusorRecipe> {
    public InfusorRecipeCategory(
            IGuiHelper guiHelper,
            IRecipeType<InfusorRecipe> recipeType
    ) {
        super(
                recipeType,
                Component.translatable("jei.nexus.infusor"),
                guiHelper.createDrawableItemLike(ModBlocks.INFUSOR_ARCANO_BASICO.get()),
                116,
                58
        );
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            InfusorRecipe recipe,
            IFocusGroup focuses
    ) {
        int[][] positions = {
                {7, 7},
                {32, 7},
                {7, 32},
                {32, 32}
        };
        for (int slot = 0; slot < InfusorRecipe.INPUT_COUNT; slot++) {
            builder.addInputSlot(positions[slot][0], positions[slot][1])
                    .setStandardSlotBackground()
                    .add(recipe.inputItem().get(slot));
        }

        builder.addOutputSlot(91, 20)
                .setOutputSlotBackground()
                .add(recipe.output())
                .addRichTooltipCallback((slot, tooltip) ->
                        tooltip.add(Component.translatable(
                                "jei.nexus.mana_cost", recipe.getManaCost())));
    }
}
