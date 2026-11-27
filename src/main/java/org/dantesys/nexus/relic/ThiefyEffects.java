package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Thiefy (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class ThiefyEffects {
    private ThiefyEffects() {}

    public static final int STEAL_INTERVAL = 100;
    public static final int SNEAK_MANA = 60, SNEAK_COOLDOWN = 3600, SNEAK_TICKS = 100;
    public static final int CLONE_MANA = 80, CLONE_COOLDOWN = 6000, CLONE_TICKS = 400, HIDE_TICKS = 200;
    public static final int SILENCE_MANA = 60, SILENCE_COOLDOWN = 1200, SILENCE_TICKS = 6000;
    public static final int HEIST_MANA = 300, HEIST_COOLDOWN = 18000;
    public static final String SILENCE_TAG = "ThiefySilencio";
    private static final String CLONE_TAG = "NexusCloneThiefy";

    private static ThiefyItem item() { return ModItems.THIEFY.get(); }
    private static boolean wielding(Player p) { return p.getMainHandItem().is(item()); }

    private static String describe(ItemStack s) { return s.getCount() + "x " + s.getHoverName().getString(); }

    private static boolean noArmor(Player p) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = p.getItemBySlot(slot);
            if (!s.isEmpty() && !RelicUtil.isRelic(s)) return false;
        }
        return true;
    }

    // ---------- Nv 1 — Roubo ----------

    private static final Map<String, Long> LAST = new HashMap<>();

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer thief) || !wielding(thief) || !item().isUnlocked(thief, 0)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        long now = thief.level().getGameTime();
        String key = thief.getUUID() + ":" + target.getUUID();
        if (LAST.getOrDefault(key, 0L) > now) return;
        LAST.put(key, now + STEAL_INTERVAL);
        double chance = 0.10 + 0.20 * Math.clamp(item().masteryLevel(thief) - 1, 0, 19) / 19.0;
        if (thief.getRandom().nextDouble() >= chance || thief.getInventory().getFreeSlot() < 0) return;
        if (target instanceof Player victim) {
            var inv = victim.getInventory();
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < 36; i++) if (!inv.getItem(i).isEmpty() && !RelicUtil.isRelic(inv.getItem(i))) slots.add(i);
            if (slots.isEmpty()) return;
            int slot = slots.get(thief.getRandom().nextInt(slots.size()));
            ItemStack loot = inv.getItem(slot);
            inv.setItem(slot, RelicUtil.note("Roubado por " + thief.getName().getString() + ": " + describe(loot)));
            thief.getInventory().add(loot);
        } else if (target instanceof Mob mob) {
            ItemStack held = mob.getMainHandItem();
            if (held.isEmpty() || RelicUtil.isRelic(held)) return;
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            thief.getInventory().add(held);
        } else return;
        thief.level().playSound(null, thief.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.6f);
        item().addMasteryXp(thief, 2);
    }

    // ---------- Nv 5 — Na Surdina ----------

    private static Player lookedPlayer(ServerPlayer thief) {
        LivingEntity t = RelicUtil.targetEntity(thief, 3, e -> e instanceof Player && e != thief);
        return t instanceof Player p ? p : null;
    }

    public static boolean canSneakSteal(ServerPlayer player, ItemStack stack) { return lookedPlayer(player) != null; }

    public static boolean sneakSteal(ServerPlayer thief, ItemStack stack, RelicAbilityContext context) {
        Player victim = lookedPlayer(thief);
        if (victim == null) return false;
        var inv = victim.getInventory();
        List<ItemStack> icons = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            icons.add(s.isEmpty() || RelicUtil.isRelic(s) ? ItemStack.EMPTY : s.copy());
        }
        long until = thief.level().getGameTime() + SNEAK_TICKS;
        IconMenu.open(thief, Component.literal("Inventário de ").append(victim.getName()), icons, slot -> {
            ItemStack real = inv.getItem(slot);
            if (real.isEmpty() || RelicUtil.isRelic(real) || !ItemStack.isSameItemSameComponents(real, icons.get(slot))) return;
            inv.setItem(slot, RelicUtil.note("Roubado por " + thief.getName().getString() + ": " + describe(real)));
            RelicUtil.giveOrDrop(thief, real);
            RelicUtil.say(thief, "Roubou " + describe(real) + ".");
        }, p -> p.level().getGameTime() < until && victim.isAlive() && victim.distanceTo(p) <= 4);
        return true;
    }

    // ---------- Nv 10 — Clone ----------

    private record Clone(Mannequin body, ServerPlayer owner, long until, Vec3 dir) {}
    private static final List<Clone> CLONES = new ArrayList<>();
    private static final Map<UUID, Long> HIDDEN = new HashMap<>();

    public static boolean clone(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Mannequin body = Mannequin.create(EntityTypes.MANNEQUIN, level);
        if (body == null) return false;
        CompoundTag tag = new CompoundTag();
        ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(player.getGameProfile())).result().ifPresent(t -> tag.put("profile", t));
        body.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        body.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        for (EquipmentSlot slot : EquipmentSlot.values()) body.setItemSlot(slot, player.getItemBySlot(slot).copy());
        var health = body.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(Math.max(2, player.getMaxHealth() / 2));
        body.setHealth(body.getMaxHealth());
        body.setCustomName(player.getName());
        body.getPersistentData().putBoolean(CLONE_TAG, true);
        level.addFreshEntity(body);
        Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
        CLONES.add(new Clone(body, player, level.getGameTime() + CLONE_TICKS, dir));
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), m -> m.getTarget() == player)) mob.setTarget(body);
        hide(player, level.getGameTime() + HIDE_TICKS);
        return true;
    }

    private static void hide(ServerPlayer player, long until) {
        HIDDEN.put(player.getUUID(), until);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) (until - player.level().getGameTime()), 0, false, false, false));
        PrivateVisuals.clearGlowOn(player);
        RelicUtil.hideEquipment(player);
    }

    private static void unhide(ServerPlayer player) {
        if (HIDDEN.remove(player.getUUID()) == null) return;
        player.removeEffect(MobEffects.INVISIBILITY);
        RelicUtil.showEquipment(player);
    }

    @SubscribeEvent
    public static void attackBreaksHide(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) unhide(p);
    }

    @SubscribeEvent
    public static void cloneDied(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mannequin m) || !m.getPersistentData().getBooleanOr(CLONE_TAG, false)) return;
        ServerLevel level = (ServerLevel) m.level();
        level.addFreshEntity(new ItemEntity(level, m.getX(), m.getY() + 0.5, m.getZ(), RelicUtil.note("Enganado! — Thiefy")));
        level.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 1, m.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
    }

    // ---------- Nv 15 — Silencio ----------

    private static final Map<UUID, Long> SILENT = new HashMap<>();

    public static boolean silenced(net.minecraft.world.entity.Entity e) {
        Long until = SILENT.get(e.getUUID());
        return until != null && until > e.level().getGameTime();
    }

    public static boolean silence(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        SILENT.put(player.getUUID(), player.level().getGameTime() + SILENCE_TICKS);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean(SILENCE_TAG, true));
        RelicUtil.say(player, "Silêncio por 5 minutos.");
        return true;
    }

    private static void endSilence(ServerPlayer player) {
        if (SILENT.remove(player.getUUID()) == null) return;
        ItemStack stack = item().findIn(player);
        if (stack.isEmpty()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(SILENCE_TAG));
        item().startCooldown(player, stack, 3, SILENCE_COOLDOWN);
        RelicUtil.say(player, "O silêncio acabou.");
    }

    @SubscribeEvent
    public static void sound(PlayLevelSoundEvent.AtEntity event) {
        if (silenced(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void soundAt(PlayLevelSoundEvent.AtPosition event) {
        if (SILENT.isEmpty() || event.getLevel().isClientSide()) return;
        Vec3 pos = event.getPosition();
        for (Player p : event.getLevel().players()) {
            if (silenced(p) && p.position().distanceToSqr(pos) < 2.25 * 2.25) { event.setCanceled(true); return; }
        }
    }

    @SubscribeEvent
    public static void vibration(VanillaGameEvent event) {
        if (event.getCause() != null && silenced(event.getCause())) event.setCanceled(true);
    }

    // ---------- Nv 20 — O Grande Roubo ----------

    public static boolean canHeist(ServerPlayer player, ItemStack stack) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && !RelicUtil.isRelic(s)) return false;
        }
        return true;
    }

    public static boolean heist(ServerPlayer thief, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) thief.level();
        AABB area = thief.getBoundingBox().inflate(10);
        List<Object> targets = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();
        for (Player p : level.getEntitiesOfClass(Player.class, area, p -> p != thief && !p.isSpectator())) {
            targets.add(p);
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(p.getGameProfile()));
            head.set(DataComponents.CUSTOM_NAME, p.getName());
            icons.add(head);
        }
        BlockPos min = BlockPos.containing(area.minX, area.minY, area.minZ), max = BlockPos.containing(area.maxX, area.maxY, area.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (targets.size() >= 54) break;
            if (level.getBlockEntity(pos) instanceof Container c && !c.isEmpty()) {
                targets.add(pos.immutable());
                ItemStack icon = new ItemStack(level.getBlockState(pos).getBlock().asItem());
                if (icon.isEmpty()) icon = new ItemStack(Items.CHEST);
                icon.set(DataComponents.CUSTOM_NAME, Component.literal(icon.getHoverName().getString() + " (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"));
                icons.add(icon);
            }
        }
        if (targets.isEmpty()) { RelicUtil.say(thief, "Nada para roubar por perto."); return false; }
        IconMenu.open(thief, Component.literal("O Grande Roubo"), icons, slot -> {
            if (slot >= targets.size()) return;
            Object t = targets.get(slot);
            if (t instanceof Player victim) robPlayer(thief, victim);
            else if (t instanceof BlockPos pos && level.getBlockEntity(pos) instanceof Container c) robContainer(thief, c);
            level.playSound(null, thief.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 0.5f);
        }, p -> true);
        return true;
    }

    private static void take(ServerPlayer thief, ItemStack s) {
        RelicUtil.giveOrDrop(thief, s);
    }

    private static void robPlayer(ServerPlayer thief, Player victim) {
        var inv = victim.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || RelicUtil.isRelic(s)) continue;
            inv.setItem(i, ItemStack.EMPTY);
            EquipmentSlot armorSlot = null;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (victim.getItemBySlot(slot) == s) armorSlot = slot;
            }
            if (armorSlot != null && thief.getItemBySlot(armorSlot).isEmpty()) thief.setItemSlot(armorSlot, s);
            else take(thief, s);
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) {
            ItemStack s = victim.getItemBySlot(slot);
            if (s.isEmpty() || RelicUtil.isRelic(s)) continue;
            victim.setItemSlot(slot, ItemStack.EMPTY);
            if (slot != EquipmentSlot.OFFHAND && thief.getItemBySlot(slot).isEmpty()) thief.setItemSlot(slot, s);
            else take(thief, s);
        }
        victim.getInventory().add(RelicUtil.note("Obrigado pela doação! — Thiefy"));
        RelicUtil.say(thief, "Roubou tudo de " + victim.getName().getString() + "!");
    }

    private static void robContainer(ServerPlayer thief, Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty() || RelicUtil.isRelic(s)) continue;
            c.setItem(i, ItemStack.EMPTY);
            take(thief, s);
        }
        if (c.getContainerSize() > 0 && c.getItem(0).isEmpty()) c.setItem(0, RelicUtil.note("Obrigado pela doação! — Thiefy"));
        c.setChanged();
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        if (now % 20 == 0 && item().isUnlocked(player, 0) && !item().findIn(player).isEmpty() && noArmor(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
        }
        Long hide = HIDDEN.get(player.getUUID());
        if (hide != null) {
            if (now >= hide) unhide(player);
            else if (now % 10 == 0) RelicUtil.hideEquipment(player);
        }
        Long silent = SILENT.get(player.getUUID());
        if (silent != null && now >= silent) endSilence(player);
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || CLONES.isEmpty()) return;
        long now = level.getGameTime();
        CLONES.removeIf(c -> {
            if (c.body.level() != level) return false;
            if (!c.body.isAlive()) return true;
            if (now >= c.until) { c.body.discard(); return true; }
            Vec3 v = c.body.getDeltaMovement();
            c.body.setDeltaMovement(c.dir.x * 0.18, v.y, c.dir.z * 0.18);
            if (c.body.horizontalCollision && c.body.onGround()) c.body.setDeltaMovement(c.body.getDeltaMovement().add(0, 0.42, 0));
            return false;
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { unhide(p); endSilence(p); }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        CLONES.forEach(c -> c.body.discard());
        CLONES.clear();
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) { unhide(p); endSilence(p); }
    }
}
