package com.gmail.sneakdevs.diamondeconomy.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.network.chat.Component;
public class CommandExceptions {
    public static final DynamicCommandExceptionType INVENTORY_ERROR = new DynamicCommandExceptionType(
            o -> Component.literal((String) o)
    );

    public static final DynamicCommandExceptionType BALANCE_ERROR = new DynamicCommandExceptionType(
            o -> Component.literal((String) o)
    );
}
