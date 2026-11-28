package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Grimy: livro de magias. Paginas liberadas pelos marcos de maestria; Shift + rolagem vira as paginas. */
public final class GrimyItem extends RelicItem {
    public GrimyItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.grimy.aprendiz"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.translatable("relic.nexus.grimy.mago_jr"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.translatable("relic.nexus.grimy.mago"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.translatable("relic.nexus.grimy.arquimago"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.translatable("relic.nexus.grimy.grao_mago_arcanum"), 0, (p, s, c) -> false));
    }

    /** Shift + rolagem: vira as paginas. */
    @Override public void selectSkill(ServerPlayer player, ItemStack stack, boolean next) {
        GrimyEffects.turnPage(player, stack, next);
    }

    @Override public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (MorpixEffects.transformed(player) || RelicEffectRules.paralyzed(player)) return false;
        return GrimyEffects.cast(player, stack, GrimyEffects.page(stack));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp) GrimyEffects.openPages(sp, stack);
            return InteractionResult.SUCCESS;
        }
        if (GrimyEffects.page(stack) == GrimyEffects.BEAM) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer sp) activate(sp, stack, 0);
        return InteractionResult.SUCCESS;
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BLOCK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return GrimyEffects.page(stack) == GrimyEffects.BEAM ? 72000 : 0; }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (entity instanceof ServerPlayer sp && (72000 - ticksRemaining) % 10 == 0) GrimyEffects.beamTick(sp, stack);
    }

    @Override public Component hudInfo(ItemStack stack) {
        int page = GrimyEffects.page(stack);
        MutableComponent line = Component.translatable("relic.nexus.grimy.pagina", page + 1, Component.translatable(GrimyEffects.NAMES[page]));
        Player player = CLIENT_PLAYER.get();
        if (player != null) {
            long left = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(GrimyEffects.CD + page, 0) - player.level().getGameTime();
            if (left > 0) line.append(" (" + (left + 19) / 20 + "s)");
        }
        return line;
    }

    @Override public List<Component> extraTooltip(ItemStack stack) {
        return List.of(Component.translatable("relic.nexus.grimy.agachar_clique_direito_escolher_pagina"));
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.grimy.skill." + slot + ".desc");
    }
}
