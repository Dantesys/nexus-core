package org.dantesys.nexus.relic;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import org.dantesys.nexus.Nexus;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Ordem das protecoes contra a morte: morte garantida ignora tudo; a anulacao da Souly bloqueia totens,
 * Infinidade e Vida Extra (nao o Renascimento da Fenix, que nao e totem).
 * Ordem: Renascimento da Fenix, Infinidade (Eterny), Vida Extra (Eterny), totens do inventario (Souly).
 */
@EventBusSubscriber(modid = Nexus.MODID)
public final class RelicDeathRules {
    private RelicDeathRules() {}

    private static final Map<LivingEntity, Long> ANNULLED = new WeakHashMap<>();

    private static boolean annulled(LivingEntity victim, DamageSource source) {
        long now = victim.level().getGameTime();
        Long mark = ANNULLED.get(victim);
        if (mark != null && mark >= now - 1) return true;
        if (SoulyEffects.annul(victim, source)) {
            ANNULLED.put(victim, now);
            return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void totem(LivingUseTotemEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (event.getSource().is(RelicUtil.MORTE_GARANTIDA) || annulled(victim, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        // Portador da Souly nao usa totem na mao: os do inventario ativam sozinhos.
        if (victim instanceof net.minecraft.world.entity.player.Player p && SoulyEffects.carrier(p)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.isCanceled()) return;
        DamageSource source = event.getSource();
        if (source.is(RelicUtil.MORTE_GARANTIDA)) return;
        boolean annulled = annulled(victim, source);
        if (PhoenixyEffects.rebirth(victim, source)) { event.setCanceled(true); return; }
        if (annulled) return;
        if (EternyEffects.infinity(victim) || EternyEffects.extraLife(victim) || SoulyEffects.inventoryTotem(victim)) {
            event.setCanceled(true);
        }
    }
}
