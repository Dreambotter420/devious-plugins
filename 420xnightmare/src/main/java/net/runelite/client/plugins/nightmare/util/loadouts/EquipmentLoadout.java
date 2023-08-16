package net.runelite.client.plugins.nightmare.util.loadouts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.client.plugins.nightmare.NightmarePlugin;
import net.runelite.client.plugins.nightmare.util.API;
import net.runelite.client.plugins.nightmare.util.Log;
import net.runelite.client.plugins.nightmare.util.OwnedItems;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;

import java.util.*;

@Slf4j
public class EquipmentLoadout {
    private List<LoadoutItem> equipmentItems;

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
        log.debug("getExtraItems Equipment");
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
            if (!isIdFoundInDesired) {
                int extraQty = current.getQuantity() - (isIdFoundInDesired ? desiredQty : 0);
                log.debug("Found extra item: "+current.getId()+ " in extra qty: "+ extraQty);
                extraItems.add(new LoadoutItem(current.getId(), extraQty, current.isNoted(), current.getNotedId()));
            }
        }
        return extraItems;
    }

    public boolean fulfilled() {
        // check extranneous items
        List<LoadoutItem> extraEquipmentItems = getExtraItems();
        if (!extraEquipmentItems.isEmpty()) {
            log.debug("Have extra items in Equipment:");
            extraEquipmentItems.forEach(
                    l -> log.debug("Name: " + l.getId() + ", qty: " + l.getItemQty()));
            return false;
        }

        // check missing items
        for (LoadoutItem item : equipmentItems) {
            if (!OwnedItems.contains(item.getId())) {
                log.debug("Do not have item: " + item.getId() + " owned");
                return false;
            }
            int currentQuantity = Equipment.getCount(true, item.getId());
            int neededQuantity = item.getItemQty() - currentQuantity;
            if (neededQuantity > 0) {
                return false;
            }
        }
        log.debug("exactly fulfilled equipment");
        // exactly fulfilled
        return true;
    }
    /**
     * Fulfills equipmentloadout by opening bank, performing an inventory fulfillment on the equipmentitems, then equipping them
     */
    public boolean fulfill() {
        log.debug("equipment fulfill");
        if (!Bank.isOpen()) {
            Time.sleep(API.clickLocalBank());
            return true;
        }
        // step 1: empty equipment
        // diff check of LoadoutItems parameter and given Equipment before depositing all
        List<LoadoutItem> extraEquipmentItems = getExtraItems();
        if (!extraEquipmentItems.isEmpty()) {
            log.debug("Found extra equipment, depositing all equipment");
            Bank.depositEquipment();
            API.shortSleep();
            return true;
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
            boolean wearingAllEquipment = true;
            Set<Integer> addedItemIds = new HashSet<>();
            for (LoadoutItem item : equipmentItems) {
                Item invyItem = Bank.Inventory.getFirst(item.getId());
                if (invyItem == null
                        || invyItem.getName() == null || invyItem.getName().equalsIgnoreCase("null")) {
                    continue;
                }

                String action = (invyItem.hasAction("Wear") ? "Wear" : (invyItem.hasAction("Wield") ? "Wield" : (invyItem.hasAction("Equip") ? "Equip" : null)));
                if (action == null) {
                    log.info("Found equipment item with no wear or wield or equip option: " +invyItem.getName());
                    continue;
                }

                if (!addedItemIds.contains(item.getId())) {
                    API.equipItem(item.getId());
                    addedItemIds.add(item.getId());
                    wearingAllEquipment = false;
                    API.sleepClientTick();
                }
            }
            if (wearingAllEquipment) {
                log.debug(
                        "Fulfilled equipment - have no extra equipment and no equipment left in inventory!");
                return true;
            }
            Time.sleepTick();
            return true;
        }
        return false;
    }

}
