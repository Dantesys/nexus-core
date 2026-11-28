package org.dantesys.nexus.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.dantesys.nexus.relic.CultryEffects;

/** Pilula do Caldeirao do Refino (Cultry): aumenta um atributo para sempre, com teto por grau. */
public class PilulaItem extends Item {
    public static final String TYPE = "PilulaTipo", GRADE = "PilulaGrau";
    public static final String[] TYPES = {"vida", "forca", "defesa", "velocidade"};
    public static final String[] TYPE_NAMES = {"item.nexus.pilula.tipo.vida", "item.nexus.pilula.tipo.forca", "item.nexus.pilula.tipo.defesa", "item.nexus.pilula.tipo.velocidade"};
    public static final String[] GRADE_NAMES = {"item.nexus.pilula.grau.inferior", "item.nexus.pilula.grau.media", "item.nexus.pilula.grau.superior"};

    public PilulaItem(Properties properties) {
        super(properties.food(new FoodProperties(1, 0.1f, true)).stacksTo(64));
    }

    public static ItemStack create(int type, int grade) {
        ItemStack stack = new ItemStack(ModItems.PILULA.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> { t.putInt(TYPE, type); t.putInt(GRADE, grade); });
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(java.util.List.of((float) grade, (float) type), java.util.List.of(), java.util.List.of(), java.util.List.of()));
        return stack;
    }

    public static int type(ItemStack s) { return Math.clamp(s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(TYPE, 0), 0, 3); }
    public static int grade(ItemStack s) { return Math.clamp(s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(GRADE, 1), 1, 3); }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.nexus.pilula.nome", Component.translatable(GRADE_NAMES[grade(stack) - 1]), Component.translatable(TYPE_NAMES[type(stack)]));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) CultryEffects.eatPill(player, type(stack), grade(stack));
        return super.finishUsingItem(stack, level, entity);
    }
}
