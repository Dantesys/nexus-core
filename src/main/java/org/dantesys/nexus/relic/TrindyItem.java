package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.dantesys.nexus.util.MatrizModos;

import java.util.List;

/** Trindy: tridente. Lealdade e Correnteza naturais. */
public final class TrindyItem extends RelicItem {
    public TrindyItem(Properties properties) {
        super(properties.attributes(TridentItem.createAttributes()), MatrizModos.LANCA, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.trindy.captura"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.trindy.vortex"), TrindyEffects.VORTEX_MANA, TrindyEffects::armVortex),
                RelicSkill.active(Component.translatable("relic.nexus.trindy.olha_a_onda"), TrindyEffects.WAVE_MANA, TrindyEffects::wave),
                RelicSkill.active(Component.translatable("relic.nexus.trindy.area_de_caca"), TrindyEffects.HUNT_MANA, TrindyEffects::armHunt),
                new RelicSkill(Component.translatable("relic.nexus.trindy.mega_tempest"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.trindy.a_area_de_caca_precisa"),
                        TrindyEffects::canTempest, TrindyEffects::armTempest, TrindyEffects.TEMPEST_MANA));
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.TRIDENT; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (TrindyEffects.flying(player)) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player) || getUseDuration(stack, entity) - remainingTime < 10) return false;
        if (player.isShiftKeyDown()) {
            // Correnteza, mesmo fora da agua (roda nos dois lados, como no vanilla).
            float strength = 3.0F;
            float yRot = player.getYRot(), xRot = player.getXRot();
            float xd = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
            float yd = -Mth.sin(xRot * Mth.DEG_TO_RAD);
            float zd = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
            float dist = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            player.push(xd * strength / dist, yd * strength / dist, zd * strength / dist);
            player.startAutoSpinAttack(20, 8.0F, stack);
            if (player.onGround()) player.move(MoverType.SELF, new Vec3(0.0, 1.2, 0.0));
            level.playSound(null, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        if (player instanceof ServerPlayer sp && isUnlocked(sp, 0)) TrindyEffects.throwTrident(sp, stack);
        return true;
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.trindy.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> TrindyEffects.VORTEX_COOLDOWN;
            case 2 -> TrindyEffects.WAVE_COOLDOWN;
            case 3 -> TrindyEffects.HUNT_COOLDOWN;
            case 4 -> TrindyEffects.TEMPEST_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, tridente em voo]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(TrindyEffects.flying(player) ? 1f : 0f);
    }
}
