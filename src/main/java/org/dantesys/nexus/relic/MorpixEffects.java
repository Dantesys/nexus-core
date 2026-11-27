package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Morpix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class MorpixEffects {
    private MorpixEffects() {}

    public static final int MIMIC_MANA = 60, MIMIC_COOLDOWN = 1200;
    public static final int INJECT_MANA = 80, INJECT_COOLDOWN = 3600;
    public static final int TRANSFORM_MANA = 300, TRANSFORM_COOLDOWN = 18000, TRANSFORM_TICKS = 1200;
    public static final String DNA_ID = "MorpixDNA", DNA_NAME = "MorpixDNANome";
    private static final String TAG = "NexusMorpix", FORM_TAG = "NexusFormaMorpix", PET_DNA = "NexusDnaPet";
    private static final Identifier GENERIC_HP = Identifier.fromNamespaceAndPath(Nexus.MODID, "morpix_vida"), GENERIC_DMG = Identifier.fromNamespaceAndPath(Nexus.MODID, "morpix_dano");
    private static final Set<String> KNOWN = Set.of("minecraft:spider", "minecraft:cave_spider", "minecraft:creeper", "minecraft:skeleton", "minecraft:stray",
            "minecraft:blaze", "minecraft:enderman", "minecraft:ghast", "minecraft:zombie", "minecraft:husk", "minecraft:slime", "minecraft:magma_cube",
            "minecraft:squid", "minecraft:glow_squid", "minecraft:chicken", "minecraft:axolotl", "minecraft:dolphin", "minecraft:wither", "minecraft:warden");

    private static MorpixItem item() { return ModItems.MORPIX.get(); }
    private static String id(Entity e) { return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(); }

    /** Normaliza variantes para o poder base. */
    private static String power(String id) {
        return switch (id) {
            case "minecraft:cave_spider" -> "minecraft:spider";
            case "minecraft:stray" -> "minecraft:skeleton";
            case "minecraft:husk" -> "minecraft:zombie";
            case "minecraft:magma_cube" -> "minecraft:slime";
            case "minecraft:glow_squid" -> "minecraft:squid";
            default -> id;
        };
    }

    public static List<String> powers(Player p) {
        String raw = p.getData(ModAttachmentType.MORPH_POWERS);
        return raw.isEmpty() ? new ArrayList<>() : new ArrayList<>(List.of(raw.split(",")));
    }

    private static void setPowers(ServerPlayer p, List<String> list) {
        p.setData(ModAttachmentType.MORPH_POWERS, String.join(",", list));
        applyAttributes(p, list);
        if (!list.contains("minecraft:blaze") && p.getAbilities().mayfly && !p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false; p.getAbilities().flying = false; p.onUpdateAbilities();
        }
    }

    private static void applyAttributes(LivingEntity e, List<String> list) {
        long generic = list.stream().filter(s -> !KNOWN.contains(s)).count();
        var hp = e.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) { if (generic > 0) hp.addOrUpdateTransientModifier(new AttributeModifier(GENERIC_HP, 4 * generic, AttributeModifier.Operation.ADD_VALUE)); else hp.removeModifier(GENERIC_HP); }
        var dmg = e.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) { if (generic > 0) dmg.addOrUpdateTransientModifier(new AttributeModifier(GENERIC_DMG, generic, AttributeModifier.Operation.ADD_VALUE)); else dmg.removeModifier(GENERIC_DMG); }
    }

    // ---------- Nv 1 — DNA ----------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void collect(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown() || !player.getMainHandItem().is(item())) return;
        if (!item().isUnlocked(player, 0) || !(event.getTarget() instanceof LivingEntity target)) return;
        if (RelicUtil.isBoss(target) && item().masteryLevel(player) < 15) { RelicUtil.say(player, "DNA de boss só na maestria 15."); return; }
        ItemStack stack = player.getMainHandItem();
        String dna = target instanceof Player ? "minecraft:player" : power(id(target));
        String name = target instanceof Player ? "Humano" : target.getType().getDescription().getString();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> { t.putString(DNA_ID, dna); t.putString(DNA_NAME, name); });
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.4, 0.3, 0);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.4f);
        RelicUtil.say(player, "DNA guardado: " + name + ".");
        event.setCanceled(true);
    }

    private static String dna(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(DNA_ID, ""); }
    private static void clearDna(ItemStack s) { CustomData.update(DataComponents.CUSTOM_DATA, s, t -> { t.remove(DNA_ID); t.remove(DNA_NAME); }); }
    public static boolean hasDna(ServerPlayer player, ItemStack stack) { return !dna(stack).isEmpty(); }

    // ---------- Nv 5 — Mimetizar / Nv 10 — Combinacao ----------

    public static boolean mimic(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        String dna = dna(stack);
        if (dna.isEmpty()) return false;
        List<String> current = powers(player);
        if (dna.equals("minecraft:player")) current.clear();
        else if (item().isUnlocked(player, 2) && !current.isEmpty()) {
            if (current.contains(dna)) { RelicUtil.say(player, "Esse DNA já está em uso."); return false; }
            current.add(dna);
            while (current.size() > 2) current.remove(0);
        } else current = new ArrayList<>(List.of(dna));
        clearDna(stack);
        setPowers(player, current);
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.6f, 1.6f);
        RelicUtil.say(player, current.isEmpty() ? "De volta à forma humana." : "Poderes: " + String.join(" + ", current.stream().map(MorpixEffects::label).toList()));
        return true;
    }

    private static String label(String id) {
        var key = Identifier.tryParse(id);
        return key == null ? id : BuiltInRegistries.ENTITY_TYPE.getOptional(key).map(t -> t.getDescription().getString()).orElse(id);
    }

    // ---------- Poderes (passivos e o clique direito) ----------

    private static final Map<UUID, Long> ACTION = new HashMap<>();

    public static void usePowers(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (ACTION.getOrDefault(player.getUUID(), 0L) > now) return;
        Form form = FORMS.get(player.getUUID());
        List<String> list = form != null ? List.of(form.type) : powers(player);
        boolean used = false;
        for (String p : list) used |= action(player, p, form != null);
        if (used) ACTION.put(player.getUUID(), now + (form != null ? 20 : 30));
    }

    private static boolean action(ServerPlayer player, String power, boolean strong) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle(), eye = player.getEyePosition();
        switch (power) {
            case "minecraft:creeper" -> {
                float damage = strong ? 14 : 8;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4), e -> RelicTargets.canHit(player, e) && e.distanceTo(player) <= 4)) {
                    e.hurtServer(level, level.damageSources().explosion(player, player), RelicUtil.reduced(e, damage));
                    RelicUtil.push(e, e.position().subtract(player.position()).normalize().scale(1.2).add(0, 0.4, 0));
                }
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
            case "minecraft:skeleton" -> {
                Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3.0F, 1.0F);
                arrow.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.DISALLOWED;
                if (strong) arrow.setBaseDamage(4);
                level.addFreshEntity(arrow);
                level.playSound(null, player.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
            case "minecraft:blaze" -> {
                SmallFireball ball = new SmallFireball(level, player, look);
                ball.setPos(eye.add(look));
                ball.getPersistentData().putBoolean(TAG, true);
                level.addFreshEntity(ball);
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
            case "minecraft:ghast" -> {
                LargeFireball ball = new LargeFireball(level, player, look, 1);
                ball.setPos(eye.add(look.scale(1.5)));
                ball.getPersistentData().putBoolean(TAG, true);
                level.addFreshEntity(ball);
                level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
            case "minecraft:enderman" -> {
                Vec3 start = player.position(), best = start;
                for (double d = 0.5; d <= 8; d += 0.5) {
                    Vec3 c = start.add(look.scale(d));
                    if (!level.noCollision(player, player.getBoundingBox().move(c.subtract(start)))) break;
                    best = c;
                }
                player.teleportTo(best.x, best.y, best.z);
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
            case "minecraft:squid" -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5), e -> RelicTargets.canHit(player, e))) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), player);
                }
                level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1, player.getZ(), 60, 1.5, 1, 1.5, 0.1);
                level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
            case "minecraft:wither" -> {
                WitherSkull skull = new WitherSkull(level, player, look);
                skull.setPos(eye.add(look));
                skull.getPersistentData().putBoolean(TAG, true);
                level.addFreshEntity(skull);
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
            case "minecraft:warden" -> {
                Vec3 end = eye.add(look.scale(15));
                for (int i = 1; i <= 15; i++) { Vec3 p = eye.add(look.scale(i)); level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0); }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1), e -> RelicTargets.canHit(player, e) && e.getBoundingBox().inflate(0.5).clip(eye, end).isPresent())) {
                    e.hurtServer(level, level.damageSources().sonicBoom(player), RelicUtil.reduced(e, 10));
                }
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.0f);
                return true;
            }
            default -> { return false; }
        }
    }

    /** Projeteis dos poderes: sem quebrar nem queimar blocos. */
    @SubscribeEvent
    public static void projectile(ProjectileImpactEvent event) {
        var proj = event.getProjectile();
        if (!proj.getPersistentData().getBooleanOr(TAG, false) || !(proj.level() instanceof ServerLevel level) || !(proj.getOwner() instanceof ServerPlayer owner)) return;
        event.setCanceled(true);
        Vec3 pos = event.getRayTraceResult().getLocation();
        if (proj instanceof LargeFireball) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(3), e -> RelicTargets.canHit(owner, e))) {
                e.hurtServer(level, level.damageSources().fireball((LargeFireball) proj, owner), RelicUtil.reduced(e, 10));
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            level.playSound(null, net.minecraft.core.BlockPos.containing(pos), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
        } else if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity e && RelicTargets.canHit(owner, e)) {
            if (proj instanceof WitherSkull skull) {
                e.hurtServer(level, level.damageSources().witherSkull(skull, owner), RelicUtil.reduced(e, 8));
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1), owner);
            } else {
                e.hurtServer(level, level.damageSources().onFire(), RelicUtil.reduced(e, 5));
                e.igniteForSeconds(5);
            }
        }
        proj.discard();
    }

    private static void passives(ServerPlayer player, List<String> list) {
        for (String p : list) {
            switch (p) {
                case "minecraft:slime" -> player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 1, true, false, true));
                case "minecraft:chicken" -> { if (player.getDeltaMovement().y < -0.1) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false, true)); }
                case "minecraft:squid", "minecraft:axolotl" -> player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false, true));
                case "minecraft:dolphin" -> { if (player.isInWater()) player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false, true)); }
                case "minecraft:blaze" -> {
                    if (!player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) { player.getAbilities().mayfly = true; player.onUpdateAbilities(); }
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, true));
                }
                default -> {}
            }
        }
    }

    @SubscribeEvent
    public static void fall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && event.getSource().is(DamageTypeTags.IS_FALL) && powers(p).contains("minecraft:slime")) event.setCanceled(true);
    }

    /** Zumbi: carne podre sem Fome. */
    @SubscribeEvent
    public static void hunger(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player p && event.getEffectInstance().getEffect().is(MobEffects.HUNGER) && powers(p).contains("minecraft:zombie")) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // ---------- Nv 15 — Cobaia ----------

    private static final Map<UUID, LivingEntity> PATIENT = new HashMap<>();

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown() || !player.getMainHandItem().is(item())) return;
        if (!(event.getTarget() instanceof LivingEntity target) || !RelicTargets.isAlly(player, target) || target == player || target.distanceTo(player) > 3) return;
        PATIENT.put(player.getUUID(), target);
        item().activate(player, player.getMainHandItem(), 3);
        PATIENT.remove(player.getUUID());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static boolean canInject(ServerPlayer player, ItemStack stack) {
        String dna = dna(stack);
        return PATIENT.containsKey(player.getUUID()) && !dna.isEmpty() && !dna.equals("minecraft:player")
                && !dna.equals("minecraft:wither") && !dna.equals("minecraft:warden");
    }

    public static boolean inject(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        LivingEntity target = PATIENT.get(player.getUUID());
        String dna = dna(stack);
        if (target == null || dna.isEmpty()) return false;
        clearDna(stack);
        if (target instanceof ServerPlayer ally) setPowers(ally, new ArrayList<>(List.of(dna)));
        else {
            target.getPersistentData().putString(PET_DNA, dna);
            applyAttributes(target, List.of(dna));
        }
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        RelicUtil.say(player, "Cobaia recebeu o DNA de " + label(dna) + ".");
        return true;
    }

    // ---------- Nv 20 — Transformacao ----------

    private static final class Form { final String type; final Mob body; final long until; Form(String t, Mob b, long u) { type = t; body = b; until = u; } }
    private static final Map<UUID, Form> FORMS = new HashMap<>();

    public static boolean transformed(Player p) { return FORMS.containsKey(p.getUUID()); }

    public static boolean canTransform(ServerPlayer player, ItemStack stack) {
        String dna = dna(stack);
        return powers(player).isEmpty() && !dna.isEmpty() && !dna.equals("minecraft:player") && !transformed(player);
    }

    public static boolean transform(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        String dna = dna(stack);
        var key = Identifier.tryParse(dna);
        var type = key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
        ServerLevel level = (ServerLevel) player.level();
        if (type == null || !(type.create(level, EntitySpawnReason.TRIGGERED) instanceof Mob body)) {
            RelicUtil.say(player, "Esse DNA não pode virar uma forma.");
            return false;
        }
        clearDna(stack);
        body.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        body.setNoAi(true);
        body.setSilent(false);
        body.getPersistentData().putBoolean(FORM_TAG, true);
        RelicTargets.setMinion(body, player);
        boolean boss = RelicUtil.isBoss(body);
        var hp = body.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(Math.max(20, hp.getBaseValue() * (boss ? 0.4 : 2.0)));
        body.setHealth(body.getMaxHealth());
        body.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        if (body instanceof net.minecraft.world.entity.boss.wither.WitherBoss w) w.setInvulnerableTicks(0);
        level.addFreshEntity(body);
        FORMS.put(player.getUUID(), new Form(power(dna), body, level.getGameTime() + TRANSFORM_TICKS));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, TRANSFORM_TICKS, 0, false, false, false));
        RelicUtil.hideEquipment(player);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 60, 0.6, 1, 0.6, 0.1);
        RelicUtil.say(player, "Transformação: " + label(dna) + " por 1 minuto.");
        return true;
    }

    private static void endForm(ServerPlayer player) {
        Form form = FORMS.remove(player.getUUID());
        if (form == null) return;
        if (form.body.isAlive()) form.body.discard();
        player.removeEffect(MobEffects.INVISIBILITY);
        RelicUtil.showEquipment(player);
        RelicUtil.say(player, "De volta à forma humana.");
    }

    /** Transformado, o dano vai para a forma; morrer a forma desfaz a transformacao. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void formDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Form form = FORMS.get(player.getUUID());
        if (form == null || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        event.setCanceled(true);
        form.body.hurtServer((ServerLevel) player.level(), event.getSource(), event.getAmount());
    }

    @SubscribeEvent
    public static void formDied(LivingDeathEvent event) {
        if (!event.getEntity().getPersistentData().getBooleanOr(FORM_TAG, false)) return;
        ServerPlayer owner = RelicMinions.owner(event.getEntity());
        if (owner != null) endForm(owner);
    }

    @SubscribeEvent
    public static void noItems(PlayerInteractEvent.RightClickItem event) {
        if (transformed(event.getEntity()) && !event.getItemStack().is(item())) event.setCanceled(true);
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Form form = FORMS.get(player.getUUID());
        if (form != null) {
            if (now >= form.until || !form.body.isAlive() || form.body.level() != player.level()) endForm(player);
            else {
                form.body.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                form.body.setYHeadRot(player.getYHeadRot());
                form.body.setYBodyRot(player.getYRot());
            }
        }
        if (now % 20 == 0) {
            List<String> list = powers(player);
            if (!list.isEmpty()) passives(player, list);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) applyAttributes(p, powers(p));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) endForm(p);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) endForm(p);
    }
}
