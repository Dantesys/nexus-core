package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.items.PlantaItem;
import org.dantesys.nexus.magic.MagicStatus;

import java.util.*;
import java.util.function.Consumer;

/** Efeitos da Krefty no servidor: colocar blocos com mana, areas, copia de blocos e a Construcao Instantanea. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class KreftyEffects {
    private KreftyEffects() {}

    private static final DustParticleOptions MANA = new DustParticleOptions(0x8000FF, 1.0f);
    private static final DustParticleOptions MANA_SMALL = new DustParticleOptions(0xB060FF, 0.6f);
    private static final DustParticleOptions DUST = new DustParticleOptions(0x9A8F80, 1.2f);
    private static final DustParticleOptions RED = new DustParticleOptions(0xFF2020, 0.9f);
    private static final ResourceKey<DamageType> BRICK = key("krefty_tijolo");
    private static final ResourceKey<DamageType> DEBRIS = key("krefty_escombros");
    private static final String COPY_TAG = "KreftyCopia", COPY_NAME = "KreftyCopiaNome";

    private record Pending(int slot, ResourceKey<Level> dim, BlockPos first) {}
    private enum Kind { CONSTRUCTION, DEMOLITION }
    private static final class Area {
        final UUID owner; final ServerLevel level; final AABB box; final Kind kind; final long expires;
        final Set<UUID> allowed = new HashSet<>();
        Area(UUID owner, ServerLevel level, AABB box, Kind kind, long expires) {
            this.owner = owner; this.level = level; this.box = box; this.kind = kind; this.expires = expires;
        }
    }
    private record Placement(BlockPos pos, BlockState state) {}
    private static final class Job {
        final UUID owner; final ServerLevel level; final Deque<Placement> queue; final int perTick;
        Job(UUID owner, ServerLevel level, List<Placement> placements) {
            this.owner = owner; this.level = level; this.queue = new ArrayDeque<>(placements);
            this.perTick = Math.max(1, (int) Math.ceil(placements.size() / (double) KreftyRules.BUILD_TICKS));
        }
    }
    /** Pedido da passiva: bloco da outra mao (item + contexto) ou bloco copiado (estado + posicao). */
    private record PlaceRequest(BlockState ruleState, int cost, BlockItem item, BlockPlaceContext context,
                                BlockPos pos, BlockState copied, Direction face) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final List<Area> AREAS = new ArrayList<>();
    private static final List<Job> JOBS = new ArrayList<>();
    private static PlaceRequest request;

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Nexus.MODID, name));
    }
    private static KreftyItem item() { return ModItems.KREFTY.get(); }
    private static void say(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal(text), true); }

    private static BlockHitResult target(Player player, double distance) {
        HitResult hit = player.pick(distance, 0.0F, false);
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block : null;
    }

    // ---------- Custos e usos livres (chamados pela KreftyItem) ----------

    static int manaCost(ServerPlayer player, ItemStack stack, int slot, int base) {
        return switch (slot) {
            case KreftyItem.SLOT_COPY_BLOCKS -> request == null ? base : request.cost();
            case KreftyItem.SLOT_BUILD_AREA, KreftyItem.SLOT_DEMOLITION -> {
                Pending pending = PENDING.get(player.getUUID());
                BlockHitResult hit = target(player, KreftyRules.MAX_TARGET_DISTANCE);
                yield pending == null || hit == null ? base : KreftyRules.areaCost(base, KreftyRules.volume(pending.first(), hit.getBlockPos()));
            }
            case KreftyItem.SLOT_INSTANT -> buildCost(player);
            default -> base;
        };
    }

    static boolean isFreeUse(ServerPlayer player, int slot) {
        return switch (slot) {
            case KreftyItem.SLOT_BUILD_AREA, KreftyItem.SLOT_DEMOLITION -> player.isShiftKeyDown() || !hasPending(player, slot);
            case KreftyItem.SLOT_INSTANT -> !(player.getOffhandItem().getItem() instanceof PlantaItem);
            default -> false;
        };
    }

    private static boolean hasPending(ServerPlayer player, int slot) {
        Pending pending = PENDING.get(player.getUUID());
        return pending != null && pending.slot() == slot && pending.dim() == player.level().dimension();
    }

    private static int mastery(ServerPlayer player) { return item().masteryLevel(player); }

    /** Bloco permitido para este jogador? Envia o motivo quando nao. */
    private static boolean allowed(ServerPlayer player, BlockState state, BlockPos pos, boolean feedback) {
        if (KreftyRules.isBlocked(state, player.level(), pos)) {
            if (feedback) say(player, "Esse bloco não pode ser copiado.");
            return false;
        }
        int min = KreftyRules.rule(state).minLevel();
        if (mastery(player) < min) {
            if (feedback) say(player, "Bloco liberado na maestria " + min + ".");
            return false;
        }
        return true;
    }

    // ---------- Copia de blocos (passiva) ----------

    public static boolean hasCopy(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(COPY_TAG);
    }

    public static String copiedName(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(COPY_NAME, "");
    }

    private static BlockState copiedState(Level level, ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompoundOrEmpty(COPY_TAG);
        if (tag.isEmpty()) return null;
        BlockState state = NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
        return state.isAir() ? null : state;
    }

    /** Estado usado para custo e HUD: bloco da outra mao, senao o copiado. */
    private static BlockState currentBlock(Player player, ItemStack krefty) {
        if (player.getOffhandItem().getItem() instanceof BlockItem blockItem) return blockItem.getBlock().defaultBlockState();
        return copiedState(player.level(), krefty);
    }

    /** Quantos blocos a mana atual permite colocar (-1 sem bloco). */
    static int affordableBlocks(ServerPlayer player, ItemStack stack) {
        BlockState state = currentBlock(player, stack);
        if (state == null) return -1;
        if (mastery(player) < KreftyRules.rule(state).minLevel()) return 0;
        MagicStatus magic = player.getData(ModAttachmentType.MAGIC_STATS);
        int resonance = Math.clamp(magic.getRessonancia(), 0, MagicStatus.MAX_STATUS);
        int cost = (int) Math.ceil(KreftyRules.rule(state).cost() * (1.0D - 0.5D * resonance / MagicStatus.MAX_STATUS));
        return cost <= 0 ? 9999 : Math.min(9999, magic.getMana() / cost);
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        if (event.getHand() != InteractionHand.MAIN_HAND || !(main.getItem() instanceof KreftyItem krefty)) return;
        ItemStack off = player.getOffhandItem();
        BlockItem blockItem = off.getItem() instanceof BlockItem bi ? bi : null;
        if (blockItem == null && !hasCopy(main)) return;
        Level level = player.level();
        BlockPos clicked = event.getPos();
        // Sem Shift, blocos com interface (bau, fornalha) continuam abrindo normalmente.
        if (!player.isShiftKeyDown() && level.getBlockState(clicked).getMenuProvider(level, clicked) != null) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(player instanceof ServerPlayer sp) || !krefty.isUnlocked(sp, KreftyItem.SLOT_COPY_BLOCKS)) return;

        BlockHitResult hit = event.getHitVec();
        PlaceRequest placeRequest;
        if (blockItem != null) {
            // Item novo, sem componentes: blocos com inventario saem vazios.
            BlockPlaceContext context = new BlockPlaceContext(sp, InteractionHand.OFF_HAND, new ItemStack(blockItem), hit);
            BlockPlaceContext updated = blockItem.updatePlacementContext(context);
            if (updated == null) return;
            BlockState rule = blockItem.getBlock().defaultBlockState();
            if (!allowed(sp, rule, updated.getClickedPos(), true)) return;
            placeRequest = new PlaceRequest(rule, KreftyRules.rule(rule).cost(), blockItem, context, updated.getClickedPos(), null, hit.getDirection());
        } else {
            BlockState copied = copiedState(level, main);
            if (copied == null) return;
            BlockPos pos = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
            if (!allowed(sp, copied, pos, true)) return;
            placeRequest = new PlaceRequest(copied, KreftyRules.rule(copied).cost(), null, null, pos, copied, hit.getDirection());
        }
        MagicStatus magic = sp.getData(ModAttachmentType.MAGIC_STATS);
        request = placeRequest;
        try {
            if (!krefty.triggerPassive(sp, main, KreftyItem.SLOT_COPY_BLOCKS) && magic.isCreated()
                    && magic.getMana() < placeRequest.cost()) say(sp, "Mana insuficiente.");
        } finally {
            request = null;
        }
        krefty.refreshModelData(sp, main);
    }

    /** Nv 1 — Copia de Blocos: executada pela passiva depois de pagar a mana. */
    public static boolean placePassive(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        PlaceRequest r = request;
        if (r == null) return false;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = r.pos();
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return false;
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        boolean placed;
        if (r.item() != null) {
            placed = r.item().place(r.context()).consumesAction();
        } else {
            BlockState current = level.getBlockState(pos);
            if (!current.canBeReplaced() || !r.copied().canSurvive(level, pos)
                    || !level.isUnobstructed(r.copied(), pos, CollisionContext.of(player))) return false;
            placed = level.setBlock(pos, r.copied(), 3);
            if (placed) {
                var sound = r.copied().getSoundType();
                level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
            }
        }
        if (!placed) return false;
        // Protecao de terreno: o evento normal de colocar bloco pode desfazer.
        if (EventHooks.onBlockPlace(player, snapshot, r.face())) {
            snapshot.restore();
            return false;
        }
        level.sendParticles(MANA_SMALL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
        item().addMasteryXp(player, KreftyRules.XP_PER_BLOCK);
        return true;
    }

    // ---------- Nv 10 — Copia "Perfeita" ----------

    public static boolean perfectCopy(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        BlockHitResult hit = target(player, KreftyRules.COPY_DISTANCE);
        if (hit == null || player.level().getBlockState(hit.getBlockPos()).isAir()) {
            if (hasCopy(stack)) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> { tag.remove(COPY_TAG); tag.remove(COPY_NAME); });
                say(player, "Cópia apagada.");
            } else say(player, "Mire em um bloco a até " + KreftyRules.COPY_DISTANCE + " blocos.");
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        if (!allowed(player, state, pos, true)) return false;
        if (KreftyRules.isMultiPart(state)) {
            say(player, "Blocos de duas partes só pela outra mão.");
            return false;
        }
        CompoundTag stateTag = NbtUtils.writeBlockState(state);
        String name = state.getBlock().getDescriptionId();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> { tag.put(COPY_TAG, stateTag); tag.putString(COPY_NAME, name); });
        player.sendSystemMessage(Component.literal("Copiado: ").append(Component.translatable(name)), true);
        ((ServerLevel) player.level()).sendParticles(MANA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0);
        return true;
    }

    // ---------- Marcacao de dois pontos ----------

    /** Retorna os dois pontos quando a marcacao termina; null enquanto marca o primeiro ou cancela. */
    private static BlockPos[] markPoints(ServerPlayer player, int slot, int maxSide) {
        UUID id = player.getUUID();
        if (player.isShiftKeyDown()) {
            if (PENDING.remove(id) != null) say(player, "Marcação cancelada.");
            return null;
        }
        BlockHitResult hit = target(player, KreftyRules.MAX_TARGET_DISTANCE);
        if (hit == null) {
            say(player, "Mire em um bloco (até " + KreftyRules.MAX_TARGET_DISTANCE + " blocos).");
            return null;
        }
        if (!hasPending(player, slot)) {
            PENDING.put(id, new Pending(slot, player.level().dimension(), hit.getBlockPos()));
            say(player, "Ponto 1 marcado. Mire no ponto 2 e use de novo (Shift cancela).");
            return null;
        }
        BlockPos first = PENDING.get(id).first(), second = hit.getBlockPos();
        if (!KreftyRules.fits(first, second, maxSide)) {
            say(player, "Área grande demais (máx. " + maxSide + " blocos por lado).");
            return null;
        }
        PENDING.remove(id);
        return new BlockPos[]{first, second};
    }

    // ---------- Nv 5 e 15 — Areas ----------

    public static boolean buildArea(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        return area(player, KreftyItem.SLOT_BUILD_AREA, Kind.CONSTRUCTION);
    }

    public static boolean demolitionArea(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        return area(player, KreftyItem.SLOT_DEMOLITION, Kind.DEMOLITION);
    }

    private static boolean area(ServerPlayer player, int slot, Kind kind) {
        BlockPos[] points = markPoints(player, slot, Config.KREFTY_MAX_AREA_SIDE.get());
        if (points == null) return false;
        ServerLevel level = (ServerLevel) player.level();
        // Os pontos costumam ser o chao: a area inclui o espaco acima deles, onde os mobs ficam.
        AABB box = AABB.encapsulatingFullBlocks(points[0], points[1]).expandTowards(0, KreftyRules.AREA_HEIGHT_ABOVE, 0);
        long duration = kind == Kind.CONSTRUCTION ? KreftyRules.BUILD_AREA_DURATION : KreftyRules.DEMOLITION_DURATION;
        Area area = new Area(player.getUUID(), level, box, kind, level.getGameTime() + duration);
        // Quem ja estava dentro pode sair; so a entrada e barrada.
        if (kind == Kind.CONSTRUCTION) {
            for (Entity entity : level.getEntities((Entity) null, box.inflate(0.5), e -> !(e instanceof Player))) area.allowed.add(entity.getUUID());
        }
        AREAS.add(area);
        level.playSound(null, BlockPos.containing(box.getCenter()), kind == Kind.CONSTRUCTION ? SoundEvents.ANVIL_PLACE
                : SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
        say(player, kind == Kind.CONSTRUCTION ? "Área de Construção ativa por " + duration / 20 + "s."
                : "Área de Demolição ativa por " + duration / 20 + "s.");
        return true;
    }

    private static void tickArea(Area area, long now, Iterator<Area> iterator) {
        ServerPlayer owner = area.level.getServer().getPlayerList().getPlayer(area.owner);
        if (owner == null || now >= area.expires) {
            iterator.remove();
            return;
        }
        ServerLevel level = area.level;
        if (now % 10 == 0) edges(area.box, p -> level.sendParticles(area.kind == Kind.CONSTRUCTION ? MANA : DUST,
                p.x, p.y, p.z, 1, 0, 0, 0, 0));

        if (area.kind == Kind.CONSTRUCTION) {
            for (Entity entity : level.getEntities((Entity) null, area.box.inflate(1.5),
                    e -> !(e instanceof Player) && (e instanceof LivingEntity || e instanceof Projectile))) {
                boolean inside = entity.getBoundingBox().intersects(area.box);
                if (area.allowed.contains(entity.getUUID())) {
                    // Saiu de vez (com folga, para nao perder a permissao encostando na borda): nao entra mais.
                    if (!entity.getBoundingBox().intersects(area.box.inflate(0.5))) area.allowed.remove(entity.getUUID());
                    continue;
                }
                if (!inside || entity instanceof LivingEntity living && RelicTargets.isAlly(owner, living)) continue;
                if (entity instanceof Projectile) entity.discard();
                else pushOut(entity, area.box);
            }
        }

        if (now % KreftyRules.AREA_PULSE != 0) return;
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            double x = area.box.minX + random.nextDouble() * area.box.getXsize();
            double z = area.box.minZ + random.nextDouble() * area.box.getZsize();
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, area.kind == Kind.CONSTRUCTION
                    ? Blocks.BRICKS.defaultBlockState() : Blocks.GRAVEL.defaultBlockState()), x, area.box.maxY + 0.5, z, 2, 0.2, 0.1, 0.2, 0);
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area.box, t -> RelicTargets.canHit(owner, t))) {
            double headY = target.getY() + target.getBbHeight() + 0.4;
            if (area.kind == Kind.CONSTRUCTION) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BRICKS.defaultBlockState()),
                        target.getX(), headY, target.getZ(), 6, 0.2, 0.1, 0.2, 0);
                ItemStack helmet = target.getItemBySlot(EquipmentSlot.HEAD);
                if (target instanceof Player && !helmet.isEmpty()) {
                    helmet.hurtAndBreak(KreftyRules.HELMET_WEAR, target, EquipmentSlot.HEAD);
                    level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 1.8f);
                } else {
                    hurt(level, target, BRICK, owner, KreftyRules.BRICK_DAMAGE);
                }
            } else {
                hurt(level, target, DEBRIS, owner, KreftyRules.DEBRIS_DAMAGE);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, KreftyRules.DEBRIS_EFFECT_TICKS, 1));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, KreftyRules.DEBRIS_EFFECT_TICKS, 0));
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), headY, target.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
            }
        }
    }

    private static void hurt(ServerLevel level, LivingEntity target, ResourceKey<DamageType> type, ServerPlayer owner, float amount) {
        target.hurtServer(level, new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), owner), amount);
    }

    /** Empurra a entidade para fora pela face (X/Z) mais proxima. */
    private static void pushOut(Entity entity, AABB box) {
        Vec3 c = entity.position();
        double margin = entity.getBbWidth() / 2 + 0.1;
        double toMinX = c.x - box.minX, toMaxX = box.maxX - c.x, toMinZ = c.z - box.minZ, toMaxZ = box.maxZ - c.z;
        double min = Math.min(Math.min(toMinX, toMaxX), Math.min(toMinZ, toMaxZ));
        double x = c.x, z = c.z;
        if (min == toMinX) x = box.minX - margin;
        else if (min == toMaxX) x = box.maxX + margin;
        else if (min == toMinZ) z = box.minZ - margin;
        else z = box.maxZ + margin;
        entity.teleportTo(x, c.y, z);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    /** Pontos nas 12 arestas da caixa, de 1 em 1 bloco. */
    private static void edges(AABB box, Consumer<Vec3> out) {
        double[] xs = {box.minX, box.maxX}, ys = {box.minY, box.maxY}, zs = {box.minZ, box.maxZ};
        for (double y : ys) for (double z : zs) for (double x = box.minX; x <= box.maxX; x += 1) out.accept(new Vec3(x, y, z));
        for (double x : xs) for (double z : zs) for (double y = box.minY; y <= box.maxY; y += 1) out.accept(new Vec3(x, y, z));
        for (double x : xs) for (double y : ys) for (double z = box.minZ; z <= box.maxZ; z += 1) out.accept(new Vec3(x, y, z));
    }

    private static void show(ServerPlayer player, ParticleOptions particle, Vec3 p) {
        ((ServerLevel) player.level()).sendParticles(player, particle, true, true, p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }

    // ---------- Nv 20 — Construcao Instantanea ----------

    private static boolean holdsBlueprint(ServerPlayer player) {
        return player.getOffhandItem().getItem() instanceof PlantaItem;
    }

    private static Blueprint blueprint(ServerPlayer player) {
        return Blueprints.get(player.level().getServer()).get(PlantaItem.id(player.getOffhandItem()));
    }

    /** Requisitos so para erguer; copiar e livre. */
    public static boolean canInstantBuild(ServerPlayer player, ItemStack stack) {
        if (!holdsBlueprint(player)) return true;
        MagicStatus magic = player.getData(ModAttachmentType.MAGIC_STATS);
        if (!KreftyRules.hasFinalRank(magic.getMaxMana()) || magic.getMana() < magic.getMaxMana()) return false;
        if (player.getHealth() < player.getMaxHealth() || player.getFoodData().getFoodLevel() < 20) return false;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof BlockItem) return false;
        }
        return true;
    }

    private static BlockPos buildOrigin(ServerPlayer player) {
        BlockHitResult hit = target(player, KreftyRules.MAX_TARGET_DISTANCE);
        if (hit == null) return null;
        BlockPos pos = hit.getBlockPos();
        return player.level().getBlockState(pos).canBeReplaced() ? pos : pos.relative(hit.getDirection());
    }

    private static List<Placement> placements(ServerPlayer player, Blueprint blueprint, BlockPos origin, Rotation rotation, int[] skipped) {
        List<Placement> list = new ArrayList<>();
        int level = mastery(player);
        blueprint.forEach(origin, rotation, (pos, state) -> {
            if (KreftyRules.isBlocked(state, player.level(), pos) || level < KreftyRules.rule(state).minLevel()) skipped[0]++;
            else list.add(new Placement(pos, state));
        });
        return list;
    }

    private static int buildCost(ServerPlayer player) {
        if (!holdsBlueprint(player)) return 0;
        Blueprint blueprint = blueprint(player);
        BlockPos origin = buildOrigin(player);
        if (blueprint == null || origin == null) return 0;
        int cost = 0;
        for (Placement p : placements(player, blueprint, origin, PlantaItem.rotation(player.getOffhandItem()), new int[1])) {
            cost += KreftyRules.rule(p.state()).cost();
        }
        return cost;
    }

    /** Celulas da area de destino que nao estao livres (nem ar nem substituiveis). */
    private static List<BlockPos> obstructed(Level level, Blueprint blueprint, BlockPos origin, Rotation rotation, int limit) {
        List<BlockPos> list = new ArrayList<>();
        BlockPos corner = blueprint.corner(origin, rotation);
        int rx = Blueprint.rotatedX(blueprint.sx(), blueprint.sz(), rotation), rz = Blueprint.rotatedZ(blueprint.sx(), blueprint.sz(), rotation);
        for (int y = 0; y < blueprint.sy(); y++) for (int z = 0; z < rz; z++) for (int x = 0; x < rx; x++) {
            BlockPos pos = corner.offset(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.canBeReplaced()) {
                list.add(pos);
                if (list.size() >= limit) return list;
            }
        }
        return list;
    }

    public static boolean instantBuild(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        return holdsBlueprint(player) ? erect(player) : copyBlueprint(player);
    }

    private static boolean copyBlueprint(ServerPlayer player) {
        BlockPos[] points = markPoints(player, KreftyItem.SLOT_INSTANT, Config.KREFTY_MAX_BLUEPRINT_SIDE.get());
        if (points == null) return false;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos min = new BlockPos(Math.min(points[0].getX(), points[1].getX()), Math.min(points[0].getY(), points[1].getY()),
                Math.min(points[0].getZ(), points[1].getZ()));
        int sx = Math.abs(points[0].getX() - points[1].getX()) + 1;
        int sy = Math.abs(points[0].getY() - points[1].getY()) + 1;
        int sz = Math.abs(points[0].getZ() - points[1].getZ()) + 1;
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> index = new HashMap<>();
        int[] data = new int[sx * sy * sz];
        int count = 0, cost = 0;
        for (int z = 0; z < sz; z++) for (int y = 0; y < sy; y++) for (int x = 0; x < sx; x++) {
            BlockPos pos = min.offset(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getBlock() instanceof LiquidBlock || KreftyRules.isBlocked(state, level, pos)) continue;
            Integer i = index.get(state);
            if (i == null) {
                palette.add(state);
                i = palette.size();
                index.put(state, i);
            }
            data[x + sx * (y + sy * z)] = i;
            count++;
            cost += KreftyRules.rule(state).cost();
        }
        if (count == 0) {
            say(player, "A área marcada está vazia.");
            return false;
        }
        String id = Blueprints.get(level.getServer()).add(new Blueprint(sx, sy, sz, palette, data));
        ItemStack planta = new ItemStack(ModItems.PLANTA.get());
        PlantaItem.write(planta, id, sx, sy, sz, count, cost);
        if (!player.getInventory().add(planta)) player.spawnAtLocation(level, planta);
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
        say(player, "Planta criada: " + sx + "×" + sy + "×" + sz + ", " + count + " blocos.");
        return true;
    }

    private static boolean erect(ServerPlayer player) {
        Blueprint blueprint = blueprint(player);
        if (blueprint == null) {
            say(player, "Planta inválida.");
            return false;
        }
        BlockPos origin = buildOrigin(player);
        if (origin == null) {
            say(player, "Mire onde a construção vai ficar.");
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        Rotation rotation = PlantaItem.rotation(player.getOffhandItem());
        List<BlockPos> blocked = obstructed(level, blueprint, origin, rotation, 9999);
        if (!blocked.isEmpty()) {
            say(player, "A área não está limpa: " + blocked.size() + " blocos no caminho.");
            return false;
        }
        int[] skipped = new int[1];
        List<Placement> list = placements(player, blueprint, origin, rotation, skipped);
        if (list.isEmpty()) {
            say(player, "Nenhum bloco desta planta está liberado para você.");
            return false;
        }
        list.sort(Comparator.comparingDouble(p -> p.pos().distSqr(origin)));
        JOBS.add(new Job(player.getUUID(), level, list));
        var food = player.getFoodData();
        food.setFoodLevel(Math.max(0, food.getFoodLevel() - KreftyRules.FINAL_HUNGER));
        item().addMasteryXp(player, Math.max(1, list.size() / KreftyRules.BLUEPRINT_BLOCKS_PER_XP));
        level.playSound(null, origin, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.4f);
        say(player, "Construção Instantânea: " + list.size() + " blocos" + (skipped[0] > 0 ? " (" + skipped[0] + " pulados)" : "") + ".");
        return true;
    }

    private static void tickJob(Job job, Iterator<Job> iterator) {
        ServerPlayer owner = job.level.getServer().getPlayerList().getPlayer(job.owner);
        for (int i = 0; i < job.perTick && !job.queue.isEmpty(); i++) {
            Placement p = job.queue.poll();
            if (!job.level.getBlockState(p.pos()).canBeReplaced()) continue;
            if (owner != null && !job.level.mayInteract(owner, p.pos())) continue;
            BlockSnapshot snapshot = BlockSnapshot.create(job.level.dimension(), job.level, p.pos());
            if (!job.level.setBlock(p.pos(), p.state(), 3)) continue;
            if (EventHooks.onBlockPlace(owner, snapshot, Direction.UP)) {
                snapshot.restore();
                continue;
            }
            if (i % 4 == 0) job.level.sendParticles(MANA, p.pos().getX() + 0.5, p.pos().getY() + 0.5, p.pos().getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
        }
        if (job.queue.isEmpty()) iterator.remove();
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        for (Iterator<Area> it = AREAS.iterator(); it.hasNext(); ) {
            Area area = it.next();
            if (area.level == level) tickArea(area, now, it);
        }
        for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
            Job job = it.next();
            if (job.level == level) tickJob(job, it);
        }
    }

    /** Previas so para o dono: a marcacao da area e o holograma da planta. */
    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().getGameTime() % 10 != 0) return;
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof KreftyItem krefty)) return;
        Pending pending = PENDING.get(player.getUUID());
        if (pending != null && pending.dim() == player.level().dimension()) {
            BlockHitResult hit = target(player, KreftyRules.MAX_TARGET_DISTANCE);
            BlockPos second = hit == null ? pending.first() : hit.getBlockPos();
            int max = pending.slot() == KreftyItem.SLOT_INSTANT ? Config.KREFTY_MAX_BLUEPRINT_SIDE.get() : Config.KREFTY_MAX_AREA_SIDE.get();
            DustParticleOptions color = KreftyRules.fits(pending.first(), second, max) ? MANA : RED;
            AABB preview = AABB.encapsulatingFullBlocks(pending.first(), second);
            if (pending.slot() != KreftyItem.SLOT_INSTANT) preview = preview.expandTowards(0, KreftyRules.AREA_HEIGHT_ABOVE, 0);
            edges(preview, p -> show(player, color, p));
            return;
        }
        if (holdsBlueprint(player) && krefty.selectedSkill(main) == KreftyItem.SLOT_INSTANT
                && krefty.isUnlocked(player, KreftyItem.SLOT_INSTANT)) hologram(player);
    }

    private static void hologram(ServerPlayer player) {
        Blueprint blueprint = blueprint(player);
        BlockPos origin = buildOrigin(player);
        if (blueprint == null || origin == null) return;
        Rotation rotation = PlantaItem.rotation(player.getOffhandItem());
        BlockPos corner = blueprint.corner(origin, rotation);
        int rx = Blueprint.rotatedX(blueprint.sx(), blueprint.sz(), rotation), rz = Blueprint.rotatedZ(blueprint.sx(), blueprint.sz(), rotation);
        edges(AABB.encapsulatingFullBlocks(corner, corner.offset(rx - 1, blueprint.sy() - 1, rz - 1)), p -> show(player, MANA, p));
        int stride = Math.max(1, blueprint.count() / 250);
        int[] i = {0};
        blueprint.forEach(origin, rotation, (pos, state) -> {
            if (i[0]++ % stride == 0) show(player, MANA_SMALL, Vec3.atCenterOf(pos));
        });
        for (BlockPos pos : obstructed(player.level(), blueprint, origin, rotation, 120)) show(player, RED, Vec3.atCenterOf(pos));
    }

    // ---------- Limpeza ----------

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        PENDING.clear();
        AREAS.clear();
        JOBS.clear();
        request = null;
    }
}
