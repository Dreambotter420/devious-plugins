/*
 * Copyright (c) 2017, Robin Weymans <Robin.weymans@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.unethicalite.scripts.kebabs;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.commons.Rand;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Equipment;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.Walker;
import net.unethicalite.api.plugins.Script;
import net.unethicalite.api.widgets.Tab;
import net.unethicalite.api.widgets.Tabs;

@Slf4j
@PluginDescriptor(
		name = "420xmaikel233xRunecrafting",
		description = "A runecrafting plugin to make our lifes easier!",
		enabledByDefault = false,
		tags = {"skilling", "automatisation", "xhook"}
)
public class xRunecraftingPlugin extends Script
{
	@Inject
	private Client client;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private Notifier notifier;
	@Inject
	private xRunecraftingConfig config;
	@Inject
	private xRunecraftingOverlay overlay;

	@Inject
	private xRunecraftingUtils utils;

	@Provides
	xRunecraftingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(xRunecraftingConfig.class);
	}

	public String CurrentTaskStatus = "Initializing...";
	public int TotalTrips = 0, CurrentXP = 0, startXP = 0;

	public long start;

	@Override
	protected void startUp() {
		overlayManager.add(overlay);
		CurrentXP = 0;
		start = System.currentTimeMillis();
		startXP = client.getSkillExperience(Skill.RUNECRAFT);
		TotalTrips = 0;
		API.ticks = 0;
		HopWorld6HLog.hopTick = 0;

	}
	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		HopWorld6HLog.hopTick = 0;
		API.ticks = 0;
	}
	@Subscribe
	public void onGameTick(GameTick event) {

		if (client.getGameState() != GameState.LOGGED_IN) {
			return;
		}
		if (client.getRealSkillLevel(Skill.RUNECRAFT) < config.xSettings().getRequiredLevel()) {
			log.info("Runecrafting lvl: " + config.xSettings().getRequiredLevel() + " is required!");
			this.stop();
			return;
		}
		API.ticks++;
		CurrentXP = Math.abs(startXP - client.getSkillExperience(Skill.RUNECRAFT));
	}

	@Override
	protected int loop() {
		Time.sleep(10,125);

		if (HopWorld6HLog.shouldHop()) {
			CurrentTaskStatus = "Hopping to another world";
			return HopWorld6HLog.hop();
		}
		TileObject inAltarRock = TileObjects.getNearest(config.xSettings().getInsideAltarWorldPoint(), config.xSettings().getAltarInsideID());
		if (inAltarRock != null) {
			if (Inventory.contains(ItemID.PURE_ESSENCE)) {
				log.info("Interact Craft-rune on inside altar");
				CurrentTaskStatus = "Interact 'Craft-rune' on Inside Altar";
				inAltarRock.interact("Craft-rune");
				Time.sleepTick();
				if (Time.sleepUntil(() -> !Inventory.contains(ItemID.PURE_ESSENCE), () -> Players.getLocal().isMoving() || Players.getLocal().isAnimating(), 100, 2000)) {
					return -2;
				}
				return -1;
			}
			//need to teleport out here
			if (!teleportBack()) {
				log.info("We screwed here no duel rings out lol");
				return -1;
			}
			return 10;
		}
		int distToOutsideAltar = config.xSettings().getOutsideAltarWorldPoint().distanceTo(Players.getLocal());
		if (distToOutsideAltar < 50) {
			if (!Inventory.contains(ItemID.PURE_ESSENCE)) {
				if (!teleportBack()) {
					log.info("We screwed here no duel rings out lol");
					return -1;
				}
				return 10;
			}
			TileObject outsideAltar = TileObjects.getFirstSurrounding(config.xSettings().getOutsideAltarWorldPoint(), 4, config.xSettings().getAltarOutsideID());

			if (distToOutsideAltar < 15 && outsideAltar != null) {
				log.info("Interact Enter on outside portal");
				CurrentTaskStatus = "Interact 'Enter' on Outside Portal";
				outsideAltar.interact("Enter");
				if (Time.sleepUntil(() ->
						Players.getLocal().distanceTo(config.xSettings().getInsideAltarWorldPoint()) < 30,
						() -> Players.getLocal().isMoving() || Players.getLocal().isAnimating(),
						100,
						2000
				)) {
					return 10;
				}
				return -1;
			}
			if (!Movement.isWalking()) {
				Movement.walkTo(config.xSettings().getOutsideAltarWorldPoint());
			}
			CurrentTaskStatus = "Walking to Outside Portal";
			return -2;
		}
		int distToBank = Players.getLocal().distanceTo(config.xSettings().getBankWorldPoint());
		if (distToBank < 50) {
			if (Bank.isOpen()) {
				BankCache.update();
				int leastDuelCharge = Jewelry.getLeastDuelingChargeID();
				if (leastDuelCharge == -1) {
					log.info("Not any more rings of dueling left! Or least only 1-dose left, f that");
					this.stop();
					return -1;
				}
				if (!Equipment.contains(leastDuelCharge)) {
					if (!Inventory.contains(leastDuelCharge)) {
						CurrentTaskStatus = "Withdraw fresh Dueling Ring";
						API.withdrawItem(leastDuelCharge, 1, false);
						return -1;
					}
					CurrentTaskStatus = "Wear fresh Dueling Ring";
					Inventory.getFirst(leastDuelCharge).interact("Wear");
					return -1;
				}
				if (Inventory.contains(ItemID.PURE_ESSENCE)) {
					log.info("close bank");
					CurrentTaskStatus = "Closing Bank";
					Bank.close();
					return -1;
				}
				Time.sleep(25,125);
				if (config.useStaminas() && Movement.getRunEnergy() <= config.staminaSetpoint()) {
					if (Inventory.contains(ItemID.STAMINA_POTION1)) {
						CurrentTaskStatus = "Drink Stamina(1)";
						Inventory.getFirst(ItemID.STAMINA_POTION1).interact("Drink");
						return -1;
					} else if (Bank.contains(ItemID.STAMINA_POTION1)) {
						CurrentTaskStatus = "Withdraw Stamina(1)";
						API.withdrawItem(ItemID.STAMINA_POTION1,1,false);
						return -1;
					}
					log.info("Ignoring config due to no staminas 1-dose left! Buy some more noob");
				}
				CurrentTaskStatus = "Deposit Inventory";
				Bank.depositInventory();
				if (!Bank.contains(ItemID.PURE_ESSENCE)) {
					log.info("No more pure ess :o");
					this.stop();
					return -2;
				}
				CurrentTaskStatus = "Withdraw-28 Pure Essence";
				API.withdrawItem(ItemID.PURE_ESSENCE, 28, false);
				if (Time.sleepUntil(() -> Inventory.getCount(true, ItemID.PURE_ESSENCE) > 0, 100, 2000)) {
					return 10;
				}
				return -2;
			}
			if (Inventory.contains(ItemID.PURE_ESSENCE) && Equipment.contains(Jewelry.ringIds)) {
				Item ring = Equipment.getFirst(Jewelry.ringIds);
				if (ring != null) {
					log.info("Teleport PvP Arena");
					CurrentTaskStatus = "Teleporting to PvP arena";
					ring.interact("PvP Arena");
					if (Time.sleepUntil(
							() -> Players.getLocal().getWorldLocation().distanceTo(config.xSettings().getOutsideAltarWorldPoint()) < 50,
							() ->  Players.getLocal().isAnimating() || Players.getLocal().isMoving(),
							100,
							2000
					)) {
						return 10;
					}
					return -2;
				}
				log.info("Script discrepancy, no duels found but also found");
				return -1;
			}
			TileObject bank = TileObjects.getFirstSurrounding(config.xSettings().getBankWorldPoint(), 4, config.xSettings().getBankID());
			if (distToBank < 15 && bank != null) {
				log.info("Interact with bank");
				CurrentTaskStatus = "Interacting with Bank";
				bank.interact(config.xSettings().getBankInteractionAction());
				if (Time.sleepUntil(() ->
								Bank.isOpen(),
						() -> Players.getLocal().isMoving() || Players.getLocal().isAnimating(),
						100,
						2000
				)) {
					return 10;
				}
				return -1;
			}
			if (!Movement.isWalking()) {
				Movement.walkTo(config.xSettings().getBankWorldPoint());
			}
			CurrentTaskStatus = "Walking to Bank";
			return -2;
		}

		return -1;
	}
	boolean teleportBack() {
		Item dueling = Equipment.getFirst(Jewelry.allRingIds);
		if (dueling != null && dueling.getName() != null) {
			log.info("Teleport castle wars");
			dueling.interact("Castle Wars");
			CurrentTaskStatus = "Teleporting to Castle Wars";
			if (Time.sleepUntil(() -> config.xSettings().getBankWorldPoint().distanceTo(Players.getLocal()) < 50, () -> Players.getLocal().isMoving() || Players.getLocal().isAnimating(), 100, 2000)) {
				TotalTrips++;
				return true;
			}
			Time.sleepTick();
			return true;
		}
		return false;
	}
	@Override
	public void onStart(String... strings) {

	}
}
