package org.dantesys.nexus.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.recipe.ForjaRecipe;

public final class ForjaRecipeCategory extends AbstractRecipeCategory<ForjaRecipe> {
    public ForjaRecipeCategory(IGuiHelper gui, IRecipeType<ForjaRecipe> type) {
        super(type, Component.translatable("jei.nexus.forja"), gui.createDrawableItemLike(ModBlocks.CONTROLADOR_FORJA.get()), 150, 62);
    }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, ForjaRecipe recipe, IFocusGroup focuses) {
        int[][] p = {{34,4},{61,4},{34,40},{61,40},{7,22}};
        for (int i = 0; i < recipe.ingredients().size(); i++) builder.addInputSlot(p[i][0], p[i][1]).setStandardSlotBackground().add(recipe.ingredients().get(i));
        builder.addOutputSlot(125,22).setOutputSlotBackground().add(recipe.result()).addRichTooltipCallback((slot, tooltip) -> {
            tooltip.add(Component.translatable("jei.nexus.mana_cost", recipe.mana()));
            tooltip.add(Component.translatable("jei.nexus.lava_cost", recipe.lava()));
            tooltip.add(Component.translatable("jei.nexus.forge_time", recipe.time() / 20.0F));
        });
    }
}
