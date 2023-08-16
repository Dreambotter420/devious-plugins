package net.unethicalite.scripts.kebabs;

import net.runelite.api.ItemID;

public class Jewelry {
    public final static int[] ringIds = new int[]{
            ItemID.RING_OF_DUELING2,
            ItemID.RING_OF_DUELING3,
            ItemID.RING_OF_DUELING4,
            ItemID.RING_OF_DUELING5,
            ItemID.RING_OF_DUELING6,
            ItemID.RING_OF_DUELING7,
            ItemID.RING_OF_DUELING8
    };
    public final static int[] allRingIds = new int[]{
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
