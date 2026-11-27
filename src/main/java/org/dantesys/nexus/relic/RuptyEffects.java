package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.particle.ModParticles;

import java.util.*;

/**
 * Rupty, a katana da ruptura. Estado so no servidor.
 * <ul>
 *   <li>Marcas de Ruptura Magica: por portador, cada uma com contador proprio.</li>
 *   <li>Marca Mortal: por alvo, permanente ate a morte (o totem da imortalidade nao a remove).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Nexus.MODID)
public final class RuptyEffects {
    private RuptyEffects() {}

    private static final ResourceKey<DamageType> CUT = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Nexus.MODID, "rupty_corte"));

    /** Marcas de cada portador. */
    private static final Map<UUID, State> STATES = new HashMap<>();
    /** Marca Mortal preparada: espada consumida, aguardando o proximo golpe. */
    private static final Map<UUID, ItemStack> PENDING_MORTAL = new HashMap<>();
    /** Marcas Mortais ativas, por UUID do alvo. */
    private static final Map<UUID, Mortal> MORTALS = new HashMap<>();
    /** Golpes das habilidades (dono e se o dano e fixo, sem reducao nem bonus). */
    private static final Map<DamageSource, Hit> HITS = new IdentityHashMap<>();
    /** Golpes corpo a corpo com a Rupty, para a passiva no pos-dano. */
    private static final Map<DamageSource, ServerPlayer> MELEE = new WeakHashMap<>();
    private static LivingEntity passiveTarget;

    private record Hit(ServerPlayer player, boolean fixed) {}
    private static final class Mark {
        final LivingEntity target;
        final long created;
        long expires;
        boolean open;
        Mark(LivingEntity target, long created) {
            this.target = target;
            this.created = created;
            this.expires = created + RuptyRules.MARK_DURATION;
        }
    }
    private record Mortal(UUID owner, ItemStack sword, float swordDamage) {}
    private static final class State {
        final List<Mark> marks = new ArrayList<>();
    }

    // ---------- Consultas ----------

    private static RuptyItem item() { return org.dantesys.nexus.items.ModItems.RUPTY.get(); }

    static boolean canHit(ServerPlayer player, LivingEntity target) { return RelicTargets.canHit(player, target); }

    private static int[] marksOn(LivingEntity target, ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        int normal = 0, open = 0;
        if (state != null) {
            for (Mark mark : state.marks) {
                if (mark.target != target) continue;
                if (mark.open) open++; else normal++;
            }
        }
        return new int[]{normal, open};
    }

    public static int marksCount(LivingEntity target, ServerPlayer player) {
        int[] m = marksOn(target, player);
        return m[0] + m[1];
    }

    private static boolean mortalBy(ServerPlayer player, LivingEntity target) {
        Mortal mortal = MORTALS.get(target.getUUID());
        return mortal != null && mortal.owner().equals(player.getUUID());
    }

    public static boolean mortalPending(ServerPlayer player) { return PENDING_MORTAL.containsKey(player.getUUID()); }

    private static ItemStack findRupty(ServerPlayer player) { return item().findIn(player); }

    private static void refresh(ServerPlayer player) {
        ItemStack stack = findRupty(player);
        if (!stack.isEmpty()) item().refreshModelData(player, stack);
    }

    /** Dano de um golpe normal da espada (base do jogador + modificadores de dano de ataque do item). */
    static float swordDamage(ItemStack sword) {
        double damage = 1.0;
        ItemAttributeModifiers modifiers = sword.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                damage += entry.modifier().amount();
            }
        }
        return (float) Math.max(0, damage);
    }

    /** Dano de um golpe normal da Rupty: atributo do jogador se ela estiver na mao principal. */
    private static float normalHit(ServerPlayer player, ItemStack rupty) {
        if (player.getMainHandItem().getItem() instanceof RuptyItem) {
            return (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        return swordDamage(rupty);
    }

    // ---------- Marcas ----------

    /** Aplica ate {@code amount} marcas de Ruptura Magica (limite de 10 por entidade). */
    private static int addMarks(ServerPlayer player, LivingEntity target, int amount) {
        if (amount <= 0 || !target.isAlive()) return 0;
        // Campo Reflexivo da Bastry: os cortes nao marcam quem esta protegido.
        if (BastryEffects.isProtected(target)) return 0;
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        long now = player.level().getGameTime();
        int added = 0;
        while (added < amount && marksCount(target, player) < RuptyRules.MAX_MARKS) {
            state.marks.add(new Mark(target, now));
            added++;
        }
        if (added > 0) {
            ServerLevel level = (ServerLevel) target.level();
            level.sendParticles(particle(ModParticles.RUPTY_FRAGMENTO), target.getX(), target.getY() + target.getBbHeight() * 0.6,
                    target.getZ(), 4 + added * 2, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.02);
            item().addMasteryXp(player, added * RuptyRules.XP_PER_MARK);
        }
        return added;
    }

    private static SimpleParticleType particle(java.util.function.Supplier<SimpleParticleType> holder) {
        return holder.get();
    }

    // ---------- Dano ----------

    private static void hit(ServerPlayer player, LivingEntity target, float amount, boolean fixed) {
        if (amount <= 0 || !canHit(player, target)) return;
        ServerLevel level = (ServerLevel) player.level();
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(CUT), player);
        HITS.put(source, new Hit(player, fixed));
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

    /** Reduz a defesa do alvo contra a Rupty; o que passa de 100% vira dano extra. */
    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        LivingEntity target = event.getEntity();
        Hit hit = HITS.get(source);
        boolean melee = hit == null && isRuptyMelee(source, player);
        if (hit == null && !melee) return;
        if (melee) MELEE.put(source, player);

        if (hit != null && hit.fixed()) {
            // Sangramento da Marca Aberta: perda fixa de vida.
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0);
            event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> 0);
            return;
        }
        int[] marks = marksOn(target, player);
        double reduction = RuptyRules.defenseReduction(marks[0], marks[1], mortalBy(player, target));
        if (reduction <= 0) return;
        float keep = (float) (1.0 - RuptyRules.defenseIgnored(reduction));
        event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, r) -> r * keep);
        event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, r) -> r * keep);
        double multiplier = RuptyRules.damageMultiplier(reduction);
        if (multiplier > 1.0) event.setAmount((float) (event.getAmount() * multiplier));
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        ServerPlayer player = MELEE.remove(event.getSource());
        if (player == null || event.getHealthDamage() <= 0) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RuptyItem rupty)) return;
        LivingEntity target = event.getEntity();

        // Marca Mortal preparada: o proximo golpe aplica a marca.
        ItemStack sword = PENDING_MORTAL.remove(player.getUUID());
        if (sword != null) applyMortal(player, target, sword);

        LivingEntity previous = passiveTarget;
        passiveTarget = target;
        try { rupty.triggerPassive(player, stack, 0); }
        finally { passiveTarget = previous; }
    }

    // ---------- Habilidades ----------

    /** Nv 1 — Ruptura Magica (passiva): cada golpe cria uma marca de corte. */
    public static boolean markPassive(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (passiveTarget == null || !passiveTarget.isAlive()) return false;
        return addMarks(player, passiveTarget, 1) > 0;
    }

    /** Nv 5 — Corte Vertical: corte em linha reta de ate 5 blocos, atingindo todos na linha. */
    public static boolean verticalCut(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition(), dir = player.getLookAngle();
        Vec3 end = start.add(dir.scale(RuptyRules.VERTICAL_RANGE));
        var block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        final Vec3 lineEnd = end;

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(start, lineEnd).inflate(1.2),
                t -> canHit(player, t) && (t.getBoundingBox().inflate(0.6).contains(start)
                        || t.getBoundingBox().inflate(0.6).clip(start, lineEnd).isPresent()));
        float damage = normalHit(player, stack);
        for (LivingEntity target : targets) {
            hit(player, target, damage, false);
            if (target.isAlive()) addMarks(player, target, RuptyRules.VERTICAL_MARKS);
        }

        // Visual: uma lamina vertical avancando pela linha.
        double length = start.distanceTo(lineEnd);
        SimpleParticleType slash = particle(ModParticles.RUPTY_CORTE);
        SimpleParticleType shard = particle(ModParticles.RUPTY_FRAGMENTO);
        for (double d = 0.6; d <= length; d += 0.6) {
            Vec3 p = start.add(dir.scale(d)).add(0, -0.3, 0);
            level.sendParticles(slash, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            for (double y = -0.9; y <= 0.9; y += 0.45) {
                level.sendParticles(shard, p.x, p.y + y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.7f);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.7f, 1.6f);
        return true;
    }

    /** Nv 10 — Rotacao: golpe em 360 graus com 3 blocos de raio, marca e depois empurra. */
    public static boolean rotation(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = RuptyRules.ROTATION_RADIUS;
        float damage = (float) (normalHit(player, stack) * RuptyRules.ROTATION_DAMAGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius, 1.5, radius),
                t -> canHit(player, t) && horizontalDistance(t, player) <= radius + t.getBbWidth() * 0.5);
        for (LivingEntity target : targets) {
            double distance = Math.max(0, horizontalDistance(target, player) - target.getBbWidth() * 0.5);
            hit(player, target, damage, false);
            if (!target.isAlive()) continue;
            addMarks(player, target, RuptyRules.rotationMarks(distance));
            // Depois das marcas, empurra.
            Vec3 away = new Vec3(target.getX() - player.getX(), 0, target.getZ() - player.getZ());
            away = away.lengthSqr() < 1.0E-4 ? player.getLookAngle().multiply(1, 0, 1) : away;
            away = away.normalize().scale(RuptyRules.ROTATION_KNOCKBACK);
            RelicUtil.push(target, target.getDeltaMovement().add(away.x, 0.3, away.z));
        }

        // Visual: dois aneis de cortes em volta do portador.
        SimpleParticleType slash = particle(ModParticles.RUPTY_CORTE);
        SimpleParticleType shard = particle(ModParticles.RUPTY_FRAGMENTO);
        double y = player.getY() + player.getBbHeight() * 0.5;
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            double r = i % 2 == 0 ? RuptyRules.ROTATION_INNER_RADIUS : radius - 0.4;
            level.sendParticles(slash, player.getX() + Math.cos(a) * r, y, player.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            level.sendParticles(shard, player.getX() + Math.cos(a) * radius, y, player.getZ() + Math.sin(a) * radius,
                    2, 0.1, 0.2, 0.1, 0.02);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 0.8f);
        return true;
    }

    /** Nv 15 — Marca Aberta: as marcas existentes dobram de efeito e de duracao e fazem sangrar. */
    public static boolean openMark(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.marks.stream().noneMatch(m -> !m.open)) {
            player.sendSystemMessage(Component.literal("Nenhuma marca de Ruptura Mágica ativa."), true);
            return false;
        }
        Set<LivingEntity> opened = new HashSet<>();
        for (Mark mark : state.marks) {
            if (mark.open) continue;
            mark.open = true;
            mark.expires = mark.created + RuptyRules.OPEN_MARK_DURATION;
            opened.add(mark.target);
        }
        SimpleParticleType slash = particle(ModParticles.RUPTY_CORTE);
        for (LivingEntity target : opened) {
            if (!(target.level() instanceof ServerLevel level)) continue;
            level.sendParticles(slash, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 0.6f);
        }
        player.sendSystemMessage(Component.literal("Marca Aberta: " + opened.size() + " alvo(s)."), true);
        return true;
    }

    /** Condicao da Marca Mortal: uma espada (que nao seja relíquia) no inventario e nenhuma marca ja preparada. */
    static boolean canUseMortal(ServerPlayer player, ItemStack stack) {
        return !mortalPending(player) && findSword(player) >= 0;
    }

    private static int findSword(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty() && s.is(ItemTags.SWORDS) && !(s.getItem() instanceof RelicItem)) return i;
        }
        return -1;
    }

    /** Nv 20 — Marca Mortal (final): consome uma espada; o proximo golpe aplica a marca permanente. */
    public static boolean mortalMark(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        int slot = findSword(player);
        if (slot < 0 || mortalPending(player)) return false;
        ItemStack sword = player.getInventory().getItem(slot).split(1);
        player.getInventory().setChanged();
        PENDING_MORTAL.put(player.getUUID(), sword);

        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(particle(ModParticles.RUPTY_MARCA_MORTAL), player.getX(), player.getY() + 1.0, player.getZ(),
                16, 0.4, 0.5, 0.4, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 0.6f);
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5f, 1.4f);
        player.sendSystemMessage(Component.literal("Marca Mortal preparada: o próximo golpe marca o alvo."), true);
        refresh(player);
        return true;
    }

    private static void applyMortal(ServerPlayer player, LivingEntity target, ItemStack sword) {
        MORTALS.put(target.getUUID(), new Mortal(player.getUUID(), sword, swordDamage(sword)));
        ServerLevel level = (ServerLevel) target.level();
        level.sendParticles(particle(ModParticles.RUPTY_MARCA_MORTAL), target.getX(), target.getY() + target.getBbHeight() * 0.6,
                target.getZ(), 24, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.04);
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.7f, 0.5f);
        player.sendSystemMessage(Component.literal("Marca Mortal aplicada!"), true);
        refresh(player);
    }

    // ---------- Tick ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        State state = STATES.get(player.getUUID());
        if (state == null) return;
        if (!player.isAlive() || findRupty(player).isEmpty()) { STATES.remove(player.getUUID()); return; }

        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        // Contador proprio por marca; somem se o alvo morrer, sumir ou mudar de dimensao.
        state.marks.removeIf(m -> now >= m.expires || !m.target.isAlive() || m.target.isRemoved() || m.target.level() != level);
        if (state.marks.isEmpty()) { STATES.remove(player.getUUID()); return; }

        Map<LivingEntity, int[]> perTarget = new HashMap<>();
        for (Mark mark : state.marks) perTarget.computeIfAbsent(mark.target, t -> new int[2])[mark.open ? 1 : 0]++;

        // Marca Aberta: meio coracao a cada 2 s por alvo (nao por marca).
        if (now % RuptyRules.OPEN_BLEED_INTERVAL == 0) {
            perTarget.forEach((target, counts) -> {
                if (counts[1] > 0) hit(player, target, RuptyRules.OPEN_BLEED_DAMAGE, true);
            });
        }
        if (now % 20 == 0) {
            SimpleParticleType shard = particle(ModParticles.RUPTY_FRAGMENTO);
            perTarget.forEach((target, counts) -> level.sendParticles(shard, target.getX(),
                    target.getY() + target.getBbHeight() * 0.6, target.getZ(), Math.min(6, counts[0] + counts[1] * 2),
                    target.getBbWidth() * 0.45, target.getBbHeight() * 0.35, target.getBbWidth() * 0.45, 0.01));
        }
    }

    /** Marca Mortal: dano periodico de metade do dano da espada consumida, ate o alvo morrer. */
    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (MORTALS.isEmpty()) return;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        boolean damageTick = now % RuptyRules.MORTAL_INTERVAL == 0;
        if (!damageTick && now % 20 != 0) return;
        for (var entry : new ArrayList<>(MORTALS.entrySet())) {
            LivingEntity target = findLiving(server, entry.getKey());
            if (target == null || !target.isAlive()) continue;
            Mortal mortal = entry.getValue();
            ServerLevel level = (ServerLevel) target.level();
            level.sendParticles(particle(ModParticles.RUPTY_MARCA_MORTAL), target.getX(), target.getY() + target.getBbHeight() + 0.3,
                    target.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
            if (!damageTick) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(mortal.owner());
            if (owner == null || owner.level() != level) continue;
            DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(CUT), owner);
            // Encantamentos de dano da espada consumida (Afiacao, Julgamento...) tambem contam.
            float base = EnchantmentHelper.modifyDamage(level, mortal.sword(), target, source, mortal.swordDamage());
            hit(owner, target, RuptyRules.mortalTickDamage(base), false);
        }
    }

    private static LivingEntity findLiving(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) return player;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }

    private static double horizontalDistance(Entity a, Entity b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ---------- Limpeza ----------

    /** So a morte de verdade remove a Marca Mortal (o totem da imortalidade cancela a morte antes disto). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        MORTALS.remove(entity.getUUID());
        if (entity instanceof ServerPlayer player) STATES.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        STATES.clear();
        PENDING_MORTAL.clear();
        MORTALS.clear();
        HITS.clear();
        MELEE.clear();
    }
}
