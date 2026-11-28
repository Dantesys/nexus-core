package org.dantesys.nexus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.dantesys.nexus.advancement.ManaAdvancements;
import org.dantesys.nexus.data.ModAttachmentType;
import org.dantesys.nexus.magic.MagicStatus;

import java.util.Collection;

/**
 * /nexus status get [jogador]
 * /nexus status created &lt;jogadores&gt; &lt;true|false&gt;
 * /nexus status mana &lt;jogadores&gt; &lt;current|max|regeneration|affinity|resonance&gt; &lt;valor&gt;
 * /nexus maestria set &lt;jogadores&gt; &lt;relíquia&gt; &lt;nível&gt;
 */
public class NexusCommands {

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> manaCommand = Commands.literal("mana");
        var target = Commands.argument("target", EntityArgument.players());
        for (ManaField field : ManaField.values()) {
            target.then(Commands.literal(field.id)
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, field.max))
                            .executes(context -> setManaValue(context, EntityArgument.getPlayers(context, "target"), field))));
        }
        manaCommand.then(target);

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
                .then(manaCommand);

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
                                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, org.dantesys.nexus.relic.RelicMastery.MAX_LEVEL))
                                                        .executes(NexusCommands::setMastery)))))));
    }

    private static int setMastery(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = net.minecraft.commands.arguments.IdentifierArgument.getId(context, "reliquia");
        var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        if (!registry.containsKey(id) || !(registry.getValue(id) instanceof org.dantesys.nexus.relic.RelicItem)) {
            throw new DynamicCommandExceptionType(value -> Component.translatable("message.nexus.nexuscommands.reliquia_invalida", String.valueOf(value))).create(id);
        }
        int level = IntegerArgumentType.getInteger(context, "nivel");
        var players = EntityArgument.getPlayers(context, "target");
        for (ServerPlayer player : players) {
            player.getData(ModAttachmentType.RELIC_MASTERY).setLevel(id.toString(), level);
            player.syncData(ModAttachmentType.RELIC_MASTERY);
        }
        context.getSource().sendSuccess(() -> Component.translatable("message.nexus.nexuscommands.maestria_de_definida_para_em", id.toString(), level, players.size()), true);
        return players.size();
    }

    private static int getStatus(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
        context.getSource().sendSuccess(() -> Component.translatable("message.nexus.nexuscommands.created_mana_regeneration_affinity_reson", player.getName(), status.isCreated(), status.getMana(), status.getMaxMana(), status.getRegeneracao(), status.getAfinidade(), status.getRessonancia()), false);
        return 1;
    }

    private static int setCreated(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, boolean created) {
        for (ServerPlayer player : players) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            status.setCreated(created);
            sync(player, status);
        }
        return success(context, players.size(), Component.translatable("message.nexus.nexuscommands.created_definido", created));
    }

    private static int setManaValue(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, ManaField field) {
        int value = IntegerArgumentType.getInteger(context, "value");
        for (ServerPlayer player : players) {
            MagicStatus status = player.getData(ModAttachmentType.MAGIC_STATS);
            switch (field) {
                case CURRENT -> status.setMana(value);
                case MAX -> status.setMaxMana(value);
                case REGENERATION -> status.setRegeneracao(value);
                case AFFINITY -> status.setAfinidade(value);
                case RESONANCE -> status.setRessonancia(value);
            }
            sync(player, status);
        }
        return success(context, players.size(), Component.translatable("message.nexus.nexuscommands.mana_definida", field.id, value));
    }

    private static void sync(ServerPlayer player, MagicStatus status) {
        player.syncData(ModAttachmentType.MAGIC_STATS);
        ManaAdvancements.check(player, status);
    }

    private static int success(CommandContext<CommandSourceStack> context, int count, Component message) {
        context.getSource().sendSuccess(() -> Component.translatable("message.nexus.nexuscommands.para_jogador_es", message, count), true);
        return count;
    }

    private enum ManaField {
        CURRENT("current"),
        MAX("max"),
        REGENERATION("regeneration"),
        AFFINITY("affinity"),
        RESONANCE("resonance");

        private final String id;
        private final int max = MagicStatus.MAX_STATUS;

        ManaField(String id) {
            this.id = id;
        }
    }
}
