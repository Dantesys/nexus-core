package org.dantesys.nexus.relic;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.dantesys.nexus.util.MatrizModos;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Dwarky: clava (mace) do anao. Mantem a mecanica da clava do vanilla: mais dano quanto maior a queda.
 */
public final class DwarkyItem extends RelicItem {
    public DwarkyItem(Properties properties) {
        super(properties.component(DataComponents.TOOL, MaceItem.createToolProperties()).attributes(org.dantesys.nexus.items.ModToolMaterial.relicMaceAttributes()),
                MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.dwarky.sangue_de_anao"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.dwarky.corrente"), DwarkyEffects.CHAIN_MANA, DwarkyEffects::chain),
                RelicSkill.active(Component.translatable("relic.nexus.dwarky.pulo_explosivo"), DwarkyEffects.LEAP_MANA, DwarkyEffects::leap),
                RelicSkill.active(Component.translatable("relic.nexus.dwarky.home_run"), DwarkyEffects.HOMERUN_MANA, DwarkyEffects::homeRun),
                new RelicSkill(Component.translatable("relic.nexus.dwarky.quebra_montanha"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.dwarky.1_pack_64_de_pedra"),
                        DwarkyEffects::canMountain, DwarkyEffects::mountain, DwarkyEffects.MOUNTAIN_MANA));
    }

    // ---------- Home Run: segurar o clique carrega o golpe ----------

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.SPEAR; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return DwarkyEffects.homeRunArmed(stack, user) ? 72000 : 0; }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!DwarkyEffects.homeRunArmed(stack, player)) return InteractionResult.PASS;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (entity instanceof ServerPlayer player) DwarkyEffects.chargeHomeRun(player, stack, getUseDuration(stack, entity) - remainingTime);
        return true;
    }

    // ---------- Mecanica da clava (copiada do MaceItem) ----------

    @Override public void hurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        if (!MaceItem.canSmashAttack(attacker)) return;
        ServerLevel level = (ServerLevel) attacker.level();
        attacker.setDeltaMovement(attacker.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
        attacker.setIgnoreFallDamageFromCurrentImpulse(true, attacker.position());
        if (attacker instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            if (mob.onGround()) player.setSpawnExtraParticlesOnFall(true);
        }
        level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                mob.onGround() ? (attacker.fallDistance > 5.0 ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND) : SoundEvents.MACE_SMASH_AIR,
                attacker.getSoundSource(), 1.0F, 1.0F);
        level.levelEvent(2013, mob.getOnPos(), 750);
        level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(3.5), nearby -> !nearby.isSpectator()
                && nearby != attacker && nearby != mob && !attacker.isAlliedTo(nearby)
                && !(nearby instanceof TamableAnimal animal && animal.isTame() && animal.isOwnedBy(attacker))
                && !(nearby instanceof ArmorStand stand && stand.isMarker()) && mob.distanceToSqr(nearby) <= 3.5 * 3.5
                && !(nearby instanceof Player p && p.isCreative() && p.getAbilities().flying)).forEach(nearby -> {
            Vec3 direction = nearby.position().subtract(mob.position());
            double power = (3.5 - direction.length()) * 0.7F * (attacker.fallDistance > 5.0 ? 2 : 1)
                    * (1.0 - nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (power > 0) {
                Vec3 push = direction.normalize().scale(power);
                nearby.push(push.x, 0.7F, push.z);
                if (nearby instanceof ServerPlayer other) other.connection.send(new ClientboundSetEntityMotionPacket(other));
            }
        });
    }

    @Override public void postHurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        if (MaceItem.canSmashAttack(attacker)) attacker.resetFallDistance();
    }

    @Override public float getAttackDamageBonus(Entity victim, float ignoredDamage, DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity attacker) || !MaceItem.canSmashAttack(attacker)) return 0;
        double fall = attacker.fallDistance;
        double damage = fall <= 3 ? 4 * fall : fall <= 8 ? 12 + 2 * (fall - 3) : 22 + fall - 8;
        return attacker.level() instanceof ServerLevel level
                ? (float) (damage + EnchantmentHelper.modifyFallBasedDamage(level, attacker.getWeaponItem(), victim, source, 0.0F) * fall)
                : (float) damage;
    }

    @Override public @Nullable DamageSource getItemDamageSource(LivingEntity attacker) {
        return MaceItem.canSmashAttack(attacker) ? attacker.damageSources().mace(attacker) : super.getItemDamageSource(attacker);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.dwarky.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> DwarkyEffects.CHAIN_COOLDOWN;
            case 2 -> DwarkyEffects.LEAP_COOLDOWN;
            case 3 -> DwarkyEffects.HOMERUN_COOLDOWN;
            case 4 -> DwarkyEffects.MOUNTAIN_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }

    /** custom_model_data: [estagio, corrente ativa]. */
    @Override protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) {
        return List.of(DwarkyEffects.chainActive(player) ? 1f : 0f);
    }
}
