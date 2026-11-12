package org.dantesys.nexus.relic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.dantesys.nexus.magic.ElementalStatus;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.util.Elementos;
import org.dantesys.nexus.util.MatrizModos;

/**
 * Base de todas as reliquias, independente de serem armas, armaduras ou acessorios.
 * A recarga pertence ao ItemStack; o custo de mana e pago pelo jogador.
 */
public class RelicItem extends Item {
    private static final int MAX_ELEMENTAL_STATUS = 1000;

    public static final int MIN_CATALYST_CAPACITY = 2;
    public static final int MAX_CATALYST_CAPACITY = 10;

    private static final String COOLDOWN_TAG = "NexusRelicCooldownUntil";

    private final MatrizModos mode;

    private final int baseManaCost;
    private final int baseCooldownTicks;
    private final int catalystCapacity;
    private final java.util.List<RelicSkill> skills;

    public java.util.List<RelicSkill> getSkills() { return skills; }
    public net.minecraft.network.chat.Component skillDescription(int slot) {
        return net.minecraft.network.chat.Component.empty();
    }
    public net.minecraft.network.chat.Component skillDescription(ItemStack stack, int slot) { return skillDescription(slot); }
    public String masteryId() { return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).toString(); }

    /** Mesma prioridade para selecionar e usar; falhar nao ativa outra reliquia. */
    public static ItemStack controlledRelic(net.minecraft.world.entity.player.Player player) {
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof RelicItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    public int selectedSkill(ItemStack stack) {
        return (int) Math.min(Math.max(0, skills.size() - 1), readLong(stack, "NexusRelicSelectedSkill"));
    }

    public void selectSkill(ServerPlayer player, ItemStack stack, boolean next) {
        if (!stack.is(this) || skills.isEmpty()) return;
        RelicMastery mastery = player.getData(ModAttachmentType.RELIC_MASTERY);
        int slot = RelicSkill.nextSlot(selectedSkill(stack), skills.size(), mastery.level(masteryId()), next);
        writeLong(stack, "NexusRelicSelectedSkill", slot);
        var name = slot == 3 && !mastery.isDiscovered(masteryId())
                ? net.minecraft.network.chat.Component.literal("Habilidade secreta: ???") : skills.get(slot).name().copy();
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Selecionada: ").append(name), true);
    }

    /** Chamar no servidor somente depois de um uso funcional bem-sucedido, inclusive fora de combate.
     * Ativacoes de habilidade ja registram o uso; nao chamar novamente para o mesmo efeito. */
    public void recordSuccessfulUse(ServerPlayer player, ItemStack stack) {
        if (!stack.is(this)) return;
        if (player.getData(ModAttachmentType.RELIC_MASTERY).addExperience(masteryId(), RelicMastery.XP_PER_USE)) {
            player.syncData(ModAttachmentType.RELIC_MASTERY);
        }
    }

    public RelicItem(Properties properties, MatrizModos mode, int manaCost,
                     int cooldownTicks, int catalystCapacity, RelicSkill first, RelicSkill second,
                     RelicSkill third, RelicSkill secret) {
        this(properties, mode, manaCost, cooldownTicks, catalystCapacity,
                java.util.List.of(first, second, third, secret));
    }

    public RelicItem(Properties properties, MatrizModos mode, int manaCost,
                     int cooldownTicks, int catalystCapacity, RelicAbility ability) {
        this(properties, mode, manaCost, cooldownTicks, catalystCapacity,
                ability == null ? java.util.List.of() : java.util.List.of(RelicSkill.active(
                        net.minecraft.network.chat.Component.literal("Habilidade 1"), ability)));
    }

    private RelicItem(Properties properties, MatrizModos mode, int manaCost,
                      int cooldownTicks, int catalystCapacity, java.util.List<RelicSkill> skills) {
        super(properties.stacksTo(1));
        if (mode == null) throw new IllegalArgumentException("Relic mode cannot be null");

        if (manaCost < 0 || cooldownTicks < 0) {
            throw new IllegalArgumentException("Relic mana and cooldown values cannot be negative");
        }
        if (catalystCapacity < MIN_CATALYST_CAPACITY || catalystCapacity > MAX_CATALYST_CAPACITY) {
            throw new IllegalArgumentException("Relics must have catalyst weight capacity between 2 and 10");
        }
        this.mode = mode;

        this.baseManaCost = manaCost;
        this.baseCooldownTicks = cooldownTicks;
        this.catalystCapacity = catalystCapacity;
        this.skills = java.util.List.copyOf(skills);
    }

    public MatrizModos getMode() { return mode; }
    public int getCatalystCapacity() { return catalystCapacity; }
    /** Mana bruta por padrao; apenas catalisadores instalados podem alterar o elemento. */
    public Elementos getElement(ItemStack stack) {
        Elementos element = Elementos.NAO_ELEMENTAL;
        for (ItemStack catalyst : RelicCatalysts.get(stack)) {
            if (catalyst.getItem() instanceof RelicCatalyst modifier) {
                element = java.util.Objects.requireNonNull(modifier.modifyRelicElement(catalyst, stack, element),
                        "Catalyst relic element cannot be null");
            }
        }
        return element;
    }
    public long getCooldownRemaining(ItemStack stack, long gameTime) {
        return getCooldownRemaining(stack, gameTime, selectedSkill(stack));
    }

    public int getManaCost(ItemStack stack) {
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.MANA_COST, baseManaCost)));
    }

    public int getCooldownTicks(ItemStack stack) {
        return Math.max(0, (int) Math.round(modify(stack, RelicAttribute.COOLDOWN, baseCooldownTicks)));
    }

    public int getCooldownTicks(ItemStack stack, int slot) { return getCooldownTicks(stack); }
    public long getCooldownRemaining(ItemStack stack, long gameTime, int slot) {
        return Math.max(0L, readLong(stack, COOLDOWN_TAG + slot) - gameTime);
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

    public boolean activate(ServerPlayer player, ItemStack stack) {
        return activate(player, stack, selectedSkill(stack));
    }

    public int getManaCost(ItemStack stack, int slot) {
        int cost = skills.get(slot).manaCost();
        return cost < 0 ? getManaCost(stack)
                : Math.max(0, (int) Math.round(modify(stack, RelicAttribute.MANA_COST, cost)));
    }

    public boolean activate(ServerPlayer player, ItemStack stack, int slot) {
        if (!stack.is(this) || slot < 0 || slot >= skills.size()) return false;
        if (skills.get(slot).kind() == RelicSkill.Kind.PASSIVE) {
            RelicMastery mastery = player.getData(ModAttachmentType.RELIC_MASTERY);
            if (!mastery.togglePassive(masteryId(), slot)) return false;
            player.syncData(ModAttachmentType.RELIC_MASTERY);
            var name = slot == 3 && !mastery.isDiscovered(masteryId())
                    ? net.minecraft.network.chat.Component.literal("Habilidade secreta") : skills.get(slot).name().copy();
            player.sendSystemMessage(name.append(mastery.isPassiveEnabled(masteryId(), slot)
                    ? ": ligada" : ": desligada"), true);
            return true;
        }
        return executeSkill(player, stack, slot, RelicSkill.Kind.ACTIVE);
    }

    /** O evento funcional da reliquia chama este metodo; nunca conceder XP por apenas equipa-la. */
    public boolean triggerPassive(ServerPlayer player, ItemStack stack, int slot) {
        return executeSkill(player, stack, slot, RelicSkill.Kind.PASSIVE);
    }

    private boolean executeSkill(ServerPlayer player, ItemStack stack, int slot, RelicSkill.Kind kind) {
        if (!stack.is(this) || slot < 0 || slot >= skills.size()) return false;
        RelicSkill skill = skills.get(slot);
        RelicMastery mastery = player.getData(ModAttachmentType.RELIC_MASTERY);
        if (kind == RelicSkill.Kind.PASSIVE && !mastery.isPassiveEnabled(masteryId(), slot)) return false;
        if (skill.kind() != kind || mastery.level(masteryId()) < RelicSkill.requiredLevel(slot)
                || !skill.condition().test(player, stack)) return false;
        long gameTime = player.level().getGameTime();
        if (getCooldownRemaining(stack, gameTime, slot) > 0) return false;

        MagicStatus magic = player.getData(ModAttachmentType.MAGIC_STATS);
        if (!magic.isCreated()) return false;
        Elementos playerElement = getElement(stack);
        if (playerElement != Elementos.NAO_ELEMENTAL && !magic.get(playerElement).isUnlocked()) return false;
        Elementos relicElement = playerElement;
        boolean combo = false;
        int resonance = playerElement == Elementos.NAO_ELEMENTAL ? 0 : statusValue(magic, playerElement, true);
        int affinity = playerElement == Elementos.NAO_ELEMENTAL ? 0 : statusAffinity(magic, playerElement);
        int cost = scaleDownToHalf(getManaCost(stack, slot), resonance);
        int cooldown = scaleDownToHalf(getCooldownTicks(stack, slot), resonance);
        double power = 1.0D + affinity / (double) MAX_ELEMENTAL_STATUS;
        RelicAbilityContext abilityContext = new RelicAbilityContext(
                playerElement, relicElement, combo, resonance, affinity, cost, cooldown, power);

        RelicPayment payment = planSameElementPayment(magic, cost);
        if (payment == null || !skill.effect().activate(player, stack, abilityContext)) return false;

        applyPayment(player, magic, payment);
        writeLong(stack, COOLDOWN_TAG + slot, gameTime + cooldown);
        player.syncData(ModAttachmentType.MAGIC_STATS);
        boolean changed = mastery.addExperience(masteryId(), RelicMastery.XP_PER_USE);
        if (slot == 3) changed |= mastery.discover(masteryId());
        if (changed) player.syncData(ModAttachmentType.RELIC_MASTERY);
        return true;
    }

    private static int statusValue(MagicStatus magic, Elementos element, boolean resonance) {
        ElementalStatus status = magic.get(element);
        if (!status.isUnlocked()) return 1;
        int value = resonance ? status.getRessonancia() : status.getAfinidade();
        return Math.clamp(value, 1, MAX_ELEMENTAL_STATUS);
    }

    private static int statusAffinity(MagicStatus magic, Elementos element) {
        return statusValue(magic, element, false);
    }

    private static int scaleDownToHalf(int base, int resonance) {
        return Math.max(0, (int) Math.ceil(base * (1.0D - 0.5D * resonance / MAX_ELEMENTAL_STATUS)));
    }

    private static RelicPayment planSameElementPayment(MagicStatus magic, int cost) {
        if (magic.getMana() < cost) return null;
        int playerMana = Math.min(magic.getMana(), cost);
        return new RelicPayment(playerMana);
    }

    private static void applyPayment(ServerPlayer player, MagicStatus magic, RelicPayment payment) {
        magic.useMana(payment.playerMana());
    }

    private record RelicPayment(int playerMana) {}

    private static long readLong(ItemStack stack, String key) {
        return Math.max(0L, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(key, 0L));
    }

    private static void writeLong(ItemStack stack, String key, long value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(key, Math.max(0L, value)));
    }

}
