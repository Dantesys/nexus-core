package org.dantesys.nexus.relic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.EndermanAngerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModFluids;
import org.dantesys.nexus.entity.DragaoDraconix;
import org.dantesys.nexus.entity.ModEntities;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.effects.ModEffects;
import org.dantesys.nexus.items.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Efeitos da Draconix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class DraconixEffects {
    private DraconixEffects() {}

    public static final int BREATH_MANA = 50, BREATH_COOLDOWN = 400, BREATH_RANGE = 8;
    private static final String SCALES = "NexusPeleEscamosa";
    private static final Identifier ARMOR = id("armadura"), TOUGH = id("dureza"), KB = id("repulsao"), WINGS = id("asas");

    private static Identifier id(String p) { return Identifier.fromNamespaceAndPath(Nexus.MODID, "draconix_" + p); }
    private static DraconixItem item() { return ModItems.DRACONIX.get(); }
    /** Passivas da relíquia: desligadas enquanto o dragão está chamado (viram habilidades dele). */
    private static boolean carrier(Player p, int slot) { return !item().findIn(p).isEmpty() && item().isUnlocked(p, slot) && !dragonOut(p); }

    public static boolean scales(Player p) { return p.getPersistentData().getBooleanOr(SCALES, false) && carrier(p, 2); }

    // ---------- Nv 1 — Olhar Draconico ----------

    @SubscribeEvent
    public static void enderman(EndermanAngerEvent event) {
        if (carrier(event.getPlayer(), 0)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void stare(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player holder) || !(event.getSource().getEntity() instanceof Player attacker) || !carrier(holder, 0)) return;
        Vec3 to = holder.getEyePosition().subtract(attacker.getEyePosition()).normalize();
        if (attacker.getLookAngle().dot(to) > 0.9) event.setAmount(event.getAmount() * 0.98f);
    }

    // ---------- Nv 5 — Baforada ----------

    public static boolean breath(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        DragaoDraconix dragon = dragon(player);
        if (dragon != null) { dragon.giantBreath(player, context.powerMultiplier()); return true; }
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        cone(level, player, eye, look, BREATH_RANGE, 30, (float) (6 * context.powerMultiplier()), 100, null);
        for (int i = 0; i < 60; i++) {
            double d = level.getRandom().nextDouble() * BREATH_RANGE;
            Vec3 spread = new Vec3(level.getRandom().nextGaussian(), level.getRandom().nextGaussian(), level.getRandom().nextGaussian()).scale(d * 0.25);
            Vec3 p = eye.add(look.scale(d)).add(spread);
            level.sendParticles(net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), p.x, p.y, p.z, 1, 0, 0, 0, 0.01);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    /** Dano em cone com fogo draconico. */
    public static void cone(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 look, double range, double halfAngle, float damage, int burnTicks, @org.jspecify.annotations.Nullable Entity exclude) {
        double minDot = Math.cos(Math.toRadians(halfAngle));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(origin, origin).inflate(range), e -> e != exclude && RelicTargets.canHit(player, e))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(origin);
            if (to.length() > range || look.dot(to.normalize()) < minDot) continue;
            e.hurtServer(level, RelicUtil.source(level, RelicUtil.damageType("fogo_draconico"), player), RelicUtil.reduced(e, damage));
            e.addEffect(new MobEffectInstance(ModEffects.FOGO_DRACONICO, burnTicks, 0), player);
        }
    }

    // ---------- Nv 10 — Pele Escamosa / Nv 15 — Asas ----------

    public static void toggleScales(ServerPlayer player) {
        if (dragonOut(player)) { RelicUtil.say(player, "O dragão está com a sua pele. Guarde-o primeiro."); return; }
        boolean on = !player.getPersistentData().getBooleanOr(SCALES, false);
        player.getPersistentData().putBoolean(SCALES, on);
        applyScales(player, on);
        RelicUtil.say(player, on ? "Pele Escamosa: ligada." : "Pele Escamosa: desligada.");
        player.level().playSound(null, player.blockPosition(), on ? SoundEvents.ARMOR_EQUIP_NETHERITE.value() : SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
        ItemStack egg = item().findIn(player);
        if (!egg.isEmpty()) item().refreshModelData(player, egg);
    }

    private static void set(Player p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, Identifier id, double v) {
        var a = p.getAttribute(attr);
        if (a == null) return;
        if (v == 0) a.removeModifier(id);
        else a.addOrUpdateTransientModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void applyScales(ServerPlayer p, boolean on) {
        set(p, Attributes.ARMOR, ARMOR, on ? 25 : 0);
        set(p, Attributes.ARMOR_TOUGHNESS, TOUGH, on ? 15 : 0);
        set(p, Attributes.KNOCKBACK_RESISTANCE, KB, on ? 0.5 : 0);
        set(p, NeoForgeMod.GLIDING_FLIGHT, WINGS, on && item().isUnlocked(p, 3) ? 1 : 0);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void fireproof(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && scales(p) && PhoenixyEffects.isFire(event.getSource())) {
            event.setCanceled(true);
            p.clearFire();
        }
    }

    private static final Map<UUID, Long> BOOST = new HashMap<>();

    public static void wingBoost(ServerPlayer player) {
        if (!scales(player)) return;
        long now = player.level().getGameTime();
        if (BOOST.getOrDefault(player.getUUID(), 0L) > now) return;
        var magic = player.getData(ModAttachmentType.MAGIC_STATS);
        int cost = Math.max(1, magic.getMaxMana() / 20);
        if (!player.hasInfiniteMaterials()) {
            if (magic.getMana() < cost) { RelicUtil.say(player, "Mana insuficiente (" + cost + ")."); return; }
            magic.useMana(cost);
            player.syncData(ModAttachmentType.MAGIC_STATS);
        }
        player.level().addFreshEntity(new FireworkRocketEntity(player.level(), new ItemStack(Items.FIREWORK_ROCKET), player));
        BOOST.put(player.getUUID(), now + 40);
    }

    // ---------- Nv 20 — O Verdadeiro Dragao ----------

    public static final String HATCHED = "NexusDragaoChocado";
    public static final int SWAP_COOLDOWN = 1200, DEATH_COOLDOWN = 36000, ROAR_COOLDOWN = 600;
    private static final Identifier KILL_DRAGON = Identifier.withDefaultNamespace("end/kill_dragon");
    private static final Map<UUID, DragaoDraconix> DRAGONS = new HashMap<>();
    private static final Map<UUID, Long> ROAR = new HashMap<>(), HINT = new HashMap<>();

    public static boolean hatched(ItemStack egg) { return RelicItem.readLong(egg, HATCHED) > 0; }

    public static @org.jspecify.annotations.Nullable DragaoDraconix dragon(Player p) {
        DragaoDraconix d = DRAGONS.get(p.getUUID());
        if (d == null) return null;
        if (d.isRemoved() || !d.isAlive() || d.level() != p.level()) { DRAGONS.remove(p.getUUID()); if (!d.isRemoved()) d.discard(); return null; }
        return d;
    }

    public static boolean dragonOut(Player p) {
        DragaoDraconix d = DRAGONS.get(p.getUUID());
        return d != null && !d.isRemoved() && d.isAlive();
    }

    /** Ativa a final: chama o dragão (ou guarda, se já estiver chamado). */
    public static boolean trueDragon(ServerPlayer player, ItemStack egg, RelicAbilityContext context) {
        DragaoDraconix out = dragon(player);
        if (out != null) {
            out.ejectPassengers();
            out.discard();
            DRAGONS.remove(player.getUUID());
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 1.3f);
            RelicUtil.say(player, "O dragão voltou para o ovo. Seus poderes voltaram.");
            return true;
        }
        if (!hatched(egg)) {
            RelicUtil.say(player, "O ovo ainda não chocou: derrote o Ender Dragon, jogue sopro do dragão na lava e entre na lava roxa sem pele e sem armadura.");
            return false;
        }
        if (player.getPersistentData().getBooleanOr(SCALES, false)) {
            RelicUtil.say(player, "Desligue a Pele Escamosa: o dragão precisa da pele dele.");
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        DragaoDraconix d = new DragaoDraconix(ModEntities.DRAGAO_DRACONIX.get(), level);
        d.setOwner(player);
        boolean airborne = !player.onGround() && (player.isFallFlying() || player.fallDistance > 3 || player.getDeltaMovement().y < -0.5);
        Vec3 at = airborne ? player.position().subtract(0, 1.2, 0) : player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5)).add(0, 0.5, 0);
        d.snapTo(at.x, at.y, at.z, player.getYRot(), 0);
        level.addFreshEntity(d);
        DRAGONS.put(player.getUUID(), d);
        applyScales(player, false);
        if (airborne) {
            player.stopFallFlying();
            player.fallDistance = 0;
            player.startRiding(d, true, true);
        }
        level.sendParticles(net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), at.x, at.y + 1, at.z, 80, 1.2, 0.8, 1.2, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 1.4f);
        RelicUtil.say(player, "O dragão atendeu ao chamado!");
        return true;
    }

    /** Rugido (Olhar Dracônico do dragão): montado, clique direito com o ovo. */
    public static void roar(ServerPlayer player) {
        DragaoDraconix d = dragon(player);
        if (d == null) return;
        long now = player.level().getGameTime();
        Long until = ROAR.get(player.getUUID());
        if (until != null && until > now) { RelicUtil.say(player, "Rugido em recarga: " + (until - now + 19) / 20 + "s"); return; }
        ROAR.put(player.getUUID(), now + ROAR_COOLDOWN);
        ServerLevel level = (ServerLevel) player.level();
        d.openMouth(20);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, d.getBoundingBox().inflate(16), e -> e != d && RelicTargets.canHit(player, e))) {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1), player);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1), player);
            if (e instanceof Mob mob && !RelicUtil.isBoss(mob)) {
                mob.setTarget(null);
                Vec3 away = mob.position().subtract(d.position()).multiply(1, 0, 1).normalize().scale(16);
                mob.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.5);
            }
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, d.getX(), d.getY() + 1.2, d.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, d.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0f, 0.8f);
    }

    public static void dragonDied(ServerPlayer player) {
        DRAGONS.remove(player.getUUID());
        ItemStack egg = item().findIn(player);
        if (!egg.isEmpty()) item().setCooldownTicks(player, egg, 4, DEATH_COOLDOWN);
        RelicUtil.say(player, "Seu dragão caiu e voltou para o ovo. Ele leva 30 minutos para chocar de novo.");
    }

    private static boolean killedEnderDragon(ServerPlayer player) {
        var adv = player.level().getServer().getAdvancements().get(KILL_DRAGON);
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static boolean wearsArmor(Player p) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            if (!p.getItemBySlot(slot).isEmpty()) return true;
        return false;
    }

    /** Chocar: dentro da lava roxa, sem pele e sem armadura, com o Ender Dragon derrotado. */
    private static void tryHatch(ServerPlayer player, ServerLevel level) {
        if (!level.getFluidState(player.blockPosition()).getType().isSame(ModFluids.LAVA_ROXA.get())) return;
        ItemStack egg = item().findIn(player);
        if (egg.isEmpty() || hatched(egg)) return;
        String problem = null;
        if (!item().isUnlocked(player, 4)) problem = "O ovo ainda não está pronto (maestria máxima).";
        else if (!killedEnderDragon(player)) problem = "O ovo só choca para quem derrotou o Ender Dragon.";
        else if (player.getPersistentData().getBooleanOr(SCALES, false)) problem = "Desligue a Pele Escamosa para chocar o ovo.";
        else if (wearsArmor(player)) problem = "Tire a armadura para chocar o ovo.";
        if (problem != null) {
            long now = level.getGameTime();
            if (HINT.getOrDefault(player.getUUID(), 0L) <= now) { RelicUtil.say(player, problem); HINT.put(player.getUUID(), now + 100); }
            return;
        }
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3)))
            if (level.getFluidState(pos).getType().isSame(ModFluids.LAVA_ROXA.get())) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        RelicItem.writeLong(egg, HATCHED, 1);
        level.sendParticles(net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), player.getX(), player.getY() + 1, player.getZ(), 150, 1.5, 1.5, 1.5, 0.05);
        level.playSound(null, center, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 1.6f);
        level.playSound(null, center, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.PLAYERS, 2.0f, 0.8f);
        RelicUtil.say(player, "O ovo chocou! Ative O Verdadeiro Dragão para chamá-lo.");
    }

    /** Sopro do dragão jogado na lava a transforma em lava roxa. */
    @SubscribeEvent
    public static void breathInLava(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ItemEntity drop) || !(drop.level() instanceof ServerLevel level) || !drop.getItem().is(Items.DRAGON_BREATH)) return;
        BlockPos at = drop.blockPosition();
        if (!level.getFluidState(at).is(FluidTags.LAVA)) return;
        int changed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 0, 1))) {
            var fluid = level.getFluidState(pos);
            if (fluid.is(FluidTags.LAVA) && fluid.isSource()) {
                level.setBlock(pos, ModFluids.LAVA_ROXA.get().defaultFluidState().createLegacyBlock(), 3);
                changed++;
            }
        }
        if (changed == 0) return;
        drop.getItem().shrink(1);
        if (drop.getItem().isEmpty()) drop.discard();
        level.sendParticles(net.minecraft.core.particles.PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 40, 0.8, 0.3, 0.8, 0.02);
        level.playSound(null, at, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 0.6f);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        DragaoDraconix d = DRAGONS.remove(event.getEntity().getUUID());
        if (d != null && !d.isRemoved()) d.discard();
        ROAR.remove(event.getEntity().getUUID());
        HINT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        DragaoDraconix d = DRAGONS.remove(event.getEntity().getUUID());
        if (d != null && !d.isRemoved()) d.discard();
    }

    @SubscribeEvent
    public static void riderFall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player p && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL) && p.getVehicle() instanceof DragaoDraconix) event.setCanceled(true);
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().getGameTime() % 10 != 0) return;
        ServerLevel level = (ServerLevel) player.level();
        tryHatch(player, level);
        if (carrier(player, 0)) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8), m -> m instanceof Enemy && !RelicUtil.isBoss(m) && m.distanceTo(player) <= 8)) {
                if (mob.getTarget() == player) mob.setTarget(null);
                Vec3 away = mob.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(10);
                mob.getNavigation().moveTo(mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.3);
            }
        }
        boolean on = scales(player);
        if (player.getPersistentData().getBooleanOr(SCALES, false) != on || player.level().getGameTime() % 20 == 0) applyScales(player, on);
        if (on) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack worn = player.getItemBySlot(slot);
                if (worn.isEmpty()) continue;
                player.setItemSlot(slot, ItemStack.EMPTY);
                RelicUtil.giveOrDrop(player, worn);
                RelicUtil.say(player, "A Pele Escamosa não deixa usar armadura.");
            }
            if (player.isInLava()) player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) applyScales(p, scales(p));
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) applyScales(p, scales(p));
    }
}
