package org.dantesys.nexus.relic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import org.dantesys.nexus.api.element.NexusElement;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.MatrizModos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de todas as reliquias. Habilidades acumuladas (niveis 1, 5, 10, 15 e 20 de maestria);
 * passivas ficam sempre ligadas depois de liberadas; ativas custam mana e tem recarga.
 * A recarga pertence ao ItemStack; a maestria pertence ao jogador.
 */
public class RelicItem extends Item {

    public static final int MIN_CATALYST_CAPACITY = 2;
    public static final int MAX_CATALYST_CAPACITY = 10;

    private static final String COOLDOWN_TAG = "NexusRelicCooldownUntil";
    private static final String SELECTED_TAG = "NexusRelicSelectedSkill";

    private final MatrizModos mode;
    private final int baseManaCost;
    private final int baseCooldownTicks;
    private final int catalystCapacity;
    private final List<RelicSkill> skills;

    public RelicItem(Properties properties, MatrizModos mode, int manaCost, int cooldownTicks,
                     int catalystCapacity, RelicSkill... skills) {
        super(properties.stacksTo(1).component(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE));
        if (mode == null) throw new IllegalArgumentException("Relic mode cannot be null");
        if (manaCost < 0 || cooldownTicks < 0) {
            throw new IllegalArgumentException("Relic mana and cooldown values cannot be negative");
        }
        if (catalystCapacity < MIN_CATALYST_CAPACITY || catalystCapacity > MAX_CATALYST_CAPACITY) {
            throw new IllegalArgumentException("Relics must have catalyst weight capacity between 2 and 10");
        }
        if (skills.length > RelicSkill.MAX_SKILLS) throw new IllegalArgumentException("Relics have at most 5 skills");
        this.mode = mode;
        this.baseManaCost = manaCost;
        this.baseCooldownTicks = cooldownTicks;
        this.catalystCapacity = catalystCapacity;
        this.skills = List.of(skills);
    }

