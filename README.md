# devious-plugins
Plugins I make for Devious Client

1. Fight Cave plugin. 99.9% Credit goes to xKylee for the plugin <https://github.com/xKylee/plugins-source/blob/master/fightcave>, I just found the trigger in the code to flick prayer and added code to enable prayers with Devious API - also it drinks pots (sara brew, super restore, ranged pots)

2. Phosani's nightmare plugin. Credit goes to xKylee for base structure of plugin which I added onto: <https://github.com/xKylee/plugins-source/tree/master/nightmare>. Kills Phosani's nightmare after you prepare the account with levels, gear, supplies. Currently requiring ectophial / ring of dueling / sanfew serum / prayer potion (optional) / anglerfish, but can set custom gear swaps in config. Script goes to ferox enclave to regear so start there, and it will do the rest until you run out of supplies. If you take damage during any point in the fight except parasite spawn (5hp) and final phase constant 15-dmg, make issue on this github project and I will look at it

**Not-To-Do-Yet List** for Phosani's Nightmare:
-Consider ammo/gear degradation for needing to not rekill
-Buy more supplies
-Put more ammo in things like toxic blowpipe and powered staffs

3. Basic P2P runecrafting plugin (fire altar). Will maybe add air altar later and more fun things to this one, but found <https://github.com/maikel233/devious-plugins/tree/main/xRunecrafting> and found it was really bad plugin, so took some stuff from it and made new script. Requires rings of dueling, and optionally stamina potion(1), and pure essense and fire tiara, and will make fire runes.



# Utils

Methods and stuff that make developing scripts easier. These can be found in various scripts integrated in each one and some maybe have discrepancies between files like API.java between plugins.

**API**

Provides some more methods that are similar in utility to some Devious API calls.

**BankCache**

Keeps record of all items in bank, call method <update()> when bank is open to update. OwnedItems uses BankCache.

**OwnedItems**

Convenience method to check Equipment, Inventory, and Bank for items or get total count.

**Loadouts**

Methods to withdraw items from bank into inventory and equipment. If using just Inventory, use InventoryLoadout, or just equipment use EquipmentLoadout, or if both use Loadout. Instantiate new Loadout(LoadoutItem... items) where you also instantiate new LoadoutItem() with ID and quantity. and call .fullilled() to check if it's fulfilled already, and call fulfill() to execute a fulfill update. Operation is short-returned; fulfill() method should be called multiple times if fulfilled() returns false as each call progresses up to 10 actions per tick towards loadout fulfillment then returns.

(Still working on Loadouts - probably not clearing old loadouts if using multiple loadouts in same script, and also noted items are bugged and not supported temporarily)

PS sry Burak wouldn't let me dev for Storm Client so :c 
