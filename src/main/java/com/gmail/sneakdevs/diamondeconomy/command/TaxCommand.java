package com.gmail.sneakdevs.diamondeconomy.command;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class TaxCommand {
    private static final int DEFAUL_MIN_TAXLEVEL = 1;
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal(DiamondEconomyConfig.getInstance().taxCommandName)
                .then(Commands.literal("all")
                        .executes(e -> taxAllCommand(e, -1, DEFAUL_MIN_TAXLEVEL))
                        .then(Commands.argument("%", IntegerArgumentType.integer(1, 100))
                                .executes(e -> taxAllCommand(e, IntegerArgumentType.getInteger(e, "%"), DEFAUL_MIN_TAXLEVEL))
                                .then(Commands.argument("minTaxLevel", IntegerArgumentType.integer(0))
                                    .executes(e -> taxAllCommand(e, IntegerArgumentType.getInteger(e, "%"), IntegerArgumentType.getInteger(e, "minTaxLevel")))
                            )
                        )
                )
                .then(Commands.literal("info")
                        .executes(e -> taxInfoCommand(e, e.getSource().getPlayer()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(e -> taxInfoCommand(e, EntityArgument.getPlayer(e, "player")))
                        )
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(e -> taxPlayerCommand(e, -1, EntityArgument.getPlayer(e, "player")))
                        .then(Commands.argument("%", IntegerArgumentType.integer(1, 100))
                                .executes(e -> taxPlayerCommand(e, IntegerArgumentType.getInteger(e, "%"), EntityArgument.getPlayer(e, "player")))
                        )
                );
    }

    public static int taxAllCommand(CommandContext<CommandSourceStack> ctx, int percent, int taxlevel) throws CommandSyntaxException {
         if (percent == -1)
             percent = DiamondEconomyConfig.getInstance().defaulTaxPercentage;

         DatabaseManager dm = DiamondUtils.getDatabaseManager();
         int playersTaxed = dm.taxAll(percent, taxlevel);

         if (playersTaxed == -1) {
             ctx.getSource().sendFailure(
                     Component.empty()
                             .append(DiamondEconomyConfig.ChatPrefix())
                             .append("Error while trying to tax all players for " + percent + "% that have min. taxtevel " + taxlevel)
             );
         } else {
             final int finalPercent = percent;
             ctx.getSource().sendSuccess(() ->
                     Component.empty()
                             .append(DiamondEconomyConfig.ChatPrefix())
                             .append("Taxed " + playersTaxed + " players for " + finalPercent + "%")
             , true);
         }

         return playersTaxed;
    }

    public static int taxPlayerCommand(CommandContext<CommandSourceStack> ctx, int percent, ServerPlayer player) throws CommandSyntaxException {
        if (player == null)
            throw CommandExceptions.PLAYER_NOT_FOUND.create();
        if (percent == -1)
             percent = DiamondEconomyConfig.getInstance().defaulTaxPercentage;

        boolean success = DiamondUtils.getDatabaseManager().taxPlayerUUID(player.getStringUUID(), percent);
        if (!success) {
             ctx.getSource().sendFailure(
                     Component.empty()
                             .append(DiamondEconomyConfig.ChatPrefix())
                             .append("Error while trying to tax player ")
                             .append(player.getDisplayName())
                             .append("for " + percent + "%")
             );
        } else {
             final int finalPercent = percent;
             ctx.getSource().sendSuccess(() -> Component.empty()
                             .append(DiamondEconomyConfig.ChatPrefix())
                             .append("Taxed ")
                             .append(player.getDisplayName())
                             .append(" for " + finalPercent + "%") // TODO output amount taxed ?
             , true);
        }
        return success ? 1 : 0;
    }

    public static int taxInfoCommand(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        if (player == null)
            throw CommandExceptions.PLAYER_NOT_FOUND.create();

        MutableComponent playerName;
        if (ctx.getSource().getPlayer() != null && ctx.getSource().getPlayer().equals(player)) {
            playerName = Component.literal("Your ");
        } else {
            playerName = Component.empty().append(player.getDisplayName()).append("'s ");
        }

        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        final int taxlevel = dm.getTaxlevel(player.getStringUUID());
        final int balance = dm.getBalanceFromUUID(player.getStringUUID());
        if (taxlevel < 0 || balance < 0) {
            ctx.getSource().sendFailure(
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("Could not load data for player ")
                            .append(playerName)
            );
            return -1;
        }

        int taxToPay = balance - taxlevel * DiamondEconomyConfig.getDefaultTax(balance);
        ctx.getSource().sendSuccess(() ->
                Component.empty()
                        .append(DiamondEconomyConfig.ChatPrefix())
                        .append(playerName)
                        .append("tax level is " + taxlevel)
                        .append(". The next tax will cost you ")
                        .append(DiamondEconomyConfig.currencyToLiteral(balance))
                        .append(" (" + DiamondEconomyConfig.getInstance().defaulTaxPercentage + "%)")
        , false);
        return 1;
    }
}