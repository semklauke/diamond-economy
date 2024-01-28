package com.gmail.sneakdevs.diamondeconomy.command;

import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandBuildContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.function.Consumer;

public class DiamondEconomyCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        // Determine register function (register command with mod prefix ?)
        Consumer<LiteralArgumentBuilder<CommandSourceStack>> registerFunc;
        if (DiamondEconomyConfig.getInstance().commandName == null) {
            registerFunc = dispatcher::register;
        } else {
            registerFunc = (builder) -> dispatcher.register(Commands.literal(DiamondEconomyConfig.getInstance().commandName).then(builder));
        }

        if (DiamondEconomyConfig.getInstance().modifyCommandName != null) {
            registerFunc.accept(ModifyCommand.buildCommand());
        }
        if (DiamondEconomyConfig.getInstance().balanceCommandName != null) {
            registerFunc.accept(BalanceCommand.buildCommand());
        }
        if (DiamondEconomyConfig.getInstance().topCommandName != null) {
            registerFunc.accept(TopCommand.buildCommand());
        }
        if (DiamondEconomyConfig.getInstance().depositCommandName != null) {
            registerFunc.accept(DepositCommand.buildCommand());
        }
        if (DiamondEconomyConfig.getInstance().sendCommandName != null) {
            registerFunc.accept(SendCommand.buildCommand());
        }
        if (DiamondEconomyConfig.getInstance().setCommandName != null) {
            registerFunc.accept(SetCommand.buildCommand());
        }
        if (DiamondEconomyConfig.getInstance().withdrawCommandName != null) {
            registerFunc.accept(WithdrawCommand.buildCommand(context));
        }
        if (DiamondEconomyConfig.getInstance().taxCommandName != null) {
            registerFunc.accept(TaxCommand.buildCommand());
        }
    }
}