package com.gmail.sneakdevs.diamondeconomy.command;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class SetCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal(DiamondEconomyConfig.getInstance().setCommandName)
                .requires((permission) -> permission.hasPermission(DiamondEconomyConfig.getInstance().opCommandsPermissionLevel))
                .then(
                        Commands.argument("players", EntityArgument.players())
                                .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(e -> {
                                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                                    return setCommand(e, EntityArgument.getPlayers(e, "players").stream().toList(), amount);
                                                }))
                )
                .then(
                        Commands.argument("amount", IntegerArgumentType.integer(0))
                                .then(
                                        Commands.argument("shouldModifyAll", BoolArgumentType.bool())
                                                .executes(e -> {
                                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                                    boolean shouldModifyAll = BoolArgumentType.getBool(e, "shouldModifyAll");
                                                    return setCommand(e, amount, shouldModifyAll);
                                                })
                                )
                                .executes(e -> {
                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                    return setCommand(e, amount, false);
                                })
                );
    }

    public static int setCommand(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, int amount) throws CommandSyntaxException {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        for (ServerPlayer player : players) {
            dm.setBalance(player.getStringUUID(), amount);
        }
        if (players.size() == 1) {
            ctx.getSource().sendSuccess(() ->
                     Component.literal("Set balance of ")
                             .append(players.iterator().next().getDisplayName())
                             .append(" to ")
                             .append(DiamondEconomyConfig.currencyToLiteral(amount))
            , true);
        } else if (players.size() > 1) {
            ctx.getSource().sendSuccess(() ->
                    Component.literal("Set balance of " + players.size() + " players to ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
            , true);
        }
        return players.size();
    }

    public static int setCommand(CommandContext<CommandSourceStack> ctx, int amount, boolean shouldModifyAll) throws CommandSyntaxException {
        if (shouldModifyAll) {
            DiamondUtils.getDatabaseManager().setAllBalance(amount);
            ctx.getSource().sendSuccess(() ->
                    Component.literal("All accounts balance to ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
            , true);
        } else {
            DiamondUtils.getDatabaseManager().setBalance(ctx.getSource().getPlayerOrException().getStringUUID(), amount);
            ctx.getSource().sendSuccess(() ->
                    Component.literal("Updated your balance to ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
            , true);
        }
        return 1;
    }
}
