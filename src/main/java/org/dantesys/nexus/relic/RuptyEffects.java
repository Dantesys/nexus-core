package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.dantesys.nexus.Nexus;
import java.util.*;

/** Efeitos de servidor. Marcas sao separadas por alvo e pelo jogador que as aplicou. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class RuptyEffects {
    private RuptyEffects() {}
    private static final Map<LivingEntity, Map<UUID, Scars>> SCARS = new WeakHashMap<>();
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<DamageSource, Attack> ATTACKS = new IdentityHashMap<>();
    private static final Map<DamageSource, Attack> MELEE = new WeakHashMap<>();
    private static final Map<LivingEntity, Float> HEALTH_BEFORE = new WeakHashMap<>();
    private static final Map<UUID, MeleeAttempt> MELEE_ATTEMPTS = new HashMap<>();
    private static LivingEntity passiveTarget;
    private record Scars(int count, long lastHit) {}
    private record Attack(ServerPlayer player, ItemStack stack, int scars, int slot) {}
    private record MeleeAttempt(LivingEntity target, ItemStack stack, long tick) {}
    private record Charge(ItemStack stack, Vec3 origin, ServerLevel level, long start) {}
    private static final class Wave {
        Vec3 position;
        final Vec3 direction;
        final ServerLevel level;
        final ItemStack stack;
        final double power;
        
        final Set<UUID> hit = new HashSet<>();
        int ticks;
        Wave(ServerPlayer player, ItemStack stack, double power) {
            position = player.getEyePosition(); direction = player.getLookAngle();
            level = (ServerLevel) player.level(); this.stack = stack; this.power = power;
        }
    }
    private record Zone(ServerLevel level, Vec3 center, ItemStack stack, double power, long start) {}
    private static final class State {
        Charge charge;
        final List<Wave> waves = new ArrayList<>();
        final List<Zone> zones = new ArrayList<>();
    }
    private static State state(ServerPlayer player) { return STATES.computeIfAbsent(player.getUUID(), id -> new State()); }
    public static int scars(LivingEntity target, ServerPlayer player) {
        var owners = SCARS.get(target);
        if (owners == null) return 0;
        var mark = owners.get(player.getUUID());
        if (mark == null) return 0;
        int count = RuptyRules.remainingScars(mark.count(), target.level().getGameTime() - mark.lastHit());
        if (count == 0) owners.remove(player.getUUID());
        return count;
    }
    private static void addScars(LivingEntity target, ServerPlayer player, int amount) {
        int count = scars(target, player);
        if (count + amount > 0) SCARS.computeIfAbsent(target, key -> new HashMap<>()).put(player.getUUID(),
                new Scars((int) Math.min(Integer.MAX_VALUE, (long) count + amount), target.level().getGameTime()));
        if (amount > 0) particles((ServerLevel) target.level(), target.position().add(0, target.getBbHeight() / 2, 0), amount);
    }
    public static boolean scar(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (passiveTarget == null) return false;
        addScars(passiveTarget, player, 1);
        return true;
    }
    public static boolean cut(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).waves.add(new Wave(player, stack, context.powerMultiplier()));
        return true;
    }
    public static boolean zone(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        state(player).zones.add(new Zone((ServerLevel) player.level(), player.position(), stack,
                context.powerMultiplier(), player.level().getGameTime()));
        return true;
    }
    public static boolean startCharge(ServerPlayer player, ItemStack stack) {
        State state = state(player);
        if (state.charge != null) return false;
        state.charge = new Charge(stack, player.position(), (ServerLevel) player.level(), player.level().getGameTime());
        player.sendSystemMessage(Component.literal("Concentrando... permaneça parado por 2 segundos."), true);
        return true;
    }
    static boolean canHit(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && !target.isSpectator() && !player.isAlliedTo(target)
                && (!(target instanceof net.minecraft.world.entity.player.Player other) || (!other.isCreative() && player.canHarmPlayer(other)));
    }
    private static void particles(ServerLevel level, Vec3 pos, int count) {
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, count, 0.3, 0.3, 0.3, 0);
    }
    private static void hit(ServerPlayer player, ItemStack stack, LivingEntity target, float damage, int marks) {
        hit(player, stack, target, damage, marks, -1);
    }
    private static void hit(ServerPlayer player, ItemStack stack, LivingEntity target, float damage, int marks, int slot) {
        DamageSource source = player.level().damageSources().playerAttack(player);
        ATTACKS.put(source, new Attack(player, stack, marks, slot));
        try { damage(target, source, damage); }
        finally { ATTACKS.remove(source); }
    }
    private static void damage(LivingEntity target, DamageSource source, float amount) {
        if (target instanceof EnderDragon dragon) dragon.head.hurt(source, amount);
        else target.hurt(source, amount);
    }
    static void electricHit(ServerPlayer player, ItemStack stack, LivingEntity target, float amount) {
        if (!canHit(player, target)) return;
        ServerLevel level = (ServerLevel) player.level();
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(Nexus.MODID, "rupty_discharge"))), player);
        ATTACKS.put(source, new Attack(player, stack, 0, -2));
        try {
            damage(target, source, amount);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
        } finally { ATTACKS.remove(source); }
    }
    private static Attack attack(DamageSource source) {
        Attack attack = ATTACKS.get(source);
        if (attack != null) return attack;
        return MELEE.get(source);
    }
    @SubscribeEvent public static void melee(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity target
                && player.getMainHandItem().getItem() instanceof RuptyItem) {
            MELEE_ATTEMPTS.put(player.getUUID(), new MeleeAttempt(target, player.getMainHandItem(), player.level().getGameTime()));
        }
    }
    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!ATTACKS.containsKey(source) && source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player) {
            MeleeAttempt attempt = MELEE_ATTEMPTS.get(player.getUUID());
            if (attempt != null && attempt.target() == event.getEntity() && attempt.tick() == player.level().getGameTime()) {
                MELEE.put(source, new Attack(player, attempt.stack(), 0, 0));
                MELEE_ATTEMPTS.remove(player.getUUID());
            }
        }
        Attack attack = attack(event.getSource());
        if (attack == null) return;
        event.setAmount((float) Math.min(Float.MAX_VALUE,
                event.getAmount() * (1 + scars(event.getEntity(), attack.player()) * RuptyRules.DAMAGE_PER_SCAR)));
    }
    @SubscribeEvent public static void beforeHealth(LivingDamageEvent.Pre event) {
        if (attack(event.getSource()) != null) HEALTH_BEFORE.put(event.getEntity(), event.getEntity().getHealth());
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event) {
        Attack attack = attack(event.getSource());
        if (attack == null) return;
        Float healthBefore = HEALTH_BEFORE.remove(event.getEntity());
        if (event.getHealthDamage() <= 0) return;
        LivingEntity target = event.getEntity();
        int previousScars = scars(target, attack.player());
        addScars(target, attack.player(), attack.scars());
        // Apenas golpes normais disparam a passiva. As ativas aplicam suas proprias marcas.
        if (!ATTACKS.containsKey(event.getSource())) {
            LivingEntity previous = passiveTarget;
            passiveTarget = target;
            try {
                RuptyItem item = (RuptyItem) attack.stack().getItem();
                if (!item.triggerPassive(attack.player(), attack.stack(), 0)) item.recordSuccessfulUse(attack.player(), attack.stack());
            }
            finally { passiveTarget = previous; }
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        Attack attack = attack(event.getSource());
        if (attack == null) return;
        if (scars(event.getEntity(), attack.player()) < 10) return;
        SCARS.remove(event.getEntity()); // Remove antes dos cortes para evitar recursao no mesmo alvo.
        ServerLevel level = (ServerLevel) event.getEntity().level();
        Vec3 center = event.getEntity().position();
        particles(level, center, 16);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(RuptyRules.BURST_RADIUS), t -> canHit(attack.player(), t))) {
            if (target.position().distanceToSqr(center) <= RuptyRules.BURST_RADIUS * RuptyRules.BURST_RADIUS)
                hit(attack.player(), attack.stack(), target, RuptyRules.BURST_DAMAGE, 0);
        }
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MELEE_ATTEMPTS.remove(player.getUUID());
        State state = STATES.get(player.getUUID());
        if (state == null) return;
        if (!player.isAlive()) { STATES.remove(player.getUUID()); return; }
        long now = player.level().getGameTime();
        if (state.charge != null) {
            Charge charge = state.charge;
            if (player.level() != charge.level() || player.position().distanceToSqr(charge.origin()) > 0.0004
                    || RelicItem.controlledRelic(player) != charge.stack() || !RuptyItem.canCharge(player)) {
                state.charge = null;
                player.sendSystemMessage(Component.literal("Concentração interrompida."), true);
            } else if (now - charge.start() >= RuptyRules.CHARGE_TICKS) {
                state.charge = null;
                ((RuptyItem) charge.stack().getItem()).finishRupture(player, charge.stack());
            } else if (now % 5 == 0) particles(charge.level(), player.getEyePosition(), 1);
        }
        state.waves.removeIf(wave -> tickWave(player, wave));
        state.zones.removeIf(zone -> {
            if (zone.level() != player.level() || now - zone.start() > RuptyRules.ZONE_DURATION) return true;
            if ((now - zone.start()) % RuptyRules.ZONE_INTERVAL == 0) {
                for (LivingEntity target : zone.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(zone.center(), zone.center()).inflate(RuptyRules.ZONE_RADIUS), t -> canHit(player, t))) {
                    if (target.position().distanceToSqr(zone.center()) <= RuptyRules.ZONE_RADIUS * RuptyRules.ZONE_RADIUS)
                        hit(player, zone.stack(), target, (float) (RuptyRules.ZONE_DAMAGE * zone.power()), 1, 2);
                }
                for (int i = 0; i < 20; i++) {
                    double angle = i * Math.PI / 10;
                    particles(zone.level(), zone.center().add(Math.cos(angle) * RuptyRules.ZONE_RADIUS, 0.3,
                            Math.sin(angle) * RuptyRules.ZONE_RADIUS), 1);
                }
            }
            return now - zone.start() >= RuptyRules.ZONE_DURATION;
        });
        if (state.charge == null && state.waves.isEmpty() && state.zones.isEmpty()) STATES.remove(player.getUUID());
    }
    private static boolean tickWave(ServerPlayer player, Wave wave) {
        if (wave.level != player.level()) return true;
        double speed = 1;
        Vec3 end = wave.position.add(wave.direction.scale(speed));
        var block = wave.level.clip(new ClipContext(wave.position, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        for (LivingEntity target : wave.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(wave.position, end).inflate(0.8), t -> canHit(player, t))) {
            if (!wave.hit.contains(target.getUUID()) && (target.getBoundingBox().inflate(0.8).contains(wave.position)
                    || target.getBoundingBox().inflate(0.8).clip(wave.position, end).isPresent())) {
                wave.hit.add(target.getUUID());
                hit(player, wave.stack, target, (float) (RuptyRules.CUT_DAMAGE * wave.power), 3, 1);
            }
        }
        particles(wave.level, end, 2);
        wave.position = end;
        return block.getType() != HitResult.Type.MISS || ++wave.ticks * speed >= RuptyRules.CUT_RANGE;
    }
    public static boolean rupture(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition(), direction = player.getLookAngle();
        Vec3 end = start;
        // Vidro, folhas e plantas; blocos resistentes interrompem o corte.
        for (double distance = 0.5; distance <= RuptyRules.RUPTURE_RANGE; distance += 0.5) {
            Vec3 point = start.add(direction.scale(distance));
            BlockPos pos = BlockPos.containing(point);
            var block = level.getBlockState(pos);
            if (!block.isAir()) {
                boolean fragile = block.is(Tags.Blocks.GLASS_BLOCKS) || block.is(Tags.Blocks.GLASS_PANES)
                        || block.is(BlockTags.LEAVES) || block.is(BlockTags.FLOWERS);
                if (fragile) {
                    if (!player.mayBuild() || !level.mayInteract(player, pos) || !player.gameMode.destroyBlock(pos)) break;
                } else if (!block.getCollisionShape(level, pos).isEmpty()) break;
            }
            end = point;
            particles(level, point, 2);
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.5), t -> canHit(player, t))) {
            if (!target.getBoundingBox().inflate(1).contains(start) && target.getBoundingBox().inflate(1).clip(start, end).isEmpty()) continue;
            boolean boss = target instanceof EnderDragon || target instanceof WitherBoss
                    || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(Tags.EntityTypes.BOSSES);
            boolean execute = RuptyRules.canExecute(scars(target, player), boss, target.getHealth(), target.getMaxHealth());
            if (execute) {
                DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                                net.minecraft.resources.Identifier.fromNamespaceAndPath(Nexus.MODID, "ruptura"))), player);
                ATTACKS.put(source, new Attack(player, stack, 0, 3));
                try { damage(target, source, Float.MAX_VALUE); }
                finally { ATTACKS.remove(source); }
            } else hit(player, stack, target, (float) (RuptyRules.RUPTURE_DAMAGE * context.powerMultiplier()), 0, 3);
        }
        return true;
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
        MELEE_ATTEMPTS.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        STATES.clear(); SCARS.clear(); ATTACKS.clear(); MELEE.clear(); MELEE_ATTEMPTS.clear(); HEALTH_BEFORE.clear();
    }
}
