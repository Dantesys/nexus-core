package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.dantesys.nexus.util.MatrizModos;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Escordy: besta com identidade de espingarda. Nao precisa de flechas: cada recarga gasta mana e soma 1 carga;
 * o disparo solta um chumbo por carga. Clique: dispara (com carga) ou recarrega; Shift + clique segurado: recarrega mais.
 */
public final class EscordyItem extends RelicItem {
    public EscordyItem(Properties properties) {
        super(properties, MatrizModos.ARCO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.escordy.cartucho"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.escordy.maos_ageis_2"), EscordyEffects.AGILE_MANA, EscordyEffects::agile),
                RelicSkill.active(Component.translatable("relic.nexus.escordy.cartucho_pronto"), EscordyEffects.READY_MANA, EscordyEffects::ready),
                RelicSkill.active(Component.translatable("relic.nexus.escordy.municao_alternativa"), EscordyEffects.FIREWORK_MANA, EscordyEffects::fireworkAmmo),
                new RelicSkill(Component.translatable("relic.nexus.escordy.mix"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.escordy.regeneracao_rank_a_ou_maior"),
                        EscordyEffects::canMix, EscordyEffects::mix, EscordyEffects.MIX_MANA));
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.CROSSBOW; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            EscordyEffects.Use result = EscordyEffects.use(sp, stack);
            if (result == EscordyEffects.Use.FIRED) return InteractionResult.SUCCESS;
            if (result == EscordyEffects.Use.NONE) return InteractionResult.FAIL;
        } else if (!EscordyEffects.clientCanCharge(player, stack)) {
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (!(entity instanceof ServerPlayer player)) return;
        int held = getUseDuration(stack, entity) - ticksRemaining;
        if (held >= EscordyEffects.chargeTicks(stack, level.getGameTime())) {
            EscordyEffects.load(player, stack);
            player.stopUsingItem();
        }
    }

    @Override public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner instanceof ServerPlayer player) EscordyEffects.tickStack(player, stack);
        super.inventoryTick(stack, level, owner, slot);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.escordy.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> EscordyEffects.AGILE_COOLDOWN;
            case 2 -> EscordyEffects.READY_COOLDOWN;
            case 3 -> EscordyEffects.FIREWORK_COOLDOWN;
            case 4 -> EscordyEffects.MIX_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, carga mostrada, limite, disparos em reserva]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        return List.of((float) EscordyEffects.shownCharge(stack, now), (float) EscordyEffects.limit(player, stack, now),
                (float) EscordyEffects.reserve(stack).length);
    }

    @Override public Component hudInfo(ItemStack stack) {
        int charge = (int) modelValue(stack, 1, 0), limit = (int) modelValue(stack, 2, 10), reserve = (int) modelValue(stack, 3, 0);
        return reserve > 0 ? Component.translatable("relic.nexus.escordy.hud_carga_reserva", charge, limit, reserve)
                : Component.translatable("relic.nexus.escordy.hud_carga", charge, limit);
    }
}
