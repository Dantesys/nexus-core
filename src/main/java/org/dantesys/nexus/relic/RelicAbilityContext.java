package org.dantesys.nexus.relic;

import org.dantesys.nexus.util.Elementos;

/** Valores finais calculados antes da execucao de uma habilidade de reliquia. */
public record RelicAbilityContext(
        Elementos playerElement,
        Elementos relicElement,
        boolean elementalCombo,
        int resonance,
        int affinity,
        int manaCost,
        int cooldownTicks,
        double powerMultiplier
) {}
