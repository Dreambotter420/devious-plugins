package net.runelite.client.plugins.nightmare.util;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.unethicalite.api.commons.Rand;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
public class API {
    public static long lastDrinkTick = -2;
    public static long ticks = 0;
    public static int bankedKebabs = 0;

    public static Predicate<Item> energyPotFilter = i ->
            i.getId() == ItemID.ENERGY_POTION4 ||
                    i.getId() == ItemID.ENERGY_POTION2 ||
                    i.getId() == ItemID.ENERGY_POTION3 ||
                    i.getId() == ItemID.ENERGY_POTION1;
    public static void checkDrinkEnergyPot() {
        Item vial = Inventory.getFirst(ItemID.VIAL);
        if (vial != null) {
            vial.interact("Drop");
            API.shortSleep();
        }
        if ((ticks - lastDrinkTick) >= 3 && Movement.getRunEnergy() <= 80) {
            Item energy = Inventory.getFirst(energyPotFilter);
            if (energy != null) {
                energy.interact("Drink");
                API.shortSleep();
                lastDrinkTick = ticks;
            }
        }
    }
    public static void shortSleep() {
        Time.sleep(50,250);
    }
    public static int shortReturn() {
        return Rand.nextInt(50,250);
    }
    public static boolean checkToggleRun() {
        if (!Movement.isRunEnabled() && Movement.getRunEnergy() >= 5) {
            shortSleep();
            Movement.toggleRun();
            return true;
        }
        return false;
    }

    public static boolean waitWalking() {
        checkToggleRun();
        checkDrinkEnergyPot();
        return Movement.isWalking();
    }
    public static int clickLocalBank() {
        return clickBank(BankLocation.getNearest());
    }
    public static int clickBank(BankLocation location) {
        log.info("ClickBank");
        if (waitWalking()) {
            return API.shortReturn();
        }
        TileObject booth = TileObjects.getNearest(b -> b.hasAction("Collect") && b.hasAction("Bank") && location.getArea().contains(b));
        NPC banker = null;
        if (booth == null) {
            banker = NPCs.getNearest(b -> b.hasAction("Collect") && b.hasAction("Bank")  && location.getArea().contains(b));
        }
        if (booth == null && banker == null) {
            booth = TileObjects.getNearest(b -> b.hasAction("Collect") && b.hasAction("Bank") && Players.getLocal().distanceTo(b) < 20);
        }
        if ((booth == null && banker == null) || booth.distanceTo(Players.getLocal()) > 20 || !Reachable.isInteractable(booth)) {
            log.info("Walking towards bank");
            Movement.walkTo(BankLocation.getNearest());
            return API.shortReturn();
        }
        if (booth != null) {
            log.info("Clicking Booth");
            booth.interact("Bank");
        } else {
            log.info("Clicking Banker");
            banker.interact("Bank");
        }
        return API.shortReturn();
    }
    public static void depositAllExcept(int... ids) {
        List<Integer> itemsDeposited = new ArrayList<>();
        for (Item i : Inventory.getAll(i -> i != null && i.getName() != null && i.getId() > 0 && !i.getName().equalsIgnoreCase("null") && !Arrays.stream(ids).anyMatch(exceptId -> exceptId == i.getId()))) {
            if (i == null || itemsDeposited.stream().anyMatch(i2 -> i2.intValue() == i.getId())) continue;
            itemsDeposited.add(i.getId());
            log.info("deposit some other items: "+ i.getName());
            Bank.depositAllExcept(ids);
            API.shortSleep();
        }
    }
}
