# devious-plugins
Plugins I make for Devious Client

Right now its just Fight Cave plugin. 99.9% Credit goes to xKylee for the plugin <https://github.com/xKylee/plugins-source/blob/master/fightcave> , I just found the trigger in the code to flick prayer and added code to enable prayers with Devious API - also it drinks pots (sara brew, super restore, ranged pots at hardcoded lvls, probably should change those)

# Utils

Methods and stuff that make developing scripts easier. 

**BankCache**

Keeps record of all items in bank, call method <update()> when bank is open to update. OwnedItems uses BankCache.

**OwnedItems**

Convenience method to check Equipment, Inventory, and Bank for items or get total count.

**Loadouts**

Methods to withdraw items from bank into inventory and equipment. If using just Inventory, use InventoryLoadout, or just equipment use EquipmentLoadout, or if both use Loadout. Instantiate new Loadout(LoadoutItem... items) where you also instantiate new LoadoutItem() with ID and quantity. and call .fullilled() to check if it's fulfilled already, and call fulfill() to execute a fulfill update. Operation is short-returned; fulfill() method should be called multiple times if fulfilled() returns false as each call progresses up to 10 actions per tick towards loadout fulfillment then returns.

(Still working on Loadouts - probably not clearing old loadouts if using multiple loadouts in same script, and also noted items are bugged and not supported temporarily)

PS sry Burak wouldn't let me dev for Storm Client so :c 
