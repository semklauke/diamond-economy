package com.gmail.sneakdevs.diamondeconomy.command;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import com.mojang.brigadier.suggestion.SuggestionProvider;

public class WithdrawCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(CommandBuildContext buildContext){
        return Commands.literal(DiamondEconomyConfig.getInstance().withdrawCommandName)
                .then(
                        Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(e -> {
                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                    return withdrawCommand(e, amount);
                                }).then(
                                        Commands.argument("item", ItemArgument.item(buildContext))
                                                .suggests(item_suggester)
                                                .executes(e -> {
                                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                                    Item item = ItemArgument.getItem(e, "item").getItem();
                                                    return withdrawCommand(e, amount, item);
                                                })
                                )
                );
    }

    public static int withdrawCommand(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        // TODO: Add greedy withdraw, this only withdraws the first item in list
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        if (dm.changeBalance(player.getStringUUID(), -amount)) {
            Item withdrawItem = DiamondEconomyConfig.getCurrency(0);
            //int itemMultiplier = DiamondEconomyConfig.getCurrencyValues()[0];
            int countNotWithdraw = DiamondUtils.giveItem(withdrawItem,  amount, player);
            ctx.getSource().sendSuccess(() ->
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("Withdrew ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount - countNotWithdraw))
            , false);
        } else {
            int balance = dm.getBalanceFromUUID(player.getStringUUID());
            ctx.getSource().sendFailure(
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append(Component.literal("Insufficient funds! ")
                                    .withStyle(Style.EMPTY.withColor(TextColor.parseColor("dark_red"))))
                            .append(Component.literal("Your balance: ")
                                    .withStyle(Style.EMPTY.withColor(TextColor.parseColor("white"))))
                            .append(DiamondEconomyConfig.currencyToLiteral(balance))
            );
        }
        return 1;
    }

    public static int withdrawCommand(CommandContext<CommandSourceStack> ctx, int amount, Item item) throws CommandSyntaxException {
        // find item in currency list
        // no iterators possible here ;(
        int i, n = DiamondEconomyConfig.getCurrencyValues().length;
        for (i = 0; i < n; i++) {
            if (DiamondEconomyConfig.getCurrency(i).equals(item)) {
                break;
            }
        }
        if (i == n) {
            // not found
            throw CommandExceptions.ITEM_ERROR.create("This item is not a valid currency");
        }

        // is amount a multiple of currency value
        int itemMultiplier = DiamondEconomyConfig.getCurrencyValues()[i];
        if (amount % itemMultiplier != 0) {
            throw CommandExceptions.ITEM_ERROR.create(
                    "You tried to withdraw an amount that can't be represented by "
                            + DiamondEconomyConfig.getCurrencyName(i)
                            + ". Try the nearest multiple "
                            + DiamondEconomyConfig.currencyToString(amount - (amount % itemMultiplier))
            );
        }

        ServerPlayer player = ctx.getSource().getPlayerOrException();
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        if (dm.changeBalance(player.getStringUUID(), -amount)) {
            int countNotWithdraw = DiamondUtils.giveItem(item,  amount / itemMultiplier, player);
            ctx.getSource().sendSuccess(() ->
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("Withdrew ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount - countNotWithdraw))
            , false);
            if (countNotWithdraw > 0)
                dm.changeBalance(player.getStringUUID(), countNotWithdraw);
        } else {
            int balance = dm.getBalanceFromUUID(player.getStringUUID());
            ctx.getSource().sendFailure(
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append(Component.literal("Insufficient funds! ")
                                    .withStyle(Style.EMPTY.withColor(TextColor.parseColor("dark_red"))))
                            .append(Component.literal("Your balance: ")
                                    .withStyle(Style.EMPTY.withColor(TextColor.parseColor("white"))))
                            .append(DiamondEconomyConfig.currencyToLiteral(balance))
            );
        }
        return 1;
    }

    private static final SuggestionProvider<CommandSourceStack> item_suggester = (context, builder) -> {

        for (String ident : DiamondEconomyConfig.getInstance().currencies) {
            builder.suggest(ident);
        }
        return builder.buildFuture();
    };

}
