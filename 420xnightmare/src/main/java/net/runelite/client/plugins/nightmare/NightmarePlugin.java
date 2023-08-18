package net.runelite.client.plugins.nightmare;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;

import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.nightmare.util.*;
import net.runelite.client.plugins.nightmare.util.loadouts.*;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Timer;
import net.runelite.client.util.Text;
import net.unethicalite.api.commons.Rand;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileItems;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.events.ExperienceGained;
import net.unethicalite.api.game.Combat;
import net.unethicalite.api.game.Game;
import net.unethicalite.api.game.Skills;
import net.unethicalite.api.input.Keyboard;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.plugins.Script;
import net.unethicalite.api.widgets.Dialog;
import net.unethicalite.api.widgets.Prayers;
import net.unethicalite.api.widgets.Widgets;
import net.unethicalite.client.Static;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
        name = "420xNightmare",
        enabledByDefault = false,
        description = "Kills Phosani's nightmare",
        tags = {"bosses", "combat", "nm", "overlay", "nightmare", "pve", "pvm", "ashihama", "helper", "phosani"}
)

@Slf4j
@Singleton
public class NightmarePlugin extends Script {
    private static final int PLAYER_TELEPORT_ANIMATION = 714;
    //Nightmare's ID during phases
    private static final int NIGHTMARE_SLEEPWALKER_TRANSITION = 9422;
    // Nightmare's attack animations
    private static final int NIGHTMARE_SLEEPWALKER_APPLYING_DAMAGE = 8604;
    private static final int NIGHTMARE_HUSK_SPAWN = 8565;
    private static final int NIGHTMARE_PARASITE_TOSS = 8606;
    private static final int NIGHTMARE_CHARGE = 8609;
    private static final int NIGHTMARE_MELEE_ATTACK = 8594;
    private static final int NIGHTMARE_RANGE_ATTACK = 8596;
    private static final int NIGHTMARE_MAGIC_ATTACK = 8595;
    private static final int NIGHTMARE_PRE_MUSHROOM = 37738;
    private static final int NIGHTMARE_MUSHROOM = 37739;
    private static final int NIGHTMARE_BLOSSOM = 37744;
    private static final int NIGHTMARE_BLOSSOM_BLOOM = 37745;
    private static final int NIGHTMARE_DURING_SHADOWS = 9103;
    private static final int NIGHTMARE_PRE_SHADOWS = 9102;
    private static final int NIGHTMARE_SHADOW = 1767;   // graphics object
    //NPC ids
    private static final int HUSK_RANGED = 9467;
    private static final int HUSK_MAGIC = 9466;
    private static final WorldArea NIGHTMARE_ABOVE_GROUND_WALK_ZONE = new WorldArea(3577, 3170, 200, 377, 0);
    private static final WorldPoint ABOVE_GROUND_ENTRANCE_WALK = new WorldPoint(3728, 3302, 0);
    private static final WorldPoint ABOVE_GROUND_ENTRANCE = new WorldPoint(3728, 3300, 0);
    public static final WorldArea NIGHTMARE_UNDERGROUND_WALK_ZONE = new WorldArea(3713, 9686, 197, 153, 1);
    private static final WorldPoint FEROX_POOL_1 = new WorldPoint(3128, 3638, 0);
    private static final WorldPoint FEROX_POOL_2 = new WorldPoint(3128, 3633, 0);
    private static final WorldArea FEROX_WEST = new WorldArea(3144, 3625, 10, 16, 0);
    private static final WorldArea FEROX_EAST = new WorldArea(3125, 3618, 19, 22, 0);
    private static final int TALKED_SISTER_SENGA_VARPLAYER = 2647;
    private static final WorldPoint POOL_OF_NIGHTMARES = new WorldPoint(3808, 9780, 1);
    private static final WorldPoint SISTER_SENGA = new WorldPoint(3811, 9777, 1);
    private static final WorldArea SENGA_POOL_UNDERGROUND_AREA = new WorldArea(3802, 9770, 13, 18, 1);
    private static final LocalPoint MIDDLE_LOCATION = new LocalPoint(6208, 8128);
    private static Instant nmDiedTimer  = null;
    private static final Set<LocalPoint> PHOSANIS_MIDDLE_LOCATIONS = ImmutableSet.of(new LocalPoint(6208, 7104), new LocalPoint(7232, 7104));
    private static final List<Integer> INACTIVE_TOTEMS = Arrays.asList(9435, 9438, 9441, 9444);
    private static final List<Integer> ACTIVE_TOTEMS = Arrays.asList(9436, 9439, 9442, 9445);
    @Getter(AccessLevel.PACKAGE)
    public static String currentAction = "Script initialize";
    @Getter(AccessLevel.PACKAGE)
    private int totalLootValue = 0;
    @Getter(AccessLevel.PACKAGE)
    private int killCount = 0;
    public static boolean prepareSleepwalkers = false;
    public static boolean waitMagicCast = false;
	public static int sleepwalkersSpawned = 0;
    private static int enablePrayDelay = 0;
    private static boolean drowsy = false;
	public static boolean finalPhase = false;
    public static boolean killedASleepwalker = false;
	public static Actor ourCurrentTarget = null;
    public static int huskSwitchTicks = 0;
    public static Actor ourLastTarget = null;
    public static boolean prepareAfterSleepwalkers = false;
    public static int eatTickTimer = 0;
    public static int drinkTickTimer = 0;
    public static boolean divinePotionExpiring = false;
    @Getter(AccessLevel.PACKAGE)
    private boolean fulfilledEquipment = false;
    @Getter(AccessLevel.PACKAGE)
    private boolean fulfilledGear = false;


    @Getter(AccessLevel.PACKAGE)
    private  boolean haveEnoughDosesForAnotherFight = false;
    @Getter(AccessLevel.PACKAGE)
    private boolean inLobby = false;
    public static Set<WorldPoint> flowerSafeArea = new HashSet<>();
    public static boolean attackedHusk = false;
    public static boolean pokedNm = false;
    public static boolean walkOfaShameHimHaha = false;
    public static boolean attackedParasite = false;
    private Set<Integer> allLoot = new HashSet<>();
    private static int walkingRandomPoint = Rand.nextInt(3, 6);
    private static Set<WorldPoint> arenaTiles = new HashSet<>();
    private static Set<WorldPoint> blacklist = new HashSet<>();
    private static Set<WorldPoint> blacklistWalk = new HashSet<>();
    private static Set<WorldPoint> whitelist = new HashSet<>();
    private static Set<WorldPoint> whitelistWalk = new HashSet<>();
    private static boolean xpGained = false;
    @Getter(AccessLevel.PACKAGE)
    private final Map<Integer, MemorizedTotem> totems = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<LocalPoint, GameObject> spores = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<Polygon, Player> huskTarget = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<Integer, Player> parasiteTargets = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<GraphicsObject> shadows = new HashSet<>();
    private final Map<WorldPoint, Integer> shadowsPoints = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<WorldPoint> walkTile = new HashSet<>();
    private final Set<NPC> husks = new HashSet<>();

    private final Set<NPC> parasites = new HashSet<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<NPC> sleepwalkers = new HashSet<>();
    private final Set<WorldPoint> flowers = new HashSet<>();
    public static int huskMagicAttackTicks = 0;
    private final Set<WorldPoint> murderRunTiles = new HashSet<>();
    private final Set<WorldPoint> sporesWorld = new HashSet<>();
    @Inject
    private Client client;
    @Inject
    private NightmareConfig config;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private InfoBoxManager infoBoxManager;
    @Inject
    private SpriteManager spriteManager;
    @Inject
    private ItemManager itemManager;
    @Inject
    private NightmareOverlay overlay;
    @Inject
    private NightmarePrayerOverlay prayerOverlay;
    @Inject
    private NightmarePrayerInfoBox prayerInfoBox;
    @Inject
    private SanfewInfoBox sanfewInfoBox;
    @Nullable
    @Getter(AccessLevel.PACKAGE)
    private NightmareAttack pendingNightmareAttack;
    @Nullable
    @Getter(AccessLevel.PACKAGE)
    private NPC nm;
    @Getter(AccessLevel.PACKAGE)
    private boolean inFight;
    private boolean cursed;
    private boolean refilledEctophial = false;
    @Getter(AccessLevel.PACKAGE)
    private int ticksUntilNextAttack = 0;
    @Getter(AccessLevel.PACKAGE)
    private boolean parasite;
    @Getter(AccessLevel.PACKAGE)
    private int ticksUntilParasite = 0;
    @Getter(AccessLevel.PACKAGE)
    private boolean nightmareCharging = false;
    @Getter(AccessLevel.PACKAGE)
    private boolean shadowsSpawning = false;
    @Getter(AccessLevel.PACKAGE)
    private int shadowsTicks;
    private int totemsAlive = 0;
    @Getter(AccessLevel.PACKAGE)
    @Setter
    private boolean flash = false;
    private List<WorldPoint> blockedTiles = new ArrayList<>();
    private Prayer[] prayersToFlick = {
            Prayer.PIETY,
            Prayer.CHIVALRY,
            Prayer.ULTIMATE_STRENGTH
    };
    private Prayer[] prayersToDefend = {
            Prayer.PROTECT_FROM_MAGIC,
            Prayer.PROTECT_FROM_MELEE,
            Prayer.PROTECT_FROM_MISSILES
    };
    private final String drowsyMsg = "You're too drowsy to run!";
    private final String undrowsyMsg = "The Nightmare's infection has worn off.";
    private boolean turnOffDefensivePrayers() {
        for (Prayer p : prayersToDefend) {
            if (Prayers.isEnabled(p)) {
                Log.log("Turning off prayer: "+p.toString());
                Prayers.toggle(p);
                API.sleepClientTick();
                return true;
            }
        }
        return false;
    }
    public NightmarePlugin() {
        inFight = false;
    }

    public static boolean isInFeroxEnclave() {
        return FEROX_EAST.contains(Players.getLocal()) || FEROX_WEST.contains(Players.getLocal());
    }

    /**
     * checks if the tile is east/west or north/south of us perpendicularly.
     * also checks if tiles are equal.
     * @param eastwest
     * @param p
     * @return True when point is directly in passed direction or equal to local player
     */
    public static boolean isDirectCompassDirectionFromUs(boolean eastwest, WorldPoint p) {
        int x = Players.getLocal().getWorldX();
        int y = Players.getLocal().getWorldY();
        int dx = p.getX();
        int dy = p.getY();
        // Check if coordinates are equal
        if (x == dx && y == dy) {
            return true;
        }
        if (eastwest) {
            return dy == y;
        } else return dx == x;
    }

    public static List<GameObject> sortTotemsClockwiseFromWest(List<GameObject> totems) {
        Player localPlayer = Players.getLocal();
        WorldPoint localPlayerLocation = localPlayer.getWorldLocation();

        // Sort by angle
        Collections.sort(totems, (npc1, npc2) -> {
            double angle1 = calculateAngle(localPlayerLocation, npc1.getWorldLocation());
            double angle2 = calculateAngle(localPlayerLocation, npc2.getWorldLocation());
            return Double.compare(angle1, angle2);
        });

        return totems;
    }

    private static double calculateAngle(WorldPoint center, WorldPoint point) {
        int dx = point.getX() - center.getX();
        int dy = point.getY() - center.getY();

        double angle = Math.atan2(dy, dx) * (180 / Math.PI);

        // Convert Math.atan2's range of [-180, 180] to [0, 360]
        if (angle < 0) {
            angle += 360;
        }

        // Adjust so that west is 0 degrees
        angle = (angle + 270) % 360;

        return angle;
    }

