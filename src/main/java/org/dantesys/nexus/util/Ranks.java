package org.dantesys.nexus.util;

public enum Ranks {
    F,E,D,C,B,A,S;

    public Ranks cycle(boolean next) {
        Ranks[] ranks = values();
        int offset = next ? 1 : ranks.length - 1;
        return ranks[(ordinal() + offset) % ranks.length];
    }

    /** Rank de um status de mana (0 a 1000). */
    public static Ranks of(int value) {
        if (value < 1) return F;
        if (value < 170) return E;
        if (value < 340) return D;
        if (value < 510) return C;
        if (value < 680) return B;
        if (value < 850) return A;
        return S;
    }
}
