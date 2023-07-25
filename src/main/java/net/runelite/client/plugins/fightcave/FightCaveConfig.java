/*
 * Copyright (c) 2018, Jordan Atwood <jordan.atwood423@gmail.com>
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

import java.awt.Font;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("fightcave")
public interface FightCaveConfig extends Config
{
	@ConfigItem(
			position = 0,
			keyName = "flick",
			name = "Flick Prayers",
			description = "The prayers will be flicked, otherwise leaves last prayer on",
			section = text
	)
	default boolean flick() { return false; }
	@ConfigSection(
		keyName = "text",
		position = 1,
		name = "Text",
		description = ""
	)
	String text = "Text";

	@ConfigItem(
		position = 2,
		keyName = "fontStyle",
		name = "Font Style",
		description = "Plain | Bold | Italics",
		section = text
	)
	default FontStyle fontStyle()
	{
		return FontStyle.BOLD;
	}

	@Range(
		min = 14,
		max = 40
	)
	@ConfigItem(
		position = 3,
		keyName = "textSize",
		name = "Text Size",
		description = "Text Size for Timers.",
		section = text
	)
	@Units(Units.POINTS)
	default int textSize()
	{
		return 32;
	}

	@ConfigItem(
		position = 4,
		keyName = "shadows",
		name = "Shadows",
		description = "Adds Shadows to text.",
		section = text
	)
	default boolean shadows()
	{
		return false;
	}

	@Getter
	@AllArgsConstructor
	enum FontStyle
	{
		BOLD("Bold", Font.BOLD),
		ITALIC("Italic", Font.ITALIC),
		PLAIN("Plain", Font.PLAIN);

		private String name;
		private int font;

		@Override
		public String toString()
		{
			return getName();
		}
	}
}
