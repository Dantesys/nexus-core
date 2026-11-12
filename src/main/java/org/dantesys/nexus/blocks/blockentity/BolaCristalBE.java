package org.dantesys.nexus.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.dantesys.nexus.advancement.ManaAdvancements;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.enchantment.ModEnchantments;
import org.dantesys.nexus.gui.menu.BolaCristalMenu;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.magic.MagicStatus;

/**
 * Bola de Cristal: mostra os 4 status de mana de quem abre e aplica o Núcleo
 * encantado com Distúrbio de Mana.
 */
public class BolaCristalBE extends BlockEntity implements Container, MenuProvider {
    public static final int NUCLEO_SLOT = 0;

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public BolaCristalBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOLA_CRISTAL_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    public boolean insertHeldItem(Player player, ItemStack heldStack) {
        if (!heldStack.is(ModItems.NUCLEO.get()) || !items.get(NUCLEO_SLOT).isEmpty()) {
            return false;
        }

        items.set(NUCLEO_SLOT, heldStack.copyWithCount(1));
        if (!player.hasInfiniteMaterials()) {
            heldStack.shrink(1);
        }
        setChanged();
        processarNucleo(player);
        return true;
    }

    public boolean returnItems(Player player) {
        ItemStack stack = removeItemNoUpdate(NUCLEO_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        setChanged();
        return true;
    }

    /** Usa o Núcleo do slot para evoluir os 4 status de quem colocou. O Núcleo é consumido. */
    public void processarNucleo(Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack nucleo = items.get(NUCLEO_SLOT);
        int nivel = getEnchantLevel(nucleo, ModEnchantments.DISTURBIO_MANA);
        if (nivel <= 0) {
            return;
        }

        MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
        aplicarDisturbioMana(status, nivel);
        items.set(NUCLEO_SLOT, ItemStack.EMPTY);
        setChanged();
        player.syncData(ModAttachmentType.MAGIC_STATS);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ManaAdvancements.check(serverPlayer, status);
        }
        player.sendSystemMessage(Component.translatable(
                "message.nexus.bola_cristal.mana_status",
                status.getMaxMana(),
                status.getRegeneracao(),
                status.getAfinidade(),
                status.getRessonancia()
        ));
    }

    private int getEnchantLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (level == null || stack.isEmpty()) {
            return 0;
        }

        var holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(enchantment);
        return stack.getEnchantments().getLevel(holder);
    }

    /**
     * Abaixo do nível 10 há chance de falha (45% no nível 1, caindo 5% por nível):
     * na falha cada status é sorteado para baixo. No sucesso cada status evolui.
     */
    private void aplicarDisturbioMana(MagicStatus status, int nivel) {
        float chanceFalha = Math.max(0.0F, 0.45F - (nivel - 1) * 0.05F);
        if (nivel < 10 && level.getRandom().nextFloat() < chanceFalha) {
            status.setMaxMana(reduzir(status.getMaxMana()));
            status.setRegeneracao(reduzir(status.getRegeneracao()));
            status.setAfinidade(reduzir(status.getAfinidade()));
            status.setRessonancia(reduzir(status.getRessonancia()));
            status.setMana(0);
            return;
        }

        status.setMaxMana(evoluirStatus(status.getMaxMana(), nivel));
        status.setRegeneracao(evoluirStatus(status.getRegeneracao(), nivel));
        status.setAfinidade(evoluirStatus(status.getAfinidade(), nivel));
        status.setRessonancia(evoluirStatus(status.getRessonancia(), nivel));
    }

    private int reduzir(int atual) {
        return level.getRandom().nextInt(1, Math.max(2, atual));
    }

    private int evoluirStatus(int atual, int nivel) {
        if (atual < 170) {
            int minimo = Math.max(1, (int) (765.0F * (nivel / 10.0F)));
            return level.getRandom().nextInt(minimo, MagicStatus.MAX_STATUS + 1);
        }

        int ganhoMaximo = (int) ((MagicStatus.MAX_STATUS - atual) * (nivel / 10.0F));
        if (ganhoMaximo <= 0) {
            return atual;
        }
        return Math.min(MagicStatus.MAX_STATUS, atual + level.getRandom().nextInt(1, ganhoMaximo + 1));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nexus.bola_cristal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BolaCristalMenu(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5
                ) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
