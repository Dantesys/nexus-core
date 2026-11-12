package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;
import java.util.function.BiPredicate;

/** O efeito deve retornar true somente quando realmente produzir um resultado. */
public record RelicSkill(Component name, Kind kind, Component conditionDescription,
                         BiPredicate<ServerPlayer, ItemStack> condition, RelicAbility effect, int manaCost) {
    public enum Kind { ACTIVE, PASSIVE }
    public RelicSkill {
        if (manaCost < -1) throw new IllegalArgumentException("Invalid skill mana cost");
        Objects.requireNonNull(name);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(conditionDescription);
        Objects.requireNonNull(condition);
        Objects.requireNonNull(effect);
    }
    /** -1 conserva o custo padrao da reliquia para definicoes antigas. */
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
    public static int requiredLevel(int slot) {
        return switch (slot) {
            case 0 -> 1;
            case 1 -> 32;
            case 2 -> 64;
            case 3 -> 100;
            default -> throw new IllegalArgumentException("Invalid relic skill slot: " + slot);
        };
    }

    public static int nextSlot(int current, int count, int level, boolean next) {
        if (count < 1 || count > 4) return 0;
        for (int step = 1; step <= count; step++) {
            int candidate = Math.floorMod(current + (next ? step : -step), count);
            if (level >= requiredLevel(candidate)) return candidate;
        }
        return 0;
    }
}
