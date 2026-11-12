package org.dantesys.nexus.relic;

import net.minecraft.core.Holder;
import org.dantesys.nexus.api.element.NexusElement;
import org.jspecify.annotations.Nullable;

/**
 * Valores finais calculados antes da execução de uma habilidade de relíquia.
 *
 * @param element elemento da relíquia (dado por catalisadores de addons) ou null para mana bruta
 * @param resonance ressonância do jogador (0 a 1000): reduz custo e recarga
 * @param affinity afinidade do jogador (0 a 1000): aumenta o poder
 */
public record RelicAbilityContext(
        @Nullable Holder<NexusElement> element,
        int resonance,
        int affinity,
        int manaCost,
        int cooldownTicks,
        double powerMultiplier
) {}