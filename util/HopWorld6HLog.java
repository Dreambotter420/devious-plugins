package net.runelite.client.plugins.nightmare.util;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.World;
import net.unethicalite.api.commons.Rand;
import net.unethicalite.api.game.Worlds;
import net.unethicalite.api.input.Keyboard;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.GrandExchange;
import net.unethicalite.api.items.Trade;
import net.unethicalite.api.widgets.Dialog;
@Slf4j
public class HopWorld6HLog
{
	public static long hopTick = 0;

	public static boolean checkHopTick() {
		return API.ticks >= hopTick;
	}

	public static void resetHopTick() {
		// Reset timer for 2-4 hours from now in ticks based on current tick
		hopTick = API.ticks + Rand.nextInt((int) (60 * 60 * 2 / 0.6D), (int) (60 * 60 * 4 / 0.6D));
	}

	public static boolean openWorldHopper() {
		if (Bank.isOpen()) {
			API.shortSleep();
			Bank.close();
			return false;
		}
		if (GrandExchange.isOpen()) {
			API.shortSleep();
			GrandExchange.close();
		}
		if (Dialog.canContinue()) {
			API.shortSleep();
			Dialog.continueSpace();
			return false;
		}
		if (Dialog.isViewingOptions()) {
			API.shortSleep();
			Keyboard.type("1");
			return false;
		}
		if (Dialog.isEnterInputOpen()) {
			API.shortSleep();
			Keyboard.sendEnter();
			return false;
		}
		if (Trade.isOpen()) {
			API.shortSleep();
			Trade.accept();
			return false;
		}
		if (Worlds.isHopperOpen()) {
			return true;
		}
		API.shortSleep();
		Worlds.openHopper();
		return Worlds.isHopperOpen();
	}

	public boolean validate() { return checkHopTick(); }

	public int execute()
	{
		if (!openWorldHopper()) {
			return API.shortReturn();
		}
		int oldWorld = Worlds.getCurrentId();
		World randWorld = Worlds.getRandom(w -> w.isMembers() &&
				w.isNormal() &&
				!w.isAllPkWorld() &&
				!w.isLeague() &&
				!w.isSkillTotal() &&
				!w.getActivity().toLowerCase().contains("fresh start") &&
				!w.isQuestSpeedRunning() &&
				!w.isTournament());
		int randWorldID = randWorld.getId();
		log.info("Hopping from: "+oldWorld+" to: "+randWorldID);
		Worlds.hopTo(randWorld);
		API.shortSleep();
		if (Worlds.getCurrentId() != oldWorld) {
			log.info("Hop successful!");
			resetHopTick();
		} else {
			log.info("Hop failed!");
		}
		return API.shortReturn();
	}
}
