package net.runelite.client.plugins.nightmare;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
public class LootTracker {

    // Map to store the previous state of each item container
    private static final Map<Integer, Map<Integer, Integer>> previousItemContainers = new HashMap<>();

    public static Map<Integer, Integer> onItemContainerChanged(ItemContainerChanged event) {
        if (event == null) {
            log.debug("clearing itemcontainer initially");
            previousItemContainers.clear();
            //handle initial population of inventory
            Map<Integer, Integer> currentContainerState = new HashMap<>();
            // Iterate through the current items and compare them with the previous state
            List<Item> items = (Bank.isOpen() ? Bank.Inventory.getAll() : Inventory.getAll());
            for (Item item : items) {
                if (item != null) {
                    int itemId = item.getId();
                    int currentQuantity = item.getQuantity();
                    // Sum quantities for items with the same ID
                    currentContainerState.put(itemId, currentContainerState.getOrDefault(itemId, 0) + currentQuantity);
                }
            }

            // Put initial state of our inventory
            previousItemContainers.put(InventoryID.INVENTORY.getId(), currentContainerState);
            return null;
        }
        Map<Integer, Integer> differences = new HashMap<>();

        // Get the current state of the item container
        ItemContainer itemContainer = event.getItemContainer();
        int containerId = event.getContainerId();

        if (containerId != InventoryID.INVENTORY.getId()) {
            return differences; // Return empty list if it's not the inventory
        }

        // Create a map to store the current state of the container, summing quantities for non-stackable items
        Map<Integer, Integer> currentContainerState = new HashMap<>();
        Item[] items = itemContainer.getItems();
        for (Item item : items) {
            if (item != null) {
                int itemId = item.getId();
                int currentQuantity = item.getQuantity();
                currentContainerState.put(itemId, currentContainerState.getOrDefault(itemId, 0) + currentQuantity);
            }
        }

        // Get the previous state of the container
        Map<Integer, Integer> previousContainer = previousItemContainers.getOrDefault(containerId, new HashMap<>());

        // Calculate the differences between the current and previous states
        for (Map.Entry<Integer, Integer> entry : currentContainerState.entrySet()) {
            int itemId = entry.getKey();
            int currentQuantity = entry.getValue();
            int previousQuantity = previousContainer.getOrDefault(itemId, 0);
            int difference = currentQuantity - previousQuantity;
            if (difference != 0) {
                differences.put(itemId, difference);
            }
        }

        // Check for items that were in the previous state but are now missing
        for (Map.Entry<Integer, Integer> entry : previousContainer.entrySet()) {
            int itemId = entry.getKey();
            if (!currentContainerState.containsKey(itemId)) {
                differences.put(itemId, -entry.getValue());
            }
        }

        // Update the previous state with the current state
        previousItemContainers.put(containerId, currentContainerState);

        return differences;
    }
}