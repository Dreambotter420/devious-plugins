package net.runelite.client.plugins.nightmare.util.loadouts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.mixins.Inject;
import net.runelite.client.plugins.nightmare.util.API;
import net.runelite.client.plugins.nightmare.util.OwnedItems;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;

import java.time.Instant;
import java.util.*;

@Slf4j
public class EquipmentLoadout {
    private List<LoadoutItem> equipmentItems;

    private static Queue<Pair> equipmentQueue = new LinkedList<>();

    public EquipmentLoadout(LoadoutItem... loadoutItems) {
        this.equipmentItems = new ArrayList<>();
        for (LoadoutItem i : loadoutItems) {
            this.equipmentItems.add(i);
        }
    }

    public void addItem(LoadoutItem loadoutItem) {
        equipmentItems.add(loadoutItem);
    }

    public List<LoadoutItem> getExtraItems() {
        log.info("getExtraItems Equipment");
        List<LoadoutItem> extraItems = new ArrayList<>();

        for (Item current : Equipment.getAll()) {
            boolean isIdFoundInDesired = false;
            int desiredQty = 0;
            for (LoadoutItem desired : equipmentItems) {
                if (current.getId() == desired.getId()) {
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

    public boolean fulfilled() {
        // check extranneous items
        List<LoadoutItem> extraEquipmentItems = getExtraItems();
        if (!extraEquipmentItems.isEmpty()) {
            log.info("Have extra items in Equipment:");
            extraEquipmentItems.forEach(
                    l -> log.info("Name: " + l.getId() + ", qty: " + l.getItemQty()));
            return false;
        }

        // check missing items
        for (LoadoutItem item : equipmentItems) {
            if (!OwnedItems.contains(item.getId())) {
                log.info("Do not have item: " + item.getId() + " owned");
                return false;
            }
            int currentQuantity = Equipment.getCount(true, item.getId());
            int neededQuantity = item.getItemQty() - currentQuantity;
            if (neededQuantity > 0) {
                return false;
            }
        }
        log.info("exactly fulfilled equipment");
        // exactly fulfilled
        return true;
    }

    public boolean fulfill() {
        log.info("equipment fulfill");
        if (!Bank.isOpen()) {
            Time.sleep(API.clickLocalBank());
            return false;
        }
        // step 1: empty equipment
        // diff check of LoadoutItems parameter and given Equipment before depositing all
        List<LoadoutItem> extraEquipmentItems = getExtraItems();
        if (!extraEquipmentItems.isEmpty()) {
            log.info("Found extra equipment, depositing all equipment");
            Bank.depositEquipment();
            API.shortSleep();
            return false;
        }

        // step 2: withdraw equipment as inventory loadout
        InventoryLoadout loadoutUnnoted = new InventoryLoadout();
        for (LoadoutItem loadoutItem : equipmentItems) {
            if (Equipment.getCount(true, loadoutItem.getId()) < loadoutItem.getItemQty()) {
                loadoutUnnoted.addItem(
                        new LoadoutItem(loadoutItem.getId(), loadoutItem.getItemQty() - Equipment.getCount(true, loadoutItem.getId())));
            }
        }
        if (loadoutUnnoted.fulfill()) {
            long startTick = API.ticks;
            boolean equipped = false;
            for (int i = 0; i < 10; i++) {
                if(haveItemToEquip()) {
                    if (API.ticks != startTick) {
                        log.info("tick drifted");
                        return false;
                    }
                    equipped = true;
                    equipItemInQueue();
                } else {
                    break;
                }
            }
            if (equipped) {
                log.info("equipped some items");
                return false;
            }
            boolean wearingAllEquipment = true;
            for (LoadoutItem item : equipmentItems) {
                Item invyItem = Bank.Inventory.getFirst(item.getId());
                if (invyItem == null
                        || invyItem.getId() == -1
                        || invyItem.getName() == null
                        || invyItem.getName().equalsIgnoreCase("null")) {
                    continue;
                }

                String[] actions = new String[]{"Wear", "Wield", "Equip"};
                String action =
                        Arrays.stream(invyItem.getActions())
                                .filter(a -> Arrays.stream(actions).anyMatch(i -> i.equals(a)))
                                .findFirst()
                                .orElse(null);
                if (action != null) {
                    equipmentQueue.add(new Pair(item.getId(), action));
                    wearingAllEquipment = false;

                }
            }
            if (wearingAllEquipment) {
                log.info(
                        "Fulfilled equipment - have no extra equipment and no equipment left in"
                                + " inventory!");
                return true;
            }
        }
        return false;
    }

    public boolean haveItemToEquip() {
        return equipmentQueue.peek() != null;
    }

    public void equipItemInQueue() {
        Pair tmp = equipmentQueue.poll();
        // Use the item ID and quantity from the popped Pair
        int itemID = tmp.id;
        String action = tmp.action;
        Item item = Bank.Inventory.getFirst(itemID);
        if (item == null) {
            log.info("Not found item in inventory after withdrawing :o");
            return;
        }
        API.shortSleep();
        item.interact(action);
    }
}
