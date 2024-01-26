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
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class ModifyCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal(DiamondEconomyConfig.getInstance().modifyCommandName)
                .requires((permission) -> permission.hasPermission(DiamondEconomyConfig.getInstance().opCommandsPermissionLevel))
                .then(
                        Commands.argument("players", EntityArgument.players())
                                .then(
                                        Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(e -> {
                                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                                    return modifyCommand(e, EntityArgument.getPlayers(e, "players").stream().toList(), amount);
                                                }))
                )
                .then(
                        Commands.argument("amount", IntegerArgumentType.integer())
                                .then(
                                        Commands.argument("shouldModifyAll", BoolArgumentType.bool())
                                                .executes(e -> {
                                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                                    boolean shouldModifyAll = BoolArgumentType.getBool(e, "shouldModifyAll");
                                                    return modifyCommand(e, amount, shouldModifyAll);
                                                })
                                )
                                .executes(e -> {
                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                    return modifyCommand(e, amount, false);
                                })
                );
    }

    public static int modifyCommand(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, int amount) {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        for (ServerPlayer player : players) {
            if (dm.changeBalance(player.getStringUUID(), amount)) {
                ctx.getSource().sendSuccess(() ->
                        Component.literal(amount >= 0 ? "Increased " : "Decreased")
                                .append(player.getDisplayName() +"'s account by ")
                                .append(DiamondEconomyConfig.currencyToLiteral(amount))
                                .append(". New balance: ")
                                .append(DiamondEconomyConfig.currencyToString(dm.getBalanceFromUUID(player.getStringUUID())))
                , true);
            } else {
               ctx.getSource().sendFailure(
                        Component.literal("For ")
                                .append(player.getDisplayName())
                                .append(" the balance limit was exceeded. No changes.")
                                //.withStyle(Style.EMPTY.withColor(0xff5555))
               );
            }

        }
        return players.size();
    }

    public static int modifyCommand(CommandContext<CommandSourceStack> ctx, int amount, boolean shouldModifyAll) throws CommandSyntaxException {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        if (shouldModifyAll) {
            dm.changeAllBalance(amount);
            ctx.getSource().sendSuccess(() ->
                    Component.literal(amount >= 0 ? "Increased " : "Decreased")
                            .append(" everyone's account by ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
            , true);
        } else {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            if (dm.changeBalance(player.getStringUUID(), amount)) {
                ctx.getSource().sendSuccess(() ->
                        Component.literal(amount >= 0 ? "Increased " : "Decreased")
                                .append(" your account by ")
                                .append(DiamondEconomyConfig.currencyToLiteral(amount))
                                .append(". New balance: ")
                                .append(DiamondEconomyConfig.currencyToString(dm.getBalanceFromUUID(player.getStringUUID())))
                , true);
            } else {
               ctx.getSource().sendFailure(
                       Component.literal("The balance limit for your account was exceeded. No changes.")
               );
            }
        }
        return 1;
    }
}
