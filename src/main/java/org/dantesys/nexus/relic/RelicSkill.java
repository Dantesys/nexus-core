package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Habilidade de reliquia. Sao acumuladas: uma nova a cada 5 niveis de maestria (1, 5, 10, 15, 20),
 * sem substituir as anteriores. A ultima (nivel 20) e a habilidade final.
 * O efeito deve retornar true somente quando realmente produzir um resultado.
 */
public record RelicSkill(Component name, Kind kind, Component conditionDescription,
                         BiPredicate<ServerPlayer, ItemStack> condition, RelicAbility effect, int manaCost) {
    public enum Kind { ACTIVE, PASSIVE }
    public static final int MAX_SKILLS = 5;

    public RelicSkill {
        if (manaCost < -1) throw new IllegalArgumentException("Invalid skill mana cost");
        Objects.requireNonNull(name);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(conditionDescription);
        Objects.requireNonNull(condition);
        Objects.requireNonNull(effect);
    }
    /** -1 conserva o custo padrao da reliquia. */
    public RelicSkill(Component name, Kind kind, Component conditionDescription,
                      BiPredicate<ServerPlayer, ItemStack> condition, RelicAbility effect) {
        this(name, kind, conditionDescription, condition, effect, -1);
    }
    public static RelicSkill active(Component name, int manaCost, RelicAbility effect) {
        return new RelicSkill(name, Kind.ACTIVE, Component.empty(), (player, stack) -> true, effect, manaCost);
    }
    public static RelicSkill passive(Component name, int manaCost, RelicAbility effect) {
        return new RelicSkill(name, Kind.PASSIVE, Component.empty(), (player, stack) -> true, effect, manaCost);
    }
    public static RelicSkill active(Component name, RelicAbility effect) {
        return new RelicSkill(name, Kind.ACTIVE, Component.empty(), (player, stack) -> true, effect);
    }

    /** Nivel de maestria que libera cada slot: 1, 5, 10, 15, 20. */
    public static int requiredLevel(int slot) {
        if (slot < 0 || slot >= MAX_SKILLS) throw new IllegalArgumentException("Invalid relic skill slot: " + slot);
        return slot == 0 ? 1 : slot * 5;
    }

    /** Estagio visual (0 a 3): dormente, desperta (nv 5), ressonante (nv 15), final (nv 20). */
    public static int visualStage(int masteryLevel) {
        if (masteryLevel >= requiredLevel(4)) return 3;
        if (masteryLevel >= requiredLevel(3)) return 2;
        if (masteryLevel >= requiredLevel(1)) return 1;
        return 0;
    }

    /** Proxima habilidade ATIVA liberada (passivas nao sao selecionaveis). Retorna -1 se nao houver. */
    public static int nextActive(int current, List<RelicSkill> skills, int level, boolean next) {
        int count = skills.size();
        if (count == 0) return -1;
        for (int step = 1; step <= count; step++) {
            int candidate = Math.floorMod(current + (next ? step : -step), count);
            if (skills.get(candidate).kind() == Kind.ACTIVE && level >= requiredLevel(candidate)) return candidate;
        }
        return -1;
    }
}
