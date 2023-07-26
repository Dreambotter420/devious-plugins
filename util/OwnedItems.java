package net.runelite.client.plugins.nightmare.util;

import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;

public class OwnedItems {
    public static boolean contains(int id) {
        if (Bank.isOpen()) {
            return Bank.Inventory.getAll().contains(id) || BankCache.contains(id) || Equipment.contains(id);
        } else {
            return Inventory.contains(id) || BankCache.contains(id) || Equipment.contains(id);
        }
    }
    public static int getCount(int id) {
        return Inventory.getCount(id) + BankCache.getCount(id) + Equipment.getCount(id);
    }
}
