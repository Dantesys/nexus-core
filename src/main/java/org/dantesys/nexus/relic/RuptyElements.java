package org.dantesys.nexus.relic;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.dantesys.nexus.util.Elementos;
import java.util.*;

/** Modificadores elementais; chamados somente para acertos confirmados da Rupty. */
public final class RuptyElements {
    private RuptyElements() {}
    private static final Map<LivingEntity, Map<UUID, Mark>> MARKS = new WeakHashMap<>();
    private static final class Mark {
        Elementos element;
        int heat, pulses;
        long lastHit, lastPulse = Long.MIN_VALUE, lastDischarge = Long.MIN_VALUE;
    }
    private static Mark mark(LivingEntity target, ServerPlayer player, Elementos element) {
        Mark mark = MARKS.computeIfAbsent(target, ignored -> new HashMap<>()).computeIfAbsent(player.getUUID(), ignored -> new Mark());
        if (mark.element != element || target.level().getGameTime() - mark.lastHit >= RuptyRules.DECAY_DELAY) {
            mark.heat = 0; mark.pulses = 0; mark.lastPulse = Long.MIN_VALUE;
        }
        mark.element = element;
        return mark;
    }
    /** Luz afeta dano recebido; Sombria afeta dano causado, mesmo vindo de outro ataque. */
    public static void modifyGeneralDamage(LivingIncomingDamageEvent event) {
        double vulnerability = strongest(event.getEntity(), Elementos.LUZ, true);
        double weakness = event.getSource().getEntity() instanceof LivingEntity attacker
                ? strongest(attacker, Elementos.SOMBRA, false) : 0;
        event.setAmount((float) Math.min(Float.MAX_VALUE, event.getAmount() * (1 + vulnerability) * (1 - weakness)));
    }
    private static double strongest(LivingEntity target, Elementos element, boolean vulnerability) {
        if (!(target.level() instanceof ServerLevel level)) return 0;
        var owners = MARKS.get(target);
        if (owners == null) return 0;
        double maximum = 0;
        for (var entry : owners.entrySet()) {
            if (entry.getValue().element != element) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) continue;
            int count = RuptyEffects.scars(target, player);
            maximum = Math.max(maximum, vulnerability ? RuptyRules.vulnerability(count) : RuptyRules.weakness(count));
        }
        return maximum;
    }
    public static void beforeHit(LivingIncomingDamageEvent event, ServerPlayer player, int slot, Elementos element) {
        if (element != Elementos.METAL) return;
        Mark mark = mark(event.getEntity(), player, element);
        long now = event.getEntity().level().getGameTime();
        int pulses = slot == 2 ? (mark.lastPulse != Long.MIN_VALUE && now - mark.lastPulse <= 20 ? mark.pulses + 1 : 1) : 0;
        double penetration = RuptyRules.penetration(RuptyEffects.scars(event.getEntity(), player), slot, pulses);
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> (float) (reduction * (1 - penetration)));
    }
    public static void afterHit(ServerPlayer player, ItemStack stack, LivingEntity target, int slot,
                                Elementos element, float actualDamage, int previousScars) {
        if (element == Elementos.NAO_ELEMENTAL) return;
        Mark mark = mark(target, player, element);
        int count = RuptyEffects.scars(target, player);
        long now = target.level().getGameTime();
        mark.lastHit = now;
        switch (element) {
            case IGNEA -> {
                if (count > 0) mark.heat = Math.min(RuptyRules.HEAT_MAX, mark.heat + 1);
                int ticks = mark.heat * RuptyRules.HEAT_FIRE_TICKS;
                if (slot == 1 || slot == 2) ticks = Math.max(ticks, RuptyRules.FIRE_TICKS);
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), ticks));
            }
            case VIDA -> {
                float heal = previousScars > 0 ? RuptyRules.healing(actualDamage) : 0;
                if (slot == 1) heal += RuptyRules.CUT_HEAL;
                if (slot == 2) heal += RuptyRules.ZONE_HEAL;
                if (heal > 0) player.heal(heal);
            }
            case LUZ -> {
                if (count > 0) target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                        Math.min(200, 20 + count * 10), 0));
                if (slot == 1 || slot == 2) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, RuptyRules.BLIND_TICKS, 0));
                if (slot == 2) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 15, 0));
            }
            case SOMBRA -> {
                if (slot > 0) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        slot == 3 ? RuptyRules.RUPTURE_DEBUFF_TICKS : RuptyRules.DEBUFF_TICKS, slot == 3 ? 2 : 0));
                if (slot == 2) target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 15, 0));
            }
            case METAL -> {
                if (slot == 2) {
                    mark.pulses = mark.lastPulse != Long.MIN_VALUE && now - mark.lastPulse <= 20 ? mark.pulses + 1 : 1;
                    mark.lastPulse = now;
                }
            }
            case TEMPESTADE -> {
                if (RuptyRules.crossedElectricThreshold(previousScars, count))
                    RuptyEffects.electricHit(player, stack, target, RuptyRules.ELECTRIC_DAMAGE);
                if (slot == 1) {
                    var neighbors = ((ServerLevel) player.level()).getEntitiesOfClass(LivingEntity.class,
                            target.getBoundingBox().inflate(RuptyRules.CHAIN_RADIUS), t -> t != target && RuptyEffects.canHit(player, t)
                                    && t.distanceToSqr(target) <= RuptyRules.CHAIN_RADIUS * RuptyRules.CHAIN_RADIUS && t.hasLineOfSight(target));
                    neighbors.sort(Comparator.comparingDouble(t -> t.distanceToSqr(target)));
                    for (int i = 0; i < Math.min(RuptyRules.CHAIN_TARGETS, neighbors.size()); i++)
                        RuptyEffects.electricHit(player, stack, neighbors.get(i), RuptyRules.ELECTRIC_DAMAGE);
                }
                if (slot == 2 && count > 0 && (mark.lastDischarge == Long.MIN_VALUE || now - mark.lastDischarge >= RuptyRules.ZONE_ELECTRIC_INTERVAL)) {
                    mark.lastDischarge = now;
                    RuptyEffects.electricHit(player, stack, target, RuptyRules.ELECTRIC_DAMAGE);
                }
                if (slot == 3) RuptyEffects.electricHit(player, stack, target, RuptyRules.RUPTURE_ELECTRIC_DAMAGE);
            }
            default -> {}
        }
    }
    public static void killed(ServerPlayer player, Elementos element, int slot) {
        if (slot == 3 && element == Elementos.VIDA) player.heal(player.getMaxHealth() * RuptyRules.KILL_HEAL_RATIO);
    }
    public static void rupturePath(ServerPlayer player, Vec3 point, Elementos element) {
        ServerLevel level = (ServerLevel) player.level();
        if (element == Elementos.IGNEA && player.mayBuild()) {
            BlockPos pos = BlockPos.containing(point);
            for (int i = 0; i < 3 && level.isEmptyBlock(pos.below()); i++) pos = pos.below();
            var fire = Blocks.FIRE.defaultBlockState();
            if (level.isEmptyBlock(pos) && fire.canSurvive(level, pos) && level.mayInteract(player, pos)) {
                var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
                level.setBlockAndUpdate(pos, fire);
                if (EventHooks.onBlockPlace(player, snapshot, Direction.UP)) snapshot.restore();
            }
        }
        if (element == Elementos.TEMPESTADE) level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                point.x, point.y, point.z, 5, 0.1, 0.1, 0.1, 0.05);
    }
    public static void ruptureEnd(ServerPlayer player, Vec3 point, Elementos element) {
        ServerLevel level = (ServerLevel) player.level();
        if (element == Elementos.LUZ) {
            level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF),
                    point.x, point.y, point.z, 1, 0, 0, 0, 0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(point, point).inflate(5), t -> RuptyEffects.canHit(player, t)))
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, RuptyRules.RUPTURE_DEBUFF_TICKS, 0));
        }
        if (element == Elementos.TEMPESTADE) {
            var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) { bolt.setPos(point.x, point.y, point.z); bolt.setVisualOnly(true); level.addFreshEntity(bolt); }
        }
    }
    public static void clear() { MARKS.clear(); }
    public static String description(Elementos element, int slot) {
        if (slot < 0 || slot > 3) return "";
        String[] lines = switch (element) {
            case IGNEA -> new String[]{"Ígnea: cortes acumulam Calor nos marcados.", "Ígnea: incendeia os atingidos.",
                    "Ígnea: mantém os atingidos em chamas.", "Ígnea: deixa um rastro de fogo no trajeto."};
            case VIDA -> new String[]{"Vida: parte do dano contra marcados cura o portador.", "Vida: acertar recupera vida.",
                    "Vida: cada inimigo atingido contribui para a cura.", "Vida: mortes causadas pela Ruptura recuperam vida."};
            case LUZ -> new String[]{"Luz: mais cicatrizes aumentam a visibilidade e vulnerabilidade.", "Luz: aplica cegueira breve.",
                    "Luz: pulsos cegam e atrasam inimigos.", "Luz: um clarão cega sobreviventes próximos."};
            case SOMBRA -> new String[]{"Sombria: cicatrizes reduzem o dano causado pelo alvo.", "Sombria: enfraquecimento curto.",
                    "Sombria: cortes contínuos mantêm escuridão e fraqueza.", "Sombria: sobreviventes ficam fortemente enfraquecidos."};
            case METAL -> new String[]{"Metal: a cada 5 cicatrizes aumenta a perfuração de armadura.", "Metal: ignora parte da proteção da armadura.",
                    "Metal: cortes consecutivos aumentam a penetração.", "Metal: grande perfuração no dano pesado."};
            case TEMPESTADE -> new String[]{"Tempestade: a cada 5 cicatrizes ocorre uma descarga.", "Tempestade: corte muito rápido com descargas em alvos próximos.",
                    "Tempestade: descargas periódicas atingem marcados.", "Tempestade: um raio acompanha o corte e causa dano extra."};
            default -> new String[]{"", "", "", ""};
        };
        return lines[slot];
    }
}