    public List<Integer> extractIDsNewlineSeperated(boolean melee) {
        List<String> strings = new ArrayList<>();
        if (melee) {
            strings = Arrays.asList((config.meleeGearText().split("\n")));
        } else {
            strings = Arrays.asList((config.mageGearText().split("\n")));
        }
        List<Integer> foundIds = new ArrayList<>();
        for (String s : strings) {
            s = s.strip();
            Pattern pattern = Pattern.compile("^[0-9]+$");
            Matcher matcher = pattern.matcher(s);

            if (matcher.find()) {
                int number = Integer.parseInt(s);
                foundIds.add(number);
            }
        }
        return foundIds;
    }
    @Override
    protected int loop() {
        if (Game.getState() != GameState.LOGGED_IN || !config.startButton()) {
            return API.returnTick();
        }
        /*if (true) {
            Log.log("Testing");
            API.equipItemInBank(ItemID.FIRE_CAPE);
            API.equipItemInBank(ItemID.PRIMORDIAL_BOOTS);
            API.equipItemInBank(ItemID.GRANITE_GLOVES);
            API.equipItemInBank(ItemID.ABYSSAL_BLUDGEON);
            API.equipItemInBank(ItemID.PRIMORDIAL_BOOTS);
            return API.returnTick();
        }*/
        long startLoopTick = API.ticks;
        refilledEctophial = false;
        if (!drowsy && !Movement.isRunEnabled() && Movement.getRunEnergy() > 5 && sporesWorld.isEmpty()) {
            Movement.toggleRun();
            API.fastSleep();
        }
        if (SENGA_POOL_UNDERGROUND_AREA.contains(Players.getLocal())) {
            Widget dialogSpriteText = Widgets.get(WidgetInfo.DIALOG_SPRITE_TEXT);
            if (dialogSpriteText != null && dialogSpriteText.isVisible() && dialogSpriteText.getText().contains("You need 60,000 x Coins to retrieve your items.")) {
                Log.log("Poor as fuck buddy, sorry mate, can't continue script, need 60k gp for dying noob ass recovery");
                this.stop();
                return -1;
            }
            Widget dialogNPCText = Widgets.get(WidgetInfo.DIALOG_NPC_TEXT);
            if (dialogNPCText != null && dialogNPCText.isVisible() && dialogNPCText.getText().contains("Sorry, but I don't have anything for you. If I did")) {
                Log.log("Collected all items from Senga! Nice");
                walkOfaShameHimHaha = false;
            }
            inLobby = true;
        } else {
            inLobby = false;
        }
        int existingDuelCharge = Jewelry.getLeastDuelingChargeID();

        //load melee equipment ids from config
        EquipmentLoadout meleeEqpt = new EquipmentLoadout();
        List<Integer> configEqptIds = extractIDsNewlineSeperated(true);
        for (int id : configEqptIds) {
            meleeEqpt.addItem(new LoadoutItem(id, 1));
        }

        //load mage gear ids from config
        EquipmentLoadout mageSwap = new EquipmentLoadout();
        List<Integer> configMageIds = extractIDsNewlineSeperated(false);
        for (int id : configMageIds) {
            if (configEqptIds.contains(id)) continue;
            mageSwap.addItem(new LoadoutItem(id, 1));
        }

        //load individual swap ids from config
        int sleepwalkerWeapon = config.rangedWeapon();
        int specWeapon = config.specWeapon();
        int minSpec = config.minSpec();
        int huskWeapon = config.huskSwitchID();
        int parasiteWeapon = config.parasiteSwitchID();
        int nmWeapon = config.nmSwitch();
        int[] allWeps = {
                sleepwalkerWeapon,
                specWeapon,
                huskWeapon,
                parasiteWeapon,
                nmWeapon
        };

        //load inventory quantities to re-gear with from config
        int sanfewWithdraw = config.sanfewWithdrawCount();
        int prayerWithdraw = config.prayerWithdrawCount();
        int superCombatWithdraw = config.superCombatWithdrawCount();
        int anglerWithdraw = config.anglerWithdrawCount();

        //load inventory quantities to check for if have enough for new fight
        int sanfewMinRekill = config.rekillMinSanfew();
        int prayerMinRekill = config.rekillMinPray();
        int superCombatMinRekill = config.rekillMinCombat();
        int anglerMinRekill = config.rekillMinAngler();

        //make base no wep melee gear
        List<Integer> wepsSet = Arrays.stream(allWeps).boxed().collect(Collectors.toList());
        List<Integer> baseMeleeNoWep = new ArrayList<>(configEqptIds);
        baseMeleeNoWep.removeAll(wepsSet);


        //make husk weapon gear switch
        List<Integer> huskFullSwapIDs = new ArrayList<>(baseMeleeNoWep);
        huskFullSwapIDs.add(huskWeapon);

        //make sleepwalker weapon gear switch
        List<Integer> sleepwalkerFullSwapIDs = new ArrayList<>(baseMeleeNoWep);
        sleepwalkerFullSwapIDs.add(sleepwalkerWeapon);

        //make special weapon gear switch
        List<Integer> specialFullSwapIDs = new ArrayList<>(baseMeleeNoWep);
        specialFullSwapIDs.add(specWeapon);

        //make parasite weapon gear switch
        List<Integer> parasiteFullSwapIDs = new ArrayList<>(baseMeleeNoWep);
        parasiteFullSwapIDs.add(parasiteWeapon);

        //make nm DPS gear switch
        List<Integer> nmDPSFulLSwapIDs = new ArrayList<>(baseMeleeNoWep);
        nmDPSFulLSwapIDs.add(nmWeapon);

        //make difference in mage gear from melee
        List<Integer> mageGearInvy = new ArrayList<>(configMageIds);
        mageGearInvy.removeIf(i -> configEqptIds.contains(i));

        //make full withdrawal inventory loadout

        InventoryLoadout invy = new InventoryLoadout(
                //supplies
                new LoadoutItem(ItemID.ANGLERFISH, anglerWithdraw),
                new LoadoutItem(ItemID.PRAYER_POTION4, prayerWithdraw),
                new LoadoutItem(ItemID.SANFEW_SERUM4, sanfewWithdraw),
                new LoadoutItem(ItemID.DIVINE_SUPER_COMBAT_POTION4, superCombatWithdraw),

                //sorry had to hardcode these lol
                new LoadoutItem(ItemID.ECTOPHIAL, 1),
                new LoadoutItem(existingDuelCharge, 1),

                //swaps
                new LoadoutItem(parasiteWeapon, 1),
                new LoadoutItem(huskWeapon, 1),
                new LoadoutItem(sleepwalkerWeapon, 1),
                new LoadoutItem(specWeapon, 1)

        );
        //add rest of mage gear swaps to full inventory loadout
        for (int i : mageGearInvy) {
            log.debug("Have mage gear in invy loadout: " +i);
            invy.addItem(new LoadoutItem(i, 1));
        }
        Loadout gear = new Loadout(meleeEqpt, invy);

        //Start sequence of evaluations for readiness for next fight and save for various future scenarios
        if (meleeEqpt.fulfilled()) {
            log.debug("Have equipment for fight");
            fulfilledEquipment = true;
        } else {
            fulfilledEquipment = false;
        }
        if (Inventory.contains(parasiteWeapon) &&
                Inventory.contains(huskWeapon) &&
                Inventory.contains(specWeapon) &&
                Inventory.contains(sleepwalkerWeapon) &&
                Inventory.contains(ItemID.ECTOPHIAL) &&
                Inventory.contains(existingDuelCharge)) {
            boolean haveMage = true;
            for (int i : mageGearInvy) {
                if (!Inventory.contains(i)) {
                    haveMage = false;
                    break;
                }
            }
            if (!haveMage) {
                fulfilledGear = false;
            } else {
                log.debug("Have invy swap gear for fight");
                fulfilledGear = true;
            }
        } else {
            fulfilledGear = false;
        }
        if (Inventory.getCount(false, ItemID.ANGLERFISH) >= anglerMinRekill &&
                (Inventory.getCount(false, ItemID.PRAYER_POTION1) * 1 +
                        Inventory.getCount(false, ItemID.PRAYER_POTION2) * 2 +
                        Inventory.getCount(false, ItemID.PRAYER_POTION3) * 3 +
                        Inventory.getCount(false, ItemID.PRAYER_POTION4) * 4 +
                        Inventory.getCount(false, ItemID.SANFEW_SERUM1) * 1 +
                        Inventory.getCount(false, ItemID.SANFEW_SERUM2) * 2 +
                        Inventory.getCount(false, ItemID.SANFEW_SERUM3) * 3 +
                        Inventory.getCount(false, ItemID.SANFEW_SERUM4) * 4) >= prayerMinRekill &&
                (Inventory.getCount(false, ItemID.SANFEW_SERUM1) * 1 +
                        Inventory.getCount(false, ItemID.SANFEW_SERUM2) * 2 +
                        Inventory.getCount(false, ItemID.SANFEW_SERUM3) * 3 +
                        Inventory.getCount(false, ItemID.SANFEW_SERUM4) * 4) >= sanfewMinRekill &&
                (Inventory.getCount(false, ItemID.DIVINE_SUPER_COMBAT_POTION1) * 1 +
                        Inventory.getCount(false, ItemID.DIVINE_SUPER_COMBAT_POTION2) * 2 +
                        Inventory.getCount(false, ItemID.DIVINE_SUPER_COMBAT_POTION3) * 3 +
                        Inventory.getCount(false, ItemID.DIVINE_SUPER_COMBAT_POTION4) * 4) >= superCombatMinRekill) {
            log.debug("Have enough doses/supplies for another fight");
            haveEnoughDosesForAnotherFight = true;
        } else {
            haveEnoughDosesForAnotherFight = false;
        }
        if (inFight) {
            //start bot actions

            //Drink at prayer
            if (CombatStuff.shouldDrinkPrayer(config.prayerSetpoint()) && CombatStuff.drinkPrayerPotion()) {
                return API.returnTick();
            }

            //Drink at str lvl
            if (CombatStuff.shouldDrinkCombatPotion() && CombatStuff.drinkCombatPotion()) {
                return API.returnTick();
            }

            if (nm == null || !nm.hasAction("Attack")) {
                // drop empty vials
                Item vial = Inventory.getFirst(i -> i.getId() == ItemID.VIAL);
                if (vial != null && vial.getName() != null) {
                    vial.interact("Drop");
                    return API.returnTick();
                }

                TileItem loots = TileItems.getNearest(g -> allLoot.contains(g.getId()));
                if (loots != null && loots.getName() != null) {
                    nmDiedTimer = null;
                    if (Inventory.isFull()) {
                        //here, invy full and have valuable loot on ground
                        //eat food items to eat
                        Item foods = Inventory.getFirst(i -> i.getId() == ItemID.ANGLERFISH || i.getId() == ItemID.SHARK || i.getId() == ItemID.BASS);
                        if (foods != null && foods.getName() != null) {
                            foods.interact("Eat");
                            eatTickTimer = 3;
                            return API.returnTick();
                        }
                        // then drink least doses of potions
                        int leastDoseID = Doses.getLeastDose();
                        if (leastDoseID > 0) {
                            Item leastPot = Inventory.getFirst(leastDoseID);
                            if (leastPot != null && leastPot.getName() != null) {
                                leastPot.interact("Drink");
                                drinkTickTimer = 3;
                            }
                            return API.returnTick();
                        }
                        return API.returnTick();
                    }
                    loots.pickup();
                    return API.returnTick();
                }
                if (!haveEnoughDosesForAnotherFight) {
                    //check if inventory has at least some space left, if so, check for supply drops on the ground and pickup
                    if (!Inventory.isFull()) {
                        for (int item : Loot.supplies) {
                            TileItem supply = TileItems.getNearest(item);
                            if (supply != null && supply.getName() != null) {
                                supply.pickup();
                                return API.returnTick();
                            }
                        }
                    }
                    if (nmDiedTimer == null) {
                        Log.log("Teleporting out due to not more doses for another fight");
                        return CombatStuff.teleportOut();
                    }
                    if (Instant.now().isAfter(nmDiedTimer)) {
                        nmDiedTimer = null;
                    }
                    return API.returnTick();
                }
                //pray offensive prayer strictly prior to fight and also when going to attack husks, parasites, etc
                prayMeleeOffensive();
                return API.returnTick();
            }

            //Pray against nightmare or husks, depending on exact situation
            //Husk about to attack, nm not
            if ((huskMagicAttackTicks == 1) &&
                    ((pendingNightmareAttack.getPrayer().equals(Prayer.PROTECT_FROM_MELEE) && ticksUntilNextAttack != 5) ||
                            (!pendingNightmareAttack.getPrayer().equals(Prayer.PROTECT_FROM_MELEE) && ticksUntilNextAttack != 4))) {
                if (!Prayers.isEnabled(Prayer.PROTECT_FROM_MAGIC)) {
                    API.enablePrayer(cursed, Prayer.PROTECT_FROM_MAGIC);
                }
            } else if (pendingNightmareAttack != null && ticksUntilNextAttack >= 4) {
                if (enablePrayDelay == 0) {
                    API.enablePrayer(cursed, pendingNightmareAttack.getPrayer());
                    enablePrayDelay = 1;
                }
            } else {
                turnOffDefensivePrayers();
            }
            //Kill the sleepwalkers that spawn, these exist in a mutually exclusive state to all other special attacks
            if (!sleepwalkers.isEmpty()) {
                log.debug("Kill sleepwalker");
                killSleepwalker();
                return API.returnTick();
            }

            // projectile for sleepwalkers spawned
            if (prepareSleepwalkers) {
                Switching.equip(sleepwalkerWeapon);
                turnOffAllPrayers();
                return API.returnTick();
            }
            // projectile for sleepwalkers spawned and all sleepwalkers dead, switch back to NM gear
            if (prepareAfterSleepwalkers) {
                //Kill the parasites spawn during sleepwalker phase
                if (!parasites.isEmpty()) {
                    NPC closestParasite = parasites.stream()
                            .min(Comparator.comparingInt(npc -> npc.distanceTo(Players.getLocal())))
                            .orElse(null);
                    if (closestParasite == null || closestParasite.getName() == null) {
                        Log.log("null NPC found of tracked PARASITES");
                        parasites.remove(closestParasite);
                        return 10;
                    }
                    log.debug("Kill parasite");
                    return killParasite(closestParasite, parasiteFullSwapIDs);
                }
                Switching.equip(nmDPSFulLSwapIDs);
                prayMeleeOffensive();
                return API.returnTick();
            }

            //Prepare for husk spawn by switching and resetting attack tick timer to get fastest hits off on husks
            if (huskSwitchTicks >= 1) {
                // switching to event rpg before they spawn
                if (!Switching.equip(huskFullSwapIDs)) {
                    return API.returnTick();
                }
                // stop attacking nm to get fastest hit on husks
                if (ourCurrentTarget != null && ourCurrentTarget.getName() != null) {
                    Log.log("reset attack ticks for husk preparation by walking our own tile");
                    Movement.walk(Players.getLocal());
                    return API.returnTick();
                }
                return API.returnTick();
            }
            //Kill the husks that spawn, the existence of husks force all shadows to spawn outside our tile range, excluding the need to check to "off tile" here
            if (!husks.isEmpty()) {
                log.debug("Kill husk");
                killHusks(huskFullSwapIDs);
                return API.returnTick();
            }

            // here is possible for blocked tiles to damage or slow us so aggregate whitelist tiles (flower power area or arena area radius around us) and blacklist tiles (everything else)
            List<WorldPoint> sortedWhitelist = new ArrayList<>();
            List<WorldPoint> sortedWhitelistWalk = new ArrayList<>();

            NPC totemNPC = getClosestTotem();
            if (whitelist.isEmpty()) {
                // no whitelisted tiles left
                Log.log("NOTHING IS SAFE!!! BLACKLIST TILE AND NO WHITELISTED");
            } else {
                // Calculate the distance to each whitelisted tile and sort by distance
                sortedWhitelist = whitelist.stream()
                        .sorted(Comparator.comparingInt(tile -> tile.distanceTo(Players.getLocal())))
                        .collect(Collectors.toList());
                sortedWhitelistWalk = whitelistWalk.stream()
                        .sorted(Comparator.comparingInt(tile -> tile.distanceTo(Players.getLocal())))
                        .collect(Collectors.toList());
                if (parasites.isEmpty() && totemNPC != null) {
                    //if we find any parasites active, ignore totem sorting, otherwise need to sort whitelist tiles from distance to nearest totem interactable tile
                    sortedWhitelistWalk = findClosestWhitelistedTilesSortedSpeciallyForTotems(sortedWhitelistWalk, totemNPC);
                }

            }
            if (!sortedWhitelist.get(0).equals(Players.getLocal().getWorldLocation())) {
                Log.log("Hopping off unsafe tile");
                //filter list for tiles directly in interactable tile to nm and check if has any

                List<WorldPoint> interactableNm = null;
                if (!parasites.isEmpty()) {
                    NPC parasyte = parasites.stream().findFirst().get();
                    if (!parasyte.isMoving()) {
                        interactableNm = Reachable.getInteractable(parasyte);
                    }
                }
                if (interactableNm == null) {
                    interactableNm = Reachable.getInteractable(nm);
                }

                //only walk to interactable nm tile when no totems, otherwise path towards totem
                if (interactableNm != null && totemNPC == null) {
                    List<WorldPoint> finalInteractableNm = new ArrayList<>();
                    //first, check if any tiles within distance of 2 and interactable tile of nm
                    List<WorldPoint> interactableWalkableWhitelist = sortedWhitelist
                            .stream()
                            .filter(wp -> finalInteractableNm.contains(wp) && wp.distanceTo(Players.getLocal()) <= 2)
                            .sorted(Comparator.comparingInt(wp -> wp.distanceTo(Players.getLocal())))
                            .collect(Collectors.toList());
                    if (interactableWalkableWhitelist.isEmpty()) {
                        //if none found, remove check for interactable tile, just check distance less than 2 whitelisted around us (1-tick walk safe point)
                        List<WorldPoint> walkableWhitelist = sortedWhitelist
                                .stream()
                                .filter(wp -> wp.distanceTo(Players.getLocal()) <= 2)
                                .sorted(Comparator.comparingInt(wp -> wp.distanceTo(Players.getLocal())))
                                .collect(Collectors.toList());
                        if (walkableWhitelist.isEmpty()) {
                            //if none found, risk it for tha bag and go for interactable tile
                            List<WorldPoint> interactableWhitelist = sortedWhitelist
                                    .stream()
                                    .filter(wp -> finalInteractableNm.contains(wp))
                                    .sorted(Comparator.comparingInt(wp -> wp.distanceTo(Players.getLocal())))
                                    .collect(Collectors.toList());
                            if (!interactableWhitelist.isEmpty()) {
                                walk(interactableWhitelist.get(0));
                                return API.returnTick();
                            }
                            Log.log("Uhhh NO safetiles around nightmare :o");
                            return API.returnTick();
                        }
                        walk(walkableWhitelist.get(0));
                        return API.returnTick();
                    }
                    walk(interactableWalkableWhitelist.get(0));
                    return API.returnTick();
                }
                // Walk to the nearest whitelisted tile and return early to let walk interaction slap on next tick
                walk(sortedWhitelist.get(0));
                return API.returnTick();
            }
            log.debug("Closest whitelist tile: " + (sortedWhitelist.isEmpty() ? "EMPTY WHITELIST!" : sortedWhitelist.get(0).toString()) + " EQUAL TO our tile: " + Players.getLocal().getWorldLocation().toString() + " with global ticks=" + API.ticks + " and shadows countdown ticks=" + shadowsTicks);
            if (ticksUntilParasite > 0 && ticksUntilParasite <= 3) {
                prayMeleeOffensive();
                if (!Switching.equip(parasiteFullSwapIDs)) {
                    return API.returnTick();
                }

                if (ourCurrentTarget != null && ourCurrentTarget.getName() != null) {
                    Log.log("reset attack ticks for parasite preparation by walking our own tile");
                    walk(Players.getLocal().getWorldLocation());
                }
                return API.returnTick();
            }
            //Kill the parasites that spawn
            if (!parasites.isEmpty()) {
                if (whitelist.isEmpty()) {
                    // no whitelisted tiles left
                    Log.log("NOTHING IS SAFE!!! BLACKLIST TILE AND NO WHITELISTED");
                    return API.returnTick();
                } else if (!sortedWhitelist.get(0).equals(Players.getLocal().getWorldLocation())) {
                    Log.log("Hop off unsafe tile for parasite future attack");
                    walk(sortedWhitelist.get(0));
                    return API.returnTick();
                }
                NPC closestParasite = parasites.stream()
                        .min(Comparator.comparingInt(npc -> npc.distanceTo(Players.getLocal())))
                        .orElse(null);
                if (closestParasite == null || closestParasite.getName() == null) {
                    Log.log("null NPC found of tracked PARASITES");
                    parasites.remove(closestParasite);
                    return 10;
                }

                log.debug("Kill parasite");
                return killParasite(closestParasite, parasiteFullSwapIDs);
            }

            //Drink sanfew serums when preggers
            if (parasite && drinkTickTimer == 0) {
                int sanfew = Doses.getLeastSanfew();
                if (sanfew == -1) {
                    Log.log("NO SANFEW LEFT WITH PARASITE IMPREGNATED!");
                } else {
                    Item sanfewPot = Inventory.getFirst(sanfew);
                    if (sanfewPot == null) {
                        Log.log("Discrepancy between contains and get item methods for sanfew serum");
                    } else {
                        Log.log("Drink sanfew potion for impregnation");
                        sanfewPot.interact("Drink");
                        drinkTickTimer = 3;
                        API.fastSleep();
                    }
                }
            }

            // Check if totems are active and need damaging, if so, get closest one
            if (totemNPC != null) {
                //need to ensure can wait 1 tick to cast spell :P
                if (sortedWhitelistWalk.isEmpty()) {
                    // no whitelisted tiles left in range of totems
                    Log.log("AFK this tick due to on whitelisted tile but out of range of possible totems (flower power + shadows + player in opposite corner of targeted totem)");
                    return API.returnTick();
                } else {
                    final WorldPoint closestSortedWhitelistWalk = sortedWhitelistWalk.get(0);
                    if (!closestSortedWhitelistWalk.equals(Players.getLocal().getWorldLocation())) {
                        if (API.getClosestAttackDistance(totemNPC, closestSortedWhitelistWalk) > 8) {
                            double closestDist = API.getPythagoreanDistance(closestSortedWhitelistWalk, closestSortedWhitelistWalk);
                            double ourDist = API.getPythagoreanDistance(Players.getLocal().getWorldLocation(), closestSortedWhitelistWalk);
                            if (closestDist < ourDist) {
                                Log.log("hop off future unsafe tile for totems (out of range) closer towards Totem");
                                walk(closestSortedWhitelistWalk);
                            } else {
                                Log.log("Chilling here AFK while totems out of our immediate range and no tiles to get us closer");
                            }
                        } else {
                            Log.log("hop off future unsafe tile for totems (in range)");
                            walk(closestSortedWhitelistWalk);
                        }
                        return API.returnTick();
                    }
                }
                //Eat at hitpoints
                if (CombatStuff.shouldEat(config.healSetpoint()) && CombatStuff.eat()) {
                    return API.returnTick();
                }

                log.debug("switch mage + attack totem");
                turnOffOffensiveMeleePray();
                if (!Switching.equip(configMageIds)) {
                    return API.fastReturn();
                }
                if (API.ticks > startLoopTick) {
                    return API.fastReturn();
                }
                if ((ourCurrentTarget != null && ourCurrentTarget.getTag() == totemNPC.getTag()) ||
                        waitMagicCast) {
                    waitMagicCast = false;
                    return API.returnTick();
                }
                Log.log("totem interact Charge");
                totemNPC.interact("Charge");
                waitMagicCast = true;
                return API.returnTick();
            }

            // Here we have dealt with all special actions DPS nightmare
            //Eat at hitpoints
            if (CombatStuff.shouldEat(config.healSetpoint()) && CombatStuff.eat()) {
                return API.returnTick();
            }

            prayMeleeOffensive();

            //dont attack nightmare if she is surging / murder run
            if (!murderRunTiles.isEmpty()) {
				if (ourCurrentTarget != null && ourCurrentTarget.getTag() == nm.getTag()) {
					walk(sortedWhitelist.get(0));
				}
                return API.returnTick();
            }
            if (nm != null && nm.getName() != null && !nm.isDead() && nm.hasAction("Attack")) {
                int result = safelyWalkToInteractAttackMeleeNPC(
                        nm,
                        nmDPSFulLSwapIDs,
                        true,
                        minSpec,
                        specWeapon,
                        specialFullSwapIDs
                );
                if (result == -1) {
                    Log.log("Not found any safe tiles near nightmare Reachable.getInteractable() filtered from blacklist :o");
                } else {
                    return result;
                }
            }
            return API.returnTick();
        }
        if (Prayers.anyActive()) {
            Prayers.disableAll();
            Time.sleepTick();
            return API.shortReturn();
        }
        if (arenaTiles.contains(Players.getLocal().getWorldLocation())) {
            // drop empty vials
            Item vial = Inventory.getFirst(i -> i.getId() == ItemID.VIAL);
            if (vial != null && vial.getName() != null) {
                vial.interact("Drop");
                return API.returnTick();
            }

            TileItem loots = TileItems.getNearest(g -> allLoot.contains(g.getId()));
            if (loots != null && loots.getName() != null) {
                nmDiedTimer = null;
                if (Inventory.isFull()) {
                    //here, invy full and have valuable loot on ground
                    //eat food items to eat
                    Item foods = Inventory.getFirst(i -> i.getId() == ItemID.ANGLERFISH || i.getId() == ItemID.SHARK || i.getId() == ItemID.BASS);
                    if (foods != null && foods.getName() != null) {
                        foods.interact("Eat");
                        eatTickTimer = 3;
                        return API.returnTick();
                    }
                    // then drink least doses of potions
                    int leastDoseID = Doses.getLeastDose();
                    if (leastDoseID > 0) {
                        Item leastPot = Inventory.getFirst(leastDoseID);
                        if (leastPot != null && leastPot.getName() != null) {
                            leastPot.interact("Drink");
                            drinkTickTimer = 3;
                        }
                        return API.returnTick();
                    }

                    // nothing left to drop, must be full of l00tz, last ditch effort is to drop event RPG
                    Widget destroyConfirmationMenu = Widgets.get(WidgetID.DESTROY_ITEM_GROUP_ID, 0);
                    if (destroyConfirmationMenu != null && destroyConfirmationMenu.isVisible()) {
                        Keyboard.type(1);
                        Time.sleepTick();
                        return API.returnTick();
                    }
                    Item goblinPaintCannon = Inventory.getFirst(ItemID.GOBLIN_PAINT_CANNON);
                    if (goblinPaintCannon != null && goblinPaintCannon.getName() != null) {
                        goblinPaintCannon.interact("Destroy");
                    }
                    return API.returnTick();
                }
                loots.pickup();
                return API.returnTick();
            }
            if (!haveEnoughDosesForAnotherFight) {
                //check if inventory has at least some space left, if so, check for supply drops on the ground and pickup
                if (!Inventory.isFull()) {
                    for (int item : Loot.supplies) {
                        TileItem supply = TileItems.getNearest(item);
                        if (supply != null && supply.getName() != null) {
                            supply.pickup();
                            return API.returnTick();
                        }
                    }
                }
                if (nmDiedTimer == null) {
                    Log.log("Teleporting out due to not more doses for another fight");
                    CombatStuff.teleportOut();
                    return API.returnTick();
                }
                if (Instant.now().isAfter(nmDiedTimer)) {
                    nmDiedTimer = null;
                }
                Log.log("Waiting for NM to die");
                return API.returnTick();
            }
            NPC sleptNM = NPCs.getNearest(n -> n.getName().equalsIgnoreCase("phosani's nightmare") && n.hasAction("Disturb"));
            if (sleptNM != null && sleptNM.getName() != null) {
                if (!pokedNm) {
                    sleptNM.interact("Disturb");
                    Time.sleepTick();
                    Time.sleepUntil(() -> !Players.getLocal().isMoving() && !Players.getLocal().isAnimating(), 100, 3000);
                }
            }
            return API.returnTick();
        }
        if (!Switching.equipWait(nmDPSFulLSwapIDs)) {
            return API.fastReturn();
        }
        fulfilledEquipment = meleeEqpt.fulfilled();
        if (Inventory.contains(parasiteWeapon) &&
                Inventory.contains(huskWeapon) &&
                Inventory.contains(specWeapon) &&
                Inventory.contains(sleepwalkerWeapon) &&
                Inventory.contains(ItemID.ECTOPHIAL) &&
                Inventory.contains(existingDuelCharge)) {
            boolean haveMage = true;
            for (int i : mageGearInvy) {
                if (!Inventory.contains(i)) {
                    haveMage = false;
                    break;
                }
            }
            if (!haveMage) {
                fulfilledGear = false;
            } else {
                log.debug("Have invy swap gear for fight");
                fulfilledGear = true;
            }
        } else {
            fulfilledGear = false;
        }
        //death walk
        if (walkOfaShameHimHaha) {
            if (!Inventory.contains(ItemID.ECTOPHIAL)) {
                if (Bank.isOpen() && Bank.Inventory.getCount(ItemID.ECTOPHIAL) > 0) {
                    Bank.close();
                    return API.returnTick();
                }
                if (!Bank.isOpen()) {
                    return API.clickLocalBank();
                }
                if (!Bank.contains(ItemID.ECTOPHIAL)) {
                    Log.log("No more teleports back to NM! Exiting script");
                    this.stop();
                    return -1;
                }
                API.bankWithdraw(ItemID.ECTOPHIAL, 1, Bank.WithdrawMode.ITEM);
                return API.returnTick();
            }
            if (!(NIGHTMARE_ABOVE_GROUND_WALK_ZONE.contains(Players.getLocal()) || NIGHTMARE_UNDERGROUND_WALK_ZONE.contains(Players.getLocal())) && Inventory.contains(ItemID.ECTOPHIAL)) {
                if (Bank.isOpen()) {
                    Bank.close();
                    return API.returnTick();
                }
                useNMTeleport();
                return API.shortReturn();
            }
            if (walkToPhosanisLobby()) {
                //collect items here
                Log.log("Collecting items here");
                //collection interface open
                Widget itemCollectOpen = Widgets.get (602, 1);
                if (itemCollectOpen != null && itemCollectOpen.isVisible()) {
                    //parse it for all items it contains
                    int stacksLeft = 0;
                    Widget stackCountWidget = Widgets.get(602, 10);
                    Widget getGearBackButton = Widgets.get(602, 6); //can have "Unlock" or "Take-All" action
                    String action = (getGearBackButton.hasAction("Unlock") ? "Unlock" : "Take-All");
                    String stackCountFullText = stackCountWidget.getText();
                    String[] firstSplit = stackCountFullText.split("<", 3);
                    if (firstSplit.length > 2) {
                        String[] secondSplit = firstSplit[1].split(">", 2);
                        if (secondSplit.length > 1) {
                            stacksLeft = Integer.parseInt(secondSplit[1]);
                            if (stacksLeft > 0) {
                                if (Inventory.isFull()) {
                                    if (Inventory.contains(ItemID.ANGLERFISH)) {
                                        if (eatTickTimer == 0) {
                                            Inventory.getFirst(ItemID.ANGLERFISH).interact("Eat");
                                            eatTickTimer = 3;
                                        }
                                        return API.returnTick();
                                    }
                                    Log.log("No more room in inventory when trying to collect items from senga, and no more anglerfish! wtf");
                                    return API.returnTick();
                                }
                                Log.log("Have stacks: "+stacksLeft);
                                getGearBackButton.interact(action);
                                return API.returnTick();
                            } else {
                                Log.log("Collected all items from Senga! Nice");
                                walkOfaShameHimHaha = false;
                            }
                        }
                    }
                    return API.returnTick();
                }
                if (Players.getLocal().isMoving()) {
                    return -2;
                }
                NPC sisterSenga = NPCs.getNearest("Sister Senga");
                if (sisterSenga == null || sisterSenga.getName() == null) {
                    Log.log("Sister senga is null and we are in lobby area! :o");
                    return -1;
                }
                sisterSenga.interact("Collect");
                return API.returnTick();
            }
            Log.log("Walk of A-Shame-Him-Haha");
            return API.returnTick();
        }
        if (!fulfilledEquipment || !haveEnoughDosesForAnotherFight || !fulfilledGear || (!inLobby && !NIGHTMARE_UNDERGROUND_WALK_ZONE.contains(Players.getLocal()))) {
            //initialize bank cache
            if (!BankCache.checkCache()) {
                BankCache.update();
                return API.clickLocalBank();
            }
            // Get lowest duel charge
            if (existingDuelCharge == -1) {
                Log.log("Detected that we have no more rings of dueling left, stopping script - need this for banking :o");
                this.stop();
            }

            //goal is to be geared at ferox enclave with full invy and staminated and angler HP boosted
            if (isInFeroxEnclave()) {
                log.debug("in ferox");
                if (HopWorld6HLog.shouldHop()) {
                    return HopWorld6HLog.hop();
                }

                if (!Switching.equip(nmDPSFulLSwapIDs)) {
                    return API.fastReturn();
                }
                //when in ferox enclave, refill from pool
                if (Skills.getBoostedLevel(Skill.PRAYER) < Skills.getLevel(Skill.PRAYER) || Skills.getBoostedLevel(Skill.HITPOINTS) < Skills.getLevel(Skill.HITPOINTS) || Movement.getRunEnergy() < 80) {
                    log.debug("need pool refill");
                    if (Bank.isOpen()) {
                        Bank.close();
                        Time.sleepTick();
                        return API.shortReturn();
                    }
                    TileObject feroxPool = getFeroxPool();
                    Log.log("Interact 'Drink' on ferox pool");
                    feroxPool.interact("Drink");
                    Time.sleepUntil(() -> Skills.getBoostedLevel(Skill.PRAYER) == Skills.getLevel(Skill.PRAYER) && Skills.getBoostedLevel(Skill.HITPOINTS) == Skills.getLevel(Skill.HITPOINTS), () -> Players.getLocal().isMoving() || Players.getLocal().isAnimating(), 100, 2000);
                    return API.shortReturn();
                }
                //eat anglerfish
                if (Skills.getBoostedLevel(Skill.HITPOINTS) <= Skills.getLevel(Skill.HITPOINTS)) {
                    log.debug("need anglerfish boost");
                    if (!Bank.isOpen()) {
                        return API.clickLocalBank();
                    }
                    if (!Inventory.contains(ItemID.ANGLERFISH)) {
                        if (Inventory.isFull()) {
                            Bank.depositInventory();
                            return API.returnTick();
                        }
                        Item ANGLERFISH = Bank.getFirst(ItemID.ANGLERFISH);
                        if (ANGLERFISH == null) {
                            Log.log("All out of ANGLERFISH :-(");
                            this.stop();
                            return -1;
                        }
                        API.bankWithdraw(ItemID.ANGLERFISH, 1, Bank.WithdrawMode.ITEM);
                        return API.returnTick();
                    }
                    if (eatTickTimer <= 0) {
                        Item angler = Bank.Inventory.getFirst(ItemID.ANGLERFISH);
                        angler.interact("Eat");
                        eatTickTimer = 3;
                    }
                    return API.returnTick();
                }

                //drink stamina
                if (!Movement.isStaminaBoosted()) {
                    log.debug("need staminated");
                    if (!Bank.isOpen()) {
                        return API.clickLocalBank();
                    }
                    if (!Inventory.contains(ItemID.STAMINA_POTION1)) {
                        if (Inventory.isFull()) {
                            Bank.depositInventory();
                            return API.returnTick();
                        }
                        Item stamina = Bank.getFirst(ItemID.STAMINA_POTION1);
                        if (stamina == null) {
                            Log.log("All out of stamina(1) doses:-(");
                            this.stop();
                            return -1;
                        }
                        API.bankWithdraw(ItemID.STAMINA_POTION1, 1, Bank.WithdrawMode.ITEM);
                        return API.returnTick();
                    }
                    if (drinkTickTimer <= 0) {
                        Item stamina = Bank.Inventory.getFirst(ItemID.STAMINA_POTION1);
                        stamina.interact("Drink");
                        drinkTickTimer = 3;
                    }
                    return API.returnTick();
                }
                if (!gear.fulfilled()) {
                    Log.log("Gear loadout fulfill start");
                    if (!gear.fulfill()) {
                        Log.log("Missing something from our required gearset, cannot continue killing da night mare :-( exiting script");
                        this.stop();
                        return API.shortReturn();
                    }
                    return API.shortReturn();
                }
                //gear fulfilled, we can teleport using ectophial
                if (Bank.isOpen()) {
                    Bank.close();
                    Time.sleepTick();
                    return API.shortReturn();
                }
                if (!Inventory.contains(i -> i.getId() == ItemID.ECTOPHIAL && i.hasAction("Empty"))) {
                    Log.log("Script error: no ectophial with fulfilled gear including it");
                    return API.shortReturn();
                }
                useNMTeleport();
                return API.shortReturn();
            }
            if (!fulfilledGear || !fulfilledEquipment || !haveEnoughDosesForAnotherFight) {
                if (!Bank.isOpen()) {
                    return API.clickLocalBank();
                }
                if (!gear.fulfill()) {
                    Log.log("Out of GEARRR!");
                    this.stop();
                }
                return API.shortReturn();
            }
        }
        //have GEAR
        if (Dialog.isEnterInputOpen()) {
            API.shortSleep();
            Keyboard.type("1", true);
        }
        if (walkToPhosanisLobby()) {
            if (Movement.isWalking() || Players.getLocal().isAnimating()) {
                return API.returnTick();
            }
            TileObject nightmarePool = TileObjects.getFirstAt(POOL_OF_NIGHTMARES, p -> p.getName().equals("Pool of Nightmares"));
            if (nightmarePool == null) {
                Log.log("Script error: In senga/nightmare pool zone but nightmare pool null");
                return API.returnTick();
            }
            nightmarePool.interact("Drink-from");
            return API.returnTick();
        }
        return API.shortReturn();
    }

