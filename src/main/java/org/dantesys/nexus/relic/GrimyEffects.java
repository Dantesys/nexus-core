package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.entity.BuracoNegro;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Magias da Grimy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class GrimyEffects {
    private GrimyEffects() {}

    public static final String[] NAMES = {"relic.nexus.grimy.pagina.0", "relic.nexus.grimy.pagina.1", "relic.nexus.grimy.pagina.2",
            "relic.nexus.grimy.pagina.3", "relic.nexus.grimy.pagina.4", "relic.nexus.grimy.pagina.5", "relic.nexus.grimy.pagina.6",
            "relic.nexus.grimy.pagina.7", "relic.nexus.grimy.pagina.8", "relic.nexus.grimy.pagina.9"};
    /** Custo em % da Capacidade de mana e recarga em ticks, por pagina. */
    private static final int[] COST = {3, 10, 8, 12, 0, 15, 20, 50, 100, 100};
    private static final int[] COOLDOWN = {20, 600, 100, 900, 0, 1200, 600, 2400, 18000, 18000};
    public static final int BEAM = 4;
    public static final String PAGE = "GrimyPagina", CD = "GrimyRecarga";
    private static final String TAG = "NexusGrimy";

    private static GrimyItem item() { return ModItems.GRIMY.get(); }
    private static ServerLevel lvl(ServerPlayer p) { return (ServerLevel) p.level(); }

    // ---------- Paginas ----------

    private static int unlockedPages(Player p) {
        int level = item().masteryLevel(p);
        return level >= 20 ? 10 : level >= 15 ? 8 : level >= 10 ? 6 : level >= 5 ? 4 : level >= 1 ? 2 : 0;
    }

    public static int page(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(PAGE, 0), 0, 9);
    }

    private static void setPage(ServerPlayer player, ItemStack stack, int page) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(PAGE, page));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
        RelicUtil.say(player, Component.translatable("relic.nexus.grimy.pagina", (page + 1), Component.translatable(NAMES[page])));
    }

    public static void turnPage(ServerPlayer player, ItemStack stack, boolean next) {
        int open = unlockedPages(player);
        if (open == 0) return;
        setPage(player, stack, Math.floorMod(page(stack) + (next ? 1 : -1), open));
    }

    public static void openPages(ServerPlayer player, ItemStack stack) {
        int open = unlockedPages(player);
        List<ItemStack> icons = new ArrayList<>();
        for (int i = 0; i < open; i++) {
            ItemStack icon = new ItemStack(i >= 8 ? Items.ENCHANTED_BOOK : Items.PAPER);
            icon.set(DataComponents.CUSTOM_NAME, Component.translatable("relic.nexus.grimy.texto", (i + 1), Component.translatable(NAMES[i])));
            icons.add(icon);
        }
        IconMenu.open(player, Component.translatable("relic.nexus.grimy.grimy_paginas"), icons, i -> setPage(player, stack, i), p -> p.getMainHandItem() == stack || p.getOffhandItem() == stack);
    }

    private static long cooldownLeft(ServerPlayer p, ItemStack stack, int page) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(CD + page, 0) - p.level().getGameTime();
    }

    private static void setCooldown(ServerPlayer p, ItemStack stack, int page, int ticks) {
        long until = p.level().getGameTime() + Math.max(0, (int) Math.ceil(ticks * (1 - 0.5 * RelicUtil.magic(p).getRessonancia() / 1000.0)));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong(CD + page, until));
    }

    private static boolean pay(ServerPlayer player, int percent) {
        if (player.hasInfiniteMaterials()) return true;
        var magic = player.getData(ModAttachmentType.MAGIC_STATS);
        int cost = Math.max(1, magic.getMaxMana() * percent / 100);
        if (magic.getMana() < cost) { RelicUtil.say(player, Component.translatable("relic.nexus.grimy.mana_insuficiente", cost)); return false; }
        magic.useMana(cost);
        player.syncData(ModAttachmentType.MAGIC_STATS);
        return true;
    }

    private static double power(ServerPlayer p) { return 1 + RelicUtil.magic(p).getAfinidade() / 1000.0; }
    private static net.minecraft.world.damagesource.DamageSource magicSource(ServerPlayer p) { return RelicUtil.source(lvl(p), RelicUtil.damageType("grimy_magia"), p); }

    public static boolean cast(ServerPlayer player, ItemStack stack, int page) {
        if (page >= unlockedPages(player)) { RelicUtil.say(player, Component.translatable("relic.nexus.grimy.pagina_ainda_bloqueada")); return false; }
        if (page == BEAM) { RelicUtil.say(player, Component.translatable("relic.nexus.grimy.segure_o_clique_direito_para")); return false; }
        long left = cooldownLeft(player, stack, page);
        if (left > 0) { RelicUtil.say(player, Component.translatable("relic.nexus.grimy.em_recarga_s", Component.translatable(NAMES[page]), (left + 19) / 20)); return false; }
        var magic = player.getData(ModAttachmentType.MAGIC_STATS);
        if (page >= 8 && !player.hasInfiniteMaterials() && magic.getMana() < magic.getMaxMana()) { RelicUtil.say(player, Component.translatable("relic.nexus.grimy.precisa_da_barra_de_mana")); return false; }
        if (page == 7 && !player.hasInfiniteMaterials() && magic.getMana() < magic.getMaxMana() / 2) { RelicUtil.say(player, Component.translatable("relic.nexus.grimy.precisa_de_pelo_menos_metade")); return false; }
        boolean done = switch (page) {
            case 0 -> missile(player);
            case 1 -> shield(player);
            case 2 -> fireball(player);
            case 3 -> reflect(player);
            case 5 -> wall(player);
            case 6 -> flash(player);
            case 7 -> explosion(player);
            case 8 -> doom(player);
            case 9 -> supernova(player);
            default -> false;
        };
        if (!done) return false;
        if (page == 7) { magic.useMana(magic.getMana()); player.syncData(ModAttachmentType.MAGIC_STATS); }
        else if (!pay(player, COST[page])) return false;
        if (page >= 8) { setCooldown(player, stack, 8, COOLDOWN[8]); setCooldown(player, stack, 9, COOLDOWN[9]); }
        else setCooldown(player, stack, page, COOLDOWN[page]);
        item().addMasteryXp(player, page >= 8 ? 10 : 1);
        return true;
    }

    // ---------- 1 — Missil Magico ----------

    private static boolean missile(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        LivingEntity target = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20), e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= 20)
                .stream().filter(e -> look.dot(e.getEyePosition().subtract(eye).normalize()) > 0.8)
                .max(Comparator.comparingDouble(e -> look.dot(e.getEyePosition().subtract(eye).normalize()))).orElse(null);
        if (target == null) { RelicUtil.say(player, Component.translatable("relic.nexus.grimy.nenhum_alvo_na_mira")); return false; }
        ShulkerBullet bullet = new ShulkerBullet(player.level(), player, target, Direction.Axis.Y);
        bullet.setPos(eye.add(look));
        bullet.getPersistentData().putBoolean(TAG, true);
        player.level().addFreshEntity(bullet);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.8f);
        return true;
    }

    // ---------- 2 — Escudo Arcano / 4 — Reflexao Magica ----------

    private static final Map<UUID, float[]> SHIELDS = new HashMap<>();
    private static final Map<UUID, Long> SHIELD_UNTIL = new HashMap<>(), REFLECT_UNTIL = new HashMap<>();

    private static boolean shield(ServerPlayer player) {
        SHIELDS.put(player.getUUID(), new float[]{12});
        SHIELD_UNTIL.put(player.getUUID(), player.level().getGameTime() + 200);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);
        return true;
    }

    private static boolean reflect(ServerPlayer player) {
        REFLECT_UNTIL.put(player.getUUID(), player.level().getGameTime() + 100);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f);
        return true;
    }

    private static boolean active(Map<UUID, Long> map, Entity e) {
        Long until = map.get(e.getUUID());
        return until != null && until > e.level().getGameTime();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void absorb(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(SHIELD_UNTIL, player)) return;
        float[] hp = SHIELDS.get(player.getUUID());
        if (hp == null || hp[0] <= 0) return;
        float absorbed = Math.min(hp[0], event.getAmount());
        hp[0] -= absorbed;
        event.setAmount(event.getAmount() - absorbed);
        if (event.getAmount() <= 0) event.setCanceled(true);
        if (hp[0] <= 0) SHIELD_UNTIL.remove(player.getUUID());
    }

    // ---------- 3 — Bola de Fogo ----------

    private static boolean fireball(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        SmallFireball ball = new SmallFireball(player.level(), player, look.scale(1.5));
        ball.setPos(player.getEyePosition().add(look));
        ball.getPersistentData().putBoolean(TAG, true);
        player.level().addFreshEntity(ball);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (!(proj.level() instanceof ServerLevel level)) return;
        HitResult hit = event.getRayTraceResult();
        // Escudo e Reflexao protegem o portador.
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof ServerPlayer target) {
            if (active(REFLECT_UNTIL, target) && proj.getOwner() != target) {
                Entity shooter = proj.getOwner();
                Vec3 dir = shooter != null ? shooter.getEyePosition().subtract(proj.position()).normalize() : proj.getDeltaMovement().scale(-1).normalize();
                double speed = Math.max(0.8, proj.getDeltaMovement().length());
                proj.setOwner(target);
                proj.setDeltaMovement(dir.scale(speed));
                proj.needsSync = true;
                event.setCanceled(true);
                return;
            }
            if (active(SHIELD_UNTIL, target) && proj.getOwner() != target) {
                level.sendParticles(ParticleTypes.ENCHANT, proj.getX(), proj.getY(), proj.getZ(), 10, 0.2, 0.2, 0.2, 0.2);
                proj.discard();
                event.setCanceled(true);
                return;
            }
        }
        if (!proj.getPersistentData().getBooleanOr(TAG, false) || !(proj.getOwner() instanceof ServerPlayer owner)) return;
        event.setCanceled(true);
        Vec3 pos = hit.getLocation();
        if (proj instanceof ShulkerBullet) {
            if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity e && RelicTargets.canHit(owner, e)) e.hurtServer(level, magicSource(owner), (float) (4 * power(owner)));
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 10, 0.2, 0.2, 0.2, 0.1);
        } else {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(3), e -> RelicTargets.canHit(owner, e))) {
                boolean direct = hit instanceof EntityHitResult eh && eh.getEntity() == e;
                e.hurtServer(level, level.damageSources().fireball(proj instanceof SmallFireball f ? f : null, owner), (float) ((direct ? 8 : 4) * power(owner)));
                e.igniteForSeconds(4);
            }
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 30, 1, 1, 1, 0.05);
            level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            level.playSound(null, BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.3f);
        }
        proj.discard();
    }

    // ---------- 5 — Raio Arcano ----------

    public static void beamTick(ServerPlayer player, ItemStack stack) {
        if (page(stack) != BEAM || unlockedPages(player) <= BEAM) { player.stopUsingItem(); return; }
        var magic = player.getData(ModAttachmentType.MAGIC_STATS);
        int cost = Math.max(1, magic.getMaxMana() / 100);
        if (!player.hasInfiniteMaterials()) {
            if (magic.getMana() < cost) { player.stopUsingItem(); return; }
            magic.useMana(cost);
            player.syncData(ModAttachmentType.MAGIC_STATS);
        }
        ServerLevel level = lvl(player);
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(5));
        for (double d = 0.5; d <= 5; d += 0.25) { Vec3 p = eye.add(look.scale(d)); level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0); }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1), e -> RelicTargets.canHit(player, e) && e.getBoundingBox().inflate(0.4).clip(eye, end).isPresent())) {
            e.setInvulnerableTime(0);
            e.hurtServer(level, magicSource(player), (float) (2 * power(player)));
            if (level.getRandom().nextFloat() < 0.25f) e.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 10, 0), player);
        }
        if (player.level().getGameTime() % 20 == 0) item().addMasteryXp(player, 1);
    }

    // ---------- 6 — YNP ----------

    private record Wall(ServerPlayer owner, ServerLevel level, Vec3 center, Vec3 normal, long until) {}
    private static final List<Wall> WALLS = new ArrayList<>();

    private static boolean wall(ServerPlayer player) {
        Vec3 flat = player.getLookAngle().multiply(1, 0, 1).normalize();
        WALLS.add(new Wall(player, lvl(player), player.position().add(flat.scale(2)), flat, player.level().getGameTime() + 200));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    private static boolean tickWall(Wall w, long now) {
        if (now >= w.until) return true;
        Vec3 side = new Vec3(-w.normal.z, 0, w.normal.x);
        AABB box = new AABB(w.center.add(side.scale(2.5)).add(w.normal.scale(-0.6)), w.center.add(side.scale(-2.5)).add(w.normal.scale(0.6)).add(0, 3, 0));
        for (Entity e : w.level.getEntities((Entity) null, box.inflate(0.3), e -> e instanceof LivingEntity || e instanceof Projectile)) {
            if (e instanceof LivingEntity l && !RelicTargets.canHit(w.owner, l)) continue;
            if (e instanceof Projectile p) {
                if (p.getOwner() instanceof LivingEntity o && (o == w.owner || RelicTargets.isAlly(w.owner, o))) continue;
                p.discard();
                continue;
            }
            double side_ = e.position().subtract(w.center).dot(w.normal);
            Vec3 push = w.normal.scale(side_ >= 0 ? 0.9 : -0.9);
            RelicUtil.push(e, new Vec3(push.x, 0.2, push.z));
        }
        if (now % 4 == 0) for (double s = -2.5; s <= 2.5; s += 0.5) for (double y = 0; y <= 3; y += 0.75) {
            Vec3 p = w.center.add(side.scale(s)).add(0, y, 0);
            w.level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        return false;
    }

    // ---------- 7 — Clarao Destruidor ----------

    private static boolean flash(ServerPlayer player) {
        ServerLevel level = lvl(player);
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        var clip = level.clip(new ClipContext(eye, eye.add(look.scale(20)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = clip.getLocation();
        for (double d = 0.5; d <= eye.distanceTo(end); d += 0.5) { Vec3 p = eye.add(look.scale(d)); level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.5, 0.5, 0.5, 0.01); }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5), e -> RelicTargets.canHit(player, e) && e.getBoundingBox().inflate(1).clip(eye, end).isPresent())) {
            float dmg = (float) (8 * power(player));
            BlockPos behind = BlockPos.containing(e.position().add(look.multiply(1, 0, 1).normalize().scale(e.getBbWidth() / 2 + 0.6)).add(0, 0.5, 0));
            if (level.getBlockState(behind).isCollisionShapeFullBlock(level, behind)) dmg *= 2;
            e.hurtServer(level, magicSource(player), dmg);
            RelicUtil.push(e, look.scale(2.0).add(0, 0.3, 0));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.8f);
        return true;
    }

    // ---------- 8 — Explosion ----------

    private record Channel(ServerPlayer owner, ServerLevel level, Vec3 target, long at) {}
    private static final List<Channel> CHANNELS = new ArrayList<>();

    private static Vec3 aim(ServerPlayer player, double range) {
        HitResult hit = player.pick(range, 0, false);
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : player.getEyePosition().add(player.getLookAngle().scale(range));
    }

    private static boolean explosion(ServerPlayer player) {
        CHANNELS.add(new Channel(player, lvl(player), aim(player, 30), player.level().getGameTime() + 40));
        player.addEffect(new MobEffectInstance(ModEffects.PARALISIA, 40, 0, false, false));
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.5f, 0.6f);
        RelicUtil.say(player, Component.translatable("relic.nexus.grimy.explosion"));
        return true;
    }

    private static boolean tickChannel(Channel c, long now) {
        ServerLevel level = c.level;
        if (now < c.at) {
            double r = 3 + (40 - (c.at - now)) * 0.1;
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12 + now * 0.1;
                level.sendParticles(ParticleTypes.ENCHANT, c.target.x + Math.cos(a) * r, c.target.y + 0.1, c.target.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0);
            }
            return false;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c.target, c.target).inflate(8), e -> e.isAlive() && !e.isSpectator())) {
            double d = e.position().distanceTo(c.target);
            if (d > 8) continue;
            e.hurtServer(level, RelicUtil.source(level, RelicUtil.damageType("grimy_magia"), c.owner), (float) (24 * (1 - d / 8) * power(c.owner)));
            RelicUtil.push(e, e.position().subtract(c.target).normalize().scale(1.5).add(0, 0.5, 0));
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.target.x, c.target.y + 1, c.target.z, 4, 2, 2, 2, 0);
        level.sendParticles(ParticleTypes.FLAME, c.target.x, c.target.y + 1, c.target.z, 200, 4, 3, 4, 0.2);
        level.playSound(null, BlockPos.containing(c.target), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.6f);
        c.owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 2));
        c.owner.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        return true;
    }

    // ---------- 9 — Dooom ----------

    private static final class Doom { final ServerPlayer owner; final ServerLevel level; final Vec3 center; final long until; final BuracoNegro core; final Map<UUID, Integer> time = new HashMap<>();
        Doom(ServerPlayer o, ServerLevel l, Vec3 c, long u, BuracoNegro b) { owner = o; level = l; center = c; until = u; core = b; } }
    private static final List<Doom> DOOMS = new ArrayList<>();

    private static boolean doom(ServerPlayer player) {
        Vec3 center = aim(player, 20);
        DOOMS.add(new Doom(player, lvl(player), center, player.level().getGameTime() + 140, BuracoNegro.spawn(lvl(player), center, 4.0F)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 0.4f);
        RelicUtil.say(player, Component.translatable("relic.nexus.grimy.dooom"));
        return true;
    }

    private static boolean tickDoom(Doom d, long now) {
        if (now >= d.until) { d.core.discard(); return true; }
        if (now % 2 == 0) for (int i = 0; i < 30; i++) {
            Vec3 dir = new Vec3(d.level.getRandom().nextGaussian(), d.level.getRandom().nextGaussian(), d.level.getRandom().nextGaussian()).normalize().scale(10);
            d.level.sendParticles(ParticleTypes.SQUID_INK, d.center.x + dir.x, d.center.y + dir.y, d.center.z + dir.z, 1, 0, 0, 0, 0);
        }
        Set<UUID> inside = new HashSet<>();
        for (LivingEntity e : d.level.getEntitiesOfClass(LivingEntity.class, new AABB(d.center, d.center).inflate(10), e -> e != d.owner && e.isAlive() && !e.isSpectator()
                && e.getBoundingBox().getCenter().distanceTo(d.center) <= 10)) {
            inside.add(e.getUUID());
            int t = d.time.merge(e.getUUID(), 1, Integer::sum);
            if (t >= 100) RelicUtil.guaranteedKill(d.level, e, d.owner);
        }
        d.time.keySet().retainAll(inside);
        return false;
    }

    // ---------- 10 — Super Nova ----------

    private record Nova(ServerPlayer owner, ServerLevel level, Vec3 start, Vec3 dir, long launched) {}
    private static final List<Nova> NOVAS = new ArrayList<>();

    private static boolean supernova(ServerPlayer player) {
        NOVAS.add(new Nova(player, lvl(player), player.getEyePosition(), player.getLookAngle(), player.level().getGameTime()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 2.0f);
        return true;
    }

    private static boolean tickNova(Nova n, long now) {
        long age = now - n.launched;
        Vec3 pos = n.start.add(n.dir.scale(Math.min(15, age)));
        ServerLevel level = n.level;
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 10, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 5, 0.1, 0.1, 0.1, 0.01);
        if (age < 15) return false;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(10), e -> RelicTargets.canHit(n.owner, e))) {
            double d = e.position().distanceTo(pos);
            if (d <= 10) e.hurtServer(level, RelicUtil.source(level, RelicUtil.damageType("grimy_magia"), n.owner), (float) (40 * (1 - d / 10) * power(n.owner)));
        }
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos, pos).inflate(64), p -> p.getLookAngle().dot(pos.subtract(p.getEyePosition()).normalize()) > 0.6)) {
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }
        level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), pos.x, pos.y, pos.z, 3, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 6, 3, 3, 3, 0);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 300, 5, 5, 5, 0.4);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5.0f, 0.4f);
        return true;
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        WALLS.removeIf(w -> w.level == level && tickWall(w, now));
        CHANNELS.removeIf(c -> c.level == level && tickChannel(c, now));
        DOOMS.removeIf(d -> d.level == level && tickDoom(d, now));
        NOVAS.removeIf(n -> n.level == level && tickNova(n, now));
        if (now % 5 == 0) {
            for (ServerPlayer p : level.players()) {
                if (active(SHIELD_UNTIL, p)) level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 6, 0.6, 0.8, 0.6, 0.1);
                if (active(REFLECT_UNTIL, p)) level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 4, 0.7, 0.9, 0.7, 0.01);
            }
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        DOOMS.forEach(d -> d.core.discard());
        DOOMS.clear(); WALLS.clear(); CHANNELS.clear(); NOVAS.clear();
    }
}
