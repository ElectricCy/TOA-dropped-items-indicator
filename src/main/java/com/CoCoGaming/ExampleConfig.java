package com.CoCoGaming;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("toadroppeditemwarning")
public interface ExampleConfig extends Config
{
	@ConfigItem(
			keyName = "notifyInTombs",
			name = "Only Show in ToA",
			description = "Only show indicator inside Tombs of Amascut"
	)
	default boolean notifyInTombs()
	{
		return true;
	}

	@ConfigItem(
			keyName = "notifyInParty",
			name = "Only Show in Party",
			description = "Only show indicator if you are in an active party"
	)
	default boolean notifyInParty()
	{
		return true;
	}
}