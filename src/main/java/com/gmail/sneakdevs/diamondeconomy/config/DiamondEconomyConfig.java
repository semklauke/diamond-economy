package com.gmail.sneakdevs.diamondeconomy.config;

import com.gmail.sneakdevs.diamondeconomy.DiamondEconomy;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

@Config(name = DiamondEconomy.MODID)
public class DiamondEconomyConfig implements ConfigData {

    @Comment("List of items used as currency")
    public String[] currencies = {"minecraft:diamond","minecraft:diamond_block"};

    @Comment("Values of each currency in the same order, decimals not allowed (must be in ascending order unless greedyWithdraw is disabled)")
    public int[] currencyValues = {1,9};

    @Comment("Where the diamondeconomy.sqlite file is located (ex: \"C:/Users/example/Desktop/server/world/diamondeconomy.sqlite\")")
    public String fileLocation = null;

    @Comment("Name of the base command (null to disable base command)")
    public String commandName = "diamonds";

    @Comment("Names of the subcommands (null to disable command)")
    public String topCommandName = "top";
    public String balanceCommandName = "balance";
    public String depositCommandName = "deposit";
    public String sendCommandName = "send";
    public String withdrawCommandName = "withdraw";
    public String taxCommandName = "tax";

    @Comment("Names of the admin subcommands (null to disable command)")
    public String setCommandName = "set";
    public String modifyCommandName = "modify";

    @Comment("Try to withdraw items using the most high value items possible (ex. diamond blocks then diamonds) \n If disabled withdraw will give player the first item in the list")
    public boolean greedyWithdraw = true;

    @Comment("Money the player starts with when they first join the server")
    public int startingMoney = 0;

    @Comment("How often to add money to each player, in seconds (0 to disable)")
    public int moneyAddTimer = 0;

    @Comment("Amount of money to add each cycle")
    public int moneyAddAmount = 0;

    @Comment("Symbol for the currency used in chat. You can specify a prefix and suffix")
    public String currencySymbolPrefix = "$";
    public String currencySymbolSuffix = "";

    @Comment("Color of currency amount in chat. Leave blank for no color (hex value starting with # or a color name)")
    public String currencyColor = "#AAAAAA";

    @Comment("Prefix for chat messages from this mod")
    public String chatPrefix = "[💰]";

    @Comment("Color of currency amount in chat. Leave blank for no color (hex value starting with # or a color name)")
    public String chatPrefixColor = "#306844";

    @Comment("Default percentage for the tax command (can e.g. be scheduled by a cronjob)")
    public int defaulTaxPercentage = 3;
    @Comment("Permission level (1-4) of the op commands in diamond economy. Set to 2 to allow command blocks to use these commands.")
    public int opCommandsPermissionLevel = 4;

    public void validatePostLoad() throws ValidationException {
        // TODO
    }

    public Style getCurrencyStyle() {
        TextColor color = DiamondEconomyConfig.parseConfigColor(this.currencyColor);
        if (color != null)
            return Style.EMPTY.withColor(color);
        return Style.EMPTY;
    }

    public Style getPrefixStyle() {
        TextColor color = DiamondEconomyConfig.parseConfigColor(this.chatPrefixColor);
        if (color != null)
            return Style.EMPTY.withColor(color);
        return Style.EMPTY;
    }

    private static TextColor parseConfigColor(String in) {
        if (in.isEmpty() || in.isBlank())
            return null;
        if (in.charAt(0) == '#') {
            // hex
            return TextColor.fromRgb(Integer.parseInt(in.substring(1), 16));
        } else {
            // by name
            return TextColor.parseColor(in).getOrThrow();
        }
    }

    public static MutableComponent ChatPrefix() {
        DiamondEconomyConfig inst = DiamondEconomyConfig.getInstance();
        return Component.literal(inst.chatPrefix + " ").withStyle(inst.getPrefixStyle());
    }

    public static MutableComponent currencyToLiteral(int c) {
        DiamondEconomyConfig inst = DiamondEconomyConfig.getInstance();
        String currencyStr = inst.currencySymbolPrefix + c + inst.currencySymbolSuffix;
        return Component.literal(currencyStr).setStyle(inst.getCurrencyStyle());
    }

    public static String currencyToString(int c) {
        return currencyToLiteral(c).getString();
    }

    public static int getDefaultTax(int balance) {
        return (int) Math.round(DiamondEconomyConfig.getInstance().defaulTaxPercentage * 0.01 * balance);
    }
    public static Item getCurrency(int num) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(DiamondEconomyConfig.getInstance().currencies[num]));
    }

    public static String getCurrencyName(int num) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(DiamondEconomyConfig.getInstance().currencies[num])).getDescription().getString();
    }

    public static int[] getCurrencyValues() {
        return DiamondEconomyConfig.getInstance().currencyValues;
    }

    public static DiamondEconomyConfig getInstance() {
        return AutoConfig.getConfigHolder(DiamondEconomyConfig.class).getConfig();
    }
}