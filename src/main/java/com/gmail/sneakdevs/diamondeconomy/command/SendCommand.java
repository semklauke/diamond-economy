package com.gmail.sneakdevs.diamondeconomy.command;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

public class SendCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal(DiamondEconomyConfig.getInstance().sendCommandName)
                .then(
                        Commands.argument("player", EntityArgument.player())
                                .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(e -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(e, "player");
                                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                                    return sendCommand(e, player, e.getSource().getPlayerOrException(), amount);
                                                })));
    }

    public static int sendCommand(CommandContext<CommandSourceStack> ctx, ServerPlayer player, ServerPlayer player1, int amount) throws CommandSyntaxException {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        long newValue = dm.getBalanceFromUUID(player.getStringUUID()) + amount;
        if (newValue >= Integer.MAX_VALUE) // max int
            throw CommandExceptions.MAX_BALANCE_ERROR.create();
        if (dm.changeBalance(player1.getStringUUID(), -amount)) {
            // sender has sufficient funds
            dm.changeBalance(player.getStringUUID(), amount);
            player.displayClientMessage(
                    Component.literal("You received ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
                            .append(" from ")
                            .append(player1.getDisplayName())
            , false);
            ctx.getSource().sendSuccess(() ->
                    Component.literal("Sent ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
                            .append(" to ")

            , false);
        } else {
            ctx.getSource().sendFailure(
                    Component.empty()
                            .append(Component.literal("Insufficient funds! ")
                                    .withStyle(Style.EMPTY.withColor(TextColor.parseColor("dark_red")))
                            )
                            .append("Your balance: ")
                            .append(DiamondEconomyConfig.currencyToLiteral(dm.getBalanceFromUUID(player1.getStringUUID())))
            );
        }
        return 1;
    }
}
