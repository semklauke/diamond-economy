package com.gmail.sneakdevs.diamondeconomy.command;

import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
public class CommandExceptions {
    public static final DynamicCommandExceptionType BALANCE_ERROR = new DynamicCommandExceptionType(
            o -> Component.literal((String) o)
    );

    public static final SimpleCommandExceptionType MAX_BALANCE_ERROR = new SimpleCommandExceptionType(
            Component.literal("You exceed the balance limit. The bank can't store that much money, sorry!")
    );
}
