package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;

import java.util.*;

/** Efeitos da Explory (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class ExploryEffects {
    private ExploryEffects() {}

    public static final int CAMP_MANA = 80, CAMP_COOLDOWN = 24000, CAMP_TICKS = 12000, CAMP_RADIUS = 15;
    public static final int HOME_MANA = 60, HOME_COOLDOWN = 12000, HOME_WARMUP = 60;
    public static final int JUMP_MANA = 300, JUMP_COOLDOWN = 18000;
    public static final String TARGET_NAME = "ExploryAlvo", TARGET_DIM = "ExploryDim", TARGET_X = "ExploryX", TARGET_Y = "ExploryY", TARGET_Z = "ExploryZ";
    private static final String TARGET_INDEX = "ExploryIndice", BACKPACK = "NexusMochila", HOME = "NexusCasa_", RETURN = "NexusRetorno";

    private static ExploryItem item() { return ModItems.EXPLORY.get(); }

    // ---------- Nv 1 — Mochila ----------

    private static int backpackRows(Player p) {
        int level = item().masteryLevel(p);
        return level >= 20 ? 6 : level >= 10 ? 3 : 1;
    }

    private static final class Backpack extends ChestMenu {
        private final SimpleContainer container;
        Backpack(int id, Player player, SimpleContainer container, int rows) {
            super(rows == 6 ? MenuType.GENERIC_9x6 : rows == 3 ? MenuType.GENERIC_9x3 : MenuType.GENERIC_9x1, id, player.getInventory(), container, rows);
            this.container = container;
        }
        @Override public void removed(Player player) {
            super.removed(player);
            if (player instanceof ServerPlayer sp) save(sp, container);
        }
    }

    public static void openBackpack(ServerPlayer player) {
        if (!item().isUnlocked(player, 0)) return;
        int rows = backpackRows(player);
        SimpleContainer container = new SimpleContainer(rows * 9);
        ListTag list = player.getPersistentData().getListOrEmpty(BACKPACK);
        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            var tag = list.getCompoundOrEmpty(i);
            int slot = tag.getIntOr("Slot", -1);
            ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(ops, tag.getCompoundOrEmpty("Item")).result().orElse(ItemStack.EMPTY);
            if (slot >= 0 && slot < container.getContainerSize()) container.setItem(slot, stack);
            else if (!stack.isEmpty()) overflow.add(stack);
        }
        overflow.forEach(s -> RelicUtil.giveOrDrop(player, s));
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new Backpack(id, p, container, rows), Component.translatable("relic.nexus.explory.mochila")));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    private static void save(ServerPlayer player, SimpleContainer container) {
        ListTag list = new ListTag();
        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (RelicUtil.isRelic(stack)) { RelicUtil.giveOrDrop(player, stack); continue; }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result().ifPresent(t -> entry.put("Item", t));
            list.add(entry);
        }
        player.getPersistentData().put(BACKPACK, list);
    }

    /** Mochila, casas e retorno ficam no jogador e sobrevivem a morte. */
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        CompoundTag from = event.getOriginal().getPersistentData(), to = event.getEntity().getPersistentData();
        for (String key : from.keySet()) {
            if (key.equals(BACKPACK) || key.startsWith(HOME) || key.equals(RETURN)) to.put(key, from.get(key).copy());
        }
    }

    // ---------- Nv 5 — Rastreador ----------

    private record Target(String name, int minLevel, ResourceKey<Level> dim, List<ResourceKey<Structure>> structures, boolean village) {}
    private static final List<Target> TARGETS = List.of(
            new Target("relic.nexus.explory.alvo.vila", 5, Level.OVERWORLD, List.of(), true),
            new Target("relic.nexus.explory.alvo.templo_deserto", 10, Level.OVERWORLD, List.of(BuiltinStructures.DESERT_PYRAMID), false),
            new Target("relic.nexus.explory.alvo.templo_selva", 10, Level.OVERWORLD, List.of(BuiltinStructures.JUNGLE_TEMPLE), false),
            new Target("relic.nexus.explory.alvo.cabana_bruxa", 10, Level.OVERWORLD, List.of(BuiltinStructures.SWAMP_HUT), false),
            new Target("relic.nexus.explory.alvo.posto_avancado", 10, Level.OVERWORLD, List.of(BuiltinStructures.PILLAGER_OUTPOST), false),
            new Target("relic.nexus.explory.alvo.fortaleza_end", 15, Level.OVERWORLD, List.of(BuiltinStructures.STRONGHOLD), false),
            new Target("relic.nexus.explory.alvo.cidade_antiga", 15, Level.OVERWORLD, List.of(BuiltinStructures.ANCIENT_CITY), false),
            new Target("relic.nexus.explory.alvo.fortaleza_nether", 15, Level.NETHER, List.of(BuiltinStructures.FORTRESS), false));

    public static void cycleTracker(ServerPlayer player, ItemStack stack) {
        int level = item().masteryLevel(player);
        List<Target> open = TARGETS.stream().filter(t -> level >= t.minLevel).toList();
        if (open.isEmpty()) { RelicUtil.say(player, Component.translatable("relic.nexus.explory.rastreador_liberado_na_maestria_5")); return; }
        int index = Math.floorMod(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr(TARGET_INDEX, -1) + 1, open.size());
        Target target = open.get(index);
        ServerLevel searchLevel = player.level().getServer().getLevel(target.dim);
        if (searchLevel == null) return;
        BlockPos origin = player.blockPosition();
        if (target.dim == Level.NETHER && player.level().dimension() != Level.NETHER) origin = new BlockPos(origin.getX() / 8, 64, origin.getZ() / 8);
        else if (target.dim != Level.NETHER && player.level().dimension() == Level.NETHER) origin = new BlockPos(origin.getX() * 8, 64, origin.getZ() * 8);
        BlockPos found;
        if (target.village) {
            found = searchLevel.findNearestMapStructure(net.minecraft.tags.StructureTags.VILLAGE, origin, 100, false);
        } else {
            var registry = searchLevel.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            var holders = target.structures.stream().map(registry::get).flatMap(Optional::stream).toList();
            found = holders.isEmpty() ? null : searchLevel.findNearestMapStructure(HolderSet.direct(holders), origin, 100, false);
        }
        final int idx = index;
        if (found == null) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> { t.putInt(TARGET_INDEX, idx); t.remove(TARGET_NAME); });
            RelicUtil.say(player, Component.translatable("relic.nexus.explory.nada_por_perto", Component.translatable(target.name)));
            return;
        }
        BlockPos pos = found;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
            t.putInt(TARGET_INDEX, idx);
            t.putString(TARGET_NAME, target.name);
            t.putString(TARGET_DIM, target.dim.identifier().toString());
            t.putInt(TARGET_X, pos.getX()); t.putInt(TARGET_Y, pos.getY()); t.putInt(TARGET_Z, pos.getZ());
        });
        RelicUtil.say(player, Component.translatable("relic.nexus.explory.rastreando", Component.translatable(target.name)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // ---------- Nv 10 — Acampamento ----------

    private static final class Camp { ResourceKey<Level> dim; BlockPos pos; long until; final Map<UUID, ServerPlayer.RespawnConfig> previous = new HashMap<>(); }
    private static final Map<UUID, Camp> CAMPS = new HashMap<>();

    private static List<ServerPlayer> groupOf(ServerPlayer owner, double radius) {
        return owner.level().getEntitiesOfClass(ServerPlayer.class, owner.getBoundingBox().inflate(radius),
                p -> p == owner || (p.getTeam() != null && p.getTeam() == owner.getTeam()) || owner.isAlliedTo(p));
    }

    public static boolean camp(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();
        if (!level.getBlockState(pos).canBeReplaced() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
            RelicUtil.say(player, Component.translatable("relic.nexus.explory.precisa_de_um_chao_firme"));
            return false;
        }
        endCamp(player.getUUID(), level.getServer());
        level.setBlockAndUpdate(pos, Blocks.CAMPFIRE.defaultBlockState());
        Camp camp = new Camp();
        camp.dim = level.dimension(); camp.pos = pos; camp.until = level.getGameTime() + CAMP_TICKS;
        for (ServerPlayer p : groupOf(player, CAMP_RADIUS)) {
            camp.previous.put(p.getUUID(), p.getRespawnConfig());
            p.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(level.dimension(), pos.above(), p.getYRot(), p.getXRot()), true), false);
        }
        CAMPS.put(player.getUUID(), camp);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        RelicUtil.say(player, Component.translatable("relic.nexus.explory.acampamento_montado_por_10_minutos"));
        return true;
    }

    private static void endCamp(UUID owner, net.minecraft.server.MinecraftServer server) {
        Camp camp = CAMPS.remove(owner);
        if (camp == null) return;
        ServerLevel level = server.getLevel(camp.dim);
        if (level != null && level.getBlockState(camp.pos).is(Blocks.CAMPFIRE)) level.removeBlock(camp.pos, false);
        for (var e : camp.previous.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) p.setRespawnPosition(e.getValue(), false);
        }
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) RelicUtil.say(p, Component.translatable("relic.nexus.explory.o_acampamento_acabou"));
    }

    @SubscribeEvent
    public static void campBroken(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (var e : new ArrayList<>(CAMPS.entrySet())) {
            if (e.getValue().dim == level.dimension() && e.getValue().pos.equals(event.getPos())) endCamp(e.getKey(), level.getServer());
        }
    }

    @SubscribeEvent
    public static void noSpawns(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof Enemy) || CAMPS.isEmpty() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getSpawnType() != net.minecraft.world.entity.EntitySpawnReason.NATURAL && event.getSpawnType() != net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION) return;
        for (Camp camp : CAMPS.values()) {
            if (camp.dim == level.dimension() && camp.pos.distToCenterSqr(event.getX(), event.getY(), event.getZ()) <= CAMP_RADIUS * CAMP_RADIUS) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
    }

    // ---------- Nv 15 — De Volta da Aventura ----------

    private record Warmup(long at, Vec3 start) {}
    private static final Map<UUID, Warmup> WARMUPS = new HashMap<>();

    public static boolean home(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        String key = HOME + player.level().dimension().identifier();
        if (player.isShiftKeyDown()) {
            CompoundTag home = new CompoundTag();
            home.putDouble("x", player.getX()); home.putDouble("y", player.getY()); home.putDouble("z", player.getZ());
            player.getPersistentData().put(key, home);
            RelicUtil.say(player, Component.translatable("relic.nexus.explory.casa_marcada_nesta_dimensao"));
            return true;
        }
        boolean hasReturn = player.getPersistentData().contains(RETURN);
        if (!hasReturn && !player.getPersistentData().contains(key)) {
            RelicUtil.say(player, Component.translatable("relic.nexus.explory.nenhuma_casa_nesta_dimensao_agache"));
            return false;
        }
        WARMUPS.put(player.getUUID(), new Warmup(player.level().getGameTime() + HOME_WARMUP, player.position()));
        RelicUtil.say(player, Component.translatable(hasReturn ? "relic.nexus.explory.voltando_aventura" : "relic.nexus.explory.voltando_casa"));
        return true;
    }

    @SubscribeEvent
    public static void hurt(LivingDamageEvent.Post event) {
        if (WARMUPS.remove(event.getEntity().getUUID()) != null && event.getEntity() instanceof ServerPlayer p) RelicUtil.say(p, Component.translatable("relic.nexus.explory.viagem_cancelada"));
    }

    private static void travel(ServerPlayer player, ServerLevel level, Vec3 to) {
        List<ServerPlayer> group = groupOf(player, 3);
        for (ServerPlayer p : group) {
            ((ServerLevel) p.level()).sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
            p.teleportTo(level, to.x, to.y, to.z, Set.of(), p.getYRot(), p.getXRot(), true);
        }
        level.playSound(null, BlockPos.containing(to), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void finishHome(ServerPlayer player) {
        var server = player.level().getServer();
        CompoundTag data = player.getPersistentData();
        if (data.contains(RETURN)) {
            CompoundTag ret = data.getCompoundOrEmpty(RETURN);
            data.remove(RETURN);
            Identifier dim = Identifier.tryParse(ret.getStringOr("dim", "minecraft:overworld"));
            ServerLevel level = dim == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
            if (level != null) { travel(player, level, new Vec3(ret.getDoubleOr("x", 0), ret.getDoubleOr("y", 64), ret.getDoubleOr("z", 0))); return; }
        }
        CompoundTag home = data.getCompoundOrEmpty(HOME + player.level().dimension().identifier());
        if (home.isEmpty()) return;
        travel(player, (ServerLevel) player.level(), new Vec3(home.getDoubleOr("x", 0), home.getDoubleOr("y", 64), home.getDoubleOr("z", 0)));
    }

    // ---------- Nv 20 — Aventura a um Passo ----------

    public static boolean canJump(ServerPlayer player, ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.getStringOr(TARGET_NAME, "").isEmpty() && item().ready(stack, player.level().getGameTime(), 3);
    }

    private static Vec3 safeSpot(ServerLevel level, int x, int z, int structureY) {
        level.getChunk(x >> 4, z >> 4);
        if (level.dimensionType().hasCeiling()) {
            for (int y = Math.max(level.getMinY() + 2, structureY - 10); y < Math.min(level.getMaxY() - 2, structureY + 40); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos.below()).isSolid() && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) return Vec3.atBottomCenterOf(pos);
            }
            return new Vec3(x + 0.5, structureY + 1, z + 0.5);
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    public static boolean adventureJump(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Identifier dim = Identifier.tryParse(tag.getStringOr(TARGET_DIM, ""));
        ServerLevel level = dim == null ? null : player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
        if (level == null) return false;
        CompoundTag ret = new CompoundTag();
        ret.putString("dim", player.level().dimension().identifier().toString());
        ret.putDouble("x", player.getX()); ret.putDouble("y", player.getY()); ret.putDouble("z", player.getZ());
        player.getPersistentData().put(RETURN, ret);
        travel(player, level, safeSpot(level, tag.getIntOr(TARGET_X, 0), tag.getIntOr(TARGET_Z, 0), tag.getIntOr(TARGET_Y, 64)));
        RelicUtil.say(player, Component.translatable("relic.nexus.explory.aventura_a_um_passo", Component.translatable(tag.getStringOr(TARGET_NAME, ""))));
        return true;
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Warmup w = WARMUPS.get(player.getUUID());
        if (w != null) {
            if (now % 5 == 0) ((ServerLevel) player.level()).sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.8, 0.4, 0.3);
            if (now >= w.at) { WARMUPS.remove(player.getUUID()); finishHome(player); }
        }
        Camp camp = CAMPS.get(player.getUUID());
        if (camp != null) {
            if (now >= camp.until) endCamp(player.getUUID(), player.level().getServer());
            else if (now % 100 == 0 && player.level().dimension() == camp.dim) {
                for (ServerPlayer p : player.level().getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(camp.pos).inflate(CAMP_RADIUS),
                        p -> p == player || (p.getTeam() != null && p.getTeam() == player.getTeam()))) {
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 0));
                }
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        WARMUPS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        for (UUID owner : new ArrayList<>(CAMPS.keySet())) endCamp(owner, event.getServer());
    }
}
