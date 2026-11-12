package org.dantesys.nexus.util;

public enum Ranks {
    F,E,D,C,B,A,S;

    public Ranks cycle(boolean next) {
        Ranks[] ranks = values();
        int offset = next ? 1 : ranks.length - 1;
        return ranks[(ordinal() + offset) % ranks.length];
    }
}
