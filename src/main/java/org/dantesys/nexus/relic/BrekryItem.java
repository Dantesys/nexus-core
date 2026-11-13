package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.items.ModToolMaterial;
import org.dantesys.nexus.util.MatrizModos;

/** Brekry: picareta focada em quebrar blocos e abrir caminho. */
public final class BrekryItem extends RelicItem {
    public BrekryItem(Properties properties) {
        super(properties.pickaxe(ModToolMaterial.STEEL_MATERIA, 1.0F, -2.8F), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.active(Component.literal("Dinamite"), BrekryEffects.DYNAMITE_MANA, BrekryEffects::dynamite),
                RelicSkill.active(Component.literal("Túnel"), BrekryEffects.TUNNEL_MANA, BrekryEffects::tunnel),
                RelicSkill.active(Component.literal("Frenesi"), BrekryEffects.FRENZY_MANA, BrekryEffects::frenzy),
                RelicSkill.active(Component.literal("Limpeza"), BrekryEffects.CLEANUP_MANA, BrekryEffects::cleanup),
                new RelicSkill(Component.literal("Breakable"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Afinidade rank A ou maior e 64 TNT no inventário (consumidas)."),
                        BrekryEffects::canBreakable, BrekryEffects::breakable, BrekryEffects.BREAKABLE_MANA));
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Planta uma bomba que explode em 5s, quebrando blocos. Não causa dano a entidades.";
            case 1 -> "Abre um túnel horizontal à frente (1×2×5, cresce com a Afinidade). Não quebra blocos com inventário.";
            case 2 -> "Velocidade de mineração por alguns segundos.";
            case 3 -> "Limpa 10×10×10 à frente (menos minérios, baús e blocos perto de líquidos) e guarda tudo num baú duplo.";
            case 4 -> "Por 10s, qualquer bloco quebra na hora e dropa ele mesmo, até os inquebráveis naturais.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 0 -> BrekryEffects.DYNAMITE_COOLDOWN;
            case 1 -> BrekryEffects.TUNNEL_COOLDOWN;
            case 2 -> BrekryEffects.FRENZY_COOLDOWN;
            case 3 -> BrekryEffects.CLEANUP_COOLDOWN;
            default -> BrekryEffects.BREAKABLE_COOLDOWN;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
