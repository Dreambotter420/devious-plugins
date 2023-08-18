package net.runelite.client.plugins.nightmare.util;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.plugins.nightmare.NightmarePlugin;
import net.runelite.client.plugins.nightmare.util.loadouts.InventoryLoadout;
import net.runelite.client.plugins.nightmare.util.loadouts.Jewelry;
import net.unethicalite.api.commons.Rand;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.input.Keyboard;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;
import net.unethicalite.api.widgets.Dialog;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.client.Static;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
public class API {
    @Inject
    private ClientThread clientThread;
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
        Time.sleep(150,325);
    }
    public static void fastSleep() {
        Time.sleep(21,35);
    }
    public static int fastReturn() {
        return Rand.nextInt(21,35);
    }

    public static boolean waitClientTick = false;
    public static void sleepClientTick() {
        waitClientTick = true;
        Time.sleepUntil(() -> !waitClientTick,10, 600);
    }
    public static int returnTick() {
        Time.sleepTick();
        return fastReturn();
    }
    public static int shortReturn() {
        return Rand.nextInt(200,325);
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
        if (NightmarePlugin.NIGHTMARE_UNDERGROUND_WALK_ZONE.contains(Players.getLocal())) {
            if (Jewelry.getLeastDuelingChargeID() != -1) {
                int sleep = CombatStuff.teleportOut();
                Time.sleepTick();
                Time.sleep(sleep);
                return CombatStuff.teleportOut();
            }
        }
        return clickBank(BankLocation.getNearest());
    }
    public static int clickBank(BankLocation location) {
        log.info("ClickBank");
        if (waitWalking()) {
            return API.shortReturn();
        }
        TileObject booth = TileObjects.getNearest(b -> b.hasAction("Collect") && (b.hasAction("Bank") || b.hasAction("Use")) && location.getArea().contains(b));
        NPC banker = null;
        if (booth == null) {
            banker = NPCs.getNearest(b -> b.hasAction("Collect") && b.hasAction("Bank")  && location.getArea().contains(b));
        }
        if (booth == null && banker == null) {
            booth = TileObjects.getNearest(b -> b.hasAction("Collect") && b.hasAction("Bank") && Players.getLocal().distanceTo(b) < 20);
        }
        if ((booth == null && banker == null)) {
            log.info("Walking towards bank");
            Movement.walkTo(BankLocation.getNearest());
            return API.returnTick();
        }
        if (booth != null) {
            if (booth.distanceTo(Players.getLocal()) >= 20 || !Reachable.isInteractable(booth)) {
                log.info("Walking towards booth");
                Movement.walkTo(booth);
            } else {
                log.info("Clicking Booth");
                String action = (booth.hasAction("Bank") ? "Bank" : "Use");
                booth.interact(action);
            }
        } else {
            log.info("Clicking Banker");
            banker.interact("Bank");
        }
        return API.returnTick();
    }
    public static void depositAllExcept(int... ids) {
        List<Integer> itemsDeposited = new ArrayList<>();
        for (Item i : Inventory.getAll(i -> i != null && i.getName() != null && i.getId() > 0 && !i.getName().equalsIgnoreCase("null") && !Arrays.stream(ids).anyMatch(exceptId -> exceptId == i.getId()))) {
            if (i == null || itemsDeposited.stream().anyMatch(i2 -> i2.intValue() == i.getId())) continue;
            itemsDeposited.add(i.getId());
            log.info("deposit some other items: "+ i.getName());
            //call this method once per item bc it short returns after interacting one item
            Bank.depositAllExcept(ids);
            shortSleep();
        }
    }
    private static String getAction(Item item, int amount, Boolean withdraw) {
        String action = withdraw ? "Withdraw" : "Deposit";
        if (amount == 1) {
            action = action + "-1";
        } else if (amount == 5) {
            action = action + "-5";
        } else if (amount == 10) {
            action = action + "-10";
        } else if (withdraw && amount >= item.getQuantity()) {
            action = action + "-All";
        } else if (!withdraw && amount >= Bank.Inventory.getCount(true, item.getId())) {
            action = action + "-All";
        } else if (item.hasAction(new String[]{action + "-" + amount})) {
            action = action + "-" + amount;
        } else {
            action = action + "-X";
        }

        return action;
    }
    public static void bankWithdraw(int id, int qty, Bank.WithdrawMode withdrawMode) {
        Item item = Bank.getFirst((x) -> {
            return x.getId() == id && !x.isPlaceholder();
        });
        if (item != null) {
            String action = getAction(item, qty, true);
            int actionIndex = item.getActionIndex(action);
            if (withdrawMode == Bank.WithdrawMode.NOTED && !Bank.isNotedWithdrawMode()) {
                Bank.setWithdrawMode(true);
                Time.sleepUntil(Bank::isNotedWithdrawMode, 1200);
            }

            if (withdrawMode == Bank.WithdrawMode.ITEM && Bank.isNotedWithdrawMode()) {
                Bank.setWithdrawMode(false);
                Time.sleepUntil(() -> {
                    return !Bank.isNotedWithdrawMode();
                }, 1200);
            }

            item.interact(actionIndex + 1);
            if (action.equals("Withdraw-X")) {
                Dialog.enterAmount(qty);
                Time.sleepTick();
            }

        }
    }
    public static void enablePrayer(boolean cursed, Prayer prayerIcon) {
        Prayer overhead = getOverhead();
        if (cursed) {
            if (prayerIcon.equals(Prayer.PROTECT_FROM_MAGIC) && (overhead == null || overhead != Prayer.PROTECT_FROM_MISSILES)) {
                log.info("[CURSED] switching prayer from: "+(overhead == null ? "off" : overhead.toString()) + " to " + prayerIcon);
                Prayers.toggle(prayerIcon);
                fastSleep();
            } else if (prayerIcon.equals(Prayer.PROTECT_FROM_MELEE) && (overhead == null || overhead != Prayer.PROTECT_FROM_MAGIC)) {
                log.info("[CURSED] switching prayer from: "+(overhead == null ? "off" : overhead.toString()) + " to " + prayerIcon);
                Prayers.toggle(prayerIcon);
                fastSleep();
            }  else if (prayerIcon.equals(Prayer.PROTECT_FROM_MISSILES) && (overhead == null || overhead != Prayer.PROTECT_FROM_MELEE)) {
                log.info("[CURSED] switching prayer from: "+(overhead == null ? "off" : overhead.toString()) + " to " + prayerIcon);
                Prayers.toggle(prayerIcon);
                fastSleep();
            }
        } else if (overhead == null || overhead != prayerIcon) {
            log.info("switching prayer from: "+(overhead == null ? "off" : overhead.toString()) + " to " +prayerIcon);
            Prayers.toggle(prayerIcon);
            fastSleep();
        }
    }
    public static Prayer getOverhead() {
        HeadIcon ourIcon = Players.getLocal().getOverheadIcon();
        if (ourIcon != null) {
            switch (ourIcon) {
                case MELEE:
                    return Prayer.PROTECT_FROM_MELEE;
                case MAGIC:
                    return Prayer.PROTECT_FROM_MAGIC;
                case RANGED:
                    return Prayer.PROTECT_FROM_MISSILES;
            }
        }
        return null;
    }
    /**
     * You can only interact with an NPC via the adjacent tiles to the NPC, and this method returns distance to closest tile
     * @param npc
     * @return
     */
    public static int getClosestLocalAttackDistanceWithinLineOfSight(NPC npc, Client client) {
        Set<WorldPoint> checkedTiles = new HashSet<>();

        List<WorldPoint> interactableTiles = Reachable.getInteractable(npc);
        WorldArea ourArea = Players.getLocal().getWorldArea();
        WorldPoint ourTile = Players.getLocal().getWorldLocation();
        int shortestDistance = Integer.MAX_VALUE;
        for (WorldPoint tile : interactableTiles) {
            if (!checkedTiles.contains(tile)) {
                if (ourArea.hasLineOfSightTo(client, tile)) {
                    int currentDistance = tile.distanceTo2D(ourTile);
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                    }
                }
                checkedTiles.add(tile);
            }
        }
        return shortestDistance;
    }
    /**
     * You can only interact with an NPC via the adjacent tiles to the NPC, and this method returns distance to closest tile
     * @param npc
     * @return
     */
    public static int getClosestLocalAttackDistance(NPC npc) {
        Set<WorldPoint> checkedTiles = new HashSet<>();
        List<WorldPoint> interactableTiles = Reachable.getInteractable(npc);
        WorldPoint ourTile = Players.getLocal().getWorldLocation();
        int shortestDistance = Integer.MAX_VALUE;
        for (WorldPoint tile : interactableTiles) {
            if (!checkedTiles.contains(tile)) {
                int currentDistance = tile.distanceTo2D(ourTile);
                if (currentDistance < shortestDistance) {
                    shortestDistance = currentDistance;
                }
                checkedTiles.add(tile);
            }
        }
        return shortestDistance;
    }

    public static double getPythagoreanDistance(WorldPoint a, WorldPoint b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        double squared = (dx * dx) + (dy * dy);
        double rooted = Math.sqrt(squared);
        return rooted;
    }
    public static void pressEsc() {
        log.info("Pressed esc");
        Keyboard.pressed(KeyEvent.VK_ESCAPE);
        /* (is only sent if a valid Unicode character could be generated.)
        log.info("Typed esc");
        Keyboard.typed(KeyEvent.VK_ESCAPE); */
        log.info("Released esc");
        Keyboard.released(KeyEvent.VK_ESCAPE);
    }
    public static String convertToRSUnits(int number) {
        String postfix = "";
        int divisor = 1;

        if (number >= 1_000_000) {
            divisor = 1_000_000;
            postfix = "M";
        } else if (number >= 1_000) {
            divisor = 1_000;
            postfix = "K";
        }

        int formattedNumber = (int) Math.ceil((double) number / divisor);

        // Using the NumberFormat class to add commas
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(formattedNumber) + postfix;
    }
    public static int getClosestAttackDistanceInLineOfSight(NPC npc, WorldPoint tileToEval, Client client) {
        Set<WorldPoint> checkedTiles = new HashSet<>();

        List<WorldPoint> interactableTiles = Reachable.getInteractable(npc);
        WorldArea ourArea = new WorldArea(tileToEval.getX(), tileToEval.getY(), 1, 1, 1);
        int shortestDistance = Integer.MAX_VALUE;
        for (WorldPoint tile : interactableTiles) {
            if (!checkedTiles.contains(tile)) {
                if (ourArea.hasLineOfSightTo(client, tile)) {
                    int currentDistance = tile.distanceTo2D(tileToEval);
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                    }
                }
                checkedTiles.add(tile);
            }
        }
        return shortestDistance;
    }
    public static int getClosestAttackDistance(NPC npc, WorldPoint tileToEval) {
        Set<WorldPoint> checkedTiles = new HashSet<>();

        List<WorldPoint> interactableTiles = Reachable.getInteractable(npc);
        WorldArea ourArea = new WorldArea(tileToEval.getX(), tileToEval.getY(), 1, 1, 1);
        int shortestDistance = Integer.MAX_VALUE;
        for (WorldPoint tile : interactableTiles) {
            if (!checkedTiles.contains(tile)) {
                int currentDistance = tile.distanceTo2D(tileToEval);
                if (currentDistance < shortestDistance) {
                    shortestDistance = currentDistance;
                }
                checkedTiles.add(tile);
            }
        }
        return shortestDistance;
    }

    public int getAmmoCount() {
        Item ammo = Equipment.fromSlot(EquipmentInventorySlot.AMMO);
        if (ammo == null || ammo.getName() == null) {
            return 0;
        }
        return ammo.getQuantity();
    }
    public static void depositItem(int itemID, int quantity) {
        if (Bank.Inventory.getCount(true, itemID) <= 0) {
            log.debug("Missing itemID from invy in queue, skipping: "+itemID);
            return;
        }

        //Withdraw by directly interacting for any clickable-action quantities and return immediately
        Item invyItem = Bank.Inventory.getFirst(itemID);
        int foundActionIndex = -1;
        int currentIndex = 0;
        for (String action : invyItem.getActions()) {
            if (currentIndex <= 0) {
                currentIndex++;
                continue;
            }
            if (action.contains("Deposit-")) {
                if (action.equals("Deposit-X")) {
                    currentIndex++;
                    continue;
                }
                if (action.equals("Deposit-All")) {
                    if (quantity != 1 && quantity != 5 && quantity != 10) {
                        log.debug("Invy count of: " +invyItem.getName() +" not 1, 5, or 10, so withdraw-x");
                        foundActionIndex = currentIndex;
                        break;
                    }
                    currentIndex++;
                    continue;
                }
                int availableActionQty = Integer.parseInt(action.replace("Deposit-",""));
                if (availableActionQty == quantity) {
                    log.debug("found action index with quantity: "+availableActionQty+" equal to deposit desired qty: "+quantity);
                    foundActionIndex = currentIndex;
                    break;
                }
            }
            currentIndex++;
        }
        if (foundActionIndex > -1) {
            log.info("Interacting action index "+foundActionIndex+" correlating to action: "+invyItem.getActions()[foundActionIndex]+" on item: "+invyItem.getName());
            invyItem.interact(foundActionIndex);
            return;
        }

        //Call API method to handle withdraw-x quantities and wait for finish
        log.debug("Deposit-x of item/qty: "+invyItem.getName()+"/"+quantity);
        Bank.deposit(itemID, quantity);
    }
    public static void equipItem(int itemID) {
        if (Bank.Inventory.getCount(true, itemID) <= 0) {
            log.debug("Missing itemID from invy in queue, skipping: "+itemID);
            return;
        }
        //Withdraw by directly interacting for any clickable-action quantities and return immediately
        Item invyItem = Bank.Inventory.getFirst(itemID);
        int foundActionIndex = -1;
        int currentIndex = 0;
        for (String action : invyItem.getActions()) {
            log.debug("FOUND DAMN ACTION: "+ action+" WITH DAMN INDEX:" + currentIndex);
            if (currentIndex == 0 || action == null || action.equalsIgnoreCase("null")) {
                currentIndex++;
                continue;
            }
            if (action.equals("Wield") || action.equals("Equip") || action.equals("Wear")) {
                foundActionIndex = currentIndex;
                break;
            }
            currentIndex++;
        }
        if (foundActionIndex > 0) {
            log.debug("Interacting action index "+foundActionIndex+" correlating to action: "+invyItem.getActions()[foundActionIndex]+" on item: "+invyItem.getName());
            invyItem.interact(foundActionIndex);
            sleepClientTick();
            return;
        }
        log.debug("Not found any actionable action to equip item: "+ invyItem.getName());
    }
}