    public void useNMTeleport() {
        Item ectophial = Inventory.getFirst(i -> i.getId() == ItemID.ECTOPHIAL && i.hasAction("Empty"));
        ectophial.interact("Empty");
        Time.sleepUntil(() -> refilledEctophial, () -> Players.getLocal().isAnimating(), 100, 8000);
    }

    public boolean walkToPhosanisLobby() {
        //walk on above ground to stairs
        if (NIGHTMARE_UNDERGROUND_WALK_ZONE.contains(Players.getLocal())) {
            Player hopAround = Players.getNearest(p -> p.distanceTo(Players.getLocal()) < 20 && !Players.getLocal().equals(p));
            // Walk underground to either sister senga or pool
            if (!inLobby) {
                if (hopAround != null) {
                    Log.log("Hopping around player: " + hopAround.getName() + "distance from us: " + hopAround.distanceTo(Players.getLocal()));
                    if (HopWorld6HLog.openWorldHopper()) {
                        HopWorld6HLog.hop();
                        return false;
                    }
                    //keep walking after opening world hopper menu alongside player
                }
                if (shouldWalk()) {
                    walkTo(POOL_OF_NIGHTMARES);
                    return false;
                }
                return false;
            }
            if (hopAround != null) {
                Log.log("Hopping around player: " + hopAround.getName() + "distance from us: " + hopAround.distanceTo(Players.getLocal()));
                if (HopWorld6HLog.openWorldHopper()) {
                    HopWorld6HLog.hop();
                    return false;
                }
                //stop walking after opening world hopper menu alongside player if in lobby
                return false;
            }
            if (Dialog.canContinue()) {
                API.shortSleep();
                Dialog.continueSpace();
                Time.sleepTick();
                return false;
            }
            if (Dialog.hasOption("Yes.")) {
                API.shortSleep();
                Dialog.chooseOption("Yes");
                Time.sleepTick();
                return false;
            }
			/*if (!haveTalkedToSisterSenga()) {
				NPC senga = NPCs.getNearest(n -> n.getWorldLocation().equals(SISTER_SENGA) && n.getName().equals("Sister Senga"));
				if (senga == null) {
					Log.log("Script error: In senga/nightmare pool zone but senga null");
					return API.shortReturn();
				}
				API.shortSleep();
				senga.interact("Talk-to");
				Time.sleepUntil(() -> Dialog.isOpen(), () -> Players.getLocal().isMoving(), 100, 2000);
				return API.shortReturn();
			}*/
            return true;
        }
        //somewhere in morytania eastern side
        if (NIGHTMARE_ABOVE_GROUND_WALK_ZONE.contains(Players.getLocal())) {
            TileObject stairs = TileObjects.getFirstSurrounding(ABOVE_GROUND_ENTRANCE, 4, g -> g.getName().equals("Stairs") && g.hasAction("Climb-down"));
            if (stairs == null || stairs.distanceTo(Players.getLocal().getWorldLocation()) > 18) {
                if (shouldWalk()) {
                    walkTo(ABOVE_GROUND_ENTRANCE_WALK);
                    return false;
                }
                return false;
            }
            stairs.interact("Climb-down");
            if (Time.sleepUntil(() -> NIGHTMARE_UNDERGROUND_WALK_ZONE.contains(Players.getLocal()), () -> Players.getLocal().isMoving() || Players.getLocal().isAnimating(), 100, 8000)) {
                Time.sleepTicks(2);
            }
            return false;
        }
        return false;
    }

