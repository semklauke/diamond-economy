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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class DepositCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal(DiamondEconomyConfig.getInstance().depositCommandName)
                .executes(DepositCommand::depositHandCommand)
                .then(Commands.literal("inv").executes(DepositCommand::depositAllCommand)
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            final int n = IntegerArgumentType.getInteger(ctx, "amount");
                            return depositAllCommand(ctx, n);
                        }))
                );
    }

    public static int depositAllCommand(CommandContext<CommandSourceStack> ctx, int n) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        int currencyCount = 0;
        for (int i = DiamondEconomyConfig.getCurrencyValues().length - 1; i >= 0; i--) {
            int currencyMultiplier = DiamondEconomyConfig.getCurrencyValues()[i];
            if (n != -1 && (currencyCount + currencyMultiplier) > n) break;
            for (int j = 0; j < player.getInventory().getContainerSize(); j++) {
                if (player.getInventory().getItem(j).getItem().equals(DiamondEconomyConfig.getCurrency(i))) {
                    int takeAmount = player.getInventory().getItem(j).getCount();
                    if (n != -1) while (currencyCount + (currencyMultiplier * takeAmount) > n && takeAmount >= 0) takeAmount--;
                    if (takeAmount > 0) {
                        currencyCount += takeAmount * currencyMultiplier;
                        player.getInventory().removeItem(j, takeAmount);
                    }
                }
            }
        }
        if (dm.changeBalance(player.getStringUUID(), currencyCount)) {
            int finalCurrencyCount = currencyCount;
            ctx.getSource().sendSuccess(() ->
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("Added ")
                            .append(DiamondEconomyConfig.currencyToLiteral(finalCurrencyCount))
                            .append(" to your account")
            , false);
        } else {
            DiamondUtils.dropItem(currencyCount, player);
            throw CommandExceptions.MAX_BALANCE_ERROR.create();
        }
        return 1;
    }

    public static int depositAllCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return depositAllCommand(ctx, -1);
    }

    public static int depositHandCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack hand = player.getInventory().getSelected();

        int i;
        for (i = DiamondEconomyConfig.getCurrencyValues().length - 1; i >= 0; i--) {
            if (hand.getItem().equals(DiamondEconomyConfig.getCurrency(i))) {
                break;
            }
        }
        if (hand.isEmpty() || i == -1) {
            // Hand has no item
            ctx.getSource().sendFailure(
                Component.empty()
                        .append(DiamondEconomyConfig.ChatPrefix())
                        .append("Put a currency item in your hand or use the /")
                        .append(Component.literal(DiamondEconomyConfig.getInstance().depositCommandName + " deposit inv [amount]")
                                        .withStyle(Style.EMPTY.withItalic(true))
                        )
                        .append("command.")
            );
        }

        // add balance to account
        final int currencyCount = hand.getCount() * DiamondEconomyConfig.getCurrencyValues()[i];
        if (dm.changeBalance(player.getStringUUID(), currencyCount)) {
            ctx.getSource().sendSuccess(() ->
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("Added ")
                            .append(DiamondEconomyConfig.currencyToLiteral(currencyCount))
                            .append(" to your account")
            , false);
            player.getInventory().removeFromSelected(true); // remove whole stack
            return currencyCount;
        } else {
            throw CommandExceptions.MAX_BALANCE_ERROR.create();
        }
    }
}
