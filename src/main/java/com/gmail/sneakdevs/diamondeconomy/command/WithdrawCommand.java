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
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;


public class WithdrawCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal(DiamondEconomyConfig.getInstance().withdrawCommandName)
                .then(
                        Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(e -> {
                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                    return withdrawCommand(e, amount);
                                })
                );
    }

    public static int withdrawCommand(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        if (dm.changeBalance(player.getStringUUID(), -amount)) {
            ctx.getSource().sendSuccess(() ->
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("Withdrew ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount - DiamondUtils.dropItem(amount, player)))
            , false);
        } else {
            int balance = dm.getBalanceFromUUID(player.getStringUUID());
            ctx.getSource().sendFailure(
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append(Component.literal("Insufficient funds! ")
                                    .withStyle(Style.EMPTY.withColor(TextColor.parseColor("dark_red")))
                            )
                            .append("Your balance: ")
                            .append(DiamondEconomyConfig.currencyToLiteral(balance))
            );
        }
        return 1;
    }

}
