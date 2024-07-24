package com.CoCoGaming;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface ExampleConfig extends Config
{
	@ConfigItem(
			keyName = "enableDroppedItemWarning",
			name = "Enable Dropped Item Warning",
			description = "Warns you if you've dropped an item before moving to the next room in TOA"
	)
	default boolean enableDroppedItemWarning()
	{
		return true;
	}
}