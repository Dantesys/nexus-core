package org.dantesys.nexus.relic;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.util.Ranks;

import java.util.*;

/** Efeitos da Escordy (documento de design). Estado guardado no ItemStack para sobreviver a trocas e saidas. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class EscordyEffects {
    private EscordyEffects() {}

    public static final int CHARGE_MANA = 4, CHARGE_TICKS = 20, AGILE_CHARGE_TICKS = 4;
    public static final int AGILE_MANA = 40, AGILE_COOLDOWN = 900, AGILE_TICKS = 200;
    public static final int READY_MANA = 50, READY_COOLDOWN = 2400, READY_TICKS = 160, READY_CHARGE = 5, READY_SHOT_DELAY = 8;
    public static final int FIREWORK_MANA = 60, FIREWORK_COOLDOWN = 600;
    public static final int MIX_MANA = 250, MIX_COOLDOWN = 6000, MIX_TICKS = 2400, MIX_LIMIT = 50;
    public static final double RANGE = 24, CLOSE = 3;
    public static final float PELLET_DAMAGE = 3.0f;

    private static final String CHARGE = "EscCarga", RESERVE = "EscReserva", MIX_UNTIL = "EscMixAte", READY_UNTIL = "EscProntoAte",
            SAVED = "EscCargaGuardada", FIREWORK = "EscFoguete", AGILE_UNTIL = "EscAgilAte";
    private static final ResourceKey<DamageType> PELLET = RelicUtil.damageType("escordy_chumbo");

    private static final Set<UUID> OWN_ROCKETS = new HashSet<>();
    private static final Map<UUID, UUID> ROCKET_OWNER = new HashMap<>();

    public enum Use { FIRED, CHARGE, NONE }

    private static EscordyItem item() { return ModItems.ESCORDY.get(); }

    // ---------- Dados no ItemStack ----------

    private static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    private static int charge(ItemStack stack) { return data(stack).getIntOr(CHARGE, 0); }
    private static void setCharge(ItemStack stack, int value) { CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(CHARGE, Math.max(0, value))); }
    public static int[] reserve(ItemStack stack) { return data(stack).getIntArray(RESERVE).orElse(new int[0]); }
    private static void setReserve(ItemStack stack, int[] reserve) { CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putIntArray(RESERVE, reserve)); }
    private static long getLong(ItemStack stack, String key) { return data(stack).getLongOr(key, 0L); }
    private static void setLong(ItemStack stack, String key, long value) { CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong(key, value)); }

    private static boolean mixActive(ItemStack stack, long now) { return getLong(stack, MIX_UNTIL) > now; }
    private static boolean readyActive(ItemStack stack, long now) { return getLong(stack, READY_UNTIL) > now; }
    private static boolean agileActive(ItemStack stack, long now) { return getLong(stack, AGILE_UNTIL) > now; }

    /** Limite normal: 10 no inicio, ate 20 com a maestria; 50 no Mix. */
    static int normalLimit(int mastery) { return Math.min(20, 10 + mastery / 2); }
    public static int limit(ServerPlayer player, ItemStack stack, long now) {
        return mixActive(stack, now) ? MIX_LIMIT : normalLimit(item().masteryLevel(player));
    }
    public static int shownCharge(ItemStack stack, long now) { return readyActive(stack, now) ? READY_CHARGE : charge(stack); }
    public static int chargeTicks(ItemStack stack, long now) { return agileActive(stack, now) ? AGILE_CHARGE_TICKS : CHARGE_TICKS; }

    /** Cliente: so comeca a puxar quando nao vai disparar (sem carga, ou Shift). */
    public static boolean clientCanCharge(Player player, ItemStack stack) {
        return reserve(stack).length == 0 && (charge(stack) == 0 || player.isShiftKeyDown());
    }

    // ---------- Cone de chumbos ----------

    /** Dano por chumbo pela distancia: total ate 3 blocos, caindo ate 15% em 24. */
    static double falloff(double distance) {
        if (distance <= CLOSE) return 1.0;
        return Math.max(0.15, 1.0 - (distance - CLOSE) / (RANGE - CLOSE) * 0.85);
    }
    /** Abertura do cone em graus: cresce com o numero de chumbos. */
    static double spread(int pellets) { return Math.min(25, 3 + pellets * 1.2); }

    private static Vec3 deviate(Vec3 look, double degrees, Random random) {
        double yaw = Math.toRadians((random.nextDouble() * 2 - 1) * degrees);
        double pitch = Math.toRadians((random.nextDouble() * 2 - 1) * degrees);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 1e-6) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(look).normalize();
        return look.add(right.scale(Math.tan(yaw))).add(up.scale(Math.tan(pitch))).normalize();
    }

    private static void firePellets(ServerPlayer player, ItemStack stack, int pellets, boolean mix) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Random random = new Random(level.getRandom().nextLong());
        Map<LivingEntity, Double> damage = new LinkedHashMap<>();
        Map<LivingEntity, Integer> hits = new HashMap<>();
        var effects = mix ? ConfigLists.effects(Config.ESCORDY_MIX_EFFECTS.get()) : List.<Holder<MobEffect>>of();
        double rocketChance = 0.01 + 0.04 * Math.clamp(RelicUtil.magic(player).getAfinidade(), 0, 1000) / 1000.0;
        int rockets = 0;
        for (int i = 0; i < pellets; i++) {
            Vec3 dir = deviate(look, spread(pellets), random);
            if (mix && random.nextDouble() < rocketChance && rockets < 5) {
                launchRocket(player, dir, Math.max(1, pellets / 10));
                rockets++;
                continue;
            }
            for (double d = 0.5; d <= RANGE; d += 0.25) {
                Vec3 p = eye.add(dir.scale(d));
                BlockPos pos = BlockPos.containing(p);
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) break;
                if (((int) (d * 4)) % 6 == 0) level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                // Campo Reflexivo: o chumbo volta para quem atirou.
                if (BastryEffects.reflectingField(level, p, player) != null) {
                    damage.merge(player, PELLET_DAMAGE * falloff(d * 2), Double::sum);
                    break;
                }
                LivingEntity hit = level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(0.3),
                        t -> t != player && t.isAlive() && !(t instanceof ArmorStand)).stream().findFirst().orElse(null);
                if (hit != null) {
                    damage.merge(hit, PELLET_DAMAGE * falloff(d), Double::sum);
                    hits.merge(hit, 1, Integer::sum);
                    break;
                }
            }
        }
        damage.forEach((target, amount) -> {
            target.setInvulnerableTime(0);
            boolean hurt = target.hurtServer(level, RelicUtil.source(level, PELLET, player), amount.floatValue());
            if (hurt && target != player) {
                item().addMasteryXp(player, 1);
                if (!effects.isEmpty()) {
                    for (int i = 0; i < hits.getOrDefault(target, 0); i++) {
                        target.addEffect(new MobEffectInstance(effects.get(random.nextInt(effects.size())), 60, 0), player);
                    }
                }
            }
        });
        Vec3 muzzle = eye.add(look.scale(1));
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.3f, 1.8f);
    }

    private static void launchRocket(ServerPlayer player, Vec3 dir, int explosions) {
        ServerLevel level = (ServerLevel) player.level();
        Random random = new Random(level.getRandom().nextLong());
        int fixed = Config.ESCORDY_FIREWORK_COLOR.get();
        List<FireworkExplosion> list = new ArrayList<>();
        for (int i = 0; i < Math.clamp(explosions, 1, 7); i++) {
            int color = fixed >= 0 ? fixed : java.awt.Color.HSBtoRGB(random.nextFloat(), 0.9f, 1.0f) & 0xFFFFFF;
            var shapes = FireworkExplosion.Shape.values();
            list.add(new FireworkExplosion(shapes[random.nextInt(shapes.length)], IntList.of(color), IntList.of(), random.nextBoolean(), true));
        }
        ItemStack rocketItem = new ItemStack(Items.FIREWORK_ROCKET);
        rocketItem.set(DataComponents.FIREWORKS, new Fireworks(1, list));
        Vec3 eye = player.getEyePosition();
        FireworkRocketEntity rocket = new FireworkRocketEntity(level, rocketItem, player, eye.x, eye.y - 0.15, eye.z, true);
        rocket.shoot(dir.x, dir.y, dir.z, 2.2F, 0.0F);
        level.addFreshEntity(rocket);
        OWN_ROCKETS.add(rocket.getUUID());
        ROCKET_OWNER.put(rocket.getUUID(), player.getUUID());
    }

    private static void fireRockets(ServerPlayer player, int charge) {
        int rockets = Math.clamp((int) Math.ceil(charge / 4.0), 1, 5);
        int explosions = Math.clamp(charge / 3, 1, 7);
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        for (int i = 0; i < rockets; i++) {
            double offset = rockets == 1 ? 0 : (i / (double) (rockets - 1) - 0.5) * 0.7;
            launchRocket(player, look.add(right.scale(offset)).normalize(), explosions);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /** O dono nao leva dano dos proprios foguetes. */
    @SubscribeEvent
    public static void incoming(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof FireworkRocketEntity rocket)) return;
        UUID owner = ROCKET_OWNER.get(rocket.getUUID());
        if (owner != null && owner.equals(event.getEntity().getUUID())) event.setCanceled(true);
    }

    // ---------- Uso: disparar ou recarregar ----------

    public static Use use(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        int[] reserve = reserve(stack);
        if (reserve.length > 0) {
            firePellets(player, stack, reserve[0], false);
            setReserve(stack, Arrays.copyOfRange(reserve, 1, reserve.length));
            item().refreshModelData(player, stack);
            return Use.FIRED;
        }
        if (readyActive(stack, now)) {
            if (player.getCooldowns().isOnCooldown(stack)) return Use.NONE;
            fire(player, stack, READY_CHARGE, false, now);
            player.getCooldowns().addCooldown(stack, READY_SHOT_DELAY);
            return Use.FIRED;
        }
        int charge = charge(stack);
        if (charge > 0 && !player.isShiftKeyDown()) {
            fire(player, stack, charge, true, now);
            return Use.FIRED;
        }
        if (charge >= limit(player, stack, now)) {
            RelicUtil.say(player, "Carga máxima.");
            return Use.NONE;
        }
        return Use.CHARGE;
    }

    private static void fire(ServerPlayer player, ItemStack stack, int pellets, boolean consume, long now) {
        boolean rockets = data(stack).getBooleanOr(FIREWORK, false);
        if (rockets) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(FIREWORK));
            fireRockets(player, pellets);
        } else {
            firePellets(player, stack, pellets, mixActive(stack, now));
        }
        if (consume) setCharge(stack, 0);
        item().refreshModelData(player, stack);
    }

    /** Recarga completa: gasta mana e soma 1 carga. */
    public static void load(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        if (readyActive(stack, now) || reserve(stack).length > 0 || charge(stack) >= limit(player, stack, now)) return;
        int cost = agileActive(stack, now) ? Math.max(1, CHARGE_MANA * 3 / 4) : CHARGE_MANA;
        var magic = RelicUtil.magic(player);
        if (!player.hasInfiniteMaterials()) {
            if (!magic.useMana(cost)) {
                RelicUtil.say(player, "Mana insuficiente para recarregar.");
                return;
            }
            player.syncData(ModAttachmentType.MAGIC_STATS);
        }
        setCharge(stack, charge(stack) + 1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        item().refreshModelData(player, stack);
    }

    /** Fim do Cartucho Pronto (devolve a carga guardada) e do Mix (carga vira reserva). */
    public static void tickStack(ServerPlayer player, ItemStack stack) {
        long now = player.level().getGameTime();
        CompoundTag tag = data(stack);
        long ready = tag.getLongOr(READY_UNTIL, 0L);
        if (ready > 0 && ready <= now) {
            int saved = tag.getIntOr(SAVED, 0);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> { t.remove(READY_UNTIL); t.remove(SAVED); t.putInt(CHARGE, saved); });
            item().refreshModelData(player, stack);
        }
        long mix = tag.getLongOr(MIX_UNTIL, 0L);
        if (mix > 0 && mix <= now) {
            int charge = charge(stack), normal = normalLimit(item().masteryLevel(player));
            List<Integer> shots = new ArrayList<>();
            for (int left = charge; left > 0; left -= normal) shots.add(Math.min(normal, left));
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> { t.remove(MIX_UNTIL); t.putInt(CHARGE, 0); });
            setReserve(stack, shots.stream().mapToInt(Integer::intValue).toArray());
            if (!shots.isEmpty()) RelicUtil.say(player, "Mix acabou: " + shots.size() + " disparos em reserva.");
            item().refreshModelData(player, stack);
        }
    }

    // ---------- Habilidades ----------

    public static boolean agile(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        setLong(stack, AGILE_UNTIL, player.level().getGameTime() + AGILE_TICKS + context.resonance() / 10);
        RelicUtil.say(player, "Mãos Ágeis!");
        return true;
    }

    public static boolean ready(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        long now = player.level().getGameTime();
        if (readyActive(stack, now)) return false;
        int saved = charge(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
            t.putLong(READY_UNTIL, now + READY_TICKS + context.resonance() / 10);
            t.putInt(SAVED, saved);
        });
        item().refreshModelData(player, stack);
        RelicUtil.say(player, "Cartucho Pronto: carga 5 sem recarregar.");
        return true;
    }

    public static boolean fireworkAmmo(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean(FIREWORK, true));
        RelicUtil.say(player, "Munição Alternativa: o próximo disparo é de fogos.");
        return true;
    }

    public static boolean canMix(ServerPlayer player, ItemStack stack) {
        if (!RelicUtil.hasRank(RelicUtil.magic(player).getRegeneracao(), Ranks.A) || reserve(stack).length > 0) return false;
        return !player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5),
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand) && e.distanceTo(player) <= 5).isEmpty();
    }

    public static boolean mix(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        setLong(stack, MIX_UNTIL, player.level().getGameTime() + MIX_TICKS);
        item().refreshModelData(player, stack);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 0.8f);
        RelicUtil.say(player, "Mix por 2 minutos!");
        return true;
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        OWN_ROCKETS.clear();
        ROCKET_OWNER.clear();
    }
}
