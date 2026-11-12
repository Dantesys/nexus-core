package org.dantesys.nexus.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.recipe.InfusorRecipe;
import org.dantesys.nexus.recipe.ForjaRecipe;
import org.dantesys.nexus.recipe.ModRecipes;
import org.jspecify.annotations.NonNull;

import java.util.List;

@JeiPlugin
@EventBusSubscriber(modid = Nexus.MODID, value = Dist.CLIENT)
public class ModJeiPlugin implements IModPlugin {
    public static final IRecipeType<InfusorRecipe> INFUSOR =
            IRecipeType.create(Nexus.MODID, "infusor", InfusorRecipe.class);
    public static final IRecipeType<ForjaRecipe> FORJA = IRecipeType.create(Nexus.MODID, "forja", ForjaRecipe.class);

    private static List<InfusorRecipe> infusorRecipes = List.of();
    private static List<ForjaRecipe> forjaRecipes = List.of();
    private static IJeiRuntime jeiRuntime;
    private static boolean infusorAddedToRuntime;
    private static boolean forjaAddedToRuntime;

    @Override
    public @NonNull Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Nexus.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new InfusorRecipeCategory(
                registration.getJeiHelpers().getGuiHelper(), INFUSOR));
        registration.addRecipeCategories(new ForjaRecipeCategory(registration.getJeiHelpers().getGuiHelper(), FORJA));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(INFUSOR, infusorRecipes);
        registration.addRecipes(FORJA, forjaRecipes);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        addRecipesToRuntime();
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
        infusorAddedToRuntime = false;
        forjaAddedToRuntime = false;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
                INFUSOR, ModBlocks.INFUSOR_ARCANO_BASICO.get());
        registration.addCraftingStation(FORJA, ModBlocks.CONTROLADOR_FORJA.get());
    }

    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        if (event.getRecipeTypes().contains(ModRecipes.INFUSOR_TYPE.get()))
            infusorRecipes = event.getRecipeMap().byType(ModRecipes.INFUSOR_TYPE.get()).stream().map(holder -> holder.value()).toList();
        if (event.getRecipeTypes().contains(ModRecipes.FORJA_TYPE.get()))
            forjaRecipes = event.getRecipeMap().byType(ModRecipes.FORJA_TYPE.get()).stream().map(holder -> holder.value()).toList();
        addRecipesToRuntime();
    }

    private static void addRecipesToRuntime() {
        if (jeiRuntime == null) return;
        var recipeManager = jeiRuntime.getRecipeManager();
        if (!infusorAddedToRuntime && !infusorRecipes.isEmpty()) {
            recipeManager.addRecipes(INFUSOR, infusorRecipes);
            infusorAddedToRuntime = true;
        }
        if (!forjaAddedToRuntime && !forjaRecipes.isEmpty()) {
            recipeManager.addRecipes(FORJA, forjaRecipes);
            forjaAddedToRuntime = true;
        }
    }
}
