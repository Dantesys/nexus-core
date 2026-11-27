package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.util.MatrizModos;

/** Phoenixy: pena da fenix; no nivel 15 vira um elitro (asas da fenix). */
public final class PhoenixyItem extends RelicItem {
    public PhoenixyItem(Properties properties) {
        super(properties.fireResistant(), MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.literal("Fogo da Vida"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Proteção da Fênix"), PhoenixyEffects.PROTECT_MANA, PhoenixyEffects::protect),
                RelicSkill.active(Component.literal("Fúria da Fênix"), PhoenixyEffects.FURY_MANA, PhoenixyEffects::fury),
                RelicSkill.passive(Component.literal("Asas Flamejantes"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.literal("Renascimento da Fênix"), 0, (p, s, c) -> false));
    }

    /** Voando com as asas, a tecla da reliquia solta a Explosao de Chamas (impulso). */
    @Override public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (player.isFallFlying() && isUnlocked(player, 3)) return PhoenixyEffects.flameBoost(player);
        return super.activate(player, stack, slot);
    }

    @Override protected boolean skillsAvailable(ServerPlayer player, ItemStack stack, int slot) {
        return !PhoenixyEffects.reborn(player);
    }

    /** Asas da fenix a partir do estagio ressonante (nivel 15). */
    @Override protected void applyStageComponents(ServerPlayer player, ItemStack stack, int stage) {
        if (stage >= 2) setArmorAsset(stack, EquipmentSlot.CHEST, "phoenixy", stage, true);
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Fogo, lava e queimadura (inclusive o dracônico) curam você no mesmo valor do dano.";
            case 1 -> "Aliados e pets a até 3 blocos ganham o Fogo da Vida por 30s.";
            case 2 -> "Por 10s, fogo da fênix num raio de 5: queima inimigos (não apaga com água) e cura você e os protegidos.";
            case 3 -> "A pena vira élitro. Voando, a tecla da relíquia dá um impulso (5% da mana). Brasas caem por onde passa.";
            case 4 -> "Ao morrer, explode em chamas e renasce com metade da vida (recarga 15 min). Mortes garantidas ignoram.";
            default -> "";
        });
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 1 -> PhoenixyEffects.PROTECT_COOLDOWN;
            case 2 -> PhoenixyEffects.FURY_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
