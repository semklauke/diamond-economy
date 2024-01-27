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
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.authlib.GameProfile;

import java.util.Collection;

public class SetCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal(DiamondEconomyConfig.getInstance().setCommandName)
                .requires((permission) -> permission.hasPermission(DiamondEconomyConfig.getInstance().opCommandsPermissionLevel))
                .then(
                        Commands.argument("players", GameProfileArgument.gameProfile())
                                .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(e -> {
                                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                                    return setCommand(e, GameProfileArgument.getGameProfiles(e, "players"), amount);
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

    public static int setCommand(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> players, int amount) throws CommandSyntaxException {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();
        int successCount = 0;
        for (GameProfile player : players) {
            ServerPlayer serverPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(player.getName());
            String playerUUID;
            if (serverPlayer != null)
                playerUUID = serverPlayer.getStringUUID();
            else
                playerUUID = dm.getUUIDFromName(player.getName());

            if (playerUUID == null) {
                // Player does not exist on server or database
                ctx.getSource().sendFailure(Component.literal("Player '" + player.getName() + "' found."));
                continue;
            }

            Component playerName = serverPlayer != null ? serverPlayer.getDisplayName() : Component.literal(player.getName());
            if (dm.setBalance(playerUUID, amount)) {
                if (players.size() == 1) {
                    ctx.getSource().sendSuccess(() ->
                            Component.empty()
                                    .append(DiamondEconomyConfig.ChatPrefix())
                                    .append("Set balance of ")
                                    .append(playerName)
                                    .append(" to ")
                                    .append(DiamondEconomyConfig.currencyToLiteral(amount))
                    , true);
                }
                successCount++;
            } else {
                ctx.getSource().sendFailure(
                        Component.empty()
                                .append(DiamondEconomyConfig.ChatPrefix())
                                .append("For ")
                                .append(playerName)
                                .append(" the balance limit was exceeded. No changes.")
               );
            }
        }

        if (successCount > 1) {
            int finalSuccessCount = successCount;
            ctx.getSource().sendSuccess(() ->
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("Set balance of ")
                            .append(finalSuccessCount + "(from " + players.size() + ") accounts to ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
            , true);
        }
        return successCount;
    }

    public static int setCommand(CommandContext<CommandSourceStack> ctx, int amount, boolean shouldModifyAll) throws CommandSyntaxException {
        if (shouldModifyAll) {
            DiamondUtils.getDatabaseManager().setAllBalance(amount);
            ctx.getSource().sendSuccess(() ->
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("All accounts balance to ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
            , true);
        } else {
            DiamondUtils.getDatabaseManager().setBalance(ctx.getSource().getPlayerOrException().getStringUUID(), amount);
            ctx.getSource().sendSuccess(() ->
                    Component.empty()
                            .append(DiamondEconomyConfig.ChatPrefix())
                            .append("Updated your balance to ")
                            .append(DiamondEconomyConfig.currencyToLiteral(amount))
            , true);
        }
        return 1;
    }
}
