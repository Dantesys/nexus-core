package org.dantesys.nexus.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.dantesys.nexus.Config;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.util.Ranks;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras da Krefty (documento de design). A mana vira material de construcao:
 * cada bloco tem custo e maestria minima (tabela configuravel); alguns sao sempre bloqueados.
 */
public final class KreftyRules {
    private KreftyRules() {}

    /** Spawner e blocos de criativo; blocos inquebraveis tambem sao sempre bloqueados. */
    public static final TagKey<Block> BLOCKED = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Nexus.MODID, "krefty_bloqueados"));

    public static final int MAX_TARGET_DISTANCE = 32, COPY_DISTANCE = 5;
    // Nv 5 — Area de Construcao
    public static final int BUILD_AREA_MANA = 30, BUILD_AREA_COOLDOWN = 600, BUILD_AREA_DURATION = 1200;
    public static final float BRICK_DAMAGE = 2;
    public static final int HELMET_WEAR = 2;
    // Nv 10 — Copia "Perfeita"
    public static final int COPY_MANA = 10, COPY_COOLDOWN = 20;
    // Nv 15 — Area de Demolicao
    public static final int DEMOLITION_MANA = 40, DEMOLITION_COOLDOWN = 600, DEMOLITION_DURATION = 600;
    public static final float DEBRIS_DAMAGE = 2;
    public static final int DEBRIS_EFFECT_TICKS = 60;
    /** Mana extra das areas por bloco de volume. */
    public static final double AREA_MANA_PER_BLOCK = 0.1;
    public static final int AREA_PULSE = 20;
    /** As areas sobem esta altura acima dos pontos marcados (que normalmente sao o chao). */
    public static final int AREA_HEIGHT_ABOVE = 3;
    // Nv 20 — Construcao Instantanea
    public static final Ranks FINAL_CAPACITY_RANK = Ranks.A;
    public static final int FINAL_COOLDOWN = 6000, BUILD_TICKS = 30, FINAL_HUNGER = 6;
    // XP tematica: colocar blocos
    public static final int XP_PER_BLOCK = 1, BLUEPRINT_BLOCKS_PER_XP = 10;

    public record BlockRule(int cost, int minLevel) {}
    record Entry(String key, boolean tag, int cost, int minLevel) {}

    /** "id=custo@nivel" ou "#tag=custo@nivel"; nivel opcional (1). Retorna null se invalido. */
    static Entry parse(String text) {
        if (text == null) return null;
        int eq = text.indexOf('=');
        if (eq <= 0) return null;
        String key = text.substring(0, eq).trim();
        String value = text.substring(eq + 1).trim();
        int at = value.indexOf('@');
        try {
            int cost = Integer.parseInt((at < 0 ? value : value.substring(0, at)).trim());
            int level = at < 0 ? 1 : Integer.parseInt(value.substring(at + 1).trim());
            boolean tag = key.startsWith("#");
            if (tag) key = key.substring(1);
            if (cost < 0 || level < 1 || Identifier.tryParse(key) == null) return null;
            return new Entry(key, tag, cost, level);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<? extends String> cachedSource;
    private static List<Entry> cachedIds = List.of(), cachedTags = List.of();

    private static void refresh() {
        List<? extends String> source = Config.KREFTY_BLOCK_COSTS.get();
        if (source == cachedSource) return;
        List<Entry> ids = new ArrayList<>(), tags = new ArrayList<>();
        for (String text : source) {
            Entry entry = parse(text);
            if (entry == null) Nexus.LOGGER.warn("Krefty: custo invalido na config: {}", text);
            else (entry.tag() ? tags : ids).add(entry);
        }
        cachedIds = ids;
        cachedTags = tags;
        cachedSource = source;
    }

    public static BlockRule rule(BlockState state) {
        refresh();
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (Entry entry : cachedIds) if (entry.key().equals(id)) return new BlockRule(entry.cost(), entry.minLevel());
        for (Entry entry : cachedTags) {
            if (state.is(TagKey.create(Registries.BLOCK, Identifier.parse(entry.key())))) return new BlockRule(entry.cost(), entry.minLevel());
        }
        return new BlockRule(Config.KREFTY_DEFAULT_COST.get(), 1);
    }

    public static boolean isBlocked(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isAir() || state.is(BLOCKED) || state.getDestroySpeed(level, pos) < 0;
    }

    /** Blocos de duas partes (portas, camas, plantas altas) so pela outra mao. */
    public static boolean isMultiPart(BlockState state) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART);
    }

    public static int areaCost(int base, int volume) {
        return base + (int) Math.ceil(Math.max(0, volume) * AREA_MANA_PER_BLOCK);
    }

    public static boolean fits(BlockPos a, BlockPos b, int maxSide) {
        return Math.abs(a.getX() - b.getX()) < maxSide && Math.abs(a.getY() - b.getY()) < maxSide
                && Math.abs(a.getZ() - b.getZ()) < maxSide;
    }

    public static int volume(BlockPos a, BlockPos b) {
        return (Math.abs(a.getX() - b.getX()) + 1) * (Math.abs(a.getY() - b.getY()) + 1) * (Math.abs(a.getZ() - b.getZ()) + 1);
    }

    public static boolean hasFinalRank(int capacity) {
        return Ranks.of(capacity).ordinal() >= FINAL_CAPACITY_RANK.ordinal();
    }
}
