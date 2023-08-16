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

import net.runelite.client.config.*;

@ConfigGroup("xRunecrafting")
public interface xRunecraftingConfig extends Config {

    @ConfigSection(
            keyName = "runecraftingSettings",
            name = "RunecraftingSettings",
            description = "",
            position = 0
    )

    String rcSettings = "RunecraftingSettings";

    @ConfigItem(
            keyName = "xSettings",
            name = "Altar settings",
            description = "For the Air altar start in Falador bank with a empty inventory.\nFire Altar start in castle-wars with duel rings(8) in the bank.",
            position = 0,
            section = rcSettings
    )
    default xRunecraftingSettings xSettings()
    {
        return xRunecraftingSettings.FireAltar;
    }
    @ConfigItem(
            keyName = "useStaminas",
            name = "Use Stamina (1)",
            description = "Withdraw and use Stamina(1) at bank",
            position = 1,
            section = rcSettings
    )
    default boolean useStaminas()
    {
        return true;
    }
    @Range(
            min=0,
            max=80
    )
    @ConfigItem(
            keyName = "staminaSetpoint",
            name = "Stamina Setpoint",
            hidden = true,
            unhide = "useStaminas",
            description = "Use staminas <= this value",
            position = 2,
            section = rcSettings
    )
    default int staminaSetpoint()
    {
        return 75;
    }
}




