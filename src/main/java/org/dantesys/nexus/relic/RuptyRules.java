package org.dantesys.nexus.relic;

/**
 * Regras da Rupty (documento de design, versao reformulada). Marcas de corte reduzem a defesa
 * do alvo contra a Rupty; o que passa de 100% de reducao vira dano extra.
 * Custos de mana sao provisorios (o documento nao os define).
 */
public final class RuptyRules {
    private RuptyRules() {}

    // Nv 1 — Ruptura Magica (passiva)
    /** Cada marca reduz 5% da defesa do alvo contra a Rupty. */
    public static final double DEFENSE_PER_MARK = 0.05;
    /** Cada marca tem contador proprio de 20 s (nao renova). */
    public static final int MARK_DURATION = 400;
    /** Limite de marcas por entidade (inclusive bosses). */
    public static final int MAX_MARKS = 10;

    // Nv 5 — Corte Vertical
    public static final int VERTICAL_MANA = 40, VERTICAL_COOLDOWN = 160, VERTICAL_MARKS = 3;
    public static final double VERTICAL_RANGE = 5;

    // Nv 10 — Rotacao
    public static final int ROTATION_MANA = 80, ROTATION_COOLDOWN = 300;
    public static final double ROTATION_RADIUS = 3, ROTATION_INNER_RADIUS = 1.5, ROTATION_DAMAGE = 1.5;
    public static final int ROTATION_INNER_MARKS = 5, ROTATION_OUTER_MARKS = 3;
    public static final double ROTATION_KNOCKBACK = 0.8;

    // Nv 15 — Marca Aberta
    public static final int OPEN_MANA = 120, OPEN_COOLDOWN = 1200;
    /** Marca aberta: dobro de efeito (10%) e de duracao (40 s). */
    public static final double OPEN_DEFENSE_PER_MARK = DEFENSE_PER_MARK * 2;
    public static final int OPEN_MARK_DURATION = MARK_DURATION * 2;
    /** Meio coracao a cada 2 s por alvo marcado (nao por marca). */
    public static final float OPEN_BLEED_DAMAGE = 1.0f;
    public static final int OPEN_BLEED_INTERVAL = 40;

    // Nv 20 — Marca Mortal (final)
    public static final int MORTAL_MANA = 300, MORTAL_COOLDOWN = 18000;
    public static final double MORTAL_DEFENSE = 0.5;
    public static final int MORTAL_INTERVAL = 40;

    // XP tematica: aplicar marcas de Ruptura Magica
    public static final int XP_PER_MARK = 1;

    /** Reducao total de defesa contra a Rupty (pode passar de 100%). */
    public static double defenseReduction(int marks, int openMarks, boolean mortal) {
        return Math.max(0, marks) * DEFENSE_PER_MARK + Math.max(0, openMarks) * OPEN_DEFENSE_PER_MARK
                + (mortal ? MORTAL_DEFENSE : 0);
    }
    /** Fracao da defesa (armadura e protecao) ignorada, ate 100%. */
    public static double defenseIgnored(double reduction) {
        return Math.clamp(reduction, 0.0, 1.0);
    }
    /** Multiplicador de dano: o que passa de 100% de reducao vira dano extra. */
    public static double damageMultiplier(double reduction) {
        return 1.0 + Math.max(0.0, reduction - 1.0);
    }
    /** Marcas da Rotacao: 5 perto do centro (ate 1,5 bloco), 3 na borda (ate 3 blocos), 0 fora. */
    public static int rotationMarks(double distance) {
        if (distance <= ROTATION_INNER_RADIUS) return ROTATION_INNER_MARKS;
        return distance <= ROTATION_RADIUS ? ROTATION_OUTER_MARKS : 0;
    }
    /** Dano da Marca Mortal a cada 2 s: metade do dano da espada consumida. */
    public static float mortalTickDamage(float swordDamage) {
        return Math.max(0f, swordDamage) * 0.5f;
    }
}
