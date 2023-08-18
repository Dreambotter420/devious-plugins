package net.runelite.client.plugins.nightmare.util.loadouts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.client.plugins.nightmare.util.API;
import net.runelite.client.plugins.nightmare.util.OwnedItems;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.input.Mouse;
import net.unethicalite.api.items.Bank;

import java.util.*;

@Slf4j
public class InventoryLoadout {
    private List<LoadoutItem> items;
    public static Queue<LoadoutItem> itemQueue = new LinkedList<>();
    private static long withdrawWaitTick = 0;

    public boolean checkMatch(int id, int qty) {
        for (LoadoutItem item : itemQueue) {
            if (item.getId() == id && item.getItemQty() == qty) {
                return true;
            }
        }
        return false;
    }

    public InventoryLoadout(LoadoutItem... items) {
        this.items = new ArrayList<>(Arrays.asList(items));
    }

    public void addItem(LoadoutItem... loadoutItem) {
        items.addAll(Arrays.asList(loadoutItem));
    }

    public boolean fulfilled() {
        // check extra items
        List<LoadoutItem> extraEquipmentItems = getExtraItems();
        if (!extraEquipmentItems.isEmpty()) {
            return false;
        }

        // check missing items
        for (LoadoutItem item : items) {
            if (!OwnedItems.contains(item.getId())) {
                return false;
            }
            Item invyItem = Bank.Inventory.getFirst(item.getId());

            if (invyItem == null
                    || invyItem.getId() == -1
                    || invyItem.getName() == null
                    || invyItem.getName().equalsIgnoreCase("null")) {
                return false;
            }

            if (invyItem.isNoted() != item.isNoted()) {
                return false;
            }

            int currentQuantity = Bank.Inventory.getCount(true, item.getId());
            int neededQuantity = item.getItemQty() - currentQuantity;
            if (neededQuantity > 0) {
                return false;
            }
        }
        itemQueue.clear();
        return true;
    }
    public boolean fulfill() {
        if (API.ticks <= withdrawWaitTick) {
            return true;
        }
        log.debug("invy fulfill");
        if (!Bank.isOpen()) {
            Time.sleep(API.clickLocalBank());
            return true;
        }
        List<LoadoutItem> extraItems = getExtraItems();
        if (!extraItems.isEmpty()) {
            log.debug("Found extra items, depositing all extra inventory");
            for (LoadoutItem l : extraItems) {
                API.depositItem(l.getId(), l.getItemQty());
                API.shortSleep();
            }
            itemQueue.clear();
            return true;
        }
        /*boolean withdrew = false;
        for (int i = 0; i < 28; i++) {
            if(haveItemToWithdraw()) {
                log.debug("have item to withdraw in queue");
                withdrew = true;
                withdrawItemInQueue();
            } else {
                break;
            }
        }
        if (withdrew) {
            return true;
        }*/
        boolean neededSomething = false;
        long currentTick = API.ticks;
        int itemsWithdrawnThisTick = 0;
        for (LoadoutItem item : items) {
            int currentQuantity = Bank.Inventory.getCount(item.getId());
            int neededQuantity = item.getItemQty() - currentQuantity;
            if (neededQuantity > 0) {
                if (Bank.getCount(true, item.getId()) <= 0 && currentQuantity <= 0) {
                    log.debug("Not have any of item in bank or inventory: " +item.getId());
                    return false;
                }
                neededSomething = true;
                log.debug("add invy item to queue: "+item.getId() + ", "+item.getItemQty());
                if (itemsWithdrawnThisTick >= 10) {
                    Time.sleepTick();
                }
                API.bankWithdraw(item.getId(), neededQuantity, (item.isNoted() ? Bank.WithdrawMode.NOTED : Bank.WithdrawMode.ITEM));

                if (currentTick == API.ticks) {
                    itemsWithdrawnThisTick++;
                } else {
                    currentTick = API.ticks;
                    itemsWithdrawnThisTick = 0;
                }
                /*if (item.isNoted()) {
                    if (!checkMatch(item.getId(), neededQuantity)) {
                        itemQueue.add(new LoadoutItem(item.getId(), neededQuantity, true, item.getNotedId()));
                    }
                } else {
                    if (!checkMatch(item.getId(), neededQuantity)) {
                        itemQueue.add(new LoadoutItem(item.getId(), neededQuantity));
                    }
                }*/
            }
        }
        if (!neededSomething) {
            log.debug("InventoryLoadout.fulfill() has nothing missing and nothing extra from loadout, fulfilled inventory!");
            return true;
        }
        log.debug("invy fulfill return after needing something to withdraw");
        Time.sleepTick();
        return true;
    }
    public List<LoadoutItem> getExtraItems() {
        log.debug("getExtraItems Inventory");
        List<LoadoutItem> extraItems = new ArrayList<>();

        // Create a map to hold the total quantity of each item ID in the inventory
        Map<Integer, Integer> inventoryCounts = new HashMap<>();
        for (Item current : Bank.Inventory.getAll()) {
            inventoryCounts.merge(current.getId(), current.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Integer, Integer> entry : inventoryCounts.entrySet()) {
            boolean isIdFoundInDesired = false;
            int desiredQty = 0;
            for (LoadoutItem desired : items) {
                int compareId = (desired.isNoted() ? desired.getNotedId() : desired.getId());
                if (entry.getKey() == compareId) {
                    desiredQty = desired.getItemQty();
                    isIdFoundInDesired = true;
                    break;
                }
            }
            if (!isIdFoundInDesired || entry.getValue() > desiredQty) {
                int extraQty = entry.getValue() - (isIdFoundInDesired ? desiredQty : 0);
                log.debug("Found extra item: "+entry.getKey()+ " in extra qty: "+ extraQty);
                extraItems.add(new LoadoutItem(entry.getKey(), extraQty));
            }
        }
        return extraItems;
    }

}
