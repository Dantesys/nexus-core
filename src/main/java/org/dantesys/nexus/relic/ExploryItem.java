package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.dantesys.nexus.util.MatrizModos;

/** Explory: bussola do explorador. */
public final class ExploryItem extends RelicItem {
    public ExploryItem(Properties properties) {
        super(properties, MatrizModos.MARTELO, 0, 0, 2,
                RelicSkill.passive(Component.translatable("relic.nexus.explory.mochila"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.translatable("relic.nexus.explory.rastreador"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.translatable("relic.nexus.explory.acampamento"), ExploryEffects.CAMP_MANA, ExploryEffects::camp),
                RelicSkill.active(Component.translatable("relic.nexus.explory.de_volta_da_aventura"), ExploryEffects.HOME_MANA, ExploryEffects::home),
                new RelicSkill(Component.translatable("relic.nexus.explory.aventura_a_um_passo_2"), RelicSkill.Kind.ACTIVE,
                        Component.translatable("relic.nexus.explory.o_rastreador_com_um_alvo"),
                        ExploryEffects::canJump, ExploryEffects::adventureJump, ExploryEffects.JUMP_MANA));
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown()) ExploryEffects.openBackpack(sp);
            else if (isUnlocked(sp, 1)) ExploryEffects.cycleTracker(sp, stack);
        }
        return InteractionResult.SUCCESS;
    }

    /** Agachado, o De Volta da Aventura so remarca a casa (sem custo nem recarga). */
    @Override protected boolean isFreeUse(ServerPlayer player, ItemStack stack, int slot) {
        return slot == 3 && player.isShiftKeyDown();
    }

    @Override public Component hudInfo(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String name = tag.getStringOr(ExploryEffects.TARGET_NAME, "");
        Player player = CLIENT_PLAYER.get();
        if (name.isEmpty() || player == null) return null;
        if (!player.level().dimension().identifier().toString().equals(tag.getStringOr(ExploryEffects.TARGET_DIM, ""))) {
            return Component.translatable("relic.nexus.explory.outra_dimensao", Component.translatable(name));
        }
        double dx = tag.getIntOr(ExploryEffects.TARGET_X, 0) + 0.5 - player.getX(), dz = tag.getIntOr(ExploryEffects.TARGET_Z, 0) + 0.5 - player.getZ();
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        float angle = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f - player.getYRot());
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        String arrow = arrows[Math.floorMod(Math.round(angle / 45f), 8)];
        return Component.translatable("relic.nexus.explory.m", Component.translatable(name), arrow, distance);
    }

    @Override public Component skillDescription(int slot) {
        return Component.translatable("relic.nexus.explory.skill." + slot + ".desc");
    }

    @Override public int getCooldownTicks(ItemStack stack, int slot) {
        int ticks = switch (slot) {
            case 2 -> ExploryEffects.CAMP_COOLDOWN;
            case 3 -> ExploryEffects.HOME_COOLDOWN;
            case 4 -> ExploryEffects.JUMP_COOLDOWN;
            default -> 0;
        };
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, ticks)));
    }
}
