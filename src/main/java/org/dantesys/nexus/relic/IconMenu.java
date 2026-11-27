package org.dantesys.nexus.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/** Tela de escolha: cada icone e uma opcao; clicar escolhe (nada sai da tela). Usada pela Thiefy e pela Grimy. */
public final class IconMenu extends ChestMenu {
    private static final MenuType<?>[] TYPES = {MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3, MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6};
    private final int size;
    private final IntConsumer onPick;
    private final Predicate<Player> valid;

    private IconMenu(int id, Player player, SimpleContainer icons, int rows, IntConsumer onPick, Predicate<Player> valid) {
        super(TYPES[rows - 1], id, player.getInventory(), icons, rows);
        this.size = rows * 9;
        this.onPick = onPick;
        this.valid = valid;
    }

    public static void open(ServerPlayer player, Component title, List<ItemStack> icons, IntConsumer onPick, Predicate<Player> valid) {
        int rows = Math.clamp((icons.size() + 8) / 9, 1, 6);
        SimpleContainer container = new SimpleContainer(rows * 9);
        for (int i = 0; i < Math.min(icons.size(), rows * 9); i++) container.setItem(i, icons.get(i));
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new IconMenu(id, p, container, rows, onPick, valid), title));
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < size) {
            if (!getSlot(slotIndex).getItem().isEmpty()) {
                player.closeContainer();
                onPick.accept(slotIndex);
            }
            return;
        }
        if (input == ContainerInput.QUICK_MOVE) return;
        super.clicked(slotIndex, buttonNum, input, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return valid.test(player); }
}
