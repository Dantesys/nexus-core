package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.items.ModToolMaterial;

import org.dantesys.nexus.util.MatrizModos;


public final class RuptyItem extends RelicItem {
    public RuptyItem(Properties properties) {
        super(properties.sword(ModToolMaterial.STEEL_MATERIA, 3.0f, -2.4f), MatrizModos.ESPADA,
                0, 0, 2,
                RelicSkill.passive(Component.literal("Cicatriz Mágica"), RuptyRules.SCAR_MANA, RuptyEffects::scar),
                RelicSkill.active(Component.literal("Defense Cut"), RuptyRules.CUT_MANA, RuptyEffects::cut),
                RelicSkill.active(Component.literal("Zona de Corte"), RuptyRules.ZONE_MANA, RuptyEffects::zone),
                new RelicSkill(Component.literal("Ruptura"), RelicSkill.Kind.ACTIVE,
                        Component.literal("Vida em 50% ou menos, mana em 50% ou mais e permanecer parado por 2 segundos após iniciar."),
                        (player, stack) -> canCharge(player), RuptyEffects::rupture, RuptyRules.RUPTURE_MANA));
    }
    public static boolean canCharge(ServerPlayer player) {
        var magic = player.getData(ModAttachmentType.MAGIC_STATS);
        return RuptyRules.canCharge(player.getHealth(), player.getMaxHealth(), magic.getMana(), magic.getMaxMana());
    }
    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Acertos aplicam cicatrizes: +5% de dano da Rupty por marca. Com 10+, a morte causa uma explosão de cortes. Após 5s sem acerto, perde 1 marca/s.";
            case 1 -> "Lança um corte na direção do olhar, causando dano e aplicando 3 cicatrizes.";
            case 2 -> "Cria uma zona de cortes por 10s; causa dano e aplica 1 cicatriz a cada 10 ticks.";
            case 3 -> "Corte frontal que executa alvos marcados. Bosses com 20% ou mais de vida recebem dano pesado. Destrói blocos frágeis.";
            default -> "";
        });
    }
    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) { case 0 -> 0; case 1 -> 40; case 2 -> 240; default -> 400; };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
    @Override public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (slot != 3) return super.activate(player, stack, slot);
        if (!stack.is(this) || player.getData(ModAttachmentType.RELIC_MASTERY).level(masteryId()) < 100
                || !canCharge(player) || getCooldownRemaining(stack, player.level().getGameTime(), slot) > 0) return false;
        return RuptyEffects.startCharge(player, stack);
    }
    public boolean finishRupture(ServerPlayer player, ItemStack stack) {
        return super.activate(player, stack, 3);
    }
}
