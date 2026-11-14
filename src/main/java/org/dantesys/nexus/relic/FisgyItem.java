package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.dantesys.nexus.util.MatrizModos;

/** Fisgy: vara de pesca de utilidade, com tom de humor. */
public final class FisgyItem extends RelicItem {
    public FisgyItem(Properties properties) {
        super(properties, MatrizModos.LANCA, 0, 0, 2,
                RelicSkill.passive(Component.literal("Pesca em Qualquer Lugar"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Peixe da Vida"), FisgyEffects.LIFE_MANA, FisgyEffects::lifeFish),
                RelicSkill.active(Component.literal("O Mar Não Está pra Peixe"), FisgyEffects.RAIN_MANA, FisgyEffects::fishRain),
                RelicSkill.active(Component.literal("Isca de Tubarão"), FisgyEffects.SHARK_MANA, FisgyEffects::sharkBait),
                new RelicSkill(Component.literal("Tornado de Peixes"), RelicSkill.Kind.ACTIVE,
                        Component.literal("1 pack (64) de peixes no inventário (qualquer peixe conta), consumidos."),
                        FisgyEffects::canTornado, FisgyEffects::tornado, FisgyEffects.TORNADO_MANA));
    }

    @Override public boolean canPerformAction(ItemInstance stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_FISHING_ROD_ACTIONS.contains(ability);
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.fishing != null) {
            if (player instanceof ServerPlayer sp && !FisgyEffects.reelCustom(sp, player.fishing)) player.fishing.retrieve(stack);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.9F);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.9F);
            if (level instanceof ServerLevel serverLevel) Projectile.spawnProjectile(new FishingHook(player, level, 0, 0), serverLevel, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Pesca fora da água (terra, areia, neve, lava, Nether, End), cada lugar com a própria tabela de peixes. "
                    + "Recolher a linha sem peixe com a boia presa num bloco (chão, parede ou teto) puxa você até ela, como um arpão.";
            case 1 -> "Fisgue um mob: ele perde a vida de um bicho aquático sorteado, que aparece ao lado. Se não aguentar, vira peixe.";
            case 2 -> "Chuva de peixes em volta: acertam e somem; os que erram viram peixe de verdade no chão.";
            case 3 -> "Fisgue um alvo: uma boca de tubarão sai do chão e morde. Na água, puxa para o fundo.";
            case 4 -> "Gasta 64 peixes e solta um tornado de peixes em linha reta, que puxa, gira e arremessa os inimigos.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> FisgyEffects.LIFE_COOLDOWN;
            case 2 -> FisgyEffects.RAIN_COOLDOWN;
            case 3 -> FisgyEffects.SHARK_COOLDOWN;
            case 4 -> FisgyEffects.TORNADO_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
