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
                RelicSkill.passive(Component.literal("Mochila"), 0, (p, s, c) -> false),
                RelicSkill.passive(Component.literal("Rastreador"), 0, (p, s, c) -> false),
                RelicSkill.active(Component.literal("Acampamento"), ExploryEffects.CAMP_MANA, ExploryEffects::camp),
                RelicSkill.active(Component.literal("De Volta da Aventura"), ExploryEffects.HOME_MANA, ExploryEffects::home),
                new RelicSkill(Component.literal("Aventura a um Passo"), RelicSkill.Kind.ACTIVE,
                        Component.literal("O Rastreador com um alvo e o De Volta da Aventura pronto."),
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
            return Component.literal(name + " (outra dimensão)");
        }
        double dx = tag.getIntOr(ExploryEffects.TARGET_X, 0) + 0.5 - player.getX(), dz = tag.getIntOr(ExploryEffects.TARGET_Z, 0) + 0.5 - player.getZ();
        int distance = (int) Math.sqrt(dx * dx + dz * dz);
        float angle = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f - player.getYRot());
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        String arrow = arrows[Math.floorMod(Math.round(angle / 45f), 8)];
        return Component.literal(name + " " + arrow + " " + distance + " m");
    }

    @Override public Component skillDescription(int slot) {
        return Component.literal(switch (slot) {
            case 0 -> "Agachar + clique direito abre sua mochila pessoal (9 espaços, 27 no nível 10, 54 no 20). Mantida ao morrer.";
            case 1 -> "Clique direito escolhe uma estrutura e a bússola aponta para ela (vila; templos e postos; fortalezas e cidade antiga).";
            case 2 -> "Acampamento por 10 min: ponto de renascimento, fogueira que cura aliados e área sem mobs hostis.";
            case 3 -> "Teleporta para a casa desta dimensão (3s de preparo, leva aliados a 3 blocos). Agachado, remarca a casa.";
            case 4 -> "Teleporta o grupo para a estrutura do Rastreador; o próximo De Volta da Aventura traz de volta.";
            default -> "";
        });
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
