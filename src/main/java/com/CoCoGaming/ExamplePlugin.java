package com.CoCoGaming;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

@Slf4j
@PluginDescriptor(
		name = "TOA Dropped Item Warning"
)
public class ExamplePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ExampleConfig config;

	private Set<WorldPoint> droppedItemLocations = new HashSet<>();
	private WorldPoint lastPlayerLocation;

	private static final Set<Integer> TOA_DOOR_IDS = new HashSet<>(Arrays.asList(
			45397,  // Zebak Puzzle
			11831,  // Zebak boss room
			45131,  // Akkha puzzle
			44558,  // Baba Puzzle
			11830   // Baba boss
	));

	private static final Set<Integer> TOA_REGION_IDS = new HashSet<>(Arrays.asList(
			14160, // TOA Lobby
			14676, // TOA Entry Room
			15698, // Path of Scabaras
			15954, // Path of Het
			14162, // Path of Crondis
			14674, // Path of Apmeken
			15172, // Wardens P1 arena
			15428  // Wardens P2 arena
	));

	@Override
	protected void startUp() throws Exception
	{
		log.info("TOA Dropped Item Warning started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("TOA Dropped Item Warning stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "TOA Dropped Item Warning is now active!", null);
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		if (config.enableDroppedItemWarning() && isInTOA())
		{
			droppedItemLocations.add(itemSpawned.getTile().getWorldLocation());
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (config.enableDroppedItemWarning() && isInTOA())
		{
			GameObject gameObject = event.getGameObject();
			if (isDoorway(gameObject))
			{
				checkForDroppedItems();
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (config.enableDroppedItemWarning() && isInTOA())
		{
			if (event.getMenuOption().equals("Enter") && TOA_DOOR_IDS.contains(event.getId()))
			{
				if (!droppedItemLocations.isEmpty())
				{
					event.consume(); // Prevent the player from entering the next room
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Warning: You have dropped items in this room! Pick them up before proceeding.", null);
				}
			}
		}
	}

	private boolean isInTOA()
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}

		WorldPoint location = client.getLocalPlayer().getWorldLocation();
		int regionId = location.getRegionID();
		return TOA_REGION_IDS.contains(regionId);
	}

	private boolean isDoorway(GameObject gameObject)
	{
		return TOA_DOOR_IDS.contains(gameObject.getId());
	}

	private void checkForDroppedItems()
	{
		WorldPoint currentPlayerLocation = client.getLocalPlayer().getWorldLocation();
		if (lastPlayerLocation != null && !currentPlayerLocation.equals(lastPlayerLocation))
		{
			if (!droppedItemLocations.isEmpty())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Warning: You have dropped items in this room!", null);
			}
		}
		lastPlayerLocation = currentPlayerLocation;
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}