package net.runelite.client.plugins.nightmare.util.loadouts;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.client.plugins.nightmare.util.API;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;

import java.util.List;
import java.util.Set;

@Slf4j
public class Switching {
    private static boolean switching = false;
    public static boolean equipWait(int... ids) {
        boolean lagged = equip(ids);
        Time.sleepTick();
        return lagged;
    }
    public static boolean equipWait(List<Integer> ids) {
        boolean lagged = equip(ids);
        if (!lagged) Time.sleepTick();
        return lagged;
    }
    public static boolean equip(int... ids) {
        if (switching) {
            return false;
        }
        long startSwitchTick = API.ticks;
        switching = true;
        for (int id : ids) {
            if (API.ticks > startSwitchTick) {
                log.info("Tick timeout for switching! For safety of course");
                switching = false;
                return false;
            }
            log.debug("[SWITCHER] check ID: "+id);
            if (Equipment.contains(id)) {
                log.debug("[SWITCHER] PASS: EQUIPPED");
                continue;
            }
            Item toEquip = Inventory.getFirst(id);
            if (toEquip != null && Inventory.contains(id)) {
                String action = (toEquip.hasAction("Wear") ? "Wear" : "Wield");
                log.debug("[SWITCHER] PASS with action: "+action);
                toEquip.interact(action);
                API.sleepClientTick();
                continue;
            }
            log.debug("[SWITCHER] FAIL");
        }
        switching = false;
        return true;
    }
    public static boolean equip(List<Integer> ids) {
        if (switching) {
            return false;
        }
        long startSwitchTick = API.ticks;
        switching = true;
        for (int id : ids) {
            if (API.ticks > startSwitchTick) {
                log.info("Tick timeout for switching! For safety of course");
                switching = false;
                return false;
            }
            log.debug("[SWITCHER] check ID: "+id);
            if (Equipment.contains(id)) {
                log.debug("[SWITCHER] PASS: EQUIPPED");
                continue;
            }
            Item toEquip = Inventory.getFirst(id);
            if (toEquip != null && Inventory.contains(id)) {
                String action = (toEquip.hasAction("Wear") ? "Wear" : "Wield");
                log.debug("[SWITCHER] PASS with action: "+action);
                toEquip.interact(action);
                API.sleepClientTick();
                continue;
            }
            log.debug("[SWITCHER] FAIL");
        }
        switching = false;
        return true;
    }
}
