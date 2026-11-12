package org.dantesys.nexus.relic;

/** Valores de balanceamento provisorios; tempos e limiares seguem as regras da Rupty. */
public final class RuptyRules {
    private RuptyRules() {}
    public static final int SCAR_MANA = 5, CUT_MANA = 60, ZONE_MANA = 120, RUPTURE_MANA = 200;
    public static final float CUT_DAMAGE = 8, ZONE_DAMAGE = 3, BURST_DAMAGE = 10, RUPTURE_DAMAGE = 40;
    public static final double DAMAGE_PER_SCAR = 0.05;
    public static final int DECAY_DELAY = 100, DECAY_INTERVAL = 20, ZONE_DURATION = 200, ZONE_INTERVAL = 10, CHARGE_TICKS = 40;
    public static final double CUT_RANGE = 16, ZONE_RADIUS = 5, BURST_RADIUS = 4, RUPTURE_RANGE = 12;
    public static final int HEAT_MAX = 10, HEAT_FIRE_TICKS = 10, FIRE_TICKS = 80;
    public static final float HEAL_RATIO = 0.05f, HEAL_HIT_CAP = 2, CUT_HEAL = 1, ZONE_HEAL = 0.25f, KILL_HEAL_RATIO = 0.1f;
    public static final int BLIND_TICKS = 40, DEBUFF_TICKS = 60, RUPTURE_DEBUFF_TICKS = 100;
    public static final int METAL_SCAR_STEP = 5, ELECTRIC_SCAR_STEP = 5, ZONE_ELECTRIC_INTERVAL = 40;
    public static final double CHAIN_RADIUS = 4;
    public static final int CHAIN_TARGETS = 2;
    public static final float ELECTRIC_DAMAGE = 2, RUPTURE_ELECTRIC_DAMAGE = 8;
    public static double weakness(int scars) { return Math.min(0.5, Math.max(0, scars) * 0.02); }
    public static double vulnerability(int scars) { return Math.min(0.3, Math.max(0, scars) * 0.02); }
    public static double penetration(int scars, int slot, int consecutivePulses) {
        double base = Math.min(0.4, Math.max(0, scars) / METAL_SCAR_STEP * 0.1);
        return Math.min(0.8, base + switch (slot) {
            case 1 -> 0.25;
            case 2 -> Math.min(0.5, Math.max(0, consecutivePulses) * 0.05);
            case 3 -> 0.65;
            default -> 0;
        });
    }
    public static float healing(float actualDamage) { return Math.min(HEAL_HIT_CAP, Math.max(0, actualDamage) * HEAL_RATIO); }
    public static boolean crossedElectricThreshold(int previous, int current) {
        return current / ELECTRIC_SCAR_STEP > previous / ELECTRIC_SCAR_STEP;
    }
    public static boolean canCharge(float health, float maxHealth, int mana, int maxMana) {
        return health > 0 && health <= maxHealth * 0.5f && maxMana > 0 && mana >= maxMana * 0.5;
    }
    public static boolean canExecute(int scars, boolean boss, float health, float maxHealth) {
        return scars > 0 && (!boss || health < maxHealth * 0.2f);
    }
    public static int remainingScars(int count, long elapsed) {
        if (elapsed < DECAY_DELAY) return count;
        return (int) Math.max(0L, count - 1L - (elapsed - DECAY_DELAY) / DECAY_INTERVAL);
    }
}
