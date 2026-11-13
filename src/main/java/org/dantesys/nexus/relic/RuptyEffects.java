package org.dantesys.nexus.relic;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.effects.ModEffects;

import java.util.*;

/**
 * Fragmentos da Rupty (estado so no servidor, por jogador). Um fragmento esta na lamina,
 * num alvo (marca) ou flutuando no chao; no Sem Defesa todos orbitam o jogador.
 */
@EventBusSubscriber(modid = Nexus.MODID)
public final class RuptyEffects {
    private RuptyEffects() {}

    private static final DustParticleOptions MANA = new DustParticleOptions(0x8000FF, 1.0f);
    private static final DustParticleOptions MANA_SMALL = new DustParticleOptions(0xB060FF, 0.6f);
    private static final Identifier REACH_ID = Identifier.fromNamespaceAndPath(Nexus.MODID, "rupty_fragments");
    private static final ResourceKey<DamageType> CUT = key("rupty_corte");
    private static final ResourceKey<DamageType> MAGIC = key("rupty_magia");

    private static final Map<UUID, State> STATES = new HashMap<>();
    /** Golpes das habilidades: dono e se ignoram toda a defesa. */
    private static final Map<DamageSource, Hit> HITS = new IdentityHashMap<>();
    /** Golpes corpo a corpo com a Rupty, marcados no dano recebido para a passiva no pos-dano. */
    private static final Map<DamageSource, ServerPlayer> MELEE = new WeakHashMap<>();
    private static LivingEntity passiveTarget;

