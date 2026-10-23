package com.walnutbeachdigital.epochsmith.era;

import java.util.Arrays;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Test command:
 *   /epochsmith era get                  - shows your own era
 *   /epochsmith era get <player>         - shows another player's era (ops only)
 *   /epochsmith era set <player> <era>   - changes a player's era (ops only)
 */
public final class EraCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epochsmith")
                .then(Commands.literal("era")
                        .then(Commands.literal("get")
                                .executes(ctx -> get(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("era", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(Era.values()).map(Era::getSerializedName), builder))
                                                .executes(ctx -> set(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "era"))))))));
    }

    private static int get(CommandSourceStack source, ServerPlayer player) {
        Era era = EraManager.getEra(player);
        source.sendSuccess(() -> Component.translatable("commands.epochsmith.era.get",
                player.getDisplayName(), era.displayName()), false);
        return 1;
    }

    private static int set(CommandSourceStack source, ServerPlayer player, String id) {
        Era era = Era.byId(id);
        if (era == null) {
            source.sendFailure(Component.translatable("commands.epochsmith.era.unknown", id));
            return 0;
        }
        EraManager.setEra(player, era, true);
        source.sendSuccess(() -> Component.translatable("commands.epochsmith.era.set",
                player.getDisplayName(), era.displayName()), true);
        return 1;
    }

    private EraCommand() {
    }
}
