package org.dantesys.nexus.relic;

import org.dantesys.nexus.util.Ranks;

/**
 * Regras da Rupty (documento de design). Os fragmentos sao o recurso central:
 * ficam na lamina, nos alvos (marcas) ou flutuando no chao. Valores de balanceamento provisorios.
 */
public final class RuptyRules {
    private RuptyRules() {}

    // Fragmentos e marcas
    public static final int BASE_FRAGMENTS = 10, FULL_FRAGMENTS = 20;
    public static final int MARKS_PER_TARGET = 5, FULL_MARKS_PER_TARGET = 10;
    /** Nivel do Fragmentar (libera os outros 10 fragmentos e dobra o limite por alvo). */
    public static final int FRAGMENTAR_LEVEL = 15;
    /** Cada marca dura 20 s (timer proprio, nao renova) e depois volta para a espada. */
    public static final int MARK_DURATION = 400;
    /** Cada marca reduz 10% da defesa do alvo contra a Rupty. */
    public static final double DEFENSE_PER_MARK = 0.10;
    /** Cada fragmento fora da lamina reduz a espada (dano e alcance) em 5%. */
    public static final double SIZE_PER_FRAGMENT = 0.05;
    public static final int BLADE_PIECES = 5;

    // Nv 5 — Estocada Fragmentada
    public static final int STAB_MANA = 50, STAB_COOLDOWN = 60, STAB_FRAGMENTS = 5;
    public static final double STAB_RANGE = 5;
    public static final float STAB_DAMAGE = 6;
    public static final int FLOAT_DURATION = 400;
    public static final float FLOAT_DAMAGE = 3;
    public static final double FLOAT_RADIUS = 0.9;

    // Nv 10 — Uniao
    public static final int UNION_MANA = 100, UNION_COOLDOWN = 300, UNION_STRENGTH_TICKS = 200;
    public static final float UNION_DAMAGE_PER_FRAGMENT = 2;

    // Nv 20 — Sem Defesa (final)
    public static final Ranks FINAL_AFFINITY_RANK = Ranks.A;
    public static final int FINAL_MANA = 300, FINAL_COOLDOWN = 12000, FINAL_DURATION = 2400, FINAL_INTERVAL = 10;
    public static final double FINAL_RADIUS = 5;
    public static final float FINAL_MAGIC_DAMAGE = 2, FINAL_CUT_DAMAGE = 2;

    // XP tematica: aplicar marcas e chama-las de volta
    public static final int XP_PER_MARK = 1, XP_PER_RECALL = 1;

    public static int totalFragments(int masteryLevel) {
        return masteryLevel >= FRAGMENTAR_LEVEL ? FULL_FRAGMENTS : BASE_FRAGMENTS;
    }
    public static int marksPerTarget(int masteryLevel) {
        return masteryLevel >= FRAGMENTAR_LEVEL ? FULL_MARKS_PER_TARGET : MARKS_PER_TARGET;
    }
    /** Fracao da defesa (armadura e protecao) ignorada pelas marcas: 10% cada, ate 100%. */
    public static double defenseIgnored(int marks) {
        return Math.min(1.0, Math.max(0, marks) * DEFENSE_PER_MARK);
    }
    /** Multiplicador de dano e alcance da lamina conforme os fragmentos fora dela. */
    /** Alcance perde menos que o dano: 2% por fragmento fora (com 20 fora, fica em 60%). */
    public static final double REACH_PER_FRAGMENT = 0.02;
    public static double reachScale(int out) {
        return Math.max(0.0, 1.0 - Math.max(0, out) * REACH_PER_FRAGMENT);
    }
    public static double bladeScale(int out) {
        return Math.max(0.0, 1.0 - Math.max(0, out) * SIZE_PER_FRAGMENT);
    }
    /** Pecas da lamina escondidas no modelo (5 pecas; 20 fragmentos fora = lamina vazia). */
    public static int hiddenPieces(int out) {
        return (int) Math.min(BLADE_PIECES, Math.round(Math.max(0, out) * SIZE_PER_FRAGMENT * BLADE_PIECES));
    }
    /** Marcas que a Estocada deixa no alvo: perto quase 5; longe menos, porque parte fica pelo caminho. */
    public static int stabMarks(double distance) {
        if (distance <= 1.5) return STAB_FRAGMENTS;
        double t = Math.min(1.0, (distance - 1.5) / (STAB_RANGE - 1.5));
        return (int) Math.max(2, Math.round(STAB_FRAGMENTS - t * 3));
    }
    /** Forca proporcional aos fragmentos que voltaram: 1-4 = I, 5-8 = II ... 17-20 = V. */
    public static int strengthAmplifier(int returned) {
        return Math.clamp((Math.max(1, returned) - 1) / 4, 0, 4);
    }
    public static boolean hasFinalRank(int affinity) {
        return Ranks.of(affinity).ordinal() >= FINAL_AFFINITY_RANK.ordinal();
    }
    /** Sem Defesa: Afinidade rank A+, os 20 fragmentos na lamina e menos de 50% de vida. */
    public static boolean canUseFinal(int affinity, int blade, int total, float health, float maxHealth) {
        return hasFinalRank(affinity) && total == FULL_FRAGMENTS && blade == total && health > 0 && health < maxHealth * 0.5f;
    }
}
