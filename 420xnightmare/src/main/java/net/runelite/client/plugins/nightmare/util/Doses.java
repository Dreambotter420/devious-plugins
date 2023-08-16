package net.runelite.client.plugins.nightmare.util;

import net.runelite.api.ItemID;
import net.unethicalite.api.items.Inventory;

public class Doses {
    private final static int[] sanfewIds = new int[]{
            ItemID.SANFEW_SERUM1,
            ItemID.SANFEW_SERUM2,
            ItemID.SANFEW_SERUM3,
            ItemID.SANFEW_SERUM4
    };

    private final static int[] prayerPotIds = new int[]{
            ItemID.PRAYER_POTION1,
            ItemID.PRAYER_POTION2,
            ItemID.PRAYER_POTION3,
            ItemID.PRAYER_POTION4
    };
    private final static int[] superCombatIds = new int[]{
            ItemID.SUPER_COMBAT_POTION1,
            ItemID.SUPER_COMBAT_POTION2,
            ItemID.SUPER_COMBAT_POTION3,
            ItemID.SUPER_COMBAT_POTION4
    };
    private final static int[] divineSuperCombatIds = new int[]{
            ItemID.DIVINE_SUPER_COMBAT_POTION1,
            ItemID.DIVINE_SUPER_COMBAT_POTION2,
            ItemID.DIVINE_SUPER_COMBAT_POTION3,
            ItemID.DIVINE_SUPER_COMBAT_POTION4
    };

    public static int getLeastSanfew() {
        for (int id : sanfewIds) {
            if (Inventory.contains(id)) {
                return id;
            }
        }
        return -1;
    }
    public static int getLeastSuperCombat() {
        for (int id : superCombatIds) {
            if (Inventory.contains(id)) {
                return id;
            }
        }
        for (int id : divineSuperCombatIds) {
            if (Inventory.contains(id)) {
                return id;
            }
        }
        return -1;
    }
    public static int getLeastPrayerPot() {
        for (int id : prayerPotIds) {
            if (Inventory.contains(id)) {
                return id;
            }
        }
        for (int id : sanfewIds) {
            if (Inventory.contains(id)) {
                return id;
            }
        }
        return -1;
    }
    private final static int[] allDosesIds = new int[]{
            ItemID.SUPER_COMBAT_POTION1, ItemID.DIVINE_SUPER_COMBAT_POTION1, ItemID.PRAYER_POTION1, ItemID.SANFEW_SERUM1,
            ItemID.SUPER_COMBAT_POTION2, ItemID.DIVINE_SUPER_COMBAT_POTION2, ItemID.PRAYER_POTION2, ItemID.SANFEW_SERUM2,
            ItemID.SUPER_COMBAT_POTION3, ItemID.DIVINE_SUPER_COMBAT_POTION3, ItemID.PRAYER_POTION3, ItemID.SANFEW_SERUM3,
            ItemID.SUPER_COMBAT_POTION4, ItemID.DIVINE_SUPER_COMBAT_POTION4, ItemID.PRAYER_POTION4, ItemID.SANFEW_SERUM4
    };

    /**
     * Searches inventory for ID of least dose of all combat-related potions in fight.
     * @return least dose ID, or -1 if not found
     */
    public static int getLeastDose() {
        for (int id : allDosesIds) {
            if (Inventory.contains(id)) {
                return id;
            }
        }
        return -1;
    }
}
