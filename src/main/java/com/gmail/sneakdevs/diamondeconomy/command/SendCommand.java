package com.gmail.sneakdevs.diamondeconomy.command;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
            .then(Commands.argument("playerName", StringArgumentType.string())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(e -> {
                        String player = StringArgumentType.getString(e, "playerName");
                        int amount = IntegerArgumentType.getInteger(e, "amount");

                        return sendCommand(e, player, e.getSource().getPlayerOrException(), amount);
                    })))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(e -> {
                        String player = EntityArgument.getPlayer(e, "player").getName().getString();
                        int amount = IntegerArgumentType.getInteger(e, "amount");

                        return sendCommand(e, player, e.getSource().getPlayerOrException(), amount);
                    })));
    }

    public static int sendCommand(CommandContext<CommandSourceStack> ctx, String player, ServerPlayer serverPlayer1, int amount) throws CommandSyntaxException {
        if (amount < 1) throw CommandExceptions.BALANCE_ERROR.create("You can't send negative money. Nice try.");

        DatabaseManager dm = DiamondUtils.getDatabaseManager();

        String playerName, playerUUID = null;
        ServerPlayer serverPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(player);
        if (serverPlayer != null) {
            playerUUID = serverPlayer.getStringUUID();
            playerName = serverPlayer.getName().getString();
        } else {
            playerUUID = dm.getUUIDFromName(player);
            if (playerUUID != null) {
                playerName = dm.getNameFromUUID(playerUUID);
            } else {
                playerName = player;
            }
        }

        if (playerUUID == null || playerName == null || playerUUID.isEmpty() || playerName.isEmpty()) {
            // Player does not exist on server or database
            ctx.getSource().sendFailure(Component.literal("Player not found."));
            return -1;
        }

        long newValue = dm.getBalanceFromUUID(playerUUID) + (long) amount;
        if (newValue >= Integer.MAX_VALUE) // max int
            throw CommandExceptions.MAX_BALANCE_ERROR.create();

        if (dm.changeBalance(serverPlayer1.getStringUUID(), -amount)) {
            // sender has sufficient funds
            dm.changeBalance(playerUUID, amount);

            ctx.getSource().sendSuccess(() ->
                    Component.literal("Sent ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
                            .append(" to " + playerName)
            , false);

            if (serverPlayer != null) {
                serverPlayer.displayClientMessage(
                        Component.literal("You received ")
                                .append(DiamondEconomyConfig.currencyToLiteral(amount))
                                .append(" from ")
                                .append(serverPlayer1.getDisplayName())
                , false);
            }
        } else {
            ctx.getSource().sendFailure(
                    Component.empty()
                            .append(Component.literal("Insufficient funds! ")
                                    .withStyle(Style.EMPTY.withColor(TextColor.parseColor("dark_red")))
                            )
                            .append("Your balance: ")
                            .append(DiamondEconomyConfig.currencyToLiteral(dm.getBalanceFromUUID(serverPlayer1.getStringUUID())))
            );
        }
        return 1;
    }
}
