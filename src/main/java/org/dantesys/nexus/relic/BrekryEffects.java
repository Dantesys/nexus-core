package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.util.Ranks;

import java.util.*;

/** Efeitos da Brekry (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class BrekryEffects {
    private BrekryEffects() {}

    public static final int DYNAMITE_MANA = 20, DYNAMITE_COOLDOWN = 100, DYNAMITE_FUSE = 100;
    public static final float DYNAMITE_POWER = 3.0F;
    public static final int TUNNEL_MANA = 15, TUNNEL_COOLDOWN = 40;
    public static final int FRENZY_MANA = 30, FRENZY_COOLDOWN = 600, FRENZY_TICKS = 300;
    public static final int CLEANUP_MANA = 150, CLEANUP_COOLDOWN = 2400, CLEANUP_SIZE = 10, CLEANUP_PER_TICK = 60;
    public static final int BREAKABLE_MANA = 200, BREAKABLE_COOLDOWN = 6000, BREAKABLE_TICKS = 200, BREAKABLE_TNT = 64;
    /** Blocos que o Breakable nunca quebra (alem da camada mais baixa de bedrock). */
    public static final TagKey<Block> BREAKABLE_BLOCKED = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Nexus.MODID, "brekry_bloqueados"));
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.parse("c:ores"));

    private static final Set<UUID> DYNAMITES = new HashSet<>();
    private static final Map<UUID, Long> BREAKABLE = new HashMap<>();

    private static BrekryItem item() { return ModItems.BREKRY.get(); }

    // ---------- Nv 1 — Dinamite ----------

    public static boolean dynamite(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        BlockHitResult hit = RelicUtil.targetBlock(player, 5);
        BlockPos pos = hit == null ? player.blockPosition() : hit.getBlockPos().relative(hit.getDirection());
        if (!level.mayInteract(player, pos)) return false;
        PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
        tnt.setFuse(DYNAMITE_FUSE);
        tnt.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(tnt);
        DYNAMITES.add(tnt.getUUID());
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    /** A dinamite so quebra blocos: nao fere nem empurra entidades. */
    @SubscribeEvent
    public static void detonate(ExplosionEvent.Detonate event) {
        var source = event.getExplosion().getDirectSourceEntity();
        if (source == null || !DYNAMITES.remove(source.getUUID())) return;
        event.getAffectedEntities().clear();
        if (event.getExplosion().getIndirectSourceEntity() instanceof ServerPlayer player) {
            item().addMasteryXp(player, Math.max(1, event.getAffectedBlocks().size() / 20));
        }
    }

    // ---------- Nv 5 — Tunel ----------

    private static boolean breakable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && state.getDestroySpeed(level, pos) >= 0;
    }

    public static boolean tunnel(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Direction facing = player.getDirection();
        Direction side = facing.getClockWise();
        int depth = 5 + context.affinity() / 100;
        int width = context.affinity() >= 700 ? 3 : 1;
        int height = context.affinity() >= 400 ? 3 : 2;
        BlockPos start = player.blockPosition();
        int broken = 0;
        for (int d = 1; d <= depth; d++) {
            for (int w = -(width / 2); w <= width / 2; w++) {
                for (int h = 0; h < height; h++) {
                    BlockPos pos = start.relative(facing, d).relative(side, w).above(h);
                    BlockState state = level.getBlockState(pos);
                    if (!breakable(level, pos, state)) continue;
                    if (RelicUtil.breakBlock(player, pos, stack, drop -> Block.popResource(level, pos, drop))) broken++;
                }
            }
        }
        if (broken == 0) {
            RelicUtil.say(player, Component.translatable("relic.nexus.brekry.nada_para_abrir_nessa_direcao"));
            return false;
        }
        item().addMasteryXp(player, Math.max(1, broken / 10));
        return true;
    }

    // ---------- Nv 10 — Frenesi ----------

    public static boolean frenzy(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, FRENZY_TICKS + context.resonance() / 5, 2));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.6f);
        return true;
    }

    // ---------- Nv 15 — Limpeza ----------

    private static final class Cleanup {
        final UUID owner; final ServerLevel level; final Deque<BlockPos> queue; final List<ItemStack> drops = new ArrayList<>();
        final BlockPos chest; final Direction facing; final ItemStack tool;
        Cleanup(UUID owner, ServerLevel level, Deque<BlockPos> queue, BlockPos chest, Direction facing, ItemStack tool) {
            this.owner = owner; this.level = level; this.queue = queue; this.chest = chest; this.facing = facing; this.tool = tool;
        }
    }
    private static final List<Cleanup> CLEANUPS = new ArrayList<>();

    private static boolean nearFluid(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!level.getFluidState(pos.relative(direction)).isEmpty()) return true;
        }
        return false;
    }

    public static boolean cleanup(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        Direction facing = player.getDirection();
        Direction side = facing.getClockWise();
        BlockPos base = player.blockPosition();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (int d = 1; d <= CLEANUP_SIZE; d++) {
            for (int w = -CLEANUP_SIZE / 2; w < CLEANUP_SIZE / 2; w++) {
                for (int h = 0; h < CLEANUP_SIZE; h++) {
                    BlockPos pos = base.relative(facing, d).relative(side, w).above(h);
                    BlockState state = level.getBlockState(pos);
                    if (!breakable(level, pos, state) || state.is(ORES) || nearFluid(level, pos)) continue;
                    queue.add(pos);
                }
            }
        }
        if (queue.isEmpty()) {
            RelicUtil.say(player, Component.translatable("relic.nexus.brekry.nada_para_limpar_a_frente"));
            return false;
        }
        CLEANUPS.add(new Cleanup(player.getUUID(), level, queue, base.relative(facing), facing, stack.copy()));
        level.playSound(null, base, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.4f);
        return true;
    }

    /** Junta o drop nas pilhas que ja existem (packs completos), em vez de um item por slot. */
    static void merge(List<ItemStack> stacks, ItemStack drop) {
        for (ItemStack stack : stacks) {
            if (drop.isEmpty()) return;
            if (!ItemStack.isSameItemSameComponents(stack, drop)) continue;
            int move = Math.min(drop.getCount(), stack.getMaxStackSize() - stack.getCount());
            if (move <= 0) continue;
            stack.grow(move);
            drop.shrink(move);
        }
        if (!drop.isEmpty()) stacks.add(drop.copy());
    }

    private static void finish(Cleanup job, ServerPlayer player) {
        ServerLevel level = job.level;
        BlockPos pos = job.chest;
        List<ItemStack> drops = job.drops;
        int index = 0;
        int row = 0;
        Direction chestFacing = job.facing.getOpposite();
        // Bau duplo virado para o jogador: a metade LEFT liga com a vizinha no sentido horario da frente do bau.
        Direction connect = chestFacing.getClockWise();
        while (index < drops.size() && row < 8) {
            BlockPos first = pos.relative(job.facing, row), second = first.relative(connect);
            row++;
            if (!level.getBlockState(first).canBeReplaced() || !level.getBlockState(second).canBeReplaced()) continue;
            level.setBlock(first, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, chestFacing).setValue(ChestBlock.TYPE, ChestType.LEFT), 3);
            level.setBlock(second, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, chestFacing).setValue(ChestBlock.TYPE, ChestType.RIGHT), 3);
            for (BlockPos half : List.of(first, second)) {
                if (!(level.getBlockEntity(half) instanceof ChestBlockEntity chest)) continue;
                for (int slot = 0; slot < chest.getContainerSize() && index < drops.size(); slot++) chest.setItem(slot, drops.get(index++));
            }
        }
        for (; index < drops.size(); index++) {
            if (player != null) RelicUtil.giveOrDrop(player, drops.get(index));
            else Block.popResource(level, pos, drops.get(index));
        }
    }

    // ---------- Nv 20 — Breakable ----------

    public static boolean canBreakable(ServerPlayer player, ItemStack stack) {
        return RelicUtil.hasRank(RelicUtil.magic(player).getAfinidade(), Ranks.A)
                && RelicUtil.count(player, s -> s.is(Items.TNT)) >= BREAKABLE_TNT;
    }

    public static boolean breakable(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        RelicUtil.consume(player, s -> s.is(Items.TNT), BREAKABLE_TNT);
        BREAKABLE.put(player.getUUID(), player.level().getGameTime() + BREAKABLE_TICKS);
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.8f);
        RelicUtil.say(player, Component.translatable("relic.nexus.brekry.breakable_10s_para_quebrar_qualquer"));
        return true;
    }

    private static boolean breakableActive(ServerPlayer player) {
        Long until = BREAKABLE.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!(player.getMainHandItem().getItem() instanceof BrekryItem) || !breakableActive(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(BREAKABLE_BLOCKED) || (state.is(Blocks.BEDROCK) && pos.getY() == level.getMinY())) return;
        if (!level.mayInteract(player, pos)) return;
        if (CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled()) return;
        event.setCanceled(true);
        ItemStack self = new ItemStack(state.getBlock().asItem());
        level.destroyBlock(pos, false, player);
        if (!self.isEmpty()) Block.popResource(level, pos, self);
        level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        item().addMasteryXp(player, 1);
    }

    // ---------- Ticks ----------

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Iterator<Cleanup> it = CLEANUPS.iterator();
        while (it.hasNext()) {
            Cleanup job = it.next();
            if (job.level != level) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(job.owner);
            for (int i = 0; i < CLEANUP_PER_TICK && !job.queue.isEmpty() && player != null; i++) {
                BlockPos pos = job.queue.poll();
                BlockState state = level.getBlockState(pos);
                if (!breakable(level, pos, state) || nearFluid(level, pos)) continue;
                RelicUtil.breakBlock(player, pos, job.tool, drop -> merge(job.drops, drop));
            }
            if (job.queue.isEmpty() || player == null) {
                finish(job, player);
                if (player != null) {
                    item().addMasteryXp(player, Math.max(1, job.drops.size() / 20));
                    RelicUtil.say(player, Component.translatable("relic.nexus.brekry.limpeza_concluida"));
                }
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { BREAKABLE.remove(event.getEntity().getUUID()); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        DYNAMITES.clear();
        BREAKABLE.clear();
        CLEANUPS.clear();
    }
}
