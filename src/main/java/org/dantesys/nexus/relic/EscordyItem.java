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
                RelicSkill.passive(Component.literal("Cartucho"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Mãos Ágeis"), EscordyEffects.AGILE_MANA, EscordyEffects::agile),
                RelicSkill.active(Component.literal("Cartucho Pronto"), EscordyEffects.READY_MANA, EscordyEffects::ready),
                RelicSkill.active(Component.literal("Munição Alternativa"), EscordyEffects.FIREWORK_MANA, EscordyEffects::fireworkAmmo),
                new RelicSkill(Component.literal("Mix"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Regeneração rank A ou maior, algum mob ou jogador num raio de 5 blocos e nenhuma carga em reserva."),
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
        return Component.literal(switch (slot) {
            case 0 -> "Sem flechas: cada recarga gasta mana e soma 1 carga (10, até 20 com a maestria). Cada carga é um chumbo; "
                    + "mais chumbos, cone mais largo. Forte de perto. Clique dispara; Shift + clique segurado recarrega mais.";
            case 1 -> "Por ~10s, recarrega muito rápido e um pouco mais barato.";
            case 2 -> "Por alguns segundos, fica sempre com carga 5, sem recarregar nem gastar mana. A carga guardada volta depois.";
            case 3 -> "O próximo disparo vira fogos de artifício (até 5 foguetes), mais fortes com mais carga. Você não leva dano.";
            case 4 -> "Por ~2 min: limite de 50 cargas, chumbos com efeitos sorteados e chance de virar foguete. "
                    + "No fim, a carga vira disparos de reserva.";
            default -> "";
        });
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
        return Component.literal("Carga " + charge + "/" + limit + (reserve > 0 ? " · Reserva " + reserve : ""));
    }
}
