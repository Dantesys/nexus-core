package org.dantesys.nexus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.events.NexusPlayerEvents;
import org.dantesys.nexus.magic.ElementalStatus;
import org.dantesys.nexus.magic.MagicStatus;
import org.dantesys.nexus.util.Elementos;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class NexusCommands {
    private static final DynamicCommandExceptionType INVALID_ELEMENT = new DynamicCommandExceptionType(
            value -> Component.literal("Elemento invalido: " + value)
    );
    private static final DynamicCommandExceptionType NO_BIOME_EVENT = new DynamicCommandExceptionType(
            value -> Component.literal("Nenhum evento de bioma Nexus encontrado para " + value)
    );

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> statusCommand = Commands.literal("status")
                .then(Commands.literal("get")
                        .executes(context -> getStatus(context, context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getStatus(context, EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("created")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setCreated(
                                                context,
                                                EntityArgument.getPlayers(context, "target"),
                                                BoolArgumentType.getBool(context, "value"))))))
                .then(Commands.literal("mana")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.literal("current")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(context -> setManaValue(context, EntityArgument.getPlayers(context, "target"), ManaField.CURRENT))))
                                .then(Commands.literal("max")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(context -> setManaValue(context, EntityArgument.getPlayers(context, "target"), ManaField.MAX))))
                                .then(Commands.literal("regeneration")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(context -> setManaValue(context, EntityArgument.getPlayers(context, "target"), ManaField.REGENERATION))))))
                .then(Commands.literal("selected")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("element", StringArgumentType.word())
                                        .suggests((context, builder) -> suggestElements(builder))
                                        .executes(context -> setSelectedMana(context, EntityArgument.getPlayers(context, "target"))))))
                .then(Commands.literal("element")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("element", StringArgumentType.word())
                                        .suggests((context, builder) -> suggestElements(builder))
                                        .then(Commands.literal("unlocked")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(context -> setElementUnlocked(context, EntityArgument.getPlayers(context, "target")))))
                                        .then(Commands.literal("affinity")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000))
                                                        .executes(context -> setElementValue(context, EntityArgument.getPlayers(context, "target"), ElementField.AFFINITY))))
                                        .then(Commands.literal("resonance")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000))
                                                        .executes(context -> setElementValue(context, EntityArgument.getPlayers(context, "target"), ElementField.RESONANCE)))))));

        LiteralArgumentBuilder<CommandSourceStack> biomeEventCommand = Commands.literal("biome_event")
                .then(Commands.literal("trigger")
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes(context -> triggerBiomeEvent(context, EntityArgument.getPlayers(context, "target"), null))
                                .then(Commands.argument("element", StringArgumentType.word())
                                        .suggests((context, builder) -> suggestBiomeElements(builder))
                                        .executes(context -> triggerBiomeEvent(
                                                context,
                                                EntityArgument.getPlayers(context, "target"),
                                                readElement(context, "element"))))))
                .then(Commands.literal("schedule")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                        .executes(context -> scheduleBiomeEvent(
                                                context,
                                                EntityArgument.getPlayers(context, "target"),
                                                IntegerArgumentType.getInteger(context, "seconds"),
                                                null))
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((context, builder) -> suggestBiomeElements(builder))
                                                .executes(context -> scheduleBiomeEvent(
                                                        context,
                                                        EntityArgument.getPlayers(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "seconds"),
                                                        readElement(context, "element")))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes(context -> clearBiomeEvent(context, EntityArgument.getPlayers(context, "target")))));

        dispatcher.register(Commands.literal("nexus")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(statusCommand)
                .then(Commands.literal("maestria")
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("reliquia", net.minecraft.commands.arguments.IdentifierArgument.id())
                                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                                                        net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                                                                .filter(item -> item instanceof org.dantesys.nexus.relic.RelicItem)
                                                                .map(net.minecraft.core.registries.BuiltInRegistries.ITEM::getKey), builder))
                                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 100))
                                                        .executes(NexusCommands::setMastery))))))
                .then(biomeEventCommand));
    }

    private static int setMastery(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = net.minecraft.commands.arguments.IdentifierArgument.getId(context, "reliquia");
        var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        if (!registry.containsKey(id) || !(registry.getValue(id) instanceof org.dantesys.nexus.relic.RelicItem)) {
            throw new DynamicCommandExceptionType(value -> Component.literal("Relíquia inválida: " + value)).create(id);
        }
        int level = IntegerArgumentType.getInteger(context, "nivel");
        var players = EntityArgument.getPlayers(context, "target");
        for (ServerPlayer player : players) {
            player.getData(ModAttachmentType.RELIC_MASTERY).setLevel(id.toString(), level);
            player.syncData(ModAttachmentType.RELIC_MASTERY);
        }
        context.getSource().sendSuccess(() -> Component.literal("Maestria de " + id + " definida para " + level
                + " em " + players.size() + " jogador(es)."), true);
        return players.size();
    }

    private static int getStatus(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
        context.getSource().sendSuccess(() -> Component.literal(
                player.getName().getString()
                        + " | created=" + status.isCreated()
                        + " | mana=" + status.getMana() + "/" + status.getMaxMana()
                        + " | regeneration=" + status.getRegeneracao()
                        + " | selected=" + status.getSelectedMana().getSerializedName()
        ), false);

        for (Elementos elemento : Elementos.values()) {
            ElementalStatus elemental = status.get(elemento);
            context.getSource().sendSuccess(() -> Component.literal(
                    elemento.getSerializedName()
                            + " | unlocked=" + elemental.isUnlocked()
                            + " | affinity=" + elemental.getAfinidade()
                            + " | resonance=" + elemental.getRessonancia()
            ), false);
        }
        return 1;
    }

    private static int setCreated(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, boolean created) {
        for (ServerPlayer player : players) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            status.setCreated(created);
            sync(player);
        }
        return success(context, players.size(), "status created definido como " + created);
    }

    private static int setManaValue(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, ManaField field) {
        int value = IntegerArgumentType.getInteger(context, "value");
        for (ServerPlayer player : players) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            switch (field) {
                case CURRENT -> status.setMana(value);
                case MAX -> status.setMaxMana(value);
                case REGENERATION -> status.setRegeneracao(value);
            }
            sync(player);
        }
        return success(context, players.size(), "mana " + field.id + " definida como " + value);
    }

    private static int setSelectedMana(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) throws CommandSyntaxException {
        Elementos elemento = readElement(context, "element");
        for (ServerPlayer player : players) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            status.setSelectedMana(elemento);
            sync(player);
        }
        return success(context, players.size(), "mana selecionada definida como " + elemento.getSerializedName());
    }

    private static int setElementUnlocked(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) throws CommandSyntaxException {
        Elementos elemento = readElement(context, "element");
        boolean value = BoolArgumentType.getBool(context, "value");
        for (ServerPlayer player : players) {
            player.getData(ModAttachmentType.MAGIC_STATS).get(elemento).setUnlocked(value);
            sync(player);
        }
        return success(context, players.size(), elemento.getSerializedName() + " unlocked definido como " + value);
    }

    private static int setElementValue(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, ElementField field) throws CommandSyntaxException {
        Elementos elemento = readElement(context, "element");
        int value = IntegerArgumentType.getInteger(context, "value");
        for (ServerPlayer player : players) {
            ElementalStatus status = player.getData(ModAttachmentType.MAGIC_STATS).get(elemento);
            switch (field) {
                case AFFINITY -> status.setAfinidade(value);
                case RESONANCE -> status.setRessonancia(value);
            }
            sync(player);
        }
        return success(context, players.size(), elemento.getSerializedName() + " " + field.id + " definido como " + value);
    }

    private static int triggerBiomeEvent(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, Elementos forcedElement) throws CommandSyntaxException {
        int triggered = 0;
        for (ServerPlayer player : players) {
            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }

            Elementos elemento = forcedElement != null ? forcedElement : NexusPlayerEvents.getNexusBiomeElement(player);
            if (elemento == null || elemento == Elementos.NAO_ELEMENTAL) {
                throw NO_BIOME_EVENT.create(player.getName().getString());
            }

            NexusPlayerEvents.triggerNexusBiomeEvent(level, player, elemento);
            triggered++;
        }
        return success(context, triggered, "evento de bioma ativado");
    }

    private static int scheduleBiomeEvent(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, int seconds, Elementos forcedElement) throws CommandSyntaxException {
        int scheduled = 0;
        for (ServerPlayer player : players) {
            Elementos elemento = forcedElement != null ? forcedElement : NexusPlayerEvents.getNexusBiomeElement(player);
            if (elemento == null || elemento == Elementos.NAO_ELEMENTAL) {
                throw NO_BIOME_EVENT.create(player.getName().getString());
            }

            NexusPlayerEvents.scheduleNexusBiomeEvent(player, elemento, seconds * 20);
            scheduled++;
        }
        return success(context, scheduled, "evento de bioma agendado em " + seconds + "s");
    }

    private static int clearBiomeEvent(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            status.setNexusBiomeEventPending(false);
            status.setNexusBiomeEventCooldown(0);
            NexusPlayerEvents.clearScheduledNexusBiomeEvent(player);
            sync(player);
        }
        return success(context, players.size(), "evento de bioma limpo");
    }

    private static Elementos readElement(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String value = StringArgumentType.getString(context, name).toLowerCase(Locale.ROOT);
        for (Elementos elemento : Elementos.values()) {
            if (elemento.getSerializedName().equals(value) || elemento.name().toLowerCase(Locale.ROOT).equals(value)) {
                return elemento;
            }
        }
        throw INVALID_ELEMENT.create(value);
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestElements(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (Elementos elemento : Elementos.values()) {
            builder.suggest(elemento.getSerializedName());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestBiomeElements(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (Elementos elemento : Elementos.values()) {
            if (elemento != Elementos.NAO_ELEMENTAL) {
                builder.suggest(elemento.getSerializedName());
            }
        }
        return builder.buildFuture();
    }

    private static void sync(ServerPlayer player) {
        player.syncData(ModAttachmentType.MAGIC_STATS);
    }

    private static int success(CommandContext<CommandSourceStack> context, int count, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message + " para " + count + " jogador(es)."), true);
        return count;
    }

    private enum ManaField {
        CURRENT("current"),
        MAX("max"),
        REGENERATION("regeneration");

        private final String id;

        ManaField(String id) {
            this.id = id;
        }
    }

    private enum ElementField {
        AFFINITY("affinity"),
        RESONANCE("resonance");

        private final String id;

        ElementField(String id) {
            this.id = id;
        }
    }
}
