package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.dantesys.nexus.util.MatrizModos;

/** Skopy: arco magico com identidade de rifle de precisao. Dispara flechas de mana. */
public final class SkopyItem extends RelicItem {
    public SkopyItem(Properties properties) {
        super(properties, MatrizModos.ARCO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Sinal Arcano"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Camuflagem"), SkopyEffects.CAMO_MANA, SkopyEffects::camouflage),
                RelicSkill.active(Component.literal("Na Mosca"), SkopyEffects.MOSCA_MANA, SkopyEffects::naMosca),
                RelicSkill.active(Component.literal("Ghost"), SkopyEffects.GHOST_MANA, SkopyEffects::ghost),
                new RelicSkill(Component.literal("Ultrasonic"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Regeneração rank A ou maior, sem cegueira, nenhuma flecha no inventário "
                                + "e nenhum mob ou jogador num raio de 5 blocos."),
                        SkopyEffects::canUltrasonic, SkopyEffects::ultrasonic, SkopyEffects.ULTRA_MANA));
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    /** Nao precisa de flechas: so de mana para o tiro. */
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.hasInfiniteMaterials() && RelicUtil.magic(player).getMana() < SkopyEffects.SHOT_MANA) {
            if (player instanceof ServerPlayer sp) RelicUtil.say(sp, "Mana insuficiente para atirar.");
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof ServerPlayer player)) return entity instanceof Player;
        float power = BowItem.getPowerForTime(getUseDuration(stack, entity) - remainingTime);
        if (power < 0.1F) return false;
        return SkopyEffects.shoot(player, stack, power);
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Flechas de mana (sem flechas). Cada acerto dá brilho (só você vê) e um efeito negativo sorteado.";
            case 1 -> "Invisibilidade total por alguns segundos; mobs perdem o alvo. O próximo tiro tem bônus de dano e quebra a invisibilidade.";
            case 2 -> "O primeiro tiro marca o alvo por 15s; o segundo é teleportado direto nele, com acerto garantido.";
            case 3 -> "O próximo tiro vira espectral: sem gravidade, até 200 blocos, atravessa até 3 obstáculos.";
            case 4 -> "Tiro instantâneo de até 1000 blocos que atravessa tudo. Dano = vida atual do alvo, -1% por obstáculo "
                    + "(bosses: no máximo 50%).";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> SkopyEffects.CAMO_COOLDOWN;
            case 2 -> SkopyEffects.MOSCA_COOLDOWN;
            case 3 -> SkopyEffects.GHOST_COOLDOWN;
            case 4 -> SkopyEffects.ULTRA_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
