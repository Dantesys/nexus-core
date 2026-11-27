package org.dantesys.nexus.relic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.effects.ModEffects;

/** Regras dos efeitos especiais das reliquias: paralisia, toxinas sem cura e remocoes autorizadas. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class RelicEffectRules {
    private RelicEffectRules() {}

    private static int allowDepth = 0;
    private static LivingEntity totemUser = null;

    /** Remove efeitos ignorando as protecoes (codigo das reliquias). */
    public static void allowRemoval(Runnable action) {
        allowDepth++;
        try { action.run(); } finally { allowDepth--; }
    }

    public static boolean paralyzed(LivingEntity e) { return e.hasEffect(ModEffects.PARALISIA); }

    @SubscribeEvent
    public static void totem(LivingUseTotemEvent event) {
        totemUser = event.getEntity();
    }

    @SubscribeEvent
    public static void remove(MobEffectEvent.Remove event) {
        if (allowDepth > 0 || event.getEffectInstance() == null) return;
        var effect = event.getEffectInstance().getEffect();
        boolean totemNow = totemUser == event.getEntity();
        // Toxina: nem leite nem Negacao; so o totem. Amplificada: nada remove.
        if (effect.is(ModEffects.TOXINA.unwrapKey().orElseThrow()) && !totemNow) event.setCanceled(true);
        if (effect.is(ModEffects.TOXINA_AMPLIFICADA.unwrapKey().orElseThrow())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (totemUser != null && event.getEntity() == totemUser) totemUser = null;
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide() || !paralyzed(living)) return;
        living.setDeltaMovement(0, Math.min(0, living.getDeltaMovement().y), 0);
        if (living instanceof Mob mob) mob.getNavigation().stop();
        if (living instanceof Player player && player.isUsingItem()) player.stopUsingItem();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(AttackEntityEvent event) {
        if (paralyzed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void incoming(LivingIncomingDamageEvent event) {
        if (event.getSource().isDirect() && event.getSource().getEntity() instanceof LivingEntity attacker && paralyzed(attacker)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (paralyzed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (paralyzed(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void startUsing(LivingEntityUseItemEvent.Start event) {
        if (paralyzed(event.getEntity())) event.setCanceled(true);
    }
}
