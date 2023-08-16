package net.runelite.client.plugins.nightmare.util.loadouts;

import net.runelite.api.ItemID;
import net.runelite.client.plugins.nightmare.util.OwnedItems;

public class Jewelry {
    private final static int[] ringIds = new int[]{
            ItemID.RING_OF_DUELING1,
            ItemID.RING_OF_DUELING2,
            ItemID.RING_OF_DUELING3,
            ItemID.RING_OF_DUELING4,
            ItemID.RING_OF_DUELING5,
            ItemID.RING_OF_DUELING6,
            ItemID.RING_OF_DUELING7,
            ItemID.RING_OF_DUELING8
    };

    public static int getLeastDuelingChargeID() {
        for (int ringId : ringIds) {
            if (OwnedItems.contains(ringId)) {
                return ringId;
            }
        }
        return -1;
    }

}
