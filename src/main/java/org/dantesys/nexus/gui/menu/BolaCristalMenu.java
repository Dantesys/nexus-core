package org.dantesys.nexus.gui.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.blocks.blockentity.BolaCristalBE;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.gui.ModMenus;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.util.Elementos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BolaCristalMenu extends AbstractContainerMenu {
    private static final int CONTAINER_SLOTS = 2;

    private final BolaCristalBE blockEntity;
    private final ContainerLevelAccess access;
    private final SimpleContainerData data;
    private final Player player;

    public BolaCristalMenu(int id, Inventory playerInventory, BolaCristalBE blockEntity) {
        super(ModMenus.BOLA_CRISTAL_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.player = playerInventory.player;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = new SimpleContainerData(4);
        addDataSlots(data);

        if (!player.level().isClientSide()) {
            blockEntity.updateFromEmerald(player);
        }

        addSlot(new Slot(blockEntity, 0, 80, 5) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.ESMERALDA_TEMPESTADE.get())
                        || stack.is(ModItems.ESMERALDA_SOMBRIA.get())
                        || stack.is(ModItems.ESMERALDA_IGNEA.get())
                        || stack.is(ModItems.ESMERALDA_LUZ.get())
                        || stack.is(ModItems.ESMERALDA_VIDA.get())
                        || stack.is(ModItems.ESMERALDA_METAL.get());
            }
        });
        addSlot(new Slot(blockEntity, 1, 80, 64) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.NUCLEO.get()) && stack.isEnchanted();
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

    public static BolaCristalMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        BolaCristalBE blockEntity = (BolaCristalBE) inventory.player.level().getBlockEntity(buffer.readBlockPos());
        return new BolaCristalMenu(id, inventory, Objects.requireNonNull(blockEntity));
    }

    public BolaCristalBE getBlockEntity() {
        return blockEntity;
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide()) {
            blockEntity.processarNucleo(player);
            blockEntity.updateFromEmerald(player);
        }
        data.set(0, blockEntity.mana);
        data.set(1, blockEntity.regen);
        data.set(2, blockEntity.afinidade);
        data.set(3, blockEntity.ressonancia);
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BOLA_CRISTAL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, CONTAINER_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    public Component getStatus(int status) {
        return Component.literal(getString(data.get(Math.clamp(status, 0, 3))));
    }

    public int getTextColor() {
        var elemento = blockEntity.getElementoExibido();

        return switch (elemento) {
            case NAO_ELEMENTAL -> 0xFF888888;
            case TEMPESTADE -> 0xFFFFFF00;
            case SOMBRA -> 0xFF363636;
            case IGNEA -> 0xFFFF0000;
            case LUZ -> 0xFFFFFFFF;
            case METAL -> 0xFF909090;
            case VIDA -> 0xFF00FF00;
        };
    }

    private static @NotNull String getString(int value) {
        if (value < 1) return "F";
        if (value < 170) return "E";
        if (value < 340) return "D";
        if (value < 510) return "C";
        if (value < 680) return "B";
        if (value < 850) return "A";
        return "S";
    }

    public String getBgStats() {
        var elemento = blockEntity.getElementoExibido();

        if (elemento == Elementos.NAO_ELEMENTAL) {
            return "textures/gui/status/nada.png";
        }

        String nomeElemento = switch (elemento) {
            case TEMPESTADE -> "tempestade";
            case SOMBRA -> "sombria";
            case IGNEA -> "ignea";
            case LUZ -> "luz";
            case METAL -> "metal";
            case VIDA -> "vida";
            case NAO_ELEMENTAL -> throw new IllegalStateException("Non-elemental status handled earlier");
        };

        return "textures/gui/status/" + nomeElemento + (isElementoBloqueado() ? "_bloqueado" : "") + ".png";
    }

    public boolean isElementoBloqueado() {
        var elemento = blockEntity.getElementoExibido();
        return elemento != Elementos.NAO_ELEMENTAL
                && !player.getData(ModAttachmentType.MAGIC_STATS).get(elemento).isUnlocked();
    }
}
