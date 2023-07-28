/*
 * Copyright (c) 2018, Jordan Atwood <jordan.atwood423@gmail.com>
 * Copyright (c) 2019, Ganom <https://github.com/Ganom>
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
package net.runelite.client.plugins.fightcave;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.Interactable;
import net.unethicalite.api.commons.Rand;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.game.Skills;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.widgets.Prayers;
import org.apache.commons.lang3.ArrayUtils;
import org.pf4j.Extension;
@Slf4j
@Extension
@PluginDescriptor(
	name = "420xFight Cave",
	enabledByDefault = false,
	description = "Displays current and upcoming wave monsters in the Fight Caves and what to pray at TzTok-Jad",
	tags = {"bosses", "combat", "minigame", "overlay", "pve", "pvm", "jad", "fire", "cape", "wave"}
)
public class FightCavePlugin extends Plugin
{
	static final int MAX_WAVE = 63;
	@Getter(AccessLevel.PACKAGE)
	static final List<EnumMap<WaveMonster, Integer>> WAVES = new ArrayList<>();
	private static final Pattern WAVE_PATTERN = Pattern.compile(".*Wave: (\\d+).*");
	private static final int FIGHT_CAVE_REGION = 9551;
	private static final int MAX_MONSTERS_OF_TYPE_PER_WAVE = 2;

	static
	{
		final WaveMonster[] waveMonsters = WaveMonster.values();

		// Add wave 1, future waves are derived from its contents
		final EnumMap<WaveMonster, Integer> waveOne = new EnumMap<>(WaveMonster.class);
		waveOne.put(waveMonsters[0], 1);
		WAVES.add(waveOne);

		for (int wave = 1; wave < MAX_WAVE; wave++)
		{
			final EnumMap<WaveMonster, Integer> prevWave = WAVES.get(wave - 1).clone();
			int maxMonsterOrdinal = -1;

			for (int i = 0; i < waveMonsters.length; i++)
			{
				final int ordinalMonsterQuantity = prevWave.getOrDefault(waveMonsters[i], 0);

				if (ordinalMonsterQuantity == MAX_MONSTERS_OF_TYPE_PER_WAVE)
				{
					maxMonsterOrdinal = i;
					break;
				}
			}

			if (maxMonsterOrdinal >= 0)
			{
				prevWave.remove(waveMonsters[maxMonsterOrdinal]);
			}

			final int addedMonsterOrdinal = maxMonsterOrdinal >= 0 ? maxMonsterOrdinal + 1 : 0;
			final WaveMonster addedMonster = waveMonsters[addedMonsterOrdinal];
			final int addedMonsterQuantity = prevWave.getOrDefault(addedMonster, 0);

			prevWave.put(addedMonster, addedMonsterQuantity + 1);

			WAVES.add(prevWave);
		}
	}

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WaveOverlay waveOverlay;

	@Inject
	private FightCaveOverlay fightCaveOverlay;

	@Getter(AccessLevel.PACKAGE)
	private Set<FightCaveContainer> fightCaveContainer = new HashSet<>();
	@Getter(AccessLevel.PACKAGE)
	private int currentWave = -1;
	@Getter(AccessLevel.PACKAGE)
	private boolean validRegion;
	@Getter(AccessLevel.PACKAGE)
	private List<Integer> mageTicks = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private List<Integer> rangedTicks = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private List<Integer> meleeTicks = new ArrayList<>();
	@Getter(AccessLevel.PACKAGE)
	private boolean mageSpawned;
	@Getter(AccessLevel.PACKAGE)
	private boolean rangedSpawned;
	@Getter(AccessLevel.PACKAGE)
	private boolean meleeSpawned;

	public static int drinkTickTimeout = 0;

	public static final int TZTOK_JAD_RANGE_ATTACK = 2652;
	public static final int TZTOK_JAD_MELEE_ATTACK = 2655;
	public static final int TZTOK_JAD_MAGIC_ATTACK = 2656;
	public static final int TOK_XIL_RANGE_ATTACK = 2633;
	public static final int TOK_XIL_MELEE_ATTACK = 2628;
	public static final int KET_ZEK_MELEE_ATTACK = 2644;
	public static final int KET_ZEK_MAGE_ATTACK = 2647;
	public static final int MEJ_KOT_MELEE_ATTACK = 2637;
	public static final int MEJ_KOT_HEAL_ATTACK = 2639;

	static String formatMonsterQuantity(final WaveMonster monster, final int quantity)
	{
		return String.format("%dx %s", quantity, monster);
	}

	@Provides
	FightCaveConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FightCaveConfig.class);
	}
	@Inject
	FightCaveConfig config;
	@Override
	public void startUp()
	{
		if (client.getGameState() == GameState.LOGGED_IN && regionCheck())
		{
			validRegion = true;
			overlayManager.add(waveOverlay);
			overlayManager.add(fightCaveOverlay);
		}
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(waveOverlay);
		overlayManager.remove(fightCaveOverlay);
		currentWave = -1;
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (!validRegion)
		{
			return;
		}

		final Matcher waveMatcher = WAVE_PATTERN.matcher(event.getMessage());

		if (event.getType() != ChatMessageType.GAMEMESSAGE || !waveMatcher.matches())
		{
			return;
		}

		currentWave = Integer.parseInt(waveMatcher.group(1));
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (regionCheck())
		{
			validRegion = true;
			overlayManager.add(waveOverlay);
			overlayManager.add(fightCaveOverlay);
		}
		else
		{
			validRegion = false;
			overlayManager.remove(fightCaveOverlay);
			overlayManager.remove(fightCaveOverlay);
		}

		fightCaveContainer.clear();
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		if (!validRegion)
		{
			return;
		}

		NPC npc = event.getNpc();

		switch (npc.getId())
		{
			case NpcID.KETZEK:
			case NpcID.KETZEK_3126:
			{
				if (isWithinAttackingRange(npc, 15)) {
					log.info("predict mage attack from spawn in range");
					mageSpawned = true;
				}
				fightCaveContainer.add(new FightCaveContainer(npc));
				break;
			}

			case NpcID.TOKXIL_3121:
			case NpcID.TOKXIL_3122:
			{
				if (isWithinAttackingRange(npc, 15)) {
					log.info("predict range attack from spawn in range");
					rangedSpawned = true;
				}
				fightCaveContainer.add(new FightCaveContainer(npc));
				break;
			}
			case NpcID.YTMEJKOT:
			case NpcID.YTMEJKOT_3124:
			{
				if (isWithinAttackingRange(npc, 1)) {
					log.info("predict melee attack from spawn in range");
					meleeSpawned = true;
				}
				fightCaveContainer.add(new FightCaveContainer(npc));
				break;
			}
			case NpcID.TZTOKJAD:
			case NpcID.TZTOKJAD_6506:
				fightCaveContainer.add(new FightCaveContainer(npc));
				break;
		}
	}
	public boolean isWithinAttackingRange(NPC npc, int distance) {
		Set<WorldPoint> checkedTiles = new HashSet<>();

		List<WorldPoint> interactableTiles = Reachable.getInteractable(npc);
		WorldArea ourArea = Players.getLocal().getWorldArea();
		WorldPoint ourTile = Players.getLocal().getWorldLocation();
		int shortestDistance = Integer.MAX_VALUE;
		for (WorldPoint tile : interactableTiles) {
			if (!checkedTiles.contains(tile)) {
				if (ourArea.hasLineOfSightTo(client, tile) && tile.distanceTo2D(ourTile) <= distance) {
					int currentDistance = tile.distanceTo2D(ourTile);
					if (currentDistance < shortestDistance) {
						shortestDistance = currentDistance;
					}
				}
				checkedTiles.add(tile);
			}
		}

		return shortestDistance <= distance;
	}
	public int getClosestAttackDistance(NPC npc) {
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

	public WorldPoint findNearestSafeTile(List<NPC> npcs) {
		List<List<WorldPoint>> npcMeleeTiles = npcs.stream().map(Reachable::getInteractable).collect(Collectors.toList());

		// Adding the tiles that the NPCs occupy
		for (NPC npc : npcs) {
			List<WorldPoint> npcLocation = npc.getWorldArea().toWorldPointList();
			npcMeleeTiles.add(npcLocation);
		}

		WorldPoint playerPoint = Players.getLocal().getWorldLocation();

		// Create a 10 tiles radius square centered on the player.
		WorldArea searchArea = new WorldArea(playerPoint.getX() - 10, playerPoint.getY() - 10, 20, 20, playerPoint.getPlane());

		WorldPoint nearestSafeTile = null;
		int shortestDistance = Integer.MAX_VALUE;

		for (int x = searchArea.getX(); x < searchArea.getX() + searchArea.getWidth(); x++) {
			for (int y = searchArea.getY(); y < searchArea.getY() + searchArea.getHeight(); y++) {
				WorldPoint tile = new WorldPoint(x, y, playerPoint.getPlane());

				// Check if the tile is within the melee range of any NPC.
				boolean isInInteractableRange = npcMeleeTiles.stream().anyMatch(tiles -> tiles.contains(tile));
				if (isInInteractableRange) {
					continue;
				}

				int currentDistance = tile.distanceTo2D(playerPoint);
				if (currentDistance < shortestDistance) {
					shortestDistance = currentDistance;
					nearestSafeTile = tile;
				}
			}
		}

		return nearestSafeTile;
	}


	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		if (!validRegion)
		{
			return;
		}

		NPC npc = event.getNpc();

		switch (npc.getId())
		{
			case NpcID.TOKXIL_3121:
			case NpcID.TOKXIL_3122:
			case NpcID.YTMEJKOT:
			case NpcID.YTMEJKOT_3124:
			case NpcID.KETZEK:
			case NpcID.KETZEK_3126:
			case NpcID.TZTOKJAD:
			case NpcID.TZTOKJAD_6506:
				fightCaveContainer.removeIf(c -> c.getNpc() == npc);
				break;
		}
	}
	public static int ticks = 0;
	@Subscribe
	private void onGameTick(GameTick Event)
	{
		if (!validRegion)
		{
			return;
		}
		ticks++;
		mageTicks.clear();
		rangedTicks.clear();
		meleeTicks.clear();

		for (FightCaveContainer npc : fightCaveContainer)
		{
			if (npc.getTicksUntilAttack() >= 0)
			{
				npc.setTicksUntilAttack(npc.getTicksUntilAttack() - 1);
			}

			for (int anims : npc.getAnimations())
			{
				if (anims == npc.getNpc().getAnimation())
				{
					if (npc.getTicksUntilAttack() < 1)
					{
						npc.setTicksUntilAttack(npc.getAttackSpeed());
					}

					switch (anims)
					{
						case TZTOK_JAD_RANGE_ATTACK:
							npc.setAttackStyle(FightCaveContainer.AttackStyle.RANGE);
							if (!Prayers.isEnabled(Prayer.PROTECT_FROM_MISSILES)) {
								shortSleep();
								Prayers.toggle(Prayer.PROTECT_FROM_MISSILES);
								shortSleep();
							}
							break;
						case TZTOK_JAD_MAGIC_ATTACK:
							npc.setAttackStyle(FightCaveContainer.AttackStyle.MAGE);
							if (!Prayers.isEnabled(Prayer.PROTECT_FROM_MAGIC)) {
								shortSleep();
								Prayers.toggle(Prayer.PROTECT_FROM_MAGIC);
								shortSleep();
							}
							break;
						case TZTOK_JAD_MELEE_ATTACK:
							npc.setAttackStyle(FightCaveContainer.AttackStyle.MELEE);
							if (!Prayers.isEnabled(Prayer.PROTECT_FROM_MELEE)) {
								shortSleep();
								Prayers.toggle(Prayer.PROTECT_FROM_MELEE);
								shortSleep();
							}
							break;
					}
				}
			}
			switch (npc.getNpc().getId()) {
				case NpcID.TOKXIL_3121:
				case NpcID.TOKXIL_3122: {
					int attackingDistance = getClosestAttackDistance(npc.getNpc());
					if (attackingDistance == 16 && npc.getNpc().isMoving()) {
						log.info("predict ranged attack from incoming into range");
						rangedTicks.add(1);
					}
					break;
				}
				case NpcID.KETZEK:
				case NpcID.KETZEK_3126: {
					int attackingDistance = getClosestAttackDistance(npc.getNpc());
					if (attackingDistance == 16 && npc.getNpc().isMoving()) {
						log.info("predict magic attack from incoming into range");
						mageTicks.add(1);
					}
					break;
				}
				case NpcID.YTMEJKOT:
				case NpcID.YTMEJKOT_3124: {
					int attackingDistance = getClosestAttackDistance(npc.getNpc());
					if (attackingDistance == 1 && npc.getNpc().isMoving()) {
						log.info("predict melee attack from incoming into range");
						meleeTicks.add(1);
					}
					break;
				}
			}
			if (!npc.getNpcName().equals("TzTok-Jad") && !npc.getNpc().isDead())
			{
				switch (npc.getAttackStyle())
				{
					case RANGE:
						if (npc.getTicksUntilAttack() > 0)
						{
							rangedTicks.add(npc.getTicksUntilAttack());
						}
						break;
					case MELEE:
						if (npc.getTicksUntilAttack() > 0)
						{
							meleeTicks.add(npc.getTicksUntilAttack());
						}
						break;
					case MAGE:
						if (npc.getTicksUntilAttack() > 0)
						{
							mageTicks.add(npc.getTicksUntilAttack());
						}
						break;
				}

				Collections.sort(mageTicks);
				Collections.sort(rangedTicks);
				Collections.sort(meleeTicks);
			}
		}
		boolean needToggle = true;
		if ((!mageTicks.isEmpty() && mageTicks.get(0) == 1) || mageSpawned) {
			enablePrayer(Prayer.PROTECT_FROM_MAGIC, ticks);
			mageSpawned = false;
		} else if ((!rangedTicks.isEmpty() && rangedTicks.get(0) == 1) || mageSpawned) {
			enablePrayer(Prayer.PROTECT_FROM_MISSILES, ticks);
			rangedSpawned = false;
		} else if (!meleeTicks.isEmpty() && meleeTicks.get(0) == 1 || meleeSpawned) {
			enablePrayer(Prayer.PROTECT_FROM_MELEE, ticks);
			meleeSpawned = false;
		} else {
			needToggle = false;
		}

		//check if we need to move off mager or ranger tiles
		List<NPC> npcList = fightCaveContainer.stream()
				.map(FightCaveContainer::getNpc)
				.filter(npc -> npc.getId() == NpcID.TOKXIL_3121
						|| npc.getId() == NpcID.TOKXIL_3122
						|| npc.getId() == NpcID.KETZEK
						|| npc.getId() == NpcID.KETZEK_3126
						|| npc.getId() == NpcID.TZTOKJAD
						|| npc.getId() == NpcID.TZTOKJAD_6506)
				.collect(Collectors.toList());
		WorldPoint safeTile = findNearestSafeTile(npcList);
		if (safeTile != null && !safeTile.equals(Players.getLocal().getWorldLocation())) {
			log.info("UNSAFE TILE! WOAH");
			shortSleep();
			Movement.walk(safeTile);
		}

		if (config.drinkSara() && Skills.getBoostedLevel(Skill.HITPOINTS) <= config.healSetpoint() && drinkTickTimeout < 0) {
			if (!drinkingSara) {
				saraDosesToStopAt = getAllSaraDoses() - 3;
				if (saraDosesToStopAt < 0) saraDosesToStopAt = 0;
				drinkingSara = true;
			}
			Item saraBrew = Inventory.getFirst(ItemID.SARADOMIN_BREW1,ItemID.SARADOMIN_BREW2,ItemID.SARADOMIN_BREW3,ItemID.SARADOMIN_BREW4);
			if (saraBrew != null && calculateSaraBoost() != Skills.getBoostedLevel(Skill.HITPOINTS) && getAllSaraDoses() > saraDosesToStopAt) {
				shortSleep();
				saraBrew.interact("Drink");
				drinkTickTimeout = 3;
			} else {
				drinkingSara = false;
			}
		}
		if (((config.drinkRestore() && Skills.getBoostedLevel(Skill.PRAYER) < 33) || (config.drinkSara() && Skills.getBoostedLevel(Skill.RANGED) < Skills.getLevel(Skill.RANGED))) && drinkTickTimeout < 0) {
			Item superRestore = Inventory.getFirst(ItemID.SUPER_RESTORE1,ItemID.SUPER_RESTORE2,ItemID.SUPER_RESTORE3,ItemID.SUPER_RESTORE4);
			if (superRestore != null) {
				shortSleep();
				superRestore.interact("Drink");
				drinkTickTimeout = 3;
			}
		}
		if (config.drinkRanging() && Skills.getBoostedLevel(Skill.RANGED) == Skills.getLevel(Skill.RANGED) && drinkTickTimeout < 0) {
			Item ranging = Inventory.getFirst(ItemID.RANGING_POTION1, ItemID.RANGING_POTION2, ItemID.RANGING_POTION3, ItemID.RANGING_POTION4);
			if (ranging != null) {
				shortSleep();
				ranging.interact("Drink");
				drinkTickTimeout = 3;
			}
		}

		//if no prayers enabled this tick, preserve existing prayer when config enabled, otherwise turn off existing prayers
		if (!needToggle) {
			flick(ticks);
		}

		if (drinkTickTimeout >= -5) drinkTickTimeout--;
	}
	public static boolean drinkingSara = false;
	public static int saraDosesToStopAt = 0;
	public static int calculateSaraBoost() {
		return (int)(Math.floor(Skills.getLevel(Skill.HITPOINTS) * 0.15)) + 2;
	}
	public static int getAllSaraDoses() {
		return Inventory.getCount(ItemID.SARADOMIN_BREW1) +
				Inventory.getCount(ItemID.SARADOMIN_BREW2) * 2 +
				Inventory.getCount(ItemID.SARADOMIN_BREW3) * 3 +
				Inventory.getCount(ItemID.SARADOMIN_BREW4) * 4;
	}
	public void enablePrayer(Prayer prayer, int originTick) {
		Prayer overhead = getOverhead();
		if (overhead == null || overhead != prayer) {
			shortSleep();
			Prayers.toggle(prayer);
		} else if (config.flick()) {
			flick(originTick);
		}
	}
	public Prayer getOverhead() {
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
	public void flick(int originTick) {
		Prayer overhead = getOverhead();
		if (config.flick()) {
			if (overhead != null) {
				log.info("flicking ENABLED");
				shortSleep();
				if (ticks > originTick) return;
				Prayers.toggle(overhead);
				shortSleep();
				if (ticks > originTick) return;
				Prayers.toggle(overhead);
			}
		} else {
			log.info("flick DISABLED");
		}
	}
	public static void shortSleep() {
		Time.sleep(50,200);
	}
	private boolean regionCheck()
	{
		return ArrayUtils.contains(client.getMapRegions(), FIGHT_CAVE_REGION);
	}
}
