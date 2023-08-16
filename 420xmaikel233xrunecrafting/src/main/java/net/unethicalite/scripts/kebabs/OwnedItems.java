package net.unethicalite.scripts.kebabs;

import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;

public class OwnedItems {
    public static boolean contains(int id) {
        if (Bank.isOpen()) {
            BankCache.update();
            return Bank.Inventory.getFirst(id) != null || BankCache.containsAny(id) || Equipment.contains(id);
        } else {
            return Inventory.contains(id) || BankCache.containsAny(id) || Equipment.contains(id);
        }
    }
    public static boolean contains(int... ids) {

        if (Bank.isOpen()) {
            BankCache.update();
            return Bank.Inventory.getAll().contains(ids) || BankCache.containsAny(ids) || Equipment.contains(ids);
        } else {
            return Inventory.contains(ids) || BankCache.containsAny(ids) || Equipment.contains(ids);
        }
    }
    public static int getCount(int id) {
        return Inventory.getCount(id) + BankCache.getCount(id) + Equipment.getCount(id);
    }
}