    /**
     * Returns int to sleep after walking, or if no tiles can interact with, return -1
     * @param npc
     * @return
     */
    public int safelyWalkToInteractAttackMeleeNPC(NPC npc, List<Integer> fullSwapIDs, boolean spec, int minSpec, int specWep, List<Integer> fullSpecSwapIDs) {
        //see if we are in valid attackable range to NPC
        List<WorldPoint> interactableTiles = Reachable.getInteractable(npc);
        List<WorldPoint> surroundingNPC = new ArrayList<>(interactableTiles);
        surroundingNPC.removeAll(blacklist);
        surroundingNPC.removeIf(p -> !Reachable.isWalkable(p) || !whitelist.contains(p));
        //See if we can move into a valid tile for next tick attack near her
        List<WorldPoint> surroundingNPCToWalk = new ArrayList<>(interactableTiles);
        surroundingNPCToWalk.removeAll(blacklistWalk);

        if (surroundingNPC.size() > 0) {
            //check list for any tiles which distance <= 1 and directly east/west, if so, just attack
            List<WorldPoint> directAttackSurroundingNPC = surroundingNPC.stream().filter(t -> t.distanceTo(Players.getLocal()) <= 1 && isDirectCompassDirectionFromUs(true, t.getWorldLocation())).collect(Collectors.toList());
            //check all blacklist-removed interactable tiles checked for being directly east/west of us, need to also check non-blacklist-removed interactable tiles for same bc server dont care if shadows or not, gonna go east/west if interactable tile available there
            if (directAttackSurroundingNPC.isEmpty() && Reachable.getInteractable(npc).stream().noneMatch(t -> isDirectCompassDirectionFromUs(true, t))) {
                //have no option to attack east/west tile directly, check if have interaction tile 1 distance north/south
                //must check east/west first, then north/south, because this how the servers prioritize interaction requests on npcs when needing to path to interaction tile
                directAttackSurroundingNPC = surroundingNPC.stream().filter(t -> t.distanceTo(Players.getLocal()) <= 1 && isDirectCompassDirectionFromUs(false, t.getWorldLocation())).collect(Collectors.toList());
            }
            boolean directlyAttack = false;
            if (!directAttackSurroundingNPC.isEmpty()) {
                directlyAttack = true;
                surroundingNPC = directAttackSurroundingNPC;
            }
            surroundingNPC.sort(Comparator.comparingInt(t -> t.distanceTo(Players.getLocal())));
            WorldPoint npcClosestTile = null;
            if (!surroundingNPC.isEmpty()) {
                npcClosestTile = surroundingNPC.get(0);
            }
            if (directlyAttack) {
                int specPercent = Combat.getSpecEnergy();
                if (ourCurrentTarget == null || ourCurrentTarget.getTag() != npc.getTag()) {
                    log.debug("switch melee + attack "+npc.getName());

                    if (spec && specPercent < minSpec && !Switching.equip(fullSwapIDs)) {
                        return API.fastReturn();
                    }
                    if (npc.getId() == NIGHTMARE_SLEEPWALKER_TRANSITION) {
                        return API.returnTick();
                    }
                    npc.interact("Attack");
                    return API.returnTick();
                }
                if (spec) {
                    // interacting with nightmare - able to spec here if have spec
                    if (specPercent >= 100 && !Equipment.contains(specWep)) {
                        Log.log("switch spec weapon at spec >= 100");
                        if (!Switching.equip(fullSpecSwapIDs)) {
                            return API.fastReturn();
                        }
                        return API.returnTick();
                    }
                    if (specPercent >= minSpec && Equipment.contains(specWep)) {
                        Log.log("toggle spec at "+specPercent+"%");
                        Combat.toggleSpec();
                        return API.returnTick();
                    }
                }

                Switching.equip(fullSwapIDs);
                return API.returnTick();
            }
            if (npcClosestTile != null) {
                if (!Switching.equip(fullSwapIDs)) {
                    return API.fastReturn();
                }
                if (((!sporesWorld.isEmpty() || !shadowsPoints.isEmpty()) && ((Movement.isRunEnabled() && npcClosestTile.distanceTo(Players.getLocal()) > 1) || npcClosestTile.distanceTo(Players.getLocal()) > 2))) {
                    Log.log("No action this tick cos "+npc.getName()+" too far to walk in range to 1 tick");
                    return API.returnTick();
                }
                walk(npcClosestTile);
            }
            return API.returnTick();
        }
        return -1;
    }
    @Override
    public void onStart(String... strings) {

    }
    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        overlayManager.add(prayerOverlay);
        overlayManager.add(prayerInfoBox);
        overlayManager.add(sanfewInfoBox);
        log.debug("Startup melee gear:" + config.meleeGearText());
        log.debug("Startup mage gear:" + config.mageGearText());
        HopWorld6HLog.resetHopTick();
        getNotedIDs();
        LootTracker.onItemContainerChanged(null);
        reset();

