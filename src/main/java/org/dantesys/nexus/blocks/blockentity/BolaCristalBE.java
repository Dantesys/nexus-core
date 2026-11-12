package org.dantesys.nexus.blocks.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.advancement.ManaAdvancements;
import org.dantesys.nexus.enchantment.ModEnchantments;
import org.dantesys.nexus.items.ModItems;
import org.dantesys.nexus.gui.menu.BolaCristalMenu;
import org.dantesys.nexus.magic.ElementalStatus;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Elementos;

public class BolaCristalBE extends BlockEntity implements Container, MenuProvider {
    private static final int ESMERALDA_SLOT = 0;
    private static final int NUCLEO_SLOT = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    public int mana;
    public int regen;
    public int afinidade;
    public int ressonancia;

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
        int slot = getSlotFor(heldStack);
        if (slot < 0 || !items.get(slot).isEmpty()) {
            return false;
        }

        items.set(slot, heldStack.copyWithCount(1));
        if (!player.hasInfiniteMaterials()) {
            heldStack.shrink(1);
        }
        setChanged();

        if (slot == NUCLEO_SLOT) {
            processarNucleo(player);
        }
        return true;
    }

    public void showStatus(Player player) {
        Elementos elemento = getElemento();
        if (elemento == null) {
            player.sendSystemMessage(Component.translatable("message.nexus.bola_cristal.sem_esmeralda"));
            return;
        }

        ElementalStatus status = player.getData(ModAttachmentType.MAGIC_STATS).get(elemento);
        player.sendSystemMessage(Component.translatable(
                "message.nexus.bola_cristal.status",
                elemento.getTooltip(),
                status.isUnlocked()
                        ? Component.translatable("message.nexus.bola_cristal.desbloqueado")
                        : Component.translatable("message.nexus.bola_cristal.bloqueado"),
                status.getAfinidade(),
                status.getRessonancia()
        ));
    }

    public void updateFromEmerald(Player player) {
        Elementos elemento = getElemento();
        MagicStatus magicStatus = player.getData(ModAttachmentType.MAGIC_STATS);
        if (elemento == null) {
            elemento = Elementos.NAO_ELEMENTAL;
        }

        ElementalStatus elementalStatus = magicStatus.get(elemento);
        mana = magicStatus.getMaxMana();
        regen = magicStatus.getRegeneracao();
        afinidade = elementalStatus.getAfinidade();
        ressonancia = elementalStatus.getRessonancia();
    }

    public boolean returnItems(Player player) {
        boolean returnedAny = false;
        for (int slot = items.size() - 1; slot >= 0; slot--) {
            ItemStack stack = removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                returnedAny = true;
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        if (returnedAny) {
            setChanged();
        }
        return returnedAny;
    }

    private int getSlotFor(ItemStack stack) {
        if (getElemento(stack) != null) {
            return ESMERALDA_SLOT;
        }
        if (stack.is(ModItems.NUCLEO.get())) {
            return NUCLEO_SLOT;
        }
        return -1;
    }

    public Elementos getElemento() {
        return getElemento(items.get(ESMERALDA_SLOT));
    }

    public Elementos getElementoExibido() {
        Elementos elemento = getElemento();
        return elemento == null ? Elementos.NAO_ELEMENTAL : elemento;
    }

    private static Elementos getElemento(ItemStack stack) {
        if (stack.is(ModItems.ESMERALDA_TEMPESTADE.get())) return Elementos.TEMPESTADE;
        if (stack.is(ModItems.ESMERALDA_SOMBRIA.get())) return Elementos.SOMBRA;
        if (stack.is(ModItems.ESMERALDA_IGNEA.get())) return Elementos.IGNEA;
        if (stack.is(ModItems.ESMERALDA_LUZ.get())) return Elementos.LUZ;
        if (stack.is(ModItems.ESMERALDA_VIDA.get())) return Elementos.VIDA;
        if (stack.is(ModItems.ESMERALDA_METAL.get())) return Elementos.METAL;
        return null;
    }

    public void processarNucleo(Player player) {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack nucleo = items.get(NUCLEO_SLOT);
        MagicStatus magicStatus = player.getData(ModAttachmentType.MAGIC_STATS);
        int disturbioMana = getEnchantLevel(nucleo, ModEnchantments.DISTURBIO_MANA);
        if (disturbioMana > 0) {
            aplicarDisturbioMana(magicStatus, disturbioMana);
            concluirProcessamento(player);
            player.sendSystemMessage(Component.translatable(
                    "message.nexus.bola_cristal.mana_status",
                    magicStatus.getMaxMana(),
                    magicStatus.getRegeneracao()
            ));
            return;
        }

        Elementos elemento = getElemento();
        if (elemento == null) {
            return;
        }

        ElementalStatus elementalStatus = magicStatus.get(elemento);
        int matriz = getEnchantLevel(nucleo, ModEnchantments.MATRIZ_ELEMENTAL);
        int disturbio = getEnchantLevel(nucleo, ModEnchantments.DISTURBIO_ELEMENTAL);
        boolean applied = false;

        if (matriz > 0 && !elementalStatus.isUnlocked()) {
            elementalStatus.setUnlocked(true);
            elementalStatus.setAfinidade(level.getRandom().nextInt(1, 1001));
            elementalStatus.setRessonancia(level.getRandom().nextInt(1, 1001));
            applied = true;
        }

        if (disturbio > 0 && elementalStatus.isUnlocked()) {
            applied = aplicarDisturbio(elementalStatus, disturbio);
        }

        if (applied) {
            concluirProcessamento(player);
            showStatus(player);
        }
    }

    private void concluirProcessamento(Player player) {
        items.set(NUCLEO_SLOT, ItemStack.EMPTY);
        setChanged();
        player.syncData(ModAttachmentType.MAGIC_STATS);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ManaAdvancements.check(serverPlayer, player.getData(ModAttachmentType.MAGIC_STATS));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nexus.bola_cristal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BolaCristalMenu(containerId, inventory, this);
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

    private boolean aplicarDisturbio(ElementalStatus status, int nivel) {
        float chanceBloqueio = Math.max(0.0F, 0.45F - (nivel - 1) * 0.05F);
        if (nivel < 10 && level.getRandom().nextFloat() < chanceBloqueio) {
            status.setUnlocked(false);
            status.setAfinidade(0);
            status.setRessonancia(0);
            return true;
        }

        status.setAfinidade(evoluirStatus(status.getAfinidade(), nivel));
        status.setRessonancia(evoluirStatus(status.getRessonancia(), nivel));
        return true;
    }

    private void aplicarDisturbioMana(MagicStatus status, int nivel) {
        float chanceFalha = Math.max(0.0F, 0.45F - (nivel - 1) * 0.05F);
        if (nivel < 10 && level.getRandom().nextFloat() < chanceFalha) {
            status.setMaxMana(level.getRandom().nextInt(1, Math.max(2,status.getMaxMana())));
            status.setRegeneracao(level.getRandom().nextInt(1, Math.max(2,status.getRegeneracao())));
            status.setMana(0);
            return;
        }

        status.setMaxMana(evoluirStatus(status.getMaxMana(), nivel));
        status.setRegeneracao(evoluirStatus(status.getRegeneracao(), nivel));
        status.setMana(Math.min(status.getMana(), status.getMaxMana()));
    }

    private int evoluirStatus(int atual, int nivel) {
        if (atual < 170) {
            int minimo = Math.max(1, (int) (765.0F * (nivel / 10.0F)));
            return level.getRandom().nextInt(minimo, 1001);
        }

        int ganhoMaximo = (int) ((1000 - atual) * (nivel / 10.0F));
        if (ganhoMaximo <= 0) {
            return atual;
        }
        return Math.min(1000, atual + level.getRandom().nextInt(1, ganhoMaximo + 1));
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
