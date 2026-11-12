package org.dantesys.nexus.relic;

import net.minecraft.world.item.ItemStack;

/** Implemente em itens que alteram atributos de reliquias. */
public interface RelicCatalyst {
    /** Elemento de funcionamento. Por padrao preserva o elemento recebido (inicialmente nao elemental). */
    default org.dantesys.nexus.util.Elementos modifyRelicElement(ItemStack catalyst, ItemStack relic,
            org.dantesys.nexus.util.Elementos currentElement) {
        return currentElement;
    }

    /** Peso consumido na capacidade da reliquia. */
    default int getRelicWeight(ItemStack catalyst) {
        return 1;
    }

    default double modifyRelicAttribute(ItemStack catalyst, ItemStack relic,
                                        RelicAttribute attribute, double currentValue) {
        return currentValue;
    }
}
