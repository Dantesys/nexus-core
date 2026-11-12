package org.dantesys.nexus.gui.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.blocks.blockentity.ControladorForjaBE;
import org.dantesys.nexus.gui.ModMenus;

import java.util.Objects;

public class ForjaFullMenu extends AbstractContainerMenu {
    private static final int CONTAINER_SLOTS = 7;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public ForjaFullMenu(int id, Inventory playerInventory, ControladorForjaBE forge) {
        super(ModMenus.FORJA_FULL_MENU.get(), id);
        access = ContainerLevelAccess.create(forge.getLevel(), forge.getBlockPos());
        int[] synced = new int[4];
        data = new ContainerData() {
            @Override
            public int get(int index) {
                if (forge.getLevel() != null && forge.getLevel().isClientSide()) {
                    return index >= 0 && index < synced.length ? synced[index] : 0;
                }
                return switch (index) {
                    case 0 -> forge.getLava();
                    case 1 -> forge.getMana();
                    case 2 -> forge.getProgress();
                    case 3 -> forge.getMaxProgress();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < synced.length) synced[index] = value;
            }

            @Override
            public int getCount() {
                return synced.length;
            }
        };
        addDataSlots(data);

        addSlot(filteredSlot(forge, ControladorForjaBE.LAVA_SLOT, 8, 16));
        addSlot(filteredSlot(forge, ControladorForjaBE.CORE_SLOT, 67, 34));
        addSlot(filteredSlot(forge, ControladorForjaBE.MATERIAL_SLOT, 37, 16));
        addSlot(filteredSlot(forge, 3, 97, 16));
        addSlot(filteredSlot(forge, 4, 37, 52));
        addSlot(filteredSlot(forge, 5, 97, 52));
        addSlot(new Slot(forge, ControladorForjaBE.RESULT_SLOT, 152, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    private static Slot filteredSlot(ControladorForjaBE forge, int index, int x, int y) {
        return new Slot(forge, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return forge.canPlaceItem(index, stack);
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CONTROLADOR_FORJA.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int forgeSlot : new int[]{0, 2, 3, 4, 5, 1}) {
                if (moved) break;
                if (getSlot(forgeSlot).mayPlace(stack)) {
                    moved = moveItemStackTo(stack, forgeSlot, forgeSlot + 1, false);
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    public int getLava() {
        return data.get(0);
    }

    public int getMana() {
        return data.get(1);
    }

    public int getProgress() {
        return data.get(2);
    }

    public int getMaxProgress() {
        return data.get(3);
    }

    public static ForjaFullMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        ControladorForjaBE forge = (ControladorForjaBE)
                inventory.player.level().getBlockEntity(buffer.readBlockPos());
        return new ForjaFullMenu(id, inventory, Objects.requireNonNull(forge));
    }
}
