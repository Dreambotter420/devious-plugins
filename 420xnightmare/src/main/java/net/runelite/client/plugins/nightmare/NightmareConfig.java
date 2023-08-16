/*
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
package net.runelite.client.plugins.nightmare;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.ItemID;
import net.runelite.client.config.*;
import net.runelite.client.plugins.nightmare.util.loadouts.LoadoutItem;

@ConfigGroup("fourtwentynightmare")
public interface NightmareConfig extends Config
{

	@ConfigSection(
			name = "Bot",
			description = "Configure Bot settings.",
			position = 0,
			keyName = "botSection"
	)
	String botSection = "Bot - Beep Boop";
	@ConfigSection(
			name = "Gear",
			description = "Configure Gear settings.",
			position = 1,
			keyName = "gearSection"
	)
	String gearSection = "Gear - Beep Boop";
	@ConfigSection(
			name = "Bank",
			description = "Configure Bank settings.",
			position = 2,
			keyName = "bankSection"
	)
	String bankSection = "Bank - Beep Boop";
	@ConfigSection(
			name = "Overlay General",
			description = "Configure overlay general settings.",
			position = 3,
			closedByDefault = true,
			keyName = "generalSection"
	)
	String generalSection = "General";

	@ConfigSection(
		name = "Totems",
		description = "Configure Totems settings.",
		position = 4,
		closedByDefault = true,
		keyName = "totemsSection"
	)
	String totemsSection = "Totems";

	@ConfigSection(
		name = "Shadows",
		description = "Configure Shadows settings.",
		position = 5,
		closedByDefault = true,
		keyName = "shadowsSection"
	)
	String shadowsSection = "Shadows";

	@ConfigSection(
		name = "Spores",
		description = "Configure Spores settings.",
		position = 6,
		closedByDefault = true,
		keyName = "sporesSection"
	)
	String sporesSection = "Spores";

	@ConfigSection(
		name = "Parasites",
		description = "Configure Parasites settings.",
		position = 7,
		closedByDefault = true,
		keyName = "parasitesSection"
	)
	String parasitesSection = "Parasites";

	@ConfigSection(
		name = "Husk",
		description = "Configure Husk settings.",
		position = 8,
		closedByDefault = true,
		keyName = "huskSection"
	)
	String huskSection = "Husk";

	@ConfigSection(
		name = "Charge",
		description = "Configure Charge settings.",
		position = 9,
		closedByDefault = true,
		keyName = "chargeSection"
	)
	String chargeSection = "Charge";

	//BOT SECTION
	@ConfigItem(
			position = 0,
			keyName = "startButton",
			name = "Script Toggle (CLICK TO BOT!)",
			description = "When this is on, bot actions take place",
			section = botSection
	)
	default boolean startButton()
	{
		return false;
	}
	@Range(
			min = 1,
			max = 98
	)
	@ConfigItem(
			position = 10,
			keyName = "healSetpoint",
			name = "Anglerfish HP Setpoint",
			description = "When hitpoints <= this value then eat anglerfish",
			section = botSection
	)
	default int healSetpoint()
	{
		return 90;
	}
	@Range(
			min = 1,
			max = 98
	)
	@ConfigItem(
			position = 20,
			keyName = "prayerSetpoint",
			name = "Prayer Setpoint",
			description = "When prayer <= this value then drink prayer pot or sanfew serum",
			section = botSection
	)
	default int prayerSetpoint()
	{
		return 16;
	}
	@Range(
			min=0,
			max=2
	)
	@ConfigItem(
			position = 30,
			keyName = "lagSafetyTicks",
			name = "Lag Safety Ticks (Grasping Claws)",
			description = "Amount of ticks prior to hitsplat of shadows to step off them",
			section = botSection
	)
	default int lagSafetyTicks() {return 1;}
	@ConfigItem(
			position = 40,
			keyName = "Offensive Prayer",
			name = "Offensive Prayer",
			description = "Prayer to be offensive with",
			section = botSection
	)
	default OffensivePrayer offensivePrayer() {return OffensivePrayer.NOOB;}

	//GEAR SECTION
	@ConfigItem(
			position = 0,
			keyName = "meleeGearText",
			name = "Melee Gear/Swaps",
			description = "Newline-seperated list of melee gear IDs to swap into",
			section = gearSection
	)
	default String meleeGearText()
	{
		return ItemID.SERPENTINE_HELM+"\n"+
				ItemID.AMULET_OF_TORTURE+"\n"+
				ItemID.FIRE_CAPE+"\n"+
				ItemID.ABYSSAL_BLUDGEON+"\n"+
				ItemID.OBSIDIAN_PLATEBODY+"\n"+
				ItemID.OBSIDIAN_PLATELEGS+"\n"+
				ItemID.PRIMORDIAL_BOOTS+"\n"+
				ItemID.GRANITE_GLOVES+"\n"+
				ItemID.BRIMSTONE_RING+"\n"+
				ItemID.ANCIENT_BLESSING+"\n";
	}
	@ConfigItem(
			position = 2,
			keyName = "mageGearText",
			name = "Mage Gear/Swaps",
			description = "Newline-seperated list of mage gear IDs to swap into",
			section = gearSection
	)
	default String mageGearText()
	{
		return ItemID.SERPENTINE_HELM+"\n"+
				ItemID.OCCULT_NECKLACE+"\n"+
				ItemID.IMBUED_GUTHIX_CAPE+"\n"+
				ItemID.TRIDENT_OF_THE_SWAMP+"\n"+
				ItemID.OBSIDIAN_PLATEBODY+"\n"+
				ItemID.OBSIDIAN_PLATELEGS+"\n"+
				ItemID.PRIMORDIAL_BOOTS+"\n"+
				ItemID.TORMENTED_BRACELET+"\n"+
				ItemID.BRIMSTONE_RING+"\n"+
				ItemID.ANCIENT_BLESSING+"\n";
	}
	@ConfigItem(
			position = 4,
			keyName = "rangedWeapon",
			name = "Sleepwalker Ranged Weapon",
			description = "Ranged weapon to swap to for sleepwalkers.",
			section = gearSection
	)
	default int rangedWeapon()
	{
		return ItemID.TOXIC_BLOWPIPE;
	}

	@ConfigItem(
			position = 5,
			keyName = "specWeapon",
			name = "Special Weapon",
			description = "Weapon ID to spec with.",
			section = gearSection
	)
	default int specWeapon()
	{
		return ItemID.GRANITE_MAUL_24225;
	}
	@Range(
			min = 10,
			max = 100
	)
	@ConfigItem(
			position = 6,
			keyName = "minSpec",
			name = "Minimum Special Attack Percentage",
			description = "Set according to how much weapon uses per attack.",
			section = gearSection
	)
	default int minSpec()
	{
		return 50;
	}
	@ConfigItem(
			position = 7,
			keyName = "huskSwitch",
			name = "Husk Swap/Gear",
			description = "Enter ID for weapon to switch into for Husks",
			section = gearSection
	)
	default int huskSwitchID()
	{
		return ItemID.GOBLIN_PAINT_CANNON;
	}
	@ConfigItem(
			position = 8,
			keyName = "parasiteSwitch",
			name = "Parasite Switch Weapon ID",
			description = "Enter ID for weapon to switch into for Parasite",
			section = gearSection
	)
	default int parasiteSwitchID()
	{
		return ItemID.ELDER_MAUL;
	}
	@ConfigItem(
			position = 8,
			keyName = "nmSwitch",
			name = "Nightmare Main Weapon ID",
			description = "Enter ID for weapon to switch into for Nightmare DPS",
			section = gearSection
	)
	default int nmSwitch()
	{
		return ItemID.ABYSSAL_BLUDGEON;
	}

	//BANK SECTION

	@ConfigItem(
			position = 0,
			keyName = "sanfewWithdrawCount",
			name = "Withdraw Sanfew",
			description = "Enter number of Sanfew Serums to withdraw when re-gearing",
			section = bankSection
	)
	default int sanfewWithdrawCount()
	{
		return 4;
	}
	@ConfigItem(
			position = 1,
			keyName = "anglerWithdrawCount",
			name = "Withdraw Anglerfish",
			description = "Enter number of Anglerfish to withdraw when re-gearing",
			section = bankSection
	)
	default int anglerWithdrawCount()
	{
		return 11;
	}

	@ConfigItem(
			position = 2,
			keyName = "superCombatWithdrawCount",
			name = "Withdraw Supr Combat",
			description = "Enter number of Divine Super Combat Potions to withdraw when re-gearing",
			section = bankSection
	)
	default int superCombatWithdrawCount()
	{
		return 2;
	}
	@ConfigItem(
			position = 3,
			keyName = "prayerWithdrawCount",
			name = "Withdraw Prayer",
			description = "Enter number of Prayer Potions to withdraw when re-gearing",
			section = bankSection
	)
	default int prayerWithdrawCount()
	{
		return 1;
	}
	@ConfigItem(
			position = 4,
			keyName = "rekillMinSanfew",
			name = "Rekill Min Sanfew",
			description = "Enter number of DOSES Sanfew Serums to have when re-killing",
			section = bankSection
	)
	default int rekillMinSanfew()
	{
		return 6;
	}
	@ConfigItem(
			position = 5,
			keyName = "rekillMinPray",
			name = "Rekill Min Pray",
			description = "Enter number of DOSES Prayer + Sanfew Potions to have when re-killing",
			section = bankSection
	)
	default int rekillMinPray()
	{
		return 8;
	}
	@ConfigItem(
			position = 6,
			keyName = "rekillMinCombat",
			name = "Rekill Min Combat",
			description = "Enter number of DOSES Divine Super Combat Potions to have when re-killing",
			section = bankSection
	)
	default int rekillMinCombat()
	{
		return 4;
	}
	@ConfigItem(
			position = 7,
			keyName = "rekillMinAngler",
			name = "Rekill Min Angler",
			description = "Enter number of ANGLERFISH to have when re-killing",
			section = bankSection
	)
	default int rekillMinAngler()
	{
		return 5;
	}

	//GENERAL SECTION
	@ConfigItem(
		keyName = "prayerHelper",
		name = "Prayer helper",
		description = "Displays the correct prayer to use at various points in the fight.",
		position = 0,
		section = generalSection,
		enumClass = PrayerDisplay.class
	)
	default PrayerDisplay prayerHelper()
	{
		return PrayerDisplay.BOTH;
	}

	@ConfigItem(
		keyName = "tickCounter",
		name = "Show Ticks",
		description = "Displays the number of ticks until next attack",
		position = 1,
		section = generalSection
	)
	default boolean ticksCounter()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideAttackNightmareTotems",
		name = "Hide Attack during Totems",
		description = "Remove the attack option on Nightmare during Totems",
		position = 2,
		section = generalSection
	)
	default boolean hideAttackNightmareTotems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAttackNightmareParasites",
		name = "Hide Attack during Parasites",
		description = "Remove the attack option on Nightmare during Parasites",
		position = 3,
		section = generalSection
	)
	default boolean hideAttackNightmareParasites()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAttackNightmareHusk",
		name = "Hide Attack during Husks",
		description = "Remove the attack option on Nightmare during Husks",
		position = 4,
		section = generalSection
	)
	default boolean hideAttackNightmareHusk()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAttackNightmareSleepwalkers",
		name = "Hide Attack during Sleepwalkers",
		description = "Remove the attack option on Nightmare during Sleepwalkers (not on last phase of Phosanis)",
		position = 5,
		section = generalSection
	)
	default boolean hideAttackNightmareSleepwalkers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAttackSleepwalkers",
		name = "Hide Attack Sleepwalkers Last Phase",
		description = "Remove the attack option on Sleepwalkers during the last phase of Phosanis",
		position = 6,
		section = generalSection
	)
	default boolean hideAttackSleepwalkers()
	{
		return false;
	}

	//TOTEMS SECTION
	@ConfigItem(
		keyName = "highlightTotems",
		name = "Highlight Totems",
		description = "Highlights Totems based on their status",
		position = 0,
		section = totemsSection
	)
	default boolean highlightTotems()
	{
		return true;
	}

	@Range(
		min = 1,
		max = 10
	)
	@ConfigItem(
		name = "Totem Outline Size",
		description = "Change the size of the totem outline.",
		position = 1,
		keyName = "totemOutlineSize",
		section = totemsSection,
		hidden = true,
		unhide = "highlightTotems"
	)
	@Units(Units.POINTS)
	default int totemOutlineSize()
	{
		return 3;
	}

	//SHADOWS SECTION
	@ConfigItem(
		keyName = "highlightShadows",
		name = "Highlight Shadows",
		description = "Highlights the Shadow Attacks",
		position = 0,
		section = shadowsSection
	)
	default boolean highlightShadows()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shadowsTickCounter",
		name = "Shadows Tick Counter",
		description = "Displays the number of ticks until shadows do damage",
		position = 1,
		section = shadowsSection
	)
	default boolean shadowsTickCounter()
	{
		return true;
	}

	@Range(
		max = 20,
		min = 1
	)
	@ConfigItem(
		keyName = "shadowsRenderDistance",
		name = "Render Distance",
		description = "Render shadows distance in tiles from your player",
		position = 2,
		section = shadowsSection
	)
	@Units("tiles")
	default int shadowsRenderDistance()
	{
		return 5;
	}

	@Alpha
	@ConfigItem(
		keyName = "shadowsBorderColour",
		name = "Shadows border colour",
		description = "Colour the edges of the area highlighted by shadows",
		position = 3,
		section = shadowsSection
	)
	default Color shadowsBorderColour()
	{
		return new Color(0, 255, 255, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "shadowsColour",
		name = "Shadows colour",
		description = "Colour for shadows highlight",
		position = 4,
		section = shadowsSection
	)
	default Color shadowsColour()
	{
		return new Color(0, 255, 255, 50);
	}

	//SPORES SECTION
	@ConfigItem(
		keyName = "highlightSpores",
		name = "Highlight Spores",
		description = "Highlights spores that will make you yawn",
		position = 0,
		section = sporesSection
	)
	default boolean highlightSpores()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "poisonBorderCol",
		name = "Poison border colour",
		description = "Colour the edges of the area highlighted by poison special will be",
		position = 1,
		section = sporesSection
	)
	default Color poisonBorderCol()
	{
		return new Color(255, 0, 0, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "poisonCol",
		name = "Poison colour",
		description = "Colour the fill of the area highlighted by poison special will be",
		position = 2,
		section = sporesSection
	)
	default Color poisonCol()
	{
		return new Color(255, 0, 0, 50);
	}

	@ConfigItem(
		keyName = "yawnInfoBox",
		name = "Yawn InfoBox",
		description = "InfoBox telling you the time until your yawning ends",
		position = 3,
		section = sporesSection
	)
	default boolean yawnInfoBox()
	{
		return true;
	}

	//PARASITES SECTION
	@ConfigItem(
		keyName = "showTicksUntilParasite",
		name = "Indicate Parasites",
		description = "Displays a red tick timer on any impregnated players",
		position = 0,
		section = parasitesSection
	)
	default boolean showTicksUntilParasite()
	{
		return true;
	}

	@ConfigItem(
		keyName = "parasitesInfoBox",
		name = "Parasites InfoBox",
		description = "InfoBox telling you the time until parasites",
		position = 1,
		section = parasitesSection
	)
	default boolean parasitesInfoBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sanfewReminder",
		name = "Sanfew Reminder",
		description = "Overlay that reminds you to drink a sanfew when impregnated",
		position = 2,
		section = parasitesSection
	)
	default boolean sanfewReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flash",
		name = "Flash screen when impregnated",
		description = "Your Screen flashes when the nightmare infects you with her parasite",
		position = 3,
		section = parasitesSection
	)
	default boolean flash()
	{
		return false;
	}

	//HUSK SECTION
	@ConfigItem(
		keyName = "highlightHusk",
		name = "Highlight Husk",
		description = "Highlights the mage and range husk",
		position = 0,
		section = huskSection
	)
	default boolean huskHighlight()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightHuskTarget",
		name = "Highlight Husk Target(s)",
		description = "Highlights whoever the husks will spawn on",
		position = 1,
		section = huskSection
	)
	default boolean highlightHuskTarget()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "huskBorderCol",
		name = "Husk Target Border Color",
		description = "Colour the edges of the area highlighted by poison special will be",
		position = 2,
		section = huskSection
	)

	default Color huskBorderCol()
	{
		return new Color(255, 0, 0, 100);
	}

	//CHARGE SECTION
	@ConfigItem(
		keyName = "highlightNightmareHitboxOnCharge",
		name = "Highlight Nightmare's Hitbox On Charge",
		description = "Highlights the hitbox of the Nightmare when she charges",
		position = 0,
		section = chargeSection
	)
	default boolean highlightNightmareHitboxOnCharge()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightNightmareChargeRange",
		name = "Highlight Nightmare's Charge Range",
		description = "Highlights the range the Nightmare will damage you with her charge attack",
		position = 1,
		section = chargeSection
	)
	default boolean highlightNightmareChargeRange()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "nightmareChargeBorderCol",
		name = "Nightmare Charge Border Color",
		description = "Color the edges of the area highlighted by the nightmare's charge attack",
		position = 2,
		section = chargeSection
	)

	default Color nightmareChargeBorderCol()
	{
		return new Color(255, 0, 0, 100);
	}

	@Alpha
	@ConfigItem(
		keyName = "nightmareChargeCol",
		name = "Nightmare charge fill color",
		description = "Color the fill of the area highlighted by the nightmare's charge attack",
		position = 3,
		section = chargeSection
	)
	default Color nightmareChargeCol()
	{
		return new Color(255, 0, 0, 50);
	}

	enum PrayerDisplay
	{
		PRAYER_TAB,
		BOTTOM_RIGHT,
		BOTH;

		public boolean showInfoBox()
		{
			switch (this)
			{
				case BOTTOM_RIGHT:
				case BOTH:
					return true;
				default:
					return false;
			}
		}

		public boolean showWidgetHelper()
		{
			switch (this)
			{
				case PRAYER_TAB:
				case BOTH:
					return true;
				default:
					return false;
			}
		}
	}
}
