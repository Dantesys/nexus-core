package org.dantesys.nexus.relic;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.dantesys.nexus.util.Ranks;

/** Pontos para cristais de mana, separados apenas pelo rank da reliquia. */
public class RelicCrystalProgress implements ValueIOSerializable {
    private final int[] points = new int[Ranks.values().length];

    public int add(Ranks rank, int amount) {
        int index = rank.ordinal();
        points[index] += Math.max(0, amount);
        int crystals = points[index] / 100;
        points[index] %= 100;
        return crystals;
    }

    @Override public void serialize(ValueOutput output) {
        for (Ranks rank : Ranks.values()) output.putInt(rank.name(), points[rank.ordinal()]);
    }

    @Override public void deserialize(ValueInput input) {
        for (Ranks rank : Ranks.values()) points[rank.ordinal()] = Math.max(0, input.getIntOr(rank.name(), 0));
    }
}
