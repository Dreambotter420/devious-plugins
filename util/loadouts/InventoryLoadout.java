package net.runelite.client.plugins.nightmare.util.loadouts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.client.plugins.nightmare.NightmarePlugin;
import net.runelite.client.plugins.nightmare.util.API;
import net.runelite.client.plugins.nightmare.util.OwnedItems;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;

@Slf4j
public class InventoryLoadout {
    private List<LoadoutItem> items;
    private static Queue<LoadoutItem> itemQueue = new LinkedList<>();

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

        return true;
    }
    public boolean fulfill() {
        log.info("invy fulfill");
        if (!Bank.isOpen()) {
            Time.sleep(API.clickLocalBank());
            return false;
        }
        List<LoadoutItem> extraItems = getExtraItems();
        if (!extraItems.isEmpty()) {
            log.info("Found extra items, depositing all inventory");
            Bank.depositInventory();
            return false;
        }
        long startTick = API.ticks;
        boolean withdrew = false;
        for (int i = 0; i < 10; i++) {
            if(haveItemToWithdraw()) {
                if (API.ticks != startTick) {
                    log.info("tick drift");
                    return false;
                }
                log.info("have item to withdraw in queue");
                withdrew = true;
                withdrawItemInQueue();
            } else {
                break;
            }
        }
        if (withdrew) {
            log.info("withdraw some items");
            return false;
        }
        boolean neededSomething = false;
        for (LoadoutItem item : items) {
            int currentQuantity = Bank.Inventory.getCount(item.getId());
            int neededQuantity = item.getItemQty() - currentQuantity;
            if (neededQuantity > 0) {
                if (Bank.getCount(true, item.getId()) <= 0 && currentQuantity <= 0) {
                    log.info("Not have any of item in bank or inventory: " +item.getId());
                    return false;
                }
                neededSomething = true;
                log.info("add invy item to queue: "+item.getId() + ", "+item.getItemQty());
                if (item.isNoted()) {
                    itemQueue.add(new LoadoutItem(item.getId(), neededQuantity, true, item.getNotedId()));
                } else {
                    itemQueue.add(new LoadoutItem(item.getId(), neededQuantity));
                }
            }
        }
        if (!neededSomething) {
            log.info("InventoryLoadout.fulfill() has nothing missing and nothing extra from loadout, fulfilled inventory!");
            return true;
        }
        log.info("invy fulfill return false");
        return false;
    }
    public List<LoadoutItem> getExtraItems() {
        log.info("getExtraItems Inventory");
        List<LoadoutItem> extraItems = new ArrayList<>();

        for (Item current : Bank.Inventory.getAll()) {
            boolean isIdFoundInDesired = false;
            int desiredQty = 0;
            for (LoadoutItem desired : items) {
                int compareId = (desired.isNoted() ? desired.getNotedId() : desired.getId());
                if (current.getId() == compareId) {
                    desiredQty = desired.getItemQty();
                    isIdFoundInDesired = true;
                    break;
                }
            }
            if (!isIdFoundInDesired || current.getQuantity() > desiredQty) {
                int extraQty = current.getQuantity() - (isIdFoundInDesired ? desiredQty : 0);
                log.info("Found extra item: "+current.getId()+ " in extra qty: "+ extraQty);
                extraItems.add(new LoadoutItem(current.getId(), extraQty, current.isNoted(), current.getNotedId()));
            }
        }
        return extraItems;
    }
    public boolean haveItemToWithdraw() {
        return itemQueue.peek() != null;
    }
    public void withdrawItemInQueue() {
        log.info("withdraw item in queue");
        LoadoutItem tmp = itemQueue.poll();
        // Use the item ID and quantity from the popped Pair
        int itemID = tmp.getId();
        int quantity = tmp.getItemQty();

        if (Bank.getCount(true, itemID) <= 0) {
            log.info("Missing itemID from bank in queue: "+itemID);
            return;
        }
        // Set noted mode
        Bank.WithdrawMode withdrawMode = (tmp.isNoted() ? Bank.WithdrawMode.NOTED : Bank.WithdrawMode.ITEM);
        API.shortSleep();
        Bank.withdraw(itemID, quantity, withdrawMode);
    }
}
