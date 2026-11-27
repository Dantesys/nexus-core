package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.items.PilulaItem;

import java.util.*;

/** Efeitos da Cultry (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class CultryEffects {
    private CultryEffects() {}

    public static final int MEDITATE_MANA = 10, MEDITATE_COOLDOWN = 100;
    public static final int FLAMES_MANA = 80, FLAMES_COOLDOWN = 1200, FIELD_TICKS = 200;
    public static final int PALM_MANA = 90, PALM_COOLDOWN = 1800;
    public static final int TRIBULATION_MANA = 400, TRIBULATION_COOLDOWN = 18000, IMMORTAL_TICKS = 1200;
    private static final String PILLS = "NexusPilulas";
    /** Teto de pilulas por tipo em cada grau (acumulado). */
    private static final int[] CAP = {3, 7, 12};
    private static final double[] BONUS = {2.0, 0.5, 1.0, 0.01};
    private static final Identifier[] PILL_IDS = {id("pilula_vida"), id("pilula_forca"), id("pilula_defesa"), id("pilula_velocidade")};
    private static final Identifier[] IMMORTAL_IDS = {id("imortal_vida"), id("imortal_dano"), id("imortal_armadura"), id("imortal_velocidade")};

    private static Identifier id(String p) { return Identifier.fromNamespaceAndPath(Nexus.MODID, "cultry_" + p); }
    private static CultryItem item() { return ModItems.CULTRY.get(); }
    private static boolean wielding(Player p) { return p.getMainHandItem().is(item()); }

    // ---------- Nv 1 — Caldeirao do Refino ----------

    @SubscribeEvent
    public static void burn(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().isDirect() && wielding(p) && item().isUnlocked(p, 0)) {
            event.getEntity().igniteForSeconds(3);
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || !wielding(p) || !item().isUnlocked(p, 0)) return;
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player || RelicMinions.isMinion(victim)) return;
        event.getDrops().clear();
        dropPill(victim, 1);
    }

    private static void dropPill(LivingEntity at, int grade) {
        ServerLevel level = (ServerLevel) at.level();
        level.addFreshEntity(new ItemEntity(level, at.getX(), at.getY() + 0.5, at.getZ(), PilulaItem.create(level.getRandom().nextInt(4), grade)));
        level.sendParticles(ParticleTypes.WAX_ON, at.getX(), at.getY() + 0.5, at.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
    }

    private static CompoundTag pills(Player p) { return p.getPersistentData().getCompoundOrEmpty(PILLS); }

    public static void eatPill(ServerPlayer player, int type, int grade) {
        CompoundTag data = pills(player).copy();
        int maxGrade = data.getIntOr("grau", 0);
        if (grade > maxGrade + 1) { RelicUtil.say(player, "Coma antes uma pílula do grau anterior."); return; }
        String key = PilulaItem.TYPES[type];
        int count = data.getIntOr(key, 0);
        if (count >= CAP[grade - 1]) { RelicUtil.say(player, "Seu corpo já absorveu o máximo deste grau."); return; }
        data.putInt(key, count + 1);
        data.putInt("grau", Math.max(maxGrade, grade));
        player.getPersistentData().put(PILLS, data);
        applyPills(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    public static boolean ateSuperior(Player p) { return pills(p).getIntOr("grau", 0) >= 3; }

    private static void applyPills(Player p) {
        CompoundTag data = pills(p);
        var attrs = List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.MOVEMENT_SPEED);
        for (int i = 0; i < 4; i++) {
            var a = p.getAttribute(attrs.get(i));
            int n = data.getIntOr(PilulaItem.TYPES[i], 0);
            if (a == null) continue;
            if (n == 0) a.removeModifier(PILL_IDS[i]);
            else a.addOrUpdateTransientModifier(new AttributeModifier(PILL_IDS[i], n * BONUS[i], i == 3 ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { applyPills(event.getEntity()); }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { applyPills(event.getEntity()); }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        CompoundTag from = event.getOriginal().getPersistentData();
        if (from.contains(PILLS)) event.getEntity().getPersistentData().put(PILLS, from.getCompoundOrEmpty(PILLS).copy());
    }

    private static final class Refinery extends ChestMenu {
        private final SimpleContainer container;
        Refinery(int id, Player player, SimpleContainer container) { super(MenuType.GENERIC_9x1, id, player.getInventory(), container, 1); this.container = container; }
        @Override public void removed(Player player) {
            super.removed(player);
            if (!(player instanceof ServerPlayer sp)) return;
            Map<String, Integer> pool = new HashMap<>();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack s = container.getItem(i);
                if (s.isEmpty()) continue;
                if (s.is(ModItems.PILULA.get()) && PilulaItem.grade(s) < 3) pool.merge(PilulaItem.type(s) + ":" + PilulaItem.grade(s), s.getCount(), Integer::sum);
                else RelicUtil.giveOrDrop(sp, s);
                container.setItem(i, ItemStack.EMPTY);
            }
            int made = 0;
            for (var e : pool.entrySet()) {
                String[] k = e.getKey().split(":");
                int type = Integer.parseInt(k[0]), grade = Integer.parseInt(k[1]);
                int upgraded = e.getValue() / 3, left = e.getValue() % 3;
                for (int i = 0; i < upgraded; i++) RelicUtil.giveOrDrop(sp, PilulaItem.create(type, grade + 1));
                if (left > 0) { ItemStack rest = PilulaItem.create(type, grade); rest.setCount(left); RelicUtil.giveOrDrop(sp, rest); }
                made += upgraded;
            }
            if (made > 0) {
                sp.level().playSound(null, sp.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 0.8f);
                RelicUtil.say(sp, made + " pílula(s) refinada(s).");
            }
        }
    }

    public static void openRefinery(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(9);
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new Refinery(id, p, container), Component.literal("Caldeirão do Refino (3 iguais → 1 melhor)")));
    }

    // ---------- Nv 5 — Meditacao ----------

    private static final Set<UUID> MEDITATING = new HashSet<>();

    public static boolean meditate(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (!player.isShiftKeyDown() || player.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            RelicUtil.say(player, "Fique parado e agachado para meditar.");
            return false;
        }
        MEDITATING.add(player.getUUID());
        RelicUtil.say(player, "Meditando...");
        return true;
    }

    @SubscribeEvent
    public static void interrupt(LivingDamageEvent.Post event) {
        if (MEDITATING.remove(event.getEntity().getUUID()) && event.getEntity() instanceof ServerPlayer p) RelicUtil.say(p, "Meditação interrompida.");
    }

    @SubscribeEvent
    public static void xp(PlayerXpEvent.PickupXp event) {
        if (MEDITATING.contains(event.getEntity().getUUID())) event.getOrb().setValue(event.getOrb().getValue() * 2);
    }

    // ---------- Nv 10 — Chamas do Caldeirao ----------

    private static final class Thrown {
        final ServerPlayer owner; final Display.ItemDisplay display; Vec3 pos, vel; long landedAt = -1; final double power;
        Thrown(ServerPlayer o, Display.ItemDisplay d, Vec3 p, Vec3 v, double pw) { owner = o; display = d; pos = p; vel = v; power = pw; }
    }
    private static final Map<UUID, Thrown> THROWN = new HashMap<>();

    public static boolean thrown(Player p) { return THROWN.containsKey(p.getUUID()); }

    public static boolean throwCauldron(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        if (thrown(player)) return false;
        Vec3 start = player.getEyePosition().subtract(0, 0.3, 0);
        Display.ItemDisplay display = Displays.item((ServerLevel) player.level(), stack, start, 1.2f);
        THROWN.put(player.getUUID(), new Thrown(player, display, start, player.getLookAngle().scale(1.2), context.powerMultiplier()));
        item().refreshModelData(player, stack);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    private static boolean tickThrown(Thrown t, ServerLevel level, long now) {
        if (t.landedAt < 0) {
            Vec3 next = t.pos.add(t.vel);
            var hit = level.clip(new net.minecraft.world.level.ClipContext(t.pos, next, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, t.owner));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS || t.pos.distanceTo(t.owner.position()) > 40) {
                t.pos = hit.getLocation();
                t.landedAt = now;
                level.playSound(null, BlockPos.containing(t.pos), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.6f);
            } else {
                t.pos = next;
                t.vel = t.vel.add(0, -0.05, 0);
            }
            t.display.teleportTo(t.pos.x, t.pos.y, t.pos.z);
            return false;
        }
        long age = now - t.landedAt;
        if (age >= FIELD_TICKS) { endThrown(t); return true; }
        double radius = 5 + 2 * (age / 40);
        Vec3 c = t.pos;
        if (now % 3 == 0) for (int i = 0; i < 10; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2, r = level.getRandom().nextDouble() * radius;
            level.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.01);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius, 4, radius), e -> RelicTargets.canHit(t.owner, e) && e.position().distanceTo(c) <= radius)) {
            double d = e.position().distanceTo(c);
            Vec3 to = c.subtract(e.position());
            double strength = e instanceof Player || RelicUtil.isBoss(e) ? 0.12 : 0.3;
            if (d > 0.5) RelicUtil.push(e, e.getDeltaMovement().scale(0.5).add(to.normalize().scale(strength)));
            if (d <= 1.5 && e instanceof Mob && !RelicUtil.isBoss(e) && !RelicMinions.isMinion(e)) {
                dropPill(e, 1);
                e.discard();
                continue;
            }
            if (now % 20 == 0) {
                float dmg = (float) ((d <= 1.5 ? 6 : 2) * t.power);
                e.igniteForSeconds(3);
                e.hurtServer(level, RelicUtil.source(level, RelicUtil.damageType("cultry_queimacao"), t.owner), RelicUtil.reduced(e, dmg));
                if (!e.isAlive() && d <= 1.5) dropPill(e, 3);
            }
        }
        return false;
    }

    private static void endThrown(Thrown t) {
        t.display.discard();
        THROWN.remove(t.owner.getUUID());
        ItemStack relic = item().findIn(t.owner);
        if (!relic.isEmpty()) item().refreshModelData(t.owner, relic);
    }

    // ---------- Nv 15 — Palma da Lotus ----------

    private record Palm(ServerPlayer owner, Vec3 center, long start, double power) {}
    private static final List<Palm> PALMS = new ArrayList<>();

    public static boolean lotusPalm(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        PALMS.add(new Palm(player, player.position(), player.level().getGameTime(), context.powerMultiplier()));
        return true;
    }

    private static boolean tickPalm(Palm p, ServerLevel level, long now) {
        long age = now - p.start;
        if (age > 70) return true;
        if (age % 10 != 0) return false;
        int pulse = (int) (age / 10) + 1;
        double outer = pulse * 2, inner = outer - 2;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.center, p.center).inflate(outer, 3, outer), e -> RelicTargets.canHit(p.owner, e))) {
            double d = e.position().distanceTo(p.center);
            if (d > outer || d <= inner) continue;
            e.hurtServer(level, level.damageSources().playerAttack(p.owner), (float) (4 * p.power));
            Vec3 away = e.position().subtract(p.center).multiply(1, 0, 1).normalize();
            RelicUtil.push(e, new Vec3(away.x * 1.2, 0.4, away.z * 1.2));
        }
        org.dantesys.nexus.entity.PalmaLotus.spawn(level, p.center.x, p.center.y, p.center.z, p.owner.getYRot() + pulse * 45, 3.0F + pulse * 0.6F);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            level.sendParticles(ParticleTypes.WAX_ON, p.center.x + Math.cos(a) * outer, p.center.y + 0.3, p.center.z + Math.sin(a) * outer, 1, 0, 0.1, 0, 0);
            if (i % 3 == 0) level.sendParticles(org.dantesys.nexus.particle.ModParticles.PETALA_LOTUS.get(), p.center.x + Math.cos(a) * outer, p.center.y + 1.5, p.center.z + Math.sin(a) * outer, 1, 0.2, 0.2, 0.2, 0);
        }
        level.sendParticles(ParticleTypes.END_ROD, p.center.x, p.center.y + 4, p.center.z, 20, 1, 1, 1, 0.05);
        level.playSound(null, BlockPos.containing(p.center), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0f, 1.2f);
        return false;
    }

    // ---------- Nv 20 — Tribulacao Celestial ----------

    private static final class Trial { final ServerPlayer owner; final long start; Trial(ServerPlayer o, long s) { owner = o; start = s; } }
    private static final List<Trial> TRIALS = new ArrayList<>();
    private static final Map<UUID, Long> IMMORTAL = new HashMap<>();

    public static boolean immortal(Player p) { Long u = IMMORTAL.get(p.getUUID()); return u != null && u > p.level().getGameTime(); }

    public static boolean canTribulation(ServerPlayer player, ItemStack stack) {
        return player.experienceLevel >= 100 && player.getHealth() >= player.getMaxHealth() && player.getFoodData().getFoodLevel() >= 20 && ateSuperior(player);
    }

    public static boolean tribulation(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        player.giveExperienceLevels(-player.experienceLevel / 2);
        TRIALS.add(new Trial(player, player.level().getGameTime()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.0f, 0.6f);
        RelicUtil.say(player, "A tribulação desceu!");
        return true;
    }

    private static boolean tickTrial(Trial t, ServerLevel level, long now) {
        long age = now - t.start;
        if (!t.owner.isAlive()) { RelicUtil.say(t.owner, "Você não suportou a tribulação."); return true; }
        if (age >= 200) {
            IMMORTAL.put(t.owner.getUUID(), now + IMMORTAL_TICKS);
            var attrs = List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.MOVEMENT_SPEED);
            for (int i = 0; i < 4; i++) {
                var a = t.owner.getAttribute(attrs.get(i));
                if (a != null) a.addOrUpdateTransientModifier(new AttributeModifier(IMMORTAL_IDS[i], i == 3 ? 0.5 : 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            t.owner.heal(t.owner.getMaxHealth());
            if (!t.owner.isCreative() && !t.owner.isSpectator()) { t.owner.getAbilities().mayfly = true; t.owner.onUpdateAbilities(); }
            RelicUtil.say(t.owner, "Imortal!");
            return true;
        }
        if (age % 20 == 0) {
            List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, t.owner.getBoundingBox().inflate(15), e -> RelicTargets.canHit(t.owner, e) && e.distanceTo(t.owner) <= 15);
            if (!foes.isEmpty()) RelicUtil.lightningStrike(level, foes.get(level.getRandom().nextInt(foes.size())), t.owner, 8);
        }
        if (age == 60 || age == 120 || age == 180) RelicUtil.lightningStrike(level, t.owner, null, 6);
        return false;
    }

    private static void endImmortal(ServerPlayer p) {
        if (IMMORTAL.remove(p.getUUID()) == null) return;
        var attrs = List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR, Attributes.MOVEMENT_SPEED);
        for (int i = 0; i < 4; i++) { var a = p.getAttribute(attrs.get(i)); if (a != null) a.removeModifier(IMMORTAL_IDS[i]); }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false; p.getAbilities().flying = false; p.onUpdateAbilities();
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
        }
    }

    @SubscribeEvent
    public static void noLightning(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && immortal(p) && event.getSource().is(DamageTypes.LIGHTNING_BOLT)) event.setCanceled(true);
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        if (MEDITATING.contains(player.getUUID())) {
            if (!player.isShiftKeyDown() || player.getDeltaMovement().horizontalDistanceSqr() > 0.003) {
                MEDITATING.remove(player.getUUID());
                RelicUtil.say(player, "Meditação encerrada.");
            } else if (now % 20 == 0) {
                player.heal(1);
                player.getFoodData().eat(1, 0.5f);
                player.giveExperiencePoints(2);
                if (now % 100 == 0) item().addMasteryXp(player, 1);
                ((ServerLevel) player.level()).sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 15, 0.5, 0.6, 0.5, 0.3);
            }
        }
        Long imm = IMMORTAL.get(player.getUUID());
        if (imm != null) {
            if (now >= imm) endImmortal(player);
            else if (now % 5 == 0) ((ServerLevel) player.level()).sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1, player.getZ(), 6, 0.4, 0.7, 0.4, 0.05);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        for (Thrown t : new ArrayList<>(THROWN.values())) if (t.owner.level() == level) tickThrown(t, level, now);
        PALMS.removeIf(p -> p.owner.level() == level && tickPalm(p, level, now));
        TRIALS.removeIf(t -> t.owner.level() == level && tickTrial(t, level, now));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        MEDITATING.remove(event.getEntity().getUUID());
        Thrown t = THROWN.get(event.getEntity().getUUID());
        if (t != null) endThrown(t);
        if (event.getEntity() instanceof ServerPlayer p) endImmortal(p);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        new ArrayList<>(THROWN.values()).forEach(CultryEffects::endThrown);
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) endImmortal(p);
    }
}
