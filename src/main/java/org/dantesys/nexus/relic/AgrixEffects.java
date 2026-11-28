package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.util.Ranks;

import java.util.*;

/** Efeitos da Agrix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class AgrixEffects {
    private AgrixEffects() {}

    public static final int GREEN_MANA = 30, GREEN_COOLDOWN = 1200, GREEN_PULSE = 30;
    public static final int GROWTH_MANA = 40, GROWTH_COOLDOWN = 1800;
    public static final int EGG_MANA = 50, EGG_COOLDOWN = 200, EGG_DISTANCE = 5;
    public static final int FARM_MANA = 200, FARM_COOLDOWN = 6000, FARM_TICKS = 400, FARM_SEED_TYPES = 3;
    private static final String EGG_TAG = "NexusDeVoltaAoOvo";
    private static final TagKey<Item> SEEDS = TagKey.create(Registries.ITEM, Identifier.parse("c:seeds"));
    private static final TagKey<Item> CROPS = TagKey.create(Registries.ITEM, Identifier.parse("c:crops"));
    private static final DustParticleOptions GREEN = new DustParticleOptions(0x40D040, 1.0f);
    private static final DustParticleOptions PINK = new DustParticleOptions(0xFF90C0, 1.0f);
    private static final DustParticleOptions GOLD = new DustParticleOptions(0xE0C040, 1.0f);

    private enum Kind { GREEN, GROWTH, FARM }
    private static final class Area {
        final UUID owner; final ServerLevel level; final AABB box; final Kind kind; final long until; final int mastery;
        Area(UUID owner, ServerLevel level, AABB box, Kind kind, long until, int mastery) {
            this.owner = owner; this.level = level; this.box = box; this.kind = kind; this.until = until; this.mastery = mastery;
        }
    }
    private static final List<Area> AREAS = new ArrayList<>();

    private static AgrixItem item() { return ModItems.AGRIX.get(); }

    private static boolean holdsAgrix(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof AgrixItem || player.getOffhandItem().getItem() instanceof AgrixItem;
    }

    // ---------- Nv 1 — Ciclo da Vida (passiva) ----------

    private static ItemStack randomSeed(ServerLevel level) {
        List<? extends String> ids = Config.AGRIX_SEEDS.get();
        if (ids.isEmpty()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(ids.get(level.getRandom().nextInt(ids.size())));
        if (id == null) return ItemStack.EMPTY;
        Item seed = BuiltInRegistries.ITEM.getValue(id);
        return new ItemStack(seed);
    }

    @SubscribeEvent
    public static void mobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !(event.getEntity() instanceof Enemy)) return;
        if (!holdsAgrix(player) || !item().isUnlocked(player, 0)) return;
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity dead = event.getEntity();
        ItemStack seed = randomSeed(level);
        if (seed.isEmpty()) return;
        BlockPos crop = dead.blockPosition();
        BlockPos ground = crop.below();
        BlockState soil = level.getBlockState(ground);
        boolean plantable = (soil.is(BlockTags.DIRT) || soil.is(Blocks.GRASS_BLOCK) || soil.is(BlockTags.SAND))
                && level.getBlockState(crop).canBeReplaced() && level.mayInteract(player, ground);
        if (plantable && seed.getItem() instanceof BlockItem blockItem) {
            level.setBlock(ground, Blocks.FARMLAND.defaultBlockState(), 3);
            BlockState plant = blockItem.getBlock().defaultBlockState();
            if (plant.canSurvive(level, crop)) {
                level.setBlock(crop, plant, 3);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, crop.getX() + 0.5, crop.getY() + 0.3, crop.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0);
                item().addMasteryXp(player, 1);
                return;
            }
        }
        dead.spawnAtLocation(level, seed);
        item().addMasteryXp(player, 1);
    }

    /** Gemeos: chance cresce com a maestria (5% + 1% por nivel, ate 25%). */
    @SubscribeEvent
    public static void baby(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player) || !holdsAgrix(player) || !item().isUnlocked(player, 0)) return;
        if (!(event.getParentA() instanceof AgeableMob parent) || !(event.getParentB() instanceof AgeableMob partner)) return;
        ServerLevel level = (ServerLevel) player.level();
        double chance = Math.min(0.25, 0.05 + item().masteryLevel(player) * 0.01);
        if (level.getRandom().nextDouble() >= chance) return;
        AgeableMob twin = parent.getBreedOffspring(level, partner);
        if (twin == null) return;
        twin.setBaby(true);
        twin.snapTo(parent.getX(), parent.getY(), parent.getZ(), parent.getYRot(), 0);
        level.addFreshEntity(twin);
        level.sendParticles(ParticleTypes.HEART, twin.getX(), twin.getY() + 0.5, twin.getZ(), 5, 0.3, 0.3, 0.3, 0);
        item().addMasteryXp(player, 2);
    }

    // ---------- Areas ----------

    private static boolean startArea(ServerPlayer player, int slot, Kind kind, int maxSide, int ticks) {
        AABB box = AreaMarker.mark(player, item(), slot, maxSide);
        if (box == null) return false;
        ServerLevel level = (ServerLevel) player.level();
        AREAS.add(new Area(player.getUUID(), level, box, kind, level.getGameTime() + ticks, item().masteryLevel(player)));
        level.playSound(null, BlockPos.containing(box.getCenter()), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        RelicUtil.say(player, Component.translatable("relic.nexus.agrix.area_ativa_por_s", ticks / 20));
        return true;
    }

    /** Nv 5 — Zona Verde: tamanho pela Afinidade, duracao pela Ressonancia. */
    public static boolean greenZone(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        return startArea(player, AgrixItem.SLOT_GREEN, Kind.GREEN, 8 + context.affinity() / 100, 400 + context.resonance());
    }

    /** Nv 10 — Crescimento Populacional. */
    public static boolean growth(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        return startArea(player, AgrixItem.SLOT_GROWTH, Kind.GROWTH, 8 + context.affinity() / 100, 600 + context.resonance() * 2);
    }

    /** Nv 20 — Fazendinha. */
    public static boolean canFarm(ServerPlayer player, ItemStack stack) {
        if (!AreaMarker.hasPending(player, item(), AgrixItem.SLOT_FARM)) return true; // marcar o primeiro ponto e livre
        if (!RelicUtil.hasRank(RelicUtil.magic(player).getRessonancia(), Ranks.A)) return false;
        return seedTypes(player).size() >= FARM_SEED_TYPES;
    }

    private static List<Item> seedTypes(ServerPlayer player) {
        LinkedHashSet<Item> types = new LinkedHashSet<>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (!s.isEmpty() && (s.is(SEEDS) || s.is(CROPS))) types.add(s.getItem());
        }
        return new ArrayList<>(types);
    }

    public static boolean farm(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        boolean pending = AreaMarker.hasPending(player, item(), AgrixItem.SLOT_FARM);
        if (!startArea(player, AgrixItem.SLOT_FARM, Kind.FARM, 8 + context.affinity() / 100, FARM_TICKS)) return false;
        if (pending) {
            List<Item> types = seedTypes(player);
            for (int i = 0; i < FARM_SEED_TYPES && i < types.size(); i++) {
                Item type = types.get(i);
                RelicUtil.consume(player, s -> s.is(type), 1);
            }
        }
        return true;
    }

    private static boolean bonemealable(BlockState state) {
        return state.getBlock() instanceof BonemealableBlock && !(state.getBlock() instanceof GrassBlock)
                && !(state.getBlock() instanceof TallGrassBlock) && !state.is(BlockTags.FLOWERS) && !state.is(Blocks.MOSS_BLOCK);
    }

    private static void tickArea(Area area, long now) {
        ServerLevel level = area.level;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(area.owner);
        DustParticleOptions color = area.kind == Kind.GREEN ? GREEN : area.kind == Kind.GROWTH ? PINK : GOLD;
        if (now % 20 == 0) AreaMarker.outline(level, area.box, color);
        switch (area.kind) {
            case GREEN -> {
                if (now % 5 == 0) {
                    var r = level.getRandom();
                    for (int i = 0; i < 6; i++) {
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, area.box.minX + r.nextDouble() * area.box.getXsize(),
                                area.box.minY + r.nextDouble() * area.box.getYsize(), area.box.minZ + r.nextDouble() * area.box.getZsize(), 1, 0, 0.2, 0, 0.02);
                    }
                }
                if (now % GREEN_PULSE != 0) return;
                for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.box.minX, area.box.minY, area.box.minZ),
                        BlockPos.containing(area.box.maxX - 1, area.box.maxY - 1, area.box.maxZ - 1))) {
                    BlockState state = level.getBlockState(pos);
                    if (!bonemealable(state)) continue;
                    BonemealableBlock block = (BonemealableBlock) state.getBlock();
                    if (block.isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION)
                            && block.isBonemealSuccess(level, level.getRandom(), pos, state, BonemealSource.INTERACTION)) {
                        block.performBonemeal(level, level.getRandom(), pos.immutable(), state, BonemealSource.INTERACTION);
                    }
                }
            }
            case GROWTH -> {
                if (now % 20 != 0) return;
                List<Animal> animals = level.getEntitiesOfClass(Animal.class, area.box);
                if (animals.size() > Config.AGRIX_ANIMAL_LIMIT.get()) return;
                double multiplier = Math.min(3.0, 2.0 + Math.max(0, area.mastery - 10) / 10.0);
                int maxCooldown = (int) Math.max(1200, 3600 - Math.max(0, area.mastery - 10) * 240);
                for (Animal animal : animals) {
                    int age = animal.getAge();
                    if (age < 0) animal.setAge(Math.min(0, age + (int) (20 * (multiplier - 1))));
                    else if (age > maxCooldown) animal.setAge(maxCooldown);
                }
            }
            case FARM -> {
                if (now % 10 != 0) return;
                for (Mob mob : level.getEntitiesOfClass(Mob.class, area.box, m -> m instanceof Enemy && m.isAlive() && !RelicUtil.isBoss(m))) {
                    convert(level, mob, owner);
                }
            }
        }
    }

    private static void convert(ServerLevel level, Mob mob, ServerPlayer owner) {
        List<? extends String> ids = Config.AGRIX_FARM_MOBS.get();
        if (ids.isEmpty()) return;
        Identifier id = Identifier.tryParse(ids.get(level.getRandom().nextInt(ids.size())));
        if (id == null) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        Entity created = type.create(level, EntitySpawnReason.CONVERSION);
        if (!(created instanceof Mob peaceful)) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipment = mob.getItemBySlot(slot);
            if (!equipment.isEmpty()) {
                mob.spawnAtLocation(level, equipment.copy());
                mob.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        peaceful.snapTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), mob.getXRot());
        if (mob.hasCustomName()) peaceful.setCustomName(mob.getCustomName());
        peaceful.setPersistenceRequired();
        level.addFreshEntity(peaceful);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + 0.5, mob.getZ(), 12, 0.4, 0.4, 0.4, 0);
        mob.discard();
        if (owner != null) item().addMasteryXp(owner, 2);
    }

    // ---------- Nv 15 — De Volta ao Ovo ----------

    public static boolean backToEgg(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        LivingEntity target = RelicUtil.targetEntity(player, EGG_DISTANCE, e -> e instanceof Mob);
        if (!(target instanceof Mob mob) || mob instanceof Enemy || RelicUtil.isBoss(mob)) {
            RelicUtil.say(player, Component.translatable("relic.nexus.agrix.olhe_para_um_mob_pacifico", EGG_DISTANCE));
            return false;
        }
        if (mob instanceof OwnableEntity pet && pet.getOwnerReference() != null && pet.getOwner() != player) {
            RelicUtil.say(player, Component.translatable("relic.nexus.agrix.esse_mob_tem_outro_dono"));
            return false;
        }
        var egg = SpawnEggItem.byId(mob.getType());
        if (egg.isEmpty()) {
            RelicUtil.say(player, Component.translatable("relic.nexus.agrix.esse_mob_nao_tem_ovo"));
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        mob.saveWithoutId(output);
        CompoundTag data = output.buildResult();
        for (String key : List.of("UUID", "Pos", "Motion", "Rotation", "FallDistance", "OnGround", "PortalCooldown")) data.remove(key);
        ItemStack eggStack = new ItemStack(egg.get());
        eggStack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(mob.getType(), data));
        CustomData.update(DataComponents.CUSTOM_DATA, eggStack, t -> t.putBoolean(EGG_TAG, true));
        level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.5, mob.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, mob.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0f, 1.0f);
        mob.discard();
        RelicUtil.giveOrDrop(player, eggStack);
        return true;
    }

    /** Ovos do De Volta ao Ovo em geradores de mobs: configuravel. */
    @SubscribeEvent
    public static void eggOnSpawner(PlayerInteractEvent.RightClickBlock event) {
        if (Config.AGRIX_EGGS_IN_SPAWNERS.get()) return;
        ItemStack held = event.getItemStack();
        if (!held.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(EGG_TAG, false)) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.is(Blocks.SPAWNER) || state.is(Blocks.TRIAL_SPAWNER)) event.setCanceled(true);
    }

    // ---------- Tick ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Iterator<Area> it = AREAS.iterator();
        while (it.hasNext()) {
            Area area = it.next();
            if (area.level != level) continue;
            if (now >= area.until) { it.remove(); continue; }
            tickArea(area, now);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { AREAS.clear(); }
}