        Log.log("TESTING: ENABLE DEATHWALK ON START");
        walkOfaShameHimHaha = true;
    }
    public void getNotedIDs() {
        if (allLoot.isEmpty()) {
            for (int i : Loot.notedLootz) {
                log.debug("Have noted item to add, unnoted ID: "+i);
                ItemComposition ic = client.getItemComposition(i);
                if (ic == null) {
                    log.debug("ItemComp null!!!");
                    continue;
                }
                log.debug("Item comp found! Name: "+ic.getName() + " linked ID: " +ic.getLinkedNoteId());
                allLoot.add(ic.getLinkedNoteId());
            }
            for (int i : Loot.lootz) {
                log.debug("Adding item: "+i);
                allLoot.add(i);
            }
        }
    }
    private static boolean isNoted(ItemComposition i) {
        return i.getNote() == 799;
    }
    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        overlayManager.remove(prayerOverlay);
        overlayManager.remove(prayerInfoBox);
        overlayManager.remove(sanfewInfoBox);
        HopWorld6HLog.hopTick = 0;
        reset();
    }

    @Provides
    NightmareConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NightmareConfig.class);
    }
    private void flick() {
        List<Prayer> prayersActive = new ArrayList<>();
        for (Prayer p : prayersToFlick) {
            if (Prayers.isEnabled(p)) {
                prayersActive.add(p);
            }
        }
        for (Prayer p : prayersActive) {
            Prayers.toggle(p);
            API.fastSleep();
            Prayers.toggle(p);
            // can only flick 1 prayer per tick, so just flick first one in list
            break;
        }
    }
    private void reset() {
        inFight = false;
        nm = null;
        pendingNightmareAttack = null;
        nightmareCharging = false;
        shadowsSpawning = false;
        cursed = false;
        flash = false;
        parasite = false;
        ticksUntilNextAttack = 0;
        ticksUntilParasite = 0;
        shadowsTicks = 0;
        totemsAlive = 0;
        totems.clear();
        spores.clear();
        sporesWorld.clear();
        shadows.clear();
        shadowsPoints.clear();
        husks.clear();
        huskTarget.clear();
        parasites.clear();
        parasiteTargets.clear();
        sleepwalkers.clear();
        refilledEctophial = false;
        walkTile.clear();
        blacklist.clear();
        blacklistWalk.clear();
        whitelist.clear();
        whitelistWalk.clear();
        prepareSleepwalkers = false;
        finalPhase = false;
        divinePotionExpiring = false;
        huskSwitchTicks = 0;
        killedASleepwalker = false;
        prepareAfterSleepwalkers = false;
        drowsy = false;
        walkOfaShameHimHaha = false;
    }

    @Subscribe
    private void onClientTick(ClientTick event) {
        API.waitClientTick = false;
    }
    @Subscribe
    private void onGameObjectSpawned(GameObjectSpawned event) {
        if (!inFight) {
            return;
        }

        GameObject gameObj = event.getGameObject();
        int id = gameObj.getId();
        if (id == NIGHTMARE_MUSHROOM || id == NIGHTMARE_PRE_MUSHROOM) {
            spores.put(gameObj.getLocalLocation(), gameObj);
            sporesWorld.add(gameObj.getWorldLocation());
        }

        if (id == NIGHTMARE_BLOSSOM || id == NIGHTMARE_BLOSSOM_BLOOM) {
            flowers.add(gameObj.getWorldLocation());
        }
    }

    /**
     * No-sleep walk direct to local region (need to ensure passed point is in local region)
     * @param p
     */
    public void walk(WorldPoint p) {
        walkTile.add(p);
        API.fastSleep();
        Movement.walk(p);
        return;
    }

    /**
     * sleep-handling walk to global map that resets randomized shouldWalk variable
     * @param p
     * @return
     */
    public int walkTo(WorldPoint p) {
        walkTile.add(p);
        API.fastSleep();
        Movement.walkTo(p);
        walkingRandomPoint = Rand.nextInt(0, 6);
        Time.sleepTicks(2);
        return API.shortReturn();
    }
    public boolean shouldWalk() {
        WorldPoint walkTileCurrent = null;
        for (WorldPoint p : walkTile) {
            walkTileCurrent = p;
            break;
        }
        if (walkTileCurrent == null) {
            return true;
        }
        if (walkTileCurrent.distanceTo(Players.getLocal().getWorldLocation()) <= walkingRandomPoint) {
            return true;
        }
        return false;
    }
    @Subscribe
    private void onGameObjectDespawned(GameObjectDespawned event) {
        if (!inFight) {
            return;
        }

        GameObject gameObj = event.getGameObject();
        int id = gameObj.getId();
        if (id == NIGHTMARE_MUSHROOM || id == NIGHTMARE_PRE_MUSHROOM) {
            spores.remove(gameObj.getLocalLocation());
            sporesWorld.remove(gameObj.getWorldLocation());
        }

        if (id == NIGHTMARE_BLOSSOM || id == NIGHTMARE_BLOSSOM_BLOOM) {
            if (!flowerSafeArea.isEmpty()) {
                flowerSafeArea.clear();
            }
            flowers.remove(gameObj.getWorldLocation());
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        if (!inFight) {
            return;
        }

        if (event.getGraphicsObject().getId() == NIGHTMARE_SHADOW) {
            shadows.add(event.getGraphicsObject());
            shadowsSpawning = true;
            shadowsTicks = 5;
            ticksUntilNextAttack = 5;
            shadowsPoints.put(WorldPoint.fromLocal(client, event.getGraphicsObject().getLocation()), 5);
        }
    }

    @Subscribe
    private void onProjectileSpawned(ProjectileSpawned event) {
        if (!inFight) {
            return;
        }

        var projectile = event.getProjectile();
        Player targetPlayer;
        switch (projectile.getId()) {
            case 1770:
                targetPlayer = (Player) projectile.getInteracting();
                parasiteTargets.putIfAbsent(targetPlayer.getId(), targetPlayer);
                break;
            case 1781:
                targetPlayer = (Player) projectile.getInteracting();
                huskTarget.putIfAbsent(targetPlayer.getCanvasTilePoly(), targetPlayer);
                huskSwitchTicks = 4;
                break;
            case 1768: //4 projectiles coming to nm to signal end of phase, start of sleepwalker transition
                if (!prepareSleepwalkers) {
                    Log.log("Preparing for sleepwalker spawn");
                    prepareSleepwalkers = true;
                }
                break;
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        Actor actor = event.getActor();
        if (!(actor instanceof NPC)) {
            if (actor instanceof Player && actor.getName() != null && actor.getName().equals(Players.getLocal().getName()) && actor.getAnimation() == PLAYER_TELEPORT_ANIMATION) {
                Log.log("Detected player teleporting - clearing arena tiles");
                arenaTiles.clear();
            }
            return;
        }

        NPC npc = (NPC) actor;

        // this will trigger once when the fight begins
        if (nm == null && npc.getName() != null && (npc.getName().equalsIgnoreCase("The Nightmare") || npc.getName().equalsIgnoreCase("Phosani's Nightmare")) && Players.getLocal().distanceTo(SISTER_SENGA) > 100) {
            Log.log("fight start");
            //reset everything
            reset();
            nm = npc;
            generateSquare(arenaTiles, nm.getWorldArea().getCenter(), 9);
            inFight = true;
        }

        if (!inFight || !npc.equals(nm)) {
            return;
        }

        int animationId = npc.getAnimation();

        if (animationId == NIGHTMARE_MAGIC_ATTACK) {
            log.debug("nightmare magic attack registered on tick: " + API.ticks + " (ticksUntilNextAttack=" + 7 + ")");
            ticksUntilNextAttack = 7;
            pendingNightmareAttack = cursed ? NightmareAttack.CURSE_MAGIC : NightmareAttack.MAGIC;
        } else if (animationId == NIGHTMARE_MELEE_ATTACK) {
            log.debug("nightmare melee attack registered on tick: " + API.ticks + " (ticksUntilNextAttack=" + 7 + ")");
            ticksUntilNextAttack = 7;
            pendingNightmareAttack = cursed ? NightmareAttack.CURSE_MELEE : NightmareAttack.MELEE;
        } else if (animationId == NIGHTMARE_RANGE_ATTACK) {
            log.debug("nightmare ranged attack registered on tick: " + API.ticks + " (ticksUntilNextAttack=" + 7 + ")");
            ticksUntilNextAttack = 7;
            pendingNightmareAttack = cursed ? NightmareAttack.CURSE_RANGE : NightmareAttack.RANGE;
        }
        // check if phosanis because the middle locations may be used in the others charge locations
        else if (animationId == NIGHTMARE_CHARGE && ((!isPhosanis(npc.getId()) && !MIDDLE_LOCATION.equals(npc.getLocalLocation())) || (isPhosanis(npc.getId()) && !PHOSANIS_MIDDLE_LOCATIONS.contains(npc.getLocalLocation())))) {
            log.debug("nightmare charge/surge/murder run attack registered on tick: " + API.ticks + " (ticksUntilNextAttack=" + 5 + ")");
            nightmareCharging = true;
            getNightmareChargeRange(npc);
            ticksUntilNextAttack = 5;
        }

        if (nightmareCharging && animationId != -1 && isPhosanis(npc.getId()) && animationId != NIGHTMARE_CHARGE) {
            log.debug("nightmare charge/surge/murder run ended on tick: " + API.ticks);
            nightmareCharging = false;
            murderRunTiles.clear();
        }

        if (animationId != NIGHTMARE_HUSK_SPAWN && !huskTarget.isEmpty()) {
            log.debug("husk target cleared on tick: " + API.ticks);
            huskTarget.clear();
        }

        if (animationId == NIGHTMARE_PARASITE_TOSS) {
            log.debug("parasite toss registered on tick: " + API.ticks);
            ticksUntilParasite = 27;

            if (config.parasitesInfoBox()) {
                Timer parasiteInfoBox = new Timer(16200L, ChronoUnit.MILLIS, itemManager.getImage(ItemID.PARASITIC_EGG), this);
                parasiteInfoBox.setTooltip("Parasites");
                infoBoxManager.addInfoBox(parasiteInfoBox);
            }
        }
        //prepare after sleepwalkers phase
        if (isPhosanis(npc.getId())) {
            if (animationId == NIGHTMARE_SLEEPWALKER_APPLYING_DAMAGE) {
                prepareAfterSleepwalkers = true;
            } else {
                prepareAfterSleepwalkers = false;
            }
        }
    }

    public List<WorldPoint> findNonCornerTiles(WorldArea area) {
        List<WorldPoint> nonCornerTiles = new ArrayList<>();

        // Iterate over each tile in the area
        for (WorldPoint tile : area.toWorldPointList()) {
            // Check if the tile is a corner tile
            if (!isCorner(tile, area)) {
                nonCornerTiles.add(tile);
            }
        }

        return nonCornerTiles;
    }

    private boolean isCorner(WorldPoint tile, WorldArea area) {
        int x = tile.getX();
        int y = tile.getY();

        // Get corner coordinates
        int minX = area.getX();
        int minY = area.getY();
        int maxX = minX + area.getWidth() - 1;
        int maxY = minY + area.getHeight() - 1;

        // Check if the tile is a corner
        return (x == minX && y == minY) || (x == minX && y == maxY) ||
                (x == maxX && y == minY) || (x == maxX && y == maxY);
    }

    private void getNightmareChargeRange(NPC nm) {
        murderRunTiles.clear();

        Set<WorldPoint> worldPoints = new HashSet<>();

        LocalPoint nmLocalPoint = nm.getLocalLocation();
        int nmX = nmLocalPoint.getX();
        int nmY = nmLocalPoint.getY();

        int offset = 1792;
        if (nmX == 6208 || nmX == 7232) {
            offset = 2048;
        }

        // Create a list of LocalPoints representing the charge path.
        List<LocalPoint> localPoints = new ArrayList<>();

        // facing west
        if (nmX == 5312 || nmX == 6336) {
            localPoints.add(new LocalPoint(nmX + offset + 256 + 64, nmY + 256 + 64));
            localPoints.add(new LocalPoint(nmX - 256 - 64, nmY + 256 + 64));
            localPoints.add(new LocalPoint(nmX - 256 - 64, nmY - 256 - 64));
            localPoints.add(new LocalPoint(nmX + offset + 256 + 64, nmY - 256 - 64));
        }
        // facing east
        else if (nmX == 7104 || nmX == 8128) {
            localPoints.add(new LocalPoint(nmX + 256 + 64, nmY + 256 + 64));
            localPoints.add(new LocalPoint(nmX - offset - 256 - 64, nmY + 256 + 64));
            localPoints.add(new LocalPoint(nmX - offset - 256 - 64, nmY - 256 - 64));
            localPoints.add(new LocalPoint(nmX + 256 + 64, nmY - 256 - 64));
        }
        // facing north
        else if (nmY == 8000 || nmY == 8128 || nmY == 9024 || nmY == 9152) {
            localPoints.add(new LocalPoint(nmX + 256 + 64, nmY + 256 + 64));
            localPoints.add(new LocalPoint(nmX - 256 - 64, nmY + 256 + 64));
            localPoints.add(new LocalPoint(nmX - 256 - 64, nmY - offset - 256 - 64));
            localPoints.add(new LocalPoint(nmX + 256 + 64, nmY - offset - 256 - 64));
        }
        // facing south
        else if (nmY == 6080 || nmY == 6208 || nmY == 7104 || nmY == 7232) {
            localPoints.add(new LocalPoint(nmX + 256 + 64, nmY + offset + 256 + 64));
            localPoints.add(new LocalPoint(nmX - 256 - 64, nmY + offset + 256 + 64));
            localPoints.add(new LocalPoint(nmX - 256 - 64, nmY - 256 - 64));
            localPoints.add(new LocalPoint(nmX + 256 + 64, nmY - 256 - 64));
        }

        // Convert LocalPoints to WorldPoints and add them to the set.
        for (LocalPoint localPoint : localPoints) {
            WorldPoint worldPoint = WorldPoint.fromLocal(client, localPoint);
            worldPoints.add(worldPoint);
        }

        // Get area filling the rectangle formed by diagonal max/mins of all points in charge range, then add these to murder run list
        generateRectangularArea(murderRunTiles, worldPoints);
    }
    private int getNotedID(int itemID) {
        ItemComposition c = client.getItemComposition(itemID);
        if (c == null) {
            Log.log("Not found item composition for itemID: "+itemID);
            return -1;
        }
        if (isNoted(c) || c.isStackable()) {
            return itemID;
        } else {
            return c.getLinkedNoteId();
        }
    }
    private int getUnnotedID(int itemID) {
        ItemComposition c = client.getItemComposition(itemID);
        if (c == null) {
            Log.log("Not found item composition for itemID: "+itemID);
            return -1;
        }
        if (!isNoted(c)) {
            return itemID;
        } else {
            return c.getLinkedNoteId();
        }
    }
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        //extract the difference in item qtys associated with ids by only tracking inventory
        for (Map.Entry<Integer, Integer> i : LootTracker.onItemContainerChanged(event).entrySet()) {
            int id = i.getKey();
            int qty = i.getValue();
            log.debug("Found change in invy! ID: "+id+" qty: " +qty);
            //only add value to total loot if bank is not open
            if (allLoot.contains(id) && !Bank.isOpen()) {
                int pricePer = itemManager.getItemPrice(id);
                String name = client.getItemComposition(id).getName();
                int totalPrice = pricePer * qty;
                Log.log("Picked up epic loot: "+qty+"x "+name+" ("+pricePer+"gp per = "+totalPrice+"gp total)");
                totalLootValue += totalPrice;
            }
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event) {
        final NPC npc = event.getNpc();

        if (npc.getName() != null && (npc.getName().equalsIgnoreCase("the nightmare") || npc.getName().equalsIgnoreCase("phosani's nightmare"))) {
            nm = npc;
        }

        //if npc is in the totems map, update its phase
        if (totems.containsKey(npc.getIndex())) {
            totems.get(npc.getIndex()).updateCurrentPhase(npc.getId());
        }
        if (INACTIVE_TOTEMS.contains(npc.getId())) {
            //else if the totem is not in the totem array and it is an inactive totem, add it to the totem map.
            totems.putIfAbsent(npc.getIndex(), new MemorizedTotem(npc));
            totemsAlive++;
        }
        if (ACTIVE_TOTEMS.contains(npc.getId())) {
            totemsAlive--;
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        if (!inFight) {
            return;
        }

        final NPC npc = event.getNpc();

        if (npc.getName() != null && npc.getName().equalsIgnoreCase("parasite")) {
            parasites.add(npc);
        }

        if (npc.getName() != null && npc.getName().equalsIgnoreCase("husk")) {
            huskMagicAttackTicks = 7;
            husks.add(npc);
        }

        if (npc.getName() != null && npc.getName().equalsIgnoreCase("sleepwalker")) {
            if (!sleepwalkers.contains(npc) && !finalPhase) {
                prepareSleepwalkers = false;
                log.debug("add spawned sleepwalker tag: " + npc.getTag());
                sleepwalkers.add(npc);
				sleepwalkersSpawned++;
            }
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        final NPC npc = event.getNpc();

        if (npc.getName() != null && npc.getName().equalsIgnoreCase("sleepwalker")) {
            if (sleepwalkers.contains(npc)) {
                log.debug("remove despawned sleepwalker tag: " + npc.getTag());
                sleepwalkers.remove(npc);
            }

        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        if (event.getActor() instanceof Player && event.getActor().getName() != null && event.getActor().getName().equals(Players.getLocal().getName()) ) {
            Log.log("We are DEAD! :-(");
            arenaTiles.clear();
            reset();
            walkOfaShameHimHaha = true;
        }
        if (event.getActor() instanceof NPC && event.getActor().getName() != null) {
            final NPC npc = (NPC) event.getActor();

            if (npc.getName() != null && npc.getName().equalsIgnoreCase("parasite")) {
                parasites.remove(npc);
            }

            if (npc.getName() != null && npc.getName().equalsIgnoreCase("husk")) {
                husks.remove(npc);
            }

            if (npc.getName() != null && npc.getName().equalsIgnoreCase("sleepwalker")) {
                if (sleepwalkers.contains(npc)) {
                    Log.log("remove despawned sleepwalker tag: " + npc.getTag());
                    sleepwalkers.remove(npc);
                }
            }
            if (npc.getName() != null && npc.getName().equalsIgnoreCase("phosani's nightmare")) {
                nmDiedTimer = Instant.now().plusSeconds(15);
                killCount++;
            }
        }
    }

    @Subscribe
    private void onChatMessage(ChatMessage event) {
        if (!inFight || nm == null || event.getType() != ChatMessageType.GAMEMESSAGE) {
            if (event.getType() != ChatMessageType.PLAYERRELATED ) {
                if (event.getMessage().toLowerCase().contains("you don't have enough inventory space.")) {
                    InventoryLoadout.itemQueue.clear();
                }
                if (event.getMessage().contains(drowsyMsg)) {
                    drowsy = true;
                }
                if (event.getMessage().contains(undrowsyMsg)) {
                    drowsy = false;
                }

                if (event.getMessage().contains("The Nightmare has reawoken!")) {
                    pokedNm = false;
                }
                if (event.getMessage().contains("The Nightmare is waking...")) {
                    pokedNm = true;
                }

                if (event.getMessage().contains("You refill the ectophial from the Ectofuntus.")) {
                    refilledEctophial = true;
                }
                if (event.getMessage().contains("Your divine potion effect is about to expire.")) {
                    divinePotionExpiring = false;
                }
            }

            if ((event.getMessage().contains("The Nightmare will awaken in 10 seconds!") || event.getMessage().contains("The Nightmare will awaken in 5 seconds!") || event.getMessage().contains("The Nightmare has awoken!"))) {
                if (!inFight) {
                    Log.log("Fight start");
                    inFight = true;
                }
            }
            return;
        }

        if (event.getMessage().contains("The Nightmare has impregnated you with a deadly parasite!")) {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer != null) {
                parasiteTargets.putIfAbsent(localPlayer.getId(), localPlayer);
            }

            flash = true;
            parasite = true;
            ticksUntilParasite = 22;
        }

        if (event.getMessage().toLowerCase().contains("the parasite within you has been weakened") || event.getMessage().toLowerCase().contains("the parasite bursts out of you, fully grown")) {
            parasite = false;
        }

        if (event.getMessage().toLowerCase().contains("the nightmare has cursed you, shuffling your prayers!")) {
            cursed = true;
        }

        if (event.getMessage().toLowerCase().contains("you feel the effects of the nightmare's curse wear off.")) {
            cursed = false;
        }

        if (config.yawnInfoBox() && event.getMessage().toLowerCase().contains("the nightmare's spores have infected you, making you feel drowsy!")) {
            Timer yawnInfoBox = new Timer(15600L, ChronoUnit.MILLIS, spriteManager.getSprite(SpriteID.SPELL_DREAM, 0), this);
            yawnInfoBox.setTooltip("Yawning");
            infoBoxManager.addInfoBox(yawnInfoBox);
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        GameState gamestate = event.getGameState();

        //if loading happens while inFight, the user has left the area (either via death or teleporting).
        if (gamestate == GameState.LOADING && inFight) {
            Log.log("Detected GameState == LOADING during fight! Must be dead or teleporting, clearing arena tiles");
            arenaTiles.clear();
            reset();
        }
    }
    @Subscribe
    private void onGameTick(final GameTick event) {
        API.ticks++;
        log.debug("Current tick: " + API.ticks);
        if (enablePrayDelay > 0) enablePrayDelay--;
        if (sleepwalkersSpawned > 0) {
            Log.log("Sleepwalker spawned: "+sleepwalkersSpawned);
        }
		if (sleepwalkersSpawned == 4) {
			finalPhase = true;
		}
		sleepwalkersSpawned = 0;
        ourLastTarget = ourCurrentTarget;
        ourCurrentTarget = Players.getLocal().getInteracting();
        if (!flowers.isEmpty() && flowerSafeArea.isEmpty()) {
            generateRectangularArea(flowerSafeArea, flowers);
        }
        walkTile.clear();
        walkTile.add(Movement.getDestination());
        if (eatTickTimer > 0) eatTickTimer--;
        if (drinkTickTimer > 0) drinkTickTimer--;

        if (!inFight || nm == null) {
            return;
        }

        //the fight has ended and everything should be reset
        if (nm.getId() == 378 || nm.getId() == 377) {
            reset();
        }
        if (huskMagicAttackTicks > 0) {
            huskMagicAttackTicks--;
        }

        ticksUntilNextAttack--;
        if (huskSwitchTicks > 0) {
            huskSwitchTicks--;
        }
        if (ticksUntilParasite > 0) {
            ticksUntilParasite--;
            if (ticksUntilParasite == 0) {
                parasiteTargets.clear();
            }
        }

        if (pendingNightmareAttack != null && ticksUntilNextAttack <= 3) {
            pendingNightmareAttack = null;
        }

        if (shadowsTicks > 0) {
            shadowsTicks--;
            if (shadowsTicks == 0) {
                shadowsSpawning = false;
                shadows.clear();
            }
        }
        shadowsPoints.replaceAll((k, v) -> v - 1);
        shadowsPoints.values().removeIf(v -> v == 0);
        updateBlacklistWhitelistTiles();
        if (xpGained) {
            //assume we one-shot the damn things since we can't even calculate if our dmg equals actor death, fuck u healthscale
            if (ourCurrentTarget != null) {
                if (ourCurrentTarget instanceof NPC && ourCurrentTarget.getName() != null) {
                    final NPC npc = (NPC) ourCurrentTarget;
                    if (sleepwalkers.stream().anyMatch(n -> n.getTag() == npc.getTag())) {
                        if (sleepwalkers.remove(npc)) {
                            log.debug("TAG MATCH - Removing sleepwalker from tracked list due to HP gained while attacking (one-shot assumption) - tick: " + API.ticks);
                        }
                    } else if (npc.getName().equalsIgnoreCase("sleepwalker")) {
                        log.debug("NAME MATCH - Removing sleepwalker from tracked list due to HP gained while attacking (one-shot assumption) - tick: " + API.ticks);
                        sleepwalkers.remove(npc);
                        killedASleepwalker = true;
                    } else if (npc.getName().equalsIgnoreCase("husk")) {
                        log.debug("Removing husk from tracked list due to HP gained while attacking (one-shot assumption) - tick: " + API.ticks);
                        husks.remove(npc);
                    } else if (npc.getName().equalsIgnoreCase("parasite")) {
                        log.debug("Removing parasite from tracked list due to HP gained while attacking (one-shot assumption) - tick: " + API.ticks);
                        parasites.remove(npc);
                    }
                }
            }
        }
		xpGained = false;
    }
    public void turnOffAllPrayers() {
        if (Prayers.anyActive()) {
            Log.log("Turning off some active prayers");
            Prayers.disableAll();
        }
    }
    public void turnOffOffensiveMeleePray() {
        if (Skills.getBoostedLevel(Skill.PRAYER) <= 0) {
            return;
        }
        for (Prayer p : prayersToFlick) {
            if (Prayers.isEnabled(p)) {
                Prayers.toggle(p);
                API.sleepClientTick();
            }
        }
        if (Prayers.isEnabled(Prayer.IMPROVED_REFLEXES)) {
            Prayers.toggle(Prayer.IMPROVED_REFLEXES);
            API.sleepClientTick();
        }
    }

    public void prayMeleeOffensive() {
        if (Skills.getBoostedLevel(Skill.PRAYER) <= 0) {
            return;
        }
        switch (config.offensivePrayer()) {
            case NOOB: {
                if (!Prayers.isEnabled(Prayer.ULTIMATE_STRENGTH)) {
                    Prayers.toggle(Prayer.ULTIMATE_STRENGTH);
                    API.sleepClientTick();
                }
                if (!Prayers.isEnabled(Prayer.IMPROVED_REFLEXES)) {
                    Prayers.toggle(Prayer.IMPROVED_REFLEXES);
                    API.sleepClientTick();
                }
                break;
            }
            case PIETY: {
                if (!Prayers.isEnabled(Prayer.PIETY)) {
                    Prayers.toggle(Prayer.PIETY);
                    API.sleepClientTick();
                }
                break;
            }
            case CHIVALRY: {
                if (!Prayers.isEnabled(Prayer.CHIVALRY)) {
                    Prayers.toggle(Prayer.CHIVALRY);
                    API.sleepClientTick();
                }
                break;
            }
        }
    }
    public List<WorldPoint> findClosestWhitelistedTilesSortedSpeciallyForTotems(List<WorldPoint> sortedWhitelist, NPC totemNPC) {
        //if we are already in range in whitelist tile dont need to walk
        int closestAttackRange = API.getClosestLocalAttackDistance(totemNPC);
		WorldPoint centerTotem = totemNPC.getWorldArea().getCenter();
        if (closestAttackRange <= 8 && centerTotem.distanceTo(Players.getLocal()) > 3 && sortedWhitelist.get(0).equals(Players.getLocal().getWorldLocation())) {
            //standing inside whitelist already
            return sortedWhitelist;
        }
        List<WorldPoint> tmpInRangeSortedWhitelist = new ArrayList<>(sortedWhitelist);
        List<WorldPoint> tmpSortedWhitelist = new ArrayList<>(sortedWhitelist);
        //remove tiles too close to totem to avoid pathing fuckery
        tmpInRangeSortedWhitelist.removeIf(p -> centerTotem.distanceTo(p) <= 3 || API.getClosestAttackDistance(totemNPC, p) > 8);

        // First, evaluate higher distance in the pair
        int higherDistance =  2;
        List<WorldPoint> higherInRangeDistanceTiles = tmpInRangeSortedWhitelist.stream()
                .filter(tile -> tile.distanceTo(Players.getLocal()) == higherDistance)
                .collect(Collectors.toList());

        if (!higherInRangeDistanceTiles.isEmpty()) {
            higherInRangeDistanceTiles.sort(Comparator.comparingDouble(tile -> API.getPythagoreanDistance(tile, centerTotem.getWorldLocation())));
            return higherInRangeDistanceTiles;
        }
        List<WorldPoint> higherDistanceTiles = tmpSortedWhitelist.stream()
                .filter(tile -> tile.distanceTo(Players.getLocal()) == higherDistance)
                .collect(Collectors.toList());
        if (!higherDistanceTiles.isEmpty()) {
            higherDistanceTiles.sort(Comparator.comparingDouble(tile -> API.getPythagoreanDistance(tile, centerTotem.getWorldLocation())));
            return higherDistanceTiles;
        }
        // If no tiles found for higher distance, evaluate lower distance in the pair
        int finalOuterdistance = 1;
        List<WorldPoint> lowerInRangeDistanceTiles = tmpInRangeSortedWhitelist.stream()
                .filter(tile -> tile.distanceTo(Players.getLocal()) == finalOuterdistance)
                .collect(Collectors.toList());

        if (!lowerInRangeDistanceTiles.isEmpty()) {
            lowerInRangeDistanceTiles.sort(Comparator.comparingDouble(tile -> API.getPythagoreanDistance(tile, centerTotem.getWorldLocation())));
            return lowerInRangeDistanceTiles;
        }

        List<WorldPoint> lowerDistanceTiles = tmpSortedWhitelist.stream()
                .filter(tile -> tile.distanceTo(Players.getLocal()) == higherDistance)
                .collect(Collectors.toList());
        if (!lowerDistanceTiles.isEmpty()) {
            lowerDistanceTiles.sort(Comparator.comparingDouble(tile -> API.getPythagoreanDistance(tile, centerTotem.getWorldLocation())));
            return lowerDistanceTiles;
        }
        return tmpSortedWhitelist;
    }

    public void getWhitelistedTiles(Set<WorldPoint> areaToModify) {
        areaToModify.clear();
        if (flowerSafeArea.isEmpty()) {
            areaToModify.addAll(arenaTiles);
            return;
        }
        areaToModify.addAll(flowerSafeArea);
    }

    private TileObject getFeroxPool() {
        return TileObjects.getFirstAt((Rand.nextInt(1, 100) > 66 ? FEROX_POOL_1 : FEROX_POOL_2), g -> g.hasAction("Drink") && g.getName().equals("Pool of Refreshment"));
    }



    public NPC getClosestTotem() {
        return totems.values().stream()
                .filter(t -> t.getNpc() != null && t.getNpc().getName() != null && t.getCurrentPhase().isActive() && (t.getCurrentPhase().getColor() == Color.ORANGE || t.getCurrentPhase().getColor() == Color.RED))
                .min(Comparator.comparingDouble(t -> calculateAngle(Players.getLocal().getWorldLocation(), t.getNpc().getWorldLocation())))
                .map(MemorizedTotem::getNpc)
                .orElse(null);
    }

    public void generateRectangularArea(Set<WorldPoint> areaToModify, Set<WorldPoint> edges) {
        areaToModify.clear();

        // Initialize min and max coordinates
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int plane = 0;

        // Find the min and max coordinates
        for (WorldPoint point : edges) {
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
            plane = point.getPlane();
        }

        // Populate the square
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                WorldPoint p = new WorldPoint(x, y, plane);
                if (Reachable.isWalkable(p)) {
                    areaToModify.add(p);
                }
            }
        }
    }

    public void generateSquare(Set<WorldPoint> areaToModify, WorldPoint center, int radius) {
        areaToModify.clear();
        int x = center.getX();
        int y = center.getY();
        int plane = center.getPlane();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                WorldPoint point = new WorldPoint(x + dx, y + dy, plane);
                if (Reachable.isWalkable(point)) {
                    areaToModify.add(point);
                }
            }
        }
    }

    public void updateBlacklistWhitelistTiles() {
        getBlacklistedTiles(blacklist, false);
        getBlacklistedTiles(blacklistWalk, true);
        getWhitelistedTiles(whitelist);
        whitelistWalk.clear();
        whitelistWalk.addAll(whitelist);

        // filter blacklist from whitelist to remove duplicates
        whitelist.removeAll(blacklist);
        whitelistWalk.removeAll(blacklistWalk);
    }

    public void getBlacklistedTiles(Set<WorldPoint> areaToModify, boolean walkShadowTiles) {
        areaToModify.clear();
        // Add mushroom (spore) tiles
        for (WorldPoint spore : sporesWorld) {
            int x = spore.getX();
            int y = spore.getY();
            int plane = spore.getPlane();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    areaToModify.add(new WorldPoint(x + dx, y + dy, plane));
                }
            }
        }
        if (walkShadowTiles && (nm.getAnimation() == NIGHTMARE_PRE_SHADOWS || nm.getAnimation() == NIGHTMARE_DURING_SHADOWS)) {
            // Add Nightmare tiles except for corner tiles when she spawning some
            areaToModify.addAll(findNonCornerTiles(nm.getWorldArea()));
        }
        if (!shadowsPoints.isEmpty()) {
            // Add Nightmare tiles except for corner tiles when shadows exactly present
            areaToModify.addAll(findNonCornerTiles(nm.getWorldArea()));
            // Add Shadow tiles
            log.debug("Have some shadow tiles to blacklist, global shadow tick = " + shadowsTicks);
            for (Map.Entry<WorldPoint, Integer> shadowPoint : shadowsPoints.entrySet()) {
                //ticks until dmg applied by this shadow - tick == 1 is when hitsplat appears, therefore need to step off at minimum tick == 2 - add safety ticks for lag
                if (shadowsTicks >= 2 && shadowsTicks <= 2 + config.lagSafetyTicks()) {
                    areaToModify.add(shadowPoint.getKey());
                }
                //add shadow-countdown tiles that are too near to dmg hitsplat to walking blacklist too
                else if (walkShadowTiles && shadowsTicks >= 2 && shadowsTicks <= 3 + config.lagSafetyTicks()) {
                    areaToModify.add(shadowPoint.getKey());
                }
            }
        }


        // Add murder run tiles
        areaToModify.addAll(murderRunTiles);
    }

    public void killSleepwalker() {
        NPC closestSleepwalker = sleepwalkers.stream()
                .filter(npc -> npc.hasAction("Attack"))
                .max(Comparator.comparingInt(npc -> npc.distanceTo(Players.getLocal())))
                .orElse(null);
        if (closestSleepwalker == null) {
            Log.log("null NPC found of tracked SLEEPWALKERS with action ATTACK");
            return;
        }

        turnOffOffensiveMeleePray();

        if (!Switching.equip(config.rangedWeapon())) {
            return;
        }

        if (ourCurrentTarget != null && ourCurrentTarget.getTag() == closestSleepwalker.getTag()) {
            return;
        }
        closestSleepwalker.interact("Attack");
        Log.log("interacted attack on sleepwalker");
    }

    public void killHusks(List<Integer> swapGear) {
        NPC closestHusk = husks.stream()
                .min(Comparator.comparingInt(npc -> npc.getId()))
                .orElse(null);
        if (closestHusk == null) {
            Log.log("null NPC found of tracked HUSKS");
            return;
        }
        prayMeleeOffensive();
        if (!Switching.equip(swapGear)) {
            return;
        }

        if (ourCurrentTarget != null && ourCurrentTarget.getTag() == closestHusk.getTag()) {
            return;
        }
        API.shortSleep();
        closestHusk.interact("Attack");
        Log.log("interacted attack on husk");
        attackedHusk = true;
    }

    public int killParasite(NPC closestParasite, List<Integer> swapGear) {


        prayMeleeOffensive();

        if (closestParasite.isMoving() && (!flowers.isEmpty() || !shadowsPoints.isEmpty() || !sporesWorld.isEmpty())) {
            Log.log("Ignoring MOVING parasite when spores/shadows/flowers! Fucker");
            return API.returnTick();
        }
        int result = safelyWalkToInteractAttackMeleeNPC(closestParasite, swapGear, false, 0, 0, null);
        if (result == -1) {
            Log.log("No safetiles found around parasite :o");
            return API.returnTick();
        }
        return result;
    }

    @Subscribe
    public void onExperienceGain(ExperienceGained xpDrop) {
        if (xpDrop.getSkill() == Skill.HITPOINTS || xpDrop.getSkill() == Skill.RANGED) {
            xpGained = true;
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (!inFight || nm == null || event.getMenuAction() != MenuAction.NPC_SECOND_OPTION) {
            return;
        }

        String target = Text.removeTags(event.getTarget()).toLowerCase();

        if ((target.contains("the nightmare") || target.contains("phosani's nightmare"))
                && ((config.hideAttackNightmareTotems() && totemsAlive > 0)
                || (config.hideAttackNightmareParasites() && parasites.size() > 0)
                || (config.hideAttackNightmareHusk() && husks.size() > 0)
                || (config.hideAttackNightmareSleepwalkers() && nm.getId() != 11154 && sleepwalkers.size() > 0))
                || (config.hideAttackSleepwalkers() && nm.getId() == 11154 && target.contains("sleepwalker"))) {
            client.setMenuOptionCount(client.getMenuOptionCount() - 1);
        }
    }

    private boolean isPhosanis(int id) {
        return (id >= 9416 && id <= 9424) || (id >= 11153 && id <= 11155);
    }
}
