package org.dantesys.nexus.items;

import net.minecraft.world.item.Item;
import org.dantesys.nexus.util.Ranks;

/** Cristal de mana com rank fixo definido pelo próprio item registrado. */
public class RelicCrystalItem extends Item {
    private final Ranks rank;

    public RelicCrystalItem(Properties properties, Ranks rank) {
        super(properties);
        this.rank = rank;
    }

    public Ranks getRank() {
        return rank;
    }
}
