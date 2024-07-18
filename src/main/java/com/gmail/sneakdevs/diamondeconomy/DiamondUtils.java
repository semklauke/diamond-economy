package com.gmail.sneakdevs.diamondeconomy;

import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.gmail.sneakdevs.diamondeconomy.sql.SQLiteDatabaseManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DiamondUtils {
    public static void registerTable(String query){
        DiamondEconomy.tableRegistry.add(query);
    }

    public static DatabaseManager getDatabaseManager() {
        return new SQLiteDatabaseManager();
    }

    public static int dropItem(int amount, ServerPlayer player) {


        if (DiamondEconomyConfig.getInstance().greedyWithdraw) {

            for (int i = DiamondEconomyConfig.getCurrencyValues().length - 1; i >= 0 && amount > 0; i--) {

                int val = DiamondEconomyConfig.getCurrencyValues()[i];
                int currSize = DiamondEconomyConfig.getCurrency(i).getDefaultMaxStackSize();
                Item curr = DiamondEconomyConfig.getCurrency(i);

                while (amount >= val * currSize) {
                    ItemEntity itemEntity = player.drop(new ItemStack(curr, currSize), true);
                    itemEntity.setNoPickUpDelay();
                    amount -= val * currSize;
                }

                if (amount >= val) {
                    ItemEntity itemEntity = player.drop(new ItemStack(curr, amount / val), true);
                    itemEntity.setNoPickUpDelay();
                    amount -= amount / val * val;
                }

            }

        } else {

            int val = DiamondEconomyConfig.getCurrencyValues()[0];
            int currSize = DiamondEconomyConfig.getCurrency(0).getDefaultMaxStackSize();
            Item curr = DiamondEconomyConfig.getCurrency(0);

            while (amount >= val * currSize) {
                ItemEntity itemEntity = player.drop(new ItemStack(curr, currSize), true);
                itemEntity.setNoPickUpDelay();
                amount -= val * currSize;
            }

            if (amount >= val) {
                ItemEntity itemEntity = player.drop(new ItemStack(curr, amount / val), true);
                itemEntity.setNoPickUpDelay();
                amount -= amount / val * val;
            }
        }

        DatabaseManager dm = getDatabaseManager();
        dm.changeBalance(player.getStringUUID(), amount);

        return amount;
    }

    // returns remaining items, that didn't fit the inventory
    public static int giveItem(Item item, int itemAmount, ServerPlayer player) {
        int stackSize = item.getDefaultMaxStackSize();

        while (itemAmount > 0) {
            int itemOut = Math.min(itemAmount, stackSize);
            player.getInventory().placeItemBackInInventory(new ItemStack(item, itemOut));
            itemAmount -= itemOut;
        }
        // TODO:
        /* Atm, this always returns 0, since placeItemBackInInventory just drops the items that
         *  don't fit in the ivv. But keeps this if we find a better solution the future.
         */
        return itemAmount;
    }
}
