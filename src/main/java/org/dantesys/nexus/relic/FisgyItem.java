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
                RelicSkill.passive(Component.translatable("relic.nexus.fisgy.pesca_em_qualquer_lugar"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.fisgy.peixe_da_vida"), FisgyEffects.LIFE_MANA, FisgyEffects::lifeFish),
                RelicSkill.active(Component.translatable("relic.nexus.fisgy.o_mar_nao_esta_pra"), FisgyEffects.RAIN_MANA, FisgyEffects::fishRain),
                RelicSkill.active(Component.translatable("relic.nexus.fisgy.isca_de_tubarao"), FisgyEffects.SHARK_MANA, FisgyEffects::sharkBait),
                new RelicSkill(Component.translatable("relic.nexus.fisgy.tornado_de_peixes"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.fisgy.1_pack_64_de_peixes"),
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
        return Component.translatable("relic.nexus.fisgy.skill." + slot + ".desc");
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
