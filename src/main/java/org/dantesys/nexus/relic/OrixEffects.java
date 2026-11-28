package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.util.Ranks;

import java.util.*;

/** Efeitos da Orix (documento de design). Valores provisorios. */
@EventBusSubscriber(modid = Nexus.MODID)
public final class OrixEffects {
    private OrixEffects() {}

    public static final int SONAR_MANA = 40, SONAR_COOLDOWN = 300;
    public static final int MIDAS_MANA = 60, MIDAS_COOLDOWN = 200, MIDAS_DISTANCE = 5;
    public static final int MINING_OFF_MANA = 120, MINING_OFF_COOLDOWN = 2400, AREA_HALF = 5;
    public static final int ALL_IN_MANA = 200, ALL_IN_COOLDOWN = 6000, ALL_IN_TICKS = 1200, ALL_IN_MIN_ITEMS = 64, ALL_IN_FORTUNE = 3;
    /** Maestria que libera cada faixa de raridade. */
    public static final int[] TIER_LEVEL = {0, 1, 5, 10, 20};
    private static final String FORTUNE_TAG = "OrixFortunaAte";

    private record Drop(Item item, int weight, int tier, boolean blackstoneOnly) {}
    private static final List<Drop> DROPS = List.of(
            new Drop(Items.COAL, 30, 1, false), new Drop(Items.RAW_COPPER, 25, 1, false),
            new Drop(Items.RAW_IRON, 15, 2, false), new Drop(Items.RAW_GOLD, 8, 2, false),
            new Drop(Items.REDSTONE, 12, 2, false), new Drop(Items.LAPIS_LAZULI, 10, 2, false),
            new Drop(Items.QUARTZ, 15, 2, true), new Drop(Items.DIAMOND, 3, 3, false),
            new Drop(Items.EMERALD, 2, 3, false), new Drop(Items.NETHERITE_SCRAP, 1, 4, false));

    private record OreBlock(Block block, int weight, int tier) {}
    private static final List<OreBlock> STONE_ORES = List.of(
            new OreBlock(Blocks.COAL_ORE, 30, 1), new OreBlock(Blocks.COPPER_ORE, 25, 1), new OreBlock(Blocks.IRON_ORE, 15, 2),
            new OreBlock(Blocks.GOLD_ORE, 8, 2), new OreBlock(Blocks.REDSTONE_ORE, 12, 2), new OreBlock(Blocks.LAPIS_ORE, 10, 2),
            new OreBlock(Blocks.DIAMOND_ORE, 3, 3), new OreBlock(Blocks.EMERALD_ORE, 2, 3));
    private static final List<OreBlock> DEEPSLATE_ORES = List.of(
            new OreBlock(Blocks.DEEPSLATE_COAL_ORE, 30, 1), new OreBlock(Blocks.DEEPSLATE_COPPER_ORE, 25, 1),
            new OreBlock(Blocks.DEEPSLATE_IRON_ORE, 15, 2), new OreBlock(Blocks.DEEPSLATE_GOLD_ORE, 8, 2),
            new OreBlock(Blocks.DEEPSLATE_REDSTONE_ORE, 12, 2), new OreBlock(Blocks.DEEPSLATE_LAPIS_ORE, 10, 2),
            new OreBlock(Blocks.DEEPSLATE_DIAMOND_ORE, 3, 3), new OreBlock(Blocks.DEEPSLATE_EMERALD_ORE, 2, 3));
    private static final List<OreBlock> BLACKSTONE_ORES = List.of(
            new OreBlock(Blocks.NETHER_GOLD_ORE, 20, 2), new OreBlock(Blocks.NETHER_QUARTZ_ORE, 20, 2),
            new OreBlock(Blocks.ANCIENT_DEBRIS, 2, 4));

    private static OrixItem item() { return ModItems.ORIX.get(); }

    private static TagKey<Block> blockTag(String id) {
        return TagKey.create(Registries.BLOCK, Identifier.parse(id));
    }
    private static final TagKey<Block> ORES = blockTag("c:ores");

