package org.dantesys.nexus;

import net.neoforged.neoforge.common.ModConfigSpec;


public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue GENERATE_MAGIC_STATUS_ON_FIRST_JOIN = BUILDER
            .comment("Se verdadeiro, gera os status magicos no primeiro acesso do jogador ao mundo.",
                    "Se falso, os status so serao gerados ao interagir com a Bola de Cristal.")
            .define("generateMagicStatusOnFirstJoin", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}
