package org.dantesys.nexus.creativetab;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.dantesys.nexus.Nexus;
import org.dantesys.nexus.blocks.ModBlocks;
import org.dantesys.nexus.enchantment.ModEnchantments;
import org.dantesys.nexus.items.ModItems;

import java.util.function.Supplier;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> MODTAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Nexus.MODID);

    public static final Supplier<CreativeModeTab> NEXUS_ITEMS = MODTAB.register("nexus_items", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.NUCLEO.get()))
            .title(Component.translatable("creativetab.nexus.items"))
            .displayItems((itemDisplayParameters, output) -> {
                ModItems.ITEMS.getEntries().forEach(item -> {
                            if(item != ModItems.BOMBA_FUMACA && ModBlocks.BLOCKS.getEntries().stream().noneMatch(block -> item.getId().equals(block.getId()))) {
                                output.accept(item.get());
                            }});

                var enchantments = itemDisplayParameters.holders()
                        .lookupOrThrow(Registries.ENCHANTMENT);
                output.accept(createEnchantedBook(
                        enchantments.getOrThrow(ModEnchantments.DISTURBIO_MANA), 10
                ));
            }).build());

    public static final Supplier<CreativeModeTab> NEXUS_BLOCKS = MODTAB.register("nexus_blocks",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.STEEL_BLOCK.get()))
                    .title(Component.translatable("creativetab.nexus.blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        ModBlocks.BLOCKS.getEntries().forEach(item -> {
                            if (item.get().asItem() != Items.AIR) {
                                output.accept(item.get());
                            }
                        });
                    }).build());

    public static void register(IEventBus bus) {
        MODTAB.register(bus);
    }

    private static ItemStack createEnchantedBook(Holder<Enchantment> enchantment, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(enchantment, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        return book;
    }
}
