package org.dantesys.nexus;

import net.neoforged.neoforge.common.ModConfigSpec;


public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue GENERATE_MAGIC_STATUS_ON_FIRST_JOIN = BUILDER
            .comment("Se verdadeiro, gera os status magicos no primeiro acesso do jogador ao mundo.",
                    "Se falso, os status so serao gerados ao interagir com a Bola de Cristal.")
            .define("generateMagicStatusOnFirstJoin", false);

    // ---------- Krefty ----------
    public static final ModConfigSpec.IntValue KREFTY_DEFAULT_COST;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> KREFTY_BLOCK_COSTS;
    public static final ModConfigSpec.IntValue KREFTY_MAX_AREA_SIDE;
    public static final ModConfigSpec.IntValue KREFTY_MAX_BLUEPRINT_SIDE;

    static {
        BUILDER.push("krefty");
        KREFTY_DEFAULT_COST = BUILDER
                .comment("Custo padrao de mana por bloco colocado pela Krefty.")
                .defineInRange("custoPadrao", 2, 0, 10000);
        KREFTY_BLOCK_COSTS = BUILDER
                .comment("Custo e maestria minima por bloco ou tag: \"id=custo@nivel\" ou \"#tag=custo@nivel\".",
                        "O primeiro que combinar vale (ids antes de tags). Blocos sempre bloqueados: tag nexus:krefty_bloqueados.")
                .defineListAllowEmpty("custosPorBloco", java.util.List.of(
                        "minecraft:netherite_block=256@20", "minecraft:ancient_debris=128@20", "minecraft:beacon=256@20",
                        "minecraft:diamond_block=96@15", "minecraft:emerald_block=96@15", "#c:ores/diamond=48@15",
                        "#c:ores/emerald=48@15", "#c:ores/netherite_scrap=96@20", "minecraft:gold_block=48@10",
                        "minecraft:lapis_block=24@10", "minecraft:redstone_block=16@10", "#c:ores=16@10",
                        "minecraft:iron_block=24@5", "minecraft:copper_block=8@5", "#c:storage_blocks=24@10",
                        "#c:obsidians=8@5", "minecraft:crying_obsidian=12@5", "#minecraft:shulker_boxes=32@10"),
                        () -> "minecraft:stone=1@1", o -> o instanceof String text && text.contains("="));
        KREFTY_MAX_AREA_SIDE = BUILDER
                .comment("Lado maximo das Areas de Construcao e de Demolicao.")
                .defineInRange("ladoMaximoArea", 16, 2, 64);
        KREFTY_MAX_BLUEPRINT_SIDE = BUILDER
                .comment("Lado maximo de uma planta da Construcao Instantanea.")
                .defineInRange("ladoMaximoPlanta", 32, 2, 64);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
