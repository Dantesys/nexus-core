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
                RelicSkill.passive(Component.translatable("relic.nexus.phoenixy.fogo_da_vida"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.phoenixy.protecao_da_fenix"), PhoenixyEffects.PROTECT_MANA, PhoenixyEffects::protect),
                RelicSkill.active(Component.translatable("relic.nexus.phoenixy.furia_da_fenix"), PhoenixyEffects.FURY_MANA, PhoenixyEffects::fury),
                RelicSkill.passive(Component.translatable("relic.nexus.phoenixy.asas_flamejantes"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.translatable("relic.nexus.phoenixy.renascimento_da_fenix_2"), 0, (p, s, c) -> false));
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
        return Component.translatable("relic.nexus.phoenixy.skill." + slot + ".desc");
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