    /** Faixa de raridade de um bloco/minerio resultante (para respeitar a maestria). */
    static int tierOf(Block block) {
        if (block == Blocks.ANCIENT_DEBRIS) return 4;
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE || block == Blocks.EMERALD_ORE
                || block == Blocks.DEEPSLATE_EMERALD_ORE) return 3;
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE || block == Blocks.COPPER_ORE
                || block == Blocks.DEEPSLATE_COPPER_ORE || block == Blocks.RAW_COPPER_BLOCK) return 1;
        return 2;
    }

    static boolean unlocked(int level, int tier) { return level >= TIER_LEVEL[Math.clamp(tier, 1, 4)]; }

    /** Chance de Minerar Tudo por bloco (0 se o bloco nao conta). */
    static double blockChance(Block block, int level) {
        if (block == Blocks.STONE) return 0.01 + level * 0.001;
        if (block == Blocks.DEEPSLATE) return 0.04 + level * 0.002;
        if (block == Blocks.BLACKSTONE) return 0.025 + level * 0.0015;
        return 0;
    }

    private static <T> T pick(List<T> options, java.util.function.ToIntFunction<T> weight, RandomSource random) {
        int total = 0;
        for (T option : options) total += weight.applyAsInt(option);
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (T option : options) {
            roll -= weight.applyAsInt(option);
            if (roll < 0) return option;
        }
        return null;
    }

    private static ItemStack rollDrop(int level, boolean blackstone, RandomSource random) {
        List<Drop> pool = DROPS.stream().filter(d -> unlocked(level, d.tier()) && (!d.blackstoneOnly() || blackstone)).toList();
        Drop drop = pick(pool, Drop::weight, random);
        return drop == null ? ItemStack.EMPTY : new ItemStack(drop.item());
    }

    // ---------- Nv 1 — Minerar Tudo (passiva) + XP tematica ----------

    private static boolean bulk;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void blockBroken(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(player.getMainHandItem().getItem() instanceof OrixItem orix)) return;
        BlockState state = event.getState();
        ServerLevel level = (ServerLevel) player.level();
        if (state.is(ORES) && !bulk) orix.addMasteryXp(player, 1);
        if (!orix.isUnlocked(player, 0)) return;
        int mastery = orix.masteryLevel(player);
        double chance = blockChance(state.getBlock(), mastery);
        if (chance > 0 && level.getRandom().nextDouble() < chance) {
            ItemStack drop = rollDrop(mastery, state.is(Blocks.BLACKSTONE), level.getRandom());
            if (!drop.isEmpty()) Block.popResource(level, event.getPos(), drop);
        }
    }

    @SubscribeEvent
    public static void mobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!(player.getMainHandItem().getItem() instanceof OrixItem orix) || !orix.isUnlocked(player, 0)) return;
        if (!(event.getEntity() instanceof Enemy)) return;
        ServerLevel level = (ServerLevel) player.level();
        int mastery = orix.masteryLevel(player);
        boolean boss = RelicUtil.isBoss(event.getEntity());
        if (!boss && level.getRandom().nextDouble() >= 0.05 + mastery * 0.005) return;
        ItemStack drop = rollDrop(mastery, false, level.getRandom());
        if (!drop.isEmpty()) event.getEntity().spawnAtLocation(level, drop);
    }

    // ---------- Nv 5 — Sonar ----------

    private static int colorFor(BlockState state) {
        for (var entry : ConfigLists.pairs(Config.ORIX_SONAR_COLORS.get()).entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null || !state.is(TagKey.create(Registries.BLOCK, id))) continue;
            try { return Integer.parseInt(entry.getValue().replace("#", ""), 16); } catch (NumberFormatException ignored) {}
        }
        return 0xFFFFFF;
    }

    public static boolean sonar(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        int half = 5 + context.affinity() / 200;
        int ticks = Math.min(400, 200 + item().masteryLevel(player) * 5 + context.resonance() / 10);
        BlockPos center = player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-half, -half, -half), center.offset(half, half, half))) {
            if (level.getBlockState(pos).is(ORES)) found.add(pos.immutable());
        }
        found.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        Map<BlockPos, Integer> outlines = new LinkedHashMap<>();
        for (BlockPos pos : found.subList(0, Math.min(400, found.size()))) outlines.put(pos, colorFor(level.getBlockState(pos)));
        PrivateVisuals.outlineBlocks(player, outlines, ticks);
        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.6f);
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48;
            level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX() + Math.cos(angle) * 2, player.getY() + 1,
                    player.getZ() + Math.sin(angle) * 2, 0, Math.cos(angle), 0, Math.sin(angle), 0.3);
        }
        RelicUtil.say(player, found.isEmpty() ? Component.translatable("relic.nexus.orix.sonar_nenhum") : Component.translatable("relic.nexus.orix.sonar_minerios", found.size()));
        return true;
    }

    // ---------- Nv 10 — Toque de Midas ----------

    static Block midasResult(BlockState state, int level, RandomSource random) {
        List<OreBlock> pool = state.is(Blocks.STONE) ? STONE_ORES : state.is(Blocks.DEEPSLATE) ? DEEPSLATE_ORES
                : state.is(Blocks.BLACKSTONE) ? BLACKSTONE_ORES : null;
        if (pool != null) {
            OreBlock ore = pick(pool.stream().filter(o -> unlocked(level, o.tier())).toList(), OreBlock::weight, random);
            return ore == null ? null : ore.block();
        }
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String to = ConfigLists.pairs(Config.ORIX_MIDAS.get()).get(id);
        Identifier key = to == null ? null : Identifier.tryParse(to);
        if (key == null) return null;
        Block result = BuiltInRegistries.BLOCK.getValue(key);
        return result == Blocks.AIR || !unlocked(level, tierOf(result)) ? null : result;
    }

    public static boolean midas(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        BlockHitResult hit = RelicUtil.targetBlock(player, MIDAS_DISTANCE);
        if (hit == null) {
            RelicUtil.say(player, Component.translatable("relic.nexus.orix.toque_um_bloco_ate_blocos", MIDAS_DISTANCE));
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)) return false;
        Block result = midasResult(level.getBlockState(pos), item().masteryLevel(player), level.getRandom());
        if (result == null) {
            RelicUtil.say(player, Component.translatable("relic.nexus.orix.o_toque_de_midas_nao"));
            return false;
        }
        level.setBlock(pos, result.defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.1);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    // ---------- Nv 15 — Mineradinha Off ----------

    /** Quebra os minerios em volta mandando os drops para o inventario; retorna quantos quebrou. */
    private static int mineOres(ServerPlayer player, ItemStack tool, BlockPos center) {
        int count = 0;
        bulk = true;
        try {
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-AREA_HALF, -AREA_HALF, -AREA_HALF), center.offset(AREA_HALF - 1, AREA_HALF - 1, AREA_HALF - 1))) {
                if (!player.level().getBlockState(pos).is(ORES)) continue;
                if (RelicUtil.breakBlock(player, pos.immutable(), tool, drop -> RelicUtil.giveOrDrop(player, drop))) count++;
            }
        } finally {
            bulk = false;
        }
        return count;
    }

    public static boolean miningOff(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        int count = mineOres(player, stack, player.blockPosition());
        if (count == 0) {
            RelicUtil.say(player, Component.translatable("relic.nexus.orix.nenhum_minerio_por_perto"));
            return false;
        }
        item().addMasteryXp(player, Math.max(1, count / 5));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 0.8f);
        RelicUtil.say(player, Component.translatable("relic.nexus.orix.mineradinha_off_minerios", count));
        return true;
    }

    // ---------- Nv 20 — All In ----------

    private static final Map<Item, Block[]> ALL_IN_BLOCKS = Map.of(
            // {pedra, ardosia, nether}
            Items.COAL, new Block[]{Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COAL_ORE},
            Items.RAW_COPPER, new Block[]{Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.COPPER_ORE},
            Items.RAW_IRON, new Block[]{Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.IRON_ORE},
            Items.RAW_GOLD, new Block[]{Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE},
            Items.REDSTONE, new Block[]{Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.REDSTONE_ORE},
            Items.LAPIS_LAZULI, new Block[]{Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.LAPIS_ORE},
            Items.DIAMOND, new Block[]{Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DIAMOND_ORE},
            Items.EMERALD, new Block[]{Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.EMERALD_ORE},
            Items.QUARTZ, new Block[]{Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_QUARTZ_ORE},
            Items.NETHERITE_SCRAP, new Block[]{Blocks.ANCIENT_DEBRIS, Blocks.ANCIENT_DEBRIS, Blocks.ANCIENT_DEBRIS});

    private record Converted(BlockState original, BlockState placed) {}
    private static final class Session {
        final ServerLevel level; final long until; final Map<BlockPos, Converted> blocks = new HashMap<>();
        Session(ServerLevel level, long until) { this.level = level; this.until = until; }
    }
    private static final List<Session> SESSIONS = new ArrayList<>();

    public static boolean canAllIn(ServerPlayer player, ItemStack stack) {
        return RelicUtil.hasRank(RelicUtil.magic(player).getRessonancia(), Ranks.A)
                && RelicUtil.count(player, s -> ALL_IN_BLOCKS.containsKey(s.getItem())) >= ALL_IN_MIN_ITEMS;
    }

    private static boolean natural(BlockState state) {
        return state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(BlockTags.BASE_STONE_NETHER);
    }

    public static boolean allIn(ServerPlayer player, ItemStack stack, RelicAbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        int mined = mineOres(player, stack, center);
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-AREA_HALF, -AREA_HALF, -AREA_HALF), center.offset(AREA_HALF - 1, AREA_HALF - 1, AREA_HALF - 1))) {
            BlockState state = level.getBlockState(pos);
            if (natural(state) && !state.hasBlockEntity() && level.mayInteract(player, pos)) candidates.add(pos.immutable());
        }
        Collections.shuffle(candidates, new Random(level.getRandom().nextLong()));
        Session session = new Session(level, level.getGameTime() + ALL_IN_TICKS);
        int index = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && index < candidates.size(); slot++) {
            ItemStack item = inventory.getItem(slot);
            Block[] blocks = ALL_IN_BLOCKS.get(item.getItem());
            if (blocks == null) continue;
            while (!item.isEmpty() && index < candidates.size()) {
                BlockPos pos = candidates.get(index++);
                BlockState original = level.getBlockState(pos);
                int variant = original.is(BlockTags.BASE_STONE_NETHER) ? 2 : original.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) ? 1 : 0;
                BlockState placed = blocks[variant].defaultBlockState();
                level.setBlock(pos, placed, 3);
                session.blocks.put(pos, new Converted(original, placed));
                item.shrink(1);
            }
        }
        SESSIONS.add(session);
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, ALL_IN_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ALL_IN_TICKS, 1));
        long until = level.getGameTime() + ALL_IN_TICKS;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(FORTUNE_TAG, until));
        level.playSound(null, center, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.5f);
        RelicUtil.say(player, Component.translatable("relic.nexus.orix.all_in_minerios_espalhados", session.blocks.size(), (mined > 0 ? Component.translatable("relic.nexus.orix.minerados", mined) : Component.empty())));
        return true;
    }

    private static void revert(Session session) {
        session.blocks.forEach((pos, c) -> {
            if (session.level.getBlockState(pos) == c.placed()) session.level.setBlock(pos, c.original(), 3);
        });
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Iterator<Session> it = SESSIONS.iterator();
        while (it.hasNext()) {
            Session session = it.next();
            if (session.level == level && level.getGameTime() >= session.until) {
                revert(session);
                it.remove();
            }
        }
    }

    /** Antes de salvar o mundo, tudo volta ao original. */
    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        SESSIONS.forEach(OrixEffects::revert);
        SESSIONS.clear();
    }

    /** Fortuna temporaria do All In. */
    @SubscribeEvent
    public static void fortune(GetEnchantmentLevelEvent event) {
        if (!(event.getStack() instanceof ItemStack stack) || !(stack.getItem() instanceof OrixItem)) return;
        long until = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(FORTUNE_TAG, 0L);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (until <= 0 || server == null || server.overworld().getGameTime() >= until) return;
        if (!event.isTargetting(Enchantments.FORTUNE)) return;
        event.getHolder(Enchantments.FORTUNE).ifPresent(holder -> {
            var enchantments = event.getEnchantments();
            enchantments.set(holder, enchantments.getLevel(holder) + ALL_IN_FORTUNE);
        });
    }
}
