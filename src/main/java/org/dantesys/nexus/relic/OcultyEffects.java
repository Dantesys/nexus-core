package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.TeamColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Oculty (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class OcultyEffects {
    private OcultyEffects() {}

    public static final int STEALTH_MANA_PER_SECOND = 3;
    public static final int MICRO_MANA = 40, MICRO_COOLDOWN = 600, MICRO_TICKS = 200, GROW_TICKS = 40;
    public static final int XRAY_MANA = 40, XRAY_COOLDOWN = 400, XRAY_HALF = 24;
    public static final int PLAN_MANA = 60, PLAN_COOLDOWN = 900, PLAN_TICKS = 120;
    public static final int SPY_MANA = 150, SPY_COOLDOWN = 3600, SPY_TICKS = 600, SPY_MAX_DISTANCE = 128;
    public static final String[] PLAN_NAMES = {"Invisibilidade verdadeira", "Velocidade", "Voo"};
    private static final String PLAN_TAG = "OcultyPlanoF", SPY_TAG = "NexusImaterialSpy";
    private static final Identifier MICRO_SCALE = Identifier.fromNamespaceAndPath(Nexus.MODID, "micro_agente");

    private static final class State {
        boolean stealth; long brokenUntil;
        long microUntil, growUntil;
        long planUntil; int plan = -1;
        long xrayFake;
    }
    private static final Map<UUID, State> STATES = new HashMap<>();

    static {
        PrivateVisuals.HIDDEN = e -> {
            State s = STATES.get(e.getUUID());
            return s != null && s.stealth;
        };
    }

    private static OcultyItem item() { return ModItems.OCULTY.get(); }
    private static State state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), id -> new State()); }
    private static boolean wearing(Player player) { return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof OcultyItem; }

    private static void refresh(ServerPlayer player) {
        ItemStack stack = RelicUtil.find(player, item());
        if (!stack.isEmpty()) item().refreshModelData(player, stack);
    }

    // ---------- Nv 1 — Modo Furtivo ----------

    private static void setStealth(ServerPlayer player, State s, boolean on) {
        if (s.stealth == on) return;
        s.stealth = on;
        if (on) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
            player.removeEffect(MobEffects.GLOWING);
            PrivateVisuals.clearGlowOn(player);
            for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48), m -> m.getTarget() == player)) mob.setTarget(null);
        } else {
            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    @SubscribeEvent
    public static void damage(LivingDamageEvent.Post event) {
        breakStealth(event.getEntity());
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) breakStealth(attacker);
    }

    private static void breakStealth(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        State s = STATES.get(player.getUUID());
        if (s == null || !s.stealth) return;
        setStealth(player, s, false);
        s.brokenUntil = player.level().getGameTime() + 60;
    }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) {
            State s = STATES.get(player.getUUID());
            if (s != null && s.stealth) event.setCanceled(true);
        }
    }

    // ---------- Nv 5 — Micro Agente ----------

    public static boolean micro(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        State s = state(player);
        int ticks = MICRO_TICKS + context.resonance() / 10;
        s.microUntil = player.level().getGameTime() + ticks;
        s.growUntil = 0;
        setScale(player, -0.9375);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, 2));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.0f, 2.0f);
        refresh(player);
        return true;
    }

    private static void setScale(ServerPlayer player, double amount) {
        var scale = player.getAttribute(Attributes.SCALE);
        if (scale == null) return;
        if (amount == 0) scale.removeModifier(MICRO_SCALE);
        else scale.addOrUpdateTransientModifier(new AttributeModifier(MICRO_SCALE, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static int microSecondsLeft(ServerPlayer player) {
        State s = STATES.get(player.getUUID());
        if (s == null) return 0;
        long left = s.microUntil - player.level().getGameTime();
        return left > 0 ? (int) ((left + 19) / 20) : 0;
    }

    // ---------- Nv 10 — X-Ray ----------

    public static boolean xray(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        int ticks = Math.min(400, 200 + context.resonance() / 5);
        int count = 0;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(XRAY_HALF),
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand))) {
            TeamColor color = target instanceof Player ? TeamColor.BLUE : target instanceof Enemy ? TeamColor.RED : TeamColor.GREEN;
            PrivateVisuals.glow(player, target, ticks, color, true);
            count++;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        RelicUtil.say(player, "X-Ray: " + count + " alvos.");
        return true;
    }

    // ---------- Nv 15 — Plano F ----------

    public static int nextPlan(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(PLAN_TAG, 0), 3);
    }

    public static boolean planF(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6),
                e -> RelicTargets.canHit(player, e))) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), player);
            if (target instanceof Mob mob) mob.setTarget(null);
        }
        throwSmokeBomb(player, level);
        State s = state(player);
        int ticks = PLAN_TICKS + context.resonance() / 10;
        int plan = nextPlan(stack);
        s.plan = plan;
        s.planUntil = level.getGameTime() + ticks;
        switch (plan) {
            case 0 -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
                PrivateVisuals.clearGlowOn(player);
                RelicUtil.hideEquipment(player);
            }
            case 1 -> player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 2));
            default -> {
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        int next = level.getRandom().nextInt(3);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(PLAN_TAG, next));
        RelicUtil.say(player, "Plano F: " + PLAN_NAMES[plan] + ". Próximo: " + PLAN_NAMES[next] + ".");
        refresh(player);
        return true;
    }

    private static final String BOMB_TAG = "NexusBombaFumaca";

    /** Bomba de fumaca: cai aos pes do jogador e solta a nuvem ao bater. */
    private static void throwSmokeBomb(ServerPlayer player, ServerLevel level) {
        var bomb = new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(
                level, player.getX(), player.getEyeY() - 0.3, player.getZ(), new ItemStack(org.dantesys.nexus.items.ModItems.BOMBA_FUMACA.get()));
        bomb.setOwner(player);
        Vec3 look = player.getLookAngle();
        bomb.shoot(look.x, -1.0, look.z, 0.4F, 0.0F);
        bomb.getPersistentData().putBoolean(BOMB_TAG, true);
        level.addFreshEntity(bomb);
        level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.5f);
    }

    @SubscribeEvent
    public static void bombImpact(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event) {
        if (!event.getProjectile().getPersistentData().getBooleanOr(BOMB_TAG, false)) return;
        if (!(event.getProjectile().level() instanceof ServerLevel level)) return;
        Vec3 pos = event.getRayTraceResult().getLocation();
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, pos.x, pos.y + 0.5, pos.z, 60, 1.5, 1, 1.5, 0.02);
        level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.3, pos.z, 30, 0.8, 0.4, 0.8, 0.05);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0f, 0.6f);
        event.getProjectile().discard();
        event.setCanceled(true);
    }

    private static void endPlan(ServerPlayer player, State s) {
        if (s.plan == 0) {
            player.removeEffect(MobEffects.INVISIBILITY);
            RelicUtil.showEquipment(player);
        } else if (s.plan == 2 && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
        }
        s.plan = -1;
        s.planUntil = 0;
    }

    // ---------- Nv 20 — Imaterial Spy ----------

    private record Spy(ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot, GameType mode, long until, UUID body) {}
    private static final Map<UUID, Spy> SPIES = new HashMap<>();
    private static final String BODY_TAG = "NexusCorpoEspiao";
    private static final Set<UUID> ORPHAN_BODIES = new HashSet<>();

    /** Corpo de espiao que voltou do disco sem o espiao ativo: some (nao fica NPC parado no mundo). */
    @SubscribeEvent
    public static void bodyJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mannequin body)) return;
        boolean orphan = ORPHAN_BODIES.remove(body.getUUID());
        if (!orphan && body.getPersistentData().getBooleanOr(BODY_TAG, false)) {
            orphan = SPIES.values().stream().noneMatch(s -> body.getUUID().equals(s.body()));
        }
        if (orphan) event.setCanceled(true);
    }

    public static boolean spying(ServerPlayer player) { return SPIES.containsKey(player.getUUID()); }

    private static boolean living(LivingEntity e, ServerPlayer self) {
        return e != self && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof Mannequin) && !e.isSpectator();
    }

    public static boolean canSpy(ServerPlayer player, ItemStack stack) {
        var level = player.level();
        if (!level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5), e -> living(e, player) && e.distanceTo(player) <= 5).isEmpty()) return false;
        return !level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(100), e -> living(e, player) && e.distanceTo(player) <= 100).isEmpty();
    }

    public static boolean spy(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Mannequin body = Mannequin.create(EntityTypes.MANNEQUIN, level);
        UUID bodyId = null;
        if (body != null) {
            CompoundTag tag = new CompoundTag();
            ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(player.getGameProfile())).result().ifPresent(t -> tag.put("profile", t));
            tag.putBoolean("Invulnerable", true);
            body.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            body.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HAND || slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) body.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
            body.setCustomName(player.getName());
            body.getPersistentData().putBoolean(BODY_TAG, true);
            level.addFreshEntity(body);
            bodyId = body.getUUID();
        }
        Spy spy = new Spy(level.dimension(), player.position(), player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer(),
                level.getGameTime() + SPY_TICKS, bodyId);
        SPIES.put(player.getUUID(), spy);
        saveSpy(player, spy);
        player.setGameMode(GameType.SPECTATOR);
        RelicUtil.say(player, "Imaterial Spy por 30s.");
        return true;
    }

    private static void saveSpy(ServerPlayer player, Spy spy) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dim", spy.dim().identifier().toString());
        tag.putDouble("x", spy.pos().x); tag.putDouble("y", spy.pos().y); tag.putDouble("z", spy.pos().z);
        tag.putFloat("yRot", spy.yRot()); tag.putFloat("xRot", spy.xRot());
        tag.putInt("mode", spy.mode().getId());
        if (spy.body() != null) tag.putString("body", spy.body().toString());
        player.getPersistentData().put(SPY_TAG, tag);
    }

    /** Volta para o corpo: modo de jogo anterior, local marcado, corpo some. */
    private static void endSpy(ServerPlayer player) {
        SPIES.remove(player.getUUID());
        CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(SPY_TAG);
        if (tag.isEmpty()) return;
        player.getPersistentData().remove(SPY_TAG);
        var server = player.level().getServer();
        Identifier dimId = Identifier.tryParse(tag.getStringOr("dim", "minecraft:overworld"));
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId == null ? Identifier.withDefaultNamespace("overworld") : dimId));
        if (level == null) level = server.overworld();
        player.setGameMode(GameType.byId(tag.getIntOr("mode", 0)));
        player.teleportTo(level, tag.getDoubleOr("x", 0), tag.getDoubleOr("y", 64), tag.getDoubleOr("z", 0), Set.of(),
                tag.getFloatOr("yRot", 0), tag.getFloatOr("xRot", 0), true);
        // O corpo some. Se o chunk dele nao estiver carregado agora, ele e apagado quando carregar.
        String body = tag.getStringOr("body", "");
        if (!body.isEmpty()) {
            UUID bodyId = UUID.fromString(body);
            Entity entity = level.getEntity(bodyId);
            if (entity != null) entity.discard();
            else ORPHAN_BODIES.add(bodyId);
        }
        RelicUtil.say(player, "De volta ao corpo.");
    }

    /** Abre (so para ver) o inventario do jogador que o espiao esta olhando. */
    public static boolean peek(ServerPlayer player) {
        LivingEntity target = RelicUtil.targetEntity(player, 20, e -> e instanceof Player && e != player);
        if (!(target instanceof Player other)) {
            RelicUtil.say(player, "Olhe para um jogador para ver o inventário.");
            return false;
        }
        Container copy = new SimpleContainer(54);
        var inventory = other.getInventory();
        for (int i = 0; i < Math.min(54, inventory.getContainerSize()); i++) copy.setItem(i, inventory.getItem(i).copy());
        player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> ChestMenu.sixRows(id, playerInventory, copy),
                Component.literal("Inventário de ").append(other.getName())));
        return true;
    }

    // ---------- Tick ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        boolean wearing = wearing(player);
        if (wearing && item().isUnlocked(player, 0) && now % 100 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        }
        State s = STATES.get(player.getUUID());
        if (wearing && item().isUnlocked(player, 0)) {
            if (s == null) s = state(player);
            boolean want = player.isShiftKeyDown() && now >= s.brokenUntil && RelicUtil.magic(player).getMana() >= STEALTH_MANA_PER_SECOND;
            if (want) {
                setStealth(player, s, true);
                if (now % 20 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
                    player.removeEffect(MobEffects.GLOWING);
                    if (!player.hasInfiniteMaterials()) {
                        RelicUtil.magic(player).useMana(STEALTH_MANA_PER_SECOND);
                        player.syncData(org.dantesys.nexus.data.ModAttachmentType.MAGIC_STATS);
                    }
                }
            } else {
                setStealth(player, s, false);
            }
        } else if (s != null && s.stealth) {
            setStealth(player, s, false);
        }
        if (s != null) {
            if (s.microUntil > 0 && now >= s.microUntil) {
                s.microUntil = 0;
                s.growUntil = now + GROW_TICKS;
            }
            if (s.growUntil > 0) {
                double progress = 1.0 - (s.growUntil - now) / (double) GROW_TICKS;
                if (progress >= 1) { setScale(player, 0); s.growUntil = 0; }
                else setScale(player, -0.9375 * (1 - progress));
                RelicUtil.pushToFreeSpace(player);
            }
            if (now % 20 == 0 && (s.microUntil > 0 || s.growUntil > 0)) refresh(player);
            if (s.plan == 0 && s.planUntil > now && now % 10 == 0) RelicUtil.hideEquipment(player);
            if (s.plan >= 0 && now >= s.planUntil) endPlan(player, s);
        }
        Spy spy = SPIES.get(player.getUUID());
        if (spy != null) {
            ServerLevel bodyLevel = player.level().getServer().getLevel(spy.dim());
            boolean visitor = bodyLevel != null && !bodyLevel.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(spy.pos(), spy.pos()).inflate(5),
                    e -> living(e, player) && e.position().distanceTo(spy.pos()) <= 5).isEmpty();
            if (now >= spy.until() || visitor || !player.isSpectator()) {
                endSpy(player);
            } else if (player.level().dimension() != spy.dim() || player.position().distanceTo(spy.pos()) > SPY_MAX_DISTANCE) {
                // So percorre a regiao em volta do corpo (evita gerar chunks novos longe).
                Vec3 back = spy.pos().add(player.position().subtract(spy.pos()).normalize().scale(SPY_MAX_DISTANCE - 2));
                if (player.level().dimension() == spy.dim()) player.teleportTo(back.x, back.y, back.z);
                else endSpy(player);
            }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().contains(SPY_TAG)) endSpy(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (spying(player)) endSpy(player);
        State s = STATES.remove(player.getUUID());
        if (s != null) {
            setScale(player, 0);
            if (s.plan >= 0) endPlan(player, s);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (spying(player)) endSpy(player);
            State s = STATES.get(player.getUUID());
            if (s != null) {
                setScale(player, 0);
                if (s.plan >= 0) endPlan(player, s);
            }
        }
        STATES.clear();
        SPIES.clear();
    }
}