    public List<RelicSkill> getSkills() { return skills; }
    public Component skillDescription(int slot) { return Component.empty(); }
    public Component skillDescription(ItemStack stack, int slot) { return skillDescription(slot); }
    /** Linhas extras na tooltip (ex.: fragmentos da Rupty). Roda no cliente. */
    public List<Component> extraTooltip(ItemStack stack) { return List.of(); }
    /** Texto curto mostrado no HUD ao lado da habilidade selecionada. Roda no cliente. */
    public @Nullable Component hudInfo(ItemStack stack) { return null; }
    public String masteryId() { return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).toString(); }
    public MatrizModos getMode() { return mode; }
    public int getCatalystCapacity() { return catalystCapacity; }

    /** Jogador local (so no cliente; definido pelo NexusClient) para o hudInfo. */
    public static java.util.function.Supplier<Player> CLIENT_PLAYER = () -> null;

    public static boolean isUnlocked(int level, int slot) { return level >= RelicSkill.requiredLevel(slot); }
    public int masteryLevel(Player player) { return player.getData(ModAttachmentType.RELIC_MASTERY).level(masteryId()); }
    public boolean isUnlocked(Player player, int slot) {
        return slot >= 0 && slot < skills.size() && isUnlocked(masteryLevel(player), slot);
    }

    /** Mesma prioridade para selecionar e usar; falhar nao ativa outra reliquia. */
    public static ItemStack controlledRelic(Player player) {
        for (var slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof RelicItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    // ---------- Selecao (so ativas) ----------

    public int selectedSkill(ItemStack stack) {
        int stored = (int) readLong(stack, SELECTED_TAG);
        if (stored >= 0 && stored < skills.size() && skills.get(stored).kind() == RelicSkill.Kind.ACTIVE) return stored;
        for (int i = 0; i < skills.size(); i++) if (skills.get(i).kind() == RelicSkill.Kind.ACTIVE) return i;
        return 0;
    }

    public void selectSkill(ServerPlayer player, ItemStack stack, boolean next) {
        if (!stack.is(this) || skills.isEmpty()) return;
        int slot = RelicSkill.nextActive(selectedSkill(stack), skills, masteryLevel(player), next);
        if (slot < 0) {
            player.sendSystemMessage(Component.literal("Nenhuma habilidade ativa liberada ainda."), true);
            return;
        }
        writeLong(stack, SELECTED_TAG, slot);
        player.sendSystemMessage(Component.literal("Selecionada: ").append(skills.get(slot).name().copy()), true);
    }

    // ---------- Custos e recargas ----------

    /** Mana bruta (null) por padrao; apenas catalisadores instalados podem alterar o elemento. */
    public @Nullable Holder<NexusElement> getElement(ItemStack stack) {
        Holder<NexusElement> element = null;
        for (ItemStack catalyst : RelicCatalysts.get(stack)) {
            if (catalyst.getItem() instanceof RelicCatalyst modifier) {
                element = modifier.modifyRelicElement(catalyst, stack, element);
            }
        }
        return element;
    }

    public double modify(ItemStack relic, RelicAttribute attribute, double baseValue) {
        double value = baseValue;
        for (ItemStack catalyst : RelicCatalysts.get(relic)) {
            if (catalyst.getItem() instanceof RelicCatalyst modifier) {
                value = modifier.modifyRelicAttribute(catalyst, relic, attribute, value);
            }
        }
        return value;
    }

    public int getManaCost(ItemStack stack) {
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.MANA_COST, baseManaCost)));
    }
    public int getManaCost(ItemStack stack, int slot) {
        int cost = skills.get(slot).manaCost();
        return cost < 0 ? getManaCost(stack)
                : Math.max(0, (int) Math.round(modify(stack, RelicAttribute.MANA_COST, cost)));
    }
    public int getCooldownTicks(ItemStack stack) {
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, baseCooldownTicks)));
    }
    public int getCooldownTicks(ItemStack stack, int slot) { return getCooldownTicks(stack); }
    public long getCooldownRemaining(ItemStack stack, long gameTime) {
        return getCooldownRemaining(stack, gameTime, selectedSkill(stack));
    }
    public long getCooldownRemaining(ItemStack stack, long gameTime, int slot) {
        return Math.max(0L, readLong(stack, COOLDOWN_TAG + slot) - gameTime);
    }

    // ---------- Ativacao ----------

    /** Custo de mana do uso (ex.: pelo tamanho de uma area). Padrao: custo fixo da habilidade. */
    protected int skillManaCost(ServerPlayer player, ItemStack stack, int slot) { return getManaCost(stack, slot); }

    /** Usos preparatorios (ex.: marcar o primeiro ponto, copiar uma planta): sem custo, sem recarga. */
    protected boolean isFreeUse(ServerPlayer player, ItemStack stack, int slot) { return false; }

    public boolean activate(ServerPlayer player, ItemStack stack) {
        return activate(player, stack, selectedSkill(stack));
    }

    public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (!stack.is(this) || slot < 0 || slot >= skills.size()) return false;
        if (!(this instanceof MorpixItem) && MorpixEffects.transformed(player)) {
            player.sendSystemMessage(Component.literal("Transformado: só os ataques da forma."), true);
            return false;
        }
        if (!allowWhileParalyzed(slot) && player.hasEffect(org.dantesys.nexus.effects.ModEffects.PARALISIA)) {
            player.sendSystemMessage(Component.literal("Paralisado!"), true);
            return false;
        }
        if (skills.get(slot).kind() == RelicSkill.Kind.PASSIVE) return false;
        return executeSkill(player, stack, slot, RelicSkill.Kind.ACTIVE, true);
    }

    /** O evento funcional da reliquia chama este metodo; passivas liberadas estao sempre ligadas. */
    public boolean triggerPassive(ServerPlayer player, ItemStack stack, int slot) {
        return executeSkill(player, stack, slot, RelicSkill.Kind.PASSIVE, false);
    }

    private boolean executeSkill(ServerPlayer player, ItemStack stack, int slot, RelicSkill.Kind kind, boolean feedback) {
        if (!stack.is(this) || slot < 0 || slot >= skills.size()) return false;
        RelicSkill skill = skills.get(slot);
        if (skill.kind() != kind) return false;
        if (!isUnlocked(player, slot)) {
            if (feedback) player.sendSystemMessage(Component.literal("Liberada na maestria " + RelicSkill.requiredLevel(slot) + "."), true);
            return false;
        }
        if (!skillsAvailable(player, stack, slot)) {
            if (feedback) player.sendSystemMessage(Component.literal("Habilidades indisponíveis no momento."), true);
            return false;
        }
        long gameTime = player.level().getGameTime();
        boolean free = isFreeUse(player, stack, slot);
        long remaining = free ? 0 : getCooldownRemaining(stack, gameTime, slot);
        if (remaining > 0) {
            if (feedback) player.sendSystemMessage(skill.name().copy().append(" em recarga: " + (remaining + 19) / 20 + "s"), true);
            return false;
        }
        if (!skill.condition().test(player, stack)) {
            if (feedback && !skill.conditionDescription().getString().isEmpty())
                player.sendSystemMessage(skill.name().copy().append(": ").append(skill.conditionDescription()), true);
            return false;
        }

        MagicStatus magic = player.getData(ModAttachmentType.MAGIC_STATS);
        if (!magic.isCreated()) return false;
        // Ressonancia reduz custo e recarga (ate a metade); afinidade aumenta o poder.
        int resonance = Math.clamp(magic.getRessonancia(), 0, MagicStatus.MAX_STATUS);
        int affinity = Math.clamp(magic.getAfinidade(), 0, MagicStatus.MAX_STATUS);
        int cost = free ? 0 : scaleDownToHalf(skillManaCost(player, stack, slot), resonance);
        int cooldown = free ? 0 : scaleDownToHalf(getCooldownTicks(stack, slot), resonance);
        double power = 1.0D + affinity / (double) MagicStatus.MAX_STATUS;
        RelicAbilityContext context = new RelicAbilityContext(getElement(stack), resonance, affinity, cost, cooldown, power);

        if (magic.getMana() < cost) {
            if (feedback) player.sendSystemMessage(Component.literal("Mana insuficiente (" + cost + ")."), true);
            return false;
        }
        if (!skill.effect().activate(player, stack, context)) return false;

        if (cost > 0) {
            magic.useMana(cost);
            player.syncData(ModAttachmentType.MAGIC_STATS);
        }
        if (cooldown > 0) writeLong(stack, COOLDOWN_TAG + slot, gameTime + cooldown);
        if (kind == RelicSkill.Kind.ACTIVE && !free) addMasteryXp(player, RelicMastery.XP_PER_USE);
        return true;
    }

    /** Pode usar a habilidade agora? (ex.: Phoenixy no primeiro minuto depois de renascer). */
    protected boolean skillsAvailable(ServerPlayer player, ItemStack stack, int slot) { return true; }
    /** Habilidades que funcionam mesmo paralisado (ex.: Sangue Toxico da Venossy). */
    protected boolean allowWhileParalyzed(int slot) { return false; }

    /** Recarga escalada pela Ressonancia, iniciada fora do fluxo normal (ex.: recarga que conta da morte do golem). */
    public void startCooldown(ServerPlayer player, ItemStack stack, int slot, int baseTicks) {
        MagicStatus magic = player.getData(ModAttachmentType.MAGIC_STATS);
        int resonance = Math.clamp(magic.getRessonancia(), 0, MagicStatus.MAX_STATUS);
        int ticks = scaleDownToHalf((int) Math.round(modify(stack, RelicAttribute.COOLDOWN, baseTicks)), resonance);
        writeLong(stack, COOLDOWN_TAG + slot, player.level().getGameTime() + ticks);
    }
    /** Recarga fixa (sem reducao da Ressonancia). */
    public void setCooldownTicks(ServerPlayer player, ItemStack stack, int slot, int ticks) {
        writeLong(stack, COOLDOWN_TAG + slot, player.level().getGameTime() + ticks);
    }
    public void clearCooldown(ItemStack stack, int slot) { writeLong(stack, COOLDOWN_TAG + slot, 0); }
    public boolean ready(ItemStack stack, long gameTime, int slot) { return getCooldownRemaining(stack, gameTime, slot) <= 0; }

    /** Primeira copia desta reliquia no inventario (maos, armadura e mochila incluidas). */
    public ItemStack findIn(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(this)) return stack;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i).is(this)) return inventory.getItem(i);
        return ItemStack.EMPTY;
    }
    public boolean heldBy(Player player) { return player.getMainHandItem().is(this) || player.getOffhandItem().is(this); }

    /** XP de maestria por acao tematica de cada reliquia. */
    public void addMasteryXp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        if (player.getData(ModAttachmentType.RELIC_MASTERY).addExperience(masteryId(), amount)) {
            player.syncData(ModAttachmentType.RELIC_MASTERY);
        }
    }

    /** Compatibilidade: um uso funcional bem-sucedido vale 1 XP. */
    public void recordSuccessfulUse(ServerPlayer player, ItemStack stack) {
        if (stack.is(this)) addMasteryXp(player, RelicMastery.XP_PER_USE);
    }

    private static int scaleDownToHalf(int base, int resonance) {
        return Math.max(0, (int) Math.ceil(base * (1.0D - 0.5D * resonance / MagicStatus.MAX_STATUS)));
    }

    // ---------- Aparencia ----------

    /** Estagio visual (0 a 3) pela maestria de quem carrega: dormente, desperta (5), ressonante (15), final (20). */
    public static int visualStage(int masteryLevel) { return RelicSkill.visualStage(masteryLevel); }

    /** Valores extras do custom_model_data depois do estagio (indice 0). */
    protected List<Float> extraModelData(ServerPlayer player, ItemStack stack) { return List.of(); }

    /** Atualiza o custom_model_data (indice 0 = estagio; demais = extraModelData) se mudou. */
    public void refreshModelData(ServerPlayer player, ItemStack stack) {
        if (!stack.is(this)) return;
        List<Float> floats = new ArrayList<>();
        floats.add((float) visualStage(masteryLevel(player)));
        floats.addAll(extraModelData(player, stack));
        applyStageComponents(player, stack, visualStage(masteryLevel(player)));
        CustomModelData current = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (current != null && current.floats().equals(floats)) return;
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(floats, List.of(), List.of(), List.of()));
    }

    /** Componentes que mudam com o estagio (ex.: textura da armadura, asas da Phoenixy). */
    protected void applyStageComponents(ServerPlayer player, ItemStack stack, int stage) {}

    protected static final String[] STAGE_NAMES = {"dormente", "desperta", "ressonante", "final"};

    /** Troca a textura de armadura (equipment/<reliquia>_<estagio>) no slot dado. */
    protected static void setArmorAsset(ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot, String relic, int stage, boolean glider) {
        var asset = net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(org.dantesys.nexus.Nexus.MODID, relic + "_" + STAGE_NAMES[Math.clamp(stage, 0, 3)]));
        var current = stack.get(DataComponents.EQUIPPABLE);
        if (current != null && current.slot() == slot && current.assetId().isPresent() && current.assetId().get().equals(asset)) return;
        stack.set(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(slot).setAsset(asset).build());
        if (glider) stack.set(DataComponents.GLIDER, net.minecraft.util.Unit.INSTANCE);
    }

    public static float modelValue(ItemStack stack, int index, float fallback) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (data == null) return fallback;
        Float value = data.getFloat(index);
        return value == null ? fallback : value;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (owner instanceof ServerPlayer player && level.getGameTime() % 10 == 0) refreshModelData(player, stack);
    }

    /** Mudancas de dados (estagio, fragmentos, recarga) nao refazem a animacao de sacar o item. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    // ---------- Dados no ItemStack ----------

    protected static long readLong(ItemStack stack, String key) {
        return Math.max(0L, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(key, 0L));
    }

    protected static void writeLong(ItemStack stack, String key, long value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(key, Math.max(0L, value)));
    }
}