    private record Hit(ServerPlayer player, boolean ignoreDefense) {}
    private record Mark(LivingEntity target, long expires) {}
    private record Floating(ServerLevel level, Vec3 pos, long expires, double power) {}
    private static final class State {
        final List<Mark> marks = new ArrayList<>();
        final List<Floating> floating = new ArrayList<>();
        long orbitUntil = -1;
        ServerLevel orbitLevel;
        double orbitPower = 1;
        boolean empty() { return marks.isEmpty() && floating.isEmpty() && orbitUntil < 0; }
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Nexus.MODID, name));
    }

    // ---------- Consultas ----------

    private static RuptyItem item() { return org.dantesys.nexus.items.ModItems.RUPTY.get(); }
    private static int level(Player player) { return item().masteryLevel(player); }
    public static int total(Player player) { return RuptyRules.totalFragments(level(player)); }
    private static int perTarget(Player player) { return RuptyRules.marksPerTarget(level(player)); }

    /** Fragmentos fora da lamina. */
    public static int out(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) return 0;
        if (state.orbitUntil >= 0) return total(player);
        return Math.min(total(player), state.marks.size() + state.floating.size());
    }
    public static int blade(ServerPlayer player) { return Math.max(0, total(player) - out(player)); }

    public static int marksOn(LivingEntity target, ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) return 0;
        int count = 0;
        for (Mark mark : state.marks) if (mark.target() == target) count++;
        return count;
    }

    private static State state(ServerPlayer player) { return STATES.computeIfAbsent(player.getUUID(), id -> new State()); }

    private static ItemStack findRupty(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof RuptyItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void changed(ServerPlayer player) {
        ItemStack stack = findRupty(player);
        if (!stack.isEmpty()) item().refreshModelData(player, stack);
    }

    static boolean canHit(ServerPlayer player, LivingEntity target) { return RelicTargets.canHit(player, target); }

    // ---------- Marcas e fragmentos ----------

    private static boolean addMark(ServerPlayer player, LivingEntity target) {
        if (blade(player) <= 0 || marksOn(target, player) >= perTarget(player)) return false;
        // Campo Reflexivo da Bastry: os fragmentos voltam para a espada sem marcar.
        if (BastryEffects.isProtected(target)) return false;
        state(player).marks.add(new Mark(target, player.level().getGameTime() + RuptyRules.MARK_DURATION));
        ServerLevel level = (ServerLevel) target.level();
        level.sendParticles(MANA, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.25, 0.3, 0.25, 0);
        item().addMasteryXp(player, RuptyRules.XP_PER_MARK);
        return true;
    }

    /** Devolve fragmentos a lamina (flutuantes primeiro, depois marcas mais antigas). */
    private static int recall(ServerPlayer player, int amount) {
        State state = STATES.get(player.getUUID());
        if (state == null) return 0;
        int returned = 0;
        while (returned < amount && !state.floating.isEmpty()) { state.floating.removeFirst(); returned++; }
        while (returned < amount && !state.marks.isEmpty()) { state.marks.removeFirst(); returned++; }
        if (returned > 0) item().addMasteryXp(player, returned * RuptyRules.XP_PER_RECALL);
        return returned;
    }

    private static void clear(ServerPlayer player) {
        if (STATES.remove(player.getUUID()) != null) changed(player);
    }

    // ---------- Dano ----------

    private static void hit(ServerPlayer player, LivingEntity target, float amount, boolean ignoreDefense,
                            ResourceKey<DamageType> type) {
        if (amount <= 0 || !canHit(player, target)) return;
        ServerLevel level = (ServerLevel) player.level();
        DamageSource source = type == null ? player.damageSources().playerAttack(player)
                : new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), player);
        HITS.put(source, new Hit(player, ignoreDefense));
        try {
            target.setInvulnerableTime(0);
            if (target instanceof EnderDragon dragon) dragon.head.hurtServer(level, source, amount);
            else target.hurtServer(level, source, amount);
        } finally {
            HITS.remove(source);
        }
    }

    private static boolean isRuptyMelee(DamageSource source, ServerPlayer player) {
        return !HITS.containsKey(source) && source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player
                && player.getMainHandItem().getItem() instanceof RuptyItem;
    }

    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        LivingEntity target = event.getEntity();
        Hit hit = HITS.get(source);
        boolean melee = hit == null && isRuptyMelee(source, player);
        if (hit == null && !melee) return;

        // Dano magico do Sem Defesa: a Pele de Mana bloqueia; o corte fisico continua.
        if (source.is(MAGIC) && target.hasEffect(ModEffects.PELE_MANA)) {
            event.setCanceled(true);
            return;
        }
        if (melee) {
            // Lamina menor = menos dano.
            event.setAmount((float) (event.getAmount() * RuptyRules.bladeScale(out(player))));
            MELEE.put(source, player);
        }
        double ignored = hit != null && hit.ignoreDefense() ? 1.0 : RuptyRules.defenseIgnored(marksOn(target, player));
        if (ignored <= 0) return;
        float keep = (float) (1.0 - ignored);
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * keep);
        event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> reduction * keep);
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        ServerPlayer player = MELEE.remove(event.getSource());
        if (player == null || event.getHealthDamage() <= 0) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RuptyItem rupty)) return;
        LivingEntity previous = passiveTarget;
        passiveTarget = event.getEntity();
        try { rupty.triggerPassive(player, stack, 0); }
        finally { passiveTarget = previous; }
    }

    // ---------- Habilidades ----------

    /** Nv 1 — Marca de Ruptura (passiva). */
    public static boolean markPassive(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (passiveTarget == null || !passiveTarget.isAlive()) return false;
        if (!addMark(player, passiveTarget)) return false;
        changed(player);
        return true;
    }

    private static boolean orbiting(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.orbitUntil >= 0) {
            player.sendSystemMessage(Component.literal("Os fragmentos estão em Sem Defesa."), true);
            return true;
        }
        return false;
    }

    /** Nv 5 — Estocada Fragmentada. */
    public static boolean stab(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (orbiting(player)) return false;
        int missing = RuptyRules.STAB_FRAGMENTS - blade(player);
        if (missing > 0) recall(player, missing);
        if (blade(player) < RuptyRules.STAB_FRAGMENTS) return false;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition(), dir = player.getLookAngle();
        Vec3 end = start.add(dir.scale(RuptyRules.STAB_RANGE));
        var block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        final Vec3 lineEnd = end;

        List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                new AABB(start, lineEnd).inflate(1), t -> canHit(player, t)
                        && (t.getBoundingBox().inflate(0.3).contains(start) || t.getBoundingBox().inflate(0.3).clip(start, lineEnd).isPresent())));
        targets.sort(Comparator.comparingDouble(t -> t.distanceToSqr(start)));
        for (LivingEntity target : targets) hit(player, target, (float) (RuptyRules.STAB_DAMAGE * context.powerMultiplier()), false, null);

        int remaining = RuptyRules.STAB_FRAGMENTS;
        Vec3 floatEnd = lineEnd;
        LivingEntity first = targets.stream().filter(LivingEntity::isAlive).findFirst().orElse(null);
        if (first != null && BastryEffects.isProtected(first)) {
            remaining = 0;
            first = null;
        }
        if (first != null) {
            int marks = RuptyRules.stabMarks(Math.sqrt(first.distanceToSqr(start)));
            for (int i = 0; i < marks && addMark(player, first); i++) remaining--;
            floatEnd = first.position().add(0, first.getBbHeight() * 0.5, 0);
        }
        // Os que nao marcaram ficam flutuando na linha por 20 s.
        State state = state(player);
        long expires = level.getGameTime() + RuptyRules.FLOAT_DURATION;
        Vec3 ground = start.add(0, -0.6, 0);
        Vec3 groundEnd = floatEnd.add(0, -0.6, 0);
        for (int i = 0; i < remaining; i++) {
            double t = (i + 1.0) / (remaining + 1.0);
            state.floating.add(new Floating(level, ground.lerp(groundEnd, t), expires, context.powerMultiplier()));
        }
        for (double d = 0.3; d <= start.distanceTo(lineEnd); d += 0.3) {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(MANA, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0.02);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.4f);
        changed(player);
        return true;
    }

    /** Nv 10 — Uniao: chama todos os fragmentos de volta, cortando quem estiver no caminho. */
    public static boolean union(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (orbiting(player)) return false;
        State state = STATES.get(player.getUUID());
        if (state == null || state.marks.isEmpty() && state.floating.isEmpty()) {
            player.sendSystemMessage(Component.literal("Nenhum fragmento fora da lâmina."), true);
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        Vec3 dest = player.position().add(0, player.getBbHeight() * 0.6, 0);
        List<Vec3> origins = new ArrayList<>();
        for (Mark mark : state.marks) {
            if (mark.target().level() == level) origins.add(mark.target().position().add(0, mark.target().getBbHeight() * 0.5, 0));
        }
        for (Floating f : state.floating) if (f.level() == level) origins.add(f.pos());

        Map<LivingEntity, Integer> cuts = new LinkedHashMap<>();
        for (Vec3 origin : origins) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, dest).inflate(1),
                    t -> canHit(player, t) && (t.getBoundingBox().inflate(0.4).contains(origin)
                            || t.getBoundingBox().inflate(0.4).clip(origin, dest).isPresent()))) {
                cuts.merge(target, 1, Integer::sum);
            }
            double length = origin.distanceTo(dest);
            for (double d = 0; d <= length; d += 0.5) {
                Vec3 p = origin.lerp(dest, d / Math.max(0.01, length));
                level.sendParticles(MANA_SMALL, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
            }
        }
        // O corte acontece com as marcas ainda no alvo (a defesa reduzida vale).
        cuts.forEach((target, count) -> hit(player, target,
                (float) (RuptyRules.UNION_DAMAGE_PER_FRAGMENT * count * context.powerMultiplier()), false, null));

        int returned = recall(player, state.marks.size() + state.floating.size());
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, RuptyRules.UNION_STRENGTH_TICKS,
                RuptyRules.strengthAmplifier(returned)));
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0f, 1.2f);
        player.sendSystemMessage(Component.literal("União: " + returned + " fragmentos voltaram."), true);
        changed(player);
        return true;
    }

    /** Nv 20 — Sem Defesa (final): os 20 fragmentos orbitam o jogador por cerca de 2 minutos. */
    public static boolean noDefense(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (orbiting(player)) return false;
        State state = state(player);
        state.orbitUntil = player.level().getGameTime() + RuptyRules.FINAL_DURATION;
        state.orbitLevel = (ServerLevel) player.level();
        state.orbitPower = context.powerMultiplier();
        state.orbitLevel.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 1.6f);
        player.sendSystemMessage(Component.literal("Sem Defesa!"), true);
        changed(player);
        return true;
    }

    // ---------- Tick ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        updateReach(player);
        State state = STATES.get(player.getUUID());
        if (state == null) return;
        if (!player.isAlive() || findRupty(player).isEmpty()) { clear(player); return; }

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        boolean changed = false;

        // Marcas: timer proprio; voltam se o alvo morrer, sumir ou mudar de dimensao.
        Iterator<Mark> marks = state.marks.iterator();
        while (marks.hasNext()) {
            Mark mark = marks.next();
            LivingEntity target = mark.target();
            if (now >= mark.expires() || !target.isAlive() || target.isRemoved() || target.level() != level) {
                marks.remove();
                changed = true;
            }
        }
        if (now % 10 == 0) {
            Map<LivingEntity, Integer> perTarget = new HashMap<>();
            for (Mark mark : state.marks) perTarget.merge(mark.target(), 1, Integer::sum);
            perTarget.forEach((target, count) -> level.sendParticles(MANA, target.getX(),
                    target.getY() + target.getBbHeight() * 0.6, target.getZ(), count, 0.3, 0.4, 0.3, 0));
        }

        // Fragmentos flutuando: qualquer mob nao aliado que passar leva dano e vira marca.
        Iterator<Floating> floating = state.floating.iterator();
        while (floating.hasNext()) {
            Floating f = floating.next();
            if (now >= f.expires() || f.level() != level) {
                floating.remove();
                changed = true;
                continue;
            }
            if (now % 4 == 0) level.sendParticles(MANA, f.pos().x, f.pos().y, f.pos().z, 1, 0.08, 0.08, 0.08, 0);
            LivingEntity victim = level.getEntitiesOfClass(LivingEntity.class, new AABB(f.pos(), f.pos()).inflate(RuptyRules.FLOAT_RADIUS),
                    t -> canHit(player, t) && marksOn(t, player) < perTarget(player)).stream().findFirst().orElse(null);
            if (victim != null && BastryEffects.isProtected(victim)) {
                floating.remove();
                changed = true;
            } else if (victim != null) {
                floating.remove();
                hit(player, victim, (float) (RuptyRules.FLOAT_DAMAGE * f.power()), false, null);
                if (victim.isAlive()) addMark(player, victim);
                changed = true;
            }
        }

        // Sem Defesa: orbita, ignora defesa e atravessa blocos.
        if (state.orbitUntil >= 0) {
            if (now >= state.orbitUntil || state.orbitLevel != level) {
                state.orbitUntil = -1;
                player.sendSystemMessage(Component.literal("Os fragmentos voltaram para a lâmina."), true);
                changed = true;
            } else {
                int total = total(player);
                double radius = RuptyRules.FINAL_RADIUS;
                if (now % 2 == 0) {
                    for (int i = 0; i < total; i++) {
                        double angle = Math.PI * 2 * i / total + now * 0.15;
                        double y = player.getY() + 0.4 + (i % 3) * 0.6;
                        level.sendParticles(MANA, player.getX() + Math.cos(angle) * radius, y,
                                player.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
                    }
                }
                if (now % RuptyRules.FINAL_INTERVAL == 0) {
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(radius + 1, 2, radius + 1), t -> canHit(player, t)
                                    && horizontalDistance(t, player) <= radius + 0.75)) {
                        hit(player, target, (float) (RuptyRules.FINAL_MAGIC_DAMAGE * state.orbitPower), true, MAGIC);
                        hit(player, target, (float) (RuptyRules.FINAL_CUT_DAMAGE * state.orbitPower), true, CUT);
                    }
                }
            }
        }

        if (changed) changed(player);
        if (state.empty()) STATES.remove(player.getUUID());
    }

    private static double horizontalDistance(LivingEntity a, LivingEntity b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Lamina menor = menos alcance (5% por fragmento fora). */
    private static void updateReach(ServerPlayer player) {
        var reach = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach == null) return;
        int out = player.getMainHandItem().getItem() instanceof RuptyItem ? out(player) : 0;
        double amount = out > 0 ? RuptyRules.bladeScale(out) - 1.0 : 0;
        AttributeModifier current = reach.getModifier(REACH_ID);
        if (amount == 0) {
            if (current != null) reach.removeModifier(REACH_ID);
        } else if (current == null || current.amount() != amount) {
            reach.addOrUpdateTransientModifier(new AttributeModifier(REACH_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    // ---------- Limpeza ----------

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        STATES.clear();
        HITS.clear();
        MELEE.clear();
    }
}
