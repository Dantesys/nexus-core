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


    // ---------- Orix ----------
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ORIX_SONAR_COLORS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ORIX_MIDAS;
    // ---------- Bastry ----------
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BASTRY_CONVERSIONS;
    // ---------- Skopy ----------
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SKOPY_EFFECTS;
    // ---------- Escordy ----------
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ESCORDY_MIX_EFFECTS;
    public static final ModConfigSpec.IntValue ESCORDY_FIREWORK_COLOR;
    // ---------- Agrix ----------
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> AGRIX_SEEDS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> AGRIX_FARM_MOBS;
    public static final ModConfigSpec.BooleanValue AGRIX_EGGS_IN_SPAWNERS;
    public static final ModConfigSpec.IntValue AGRIX_ANIMAL_LIMIT;

    static {
        java.util.function.Predicate<Object> text = o -> o instanceof String;
        BUILDER.push("orix");
        ORIX_SONAR_COLORS = BUILDER.comment("Cor do contorno do Sonar por tag de minerio: \"tag=RRGGBB\" (o primeiro que combinar vale).")
                .defineListAllowEmpty("coresSonar", java.util.List.of("c:ores/coal=4A4A4A", "c:ores/copper=E0734D", "c:ores/iron=D8AF93",
                        "c:ores/gold=FCEE4B", "c:ores/redstone=FF2020", "c:ores/lapis=2050E0", "c:ores/diamond=4AEDD9",
                        "c:ores/emerald=17DD62", "c:ores/quartz=F0E8DD", "c:ores/netherite_scrap=8A5A40", "c:ores=FFFFFF"),
                        () -> "c:ores=FFFFFF", text);
        ORIX_MIDAS = BUILDER.comment("Toque de Midas: \"bloco=resultado\". Pedra, ardosia e pedra negra viram minerio sorteado (fixo no codigo).")
                .defineListAllowEmpty("midas", java.util.List.of(
                        "minecraft:iron_ore=minecraft:raw_iron_block", "minecraft:deepslate_iron_ore=minecraft:raw_iron_block",
                        "minecraft:copper_ore=minecraft:raw_copper_block", "minecraft:deepslate_copper_ore=minecraft:raw_copper_block",
                        "minecraft:gold_ore=minecraft:raw_gold_block", "minecraft:deepslate_gold_ore=minecraft:raw_gold_block",
                        "minecraft:nether_gold_ore=minecraft:raw_gold_block",
                        "minecraft:coal_ore=minecraft:lapis_ore", "minecraft:deepslate_coal_ore=minecraft:deepslate_lapis_ore",
                        "minecraft:lapis_ore=minecraft:redstone_ore", "minecraft:deepslate_lapis_ore=minecraft:deepslate_redstone_ore",
                        "minecraft:redstone_ore=minecraft:diamond_ore", "minecraft:deepslate_redstone_ore=minecraft:deepslate_diamond_ore",
                        "minecraft:diamond_ore=minecraft:emerald_ore", "minecraft:deepslate_diamond_ore=minecraft:deepslate_emerald_ore",
                        "minecraft:nether_quartz_ore=minecraft:ancient_debris"), () -> "a=b", text);
        BUILDER.pop();
        BUILDER.push("bastry");
        BASTRY_CONVERSIONS = BUILDER.comment("Intocavel: efeito negativo=efeito positivo.")
                .defineListAllowEmpty("conversoes", java.util.List.of("minecraft:slowness=minecraft:speed", "minecraft:weakness=minecraft:strength",
                        "minecraft:poison=minecraft:regeneration", "minecraft:wither=minecraft:regeneration",
                        "minecraft:mining_fatigue=minecraft:haste", "minecraft:blindness=minecraft:night_vision",
                        "minecraft:darkness=minecraft:night_vision", "minecraft:hunger=minecraft:saturation",
                        "minecraft:levitation=minecraft:slow_falling", "minecraft:nausea=minecraft:resistance"), () -> "a=b", text);
        BUILDER.pop();
        BUILDER.push("skopy");
        SKOPY_EFFECTS = BUILDER.comment("Sinal Arcano: efeitos negativos sorteados.")
                .defineListAllowEmpty("efeitos", java.util.List.of("minecraft:slowness", "minecraft:weakness", "minecraft:mining_fatigue",
                        "minecraft:nausea"), () -> "minecraft:slowness", text);
        BUILDER.pop();
        BUILDER.push("escordy");
        ESCORDY_MIX_EFFECTS = BUILDER.comment("Mix: efeitos sorteados pelos chumbos (sem dano instantaneo).")
                .defineListAllowEmpty("efeitosMix", java.util.List.of("minecraft:slowness", "minecraft:weakness", "minecraft:poison",
                        "minecraft:nausea", "minecraft:blindness", "minecraft:hunger", "minecraft:levitation"), () -> "minecraft:poison", text);
        ESCORDY_FIREWORK_COLOR = BUILDER.comment("Cor dos foguetes (RGB decimal). -1 = cores aleatorias.")
                .defineInRange("corFoguetes", -1, -1, 0xFFFFFF);
        BUILDER.pop();
        BUILDER.push("agrix");
        AGRIX_SEEDS = BUILDER.comment("Ciclo da Vida: sementes sorteadas.")
                .defineListAllowEmpty("sementes", java.util.List.of("minecraft:wheat_seeds", "minecraft:carrot", "minecraft:potato",
                        "minecraft:beetroot_seeds"), () -> "minecraft:wheat_seeds", text);
        AGRIX_FARM_MOBS = BUILDER.comment("Fazendinha: mobs pacificos sorteados.")
                .defineListAllowEmpty("mobsFazendinha", java.util.List.of("minecraft:cow", "minecraft:pig", "minecraft:sheep",
                        "minecraft:chicken", "minecraft:rabbit"), () -> "minecraft:cow", text);
        AGRIX_EGGS_IN_SPAWNERS = BUILDER.comment("Permite usar ovos do De Volta ao Ovo em geradores de mobs.")
                .define("ovosEmGeradores", false);
        AGRIX_ANIMAL_LIMIT = BUILDER.comment("Crescimento Populacional: acima desse numero de animais na area, nao acelera.")
                .defineInRange("limiteAnimais", 40, 1, 1000);
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
