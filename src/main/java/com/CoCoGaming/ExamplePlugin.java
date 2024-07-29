package com.CoCoGaming;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.api.SoundEffectID;
import net.runelite.client.callback.ClientThread;

import java.util.HashSet;
import java.util.Set;
import java.awt.event.MouseEvent;

@Slf4j
@PluginDescriptor(
		name = "TOA Dropped Item Warning"
)
public class ExamplePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ExampleConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DroppedItemOverlay droppedItemOverlay;

	@Inject
	private MouseManager mouseManager;

	private Set<WorldPoint> droppedItemLocations = new HashSet<>();

	private static final Set<Integer> TOA_DOOR_IDS = new HashSet<>();
	static {
		TOA_DOOR_IDS.add(44549);
		TOA_DOOR_IDS.add(44548);
		TOA_DOOR_IDS.add(44560);
		TOA_DOOR_IDS.add(46087);
		TOA_DOOR_IDS.add(46089);
		TOA_DOOR_IDS.add(46161);
		TOA_DOOR_IDS.add(45397);
		TOA_DOOR_IDS.add(11831);
		TOA_DOOR_IDS.add(46155);
		TOA_DOOR_IDS.add(45337);
		TOA_DOOR_IDS.add(11832);
		TOA_DOOR_IDS.add(46164);
		TOA_DOOR_IDS.add(45131);
		TOA_DOOR_IDS.add(11833);
		TOA_DOOR_IDS.add(45046);
		TOA_DOOR_IDS.add(44558);
		TOA_DOOR_IDS.add(11830);
		// Add more door IDs as needed
	}

	private boolean inToa = false;
	private boolean overrideItemWarning = false;
	private int currentRoomId = -1;
	private boolean overlayVisible = false;

	public MouseAdapter mouseListener;

	@Override
	protected void startUp() throws Exception
	{
		log.info("TOA Dropped Item Warning started!");
		droppedItemLocations.clear();
		checkIfInToa();
		mouseListener = new MouseAdapter() {
			@Override
			public MouseEvent mousePressed(MouseEvent e) {
				handleMousePress(e);
				return e;
			}

			@Override
			public MouseEvent mouseReleased(MouseEvent e) {
				handleMouseRelease(e);
				return e;
			}
		};
		mouseManager.registerMouseListener(mouseListener);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("TOA Dropped Item Warning stopped!");
		hideOverlay();
		droppedItemLocations.clear();
		mouseManager.unregisterMouseListener(mouseListener);
		overlayManager.remove(droppedItemOverlay);
	}

	private void handleMousePress(MouseEvent e) {
		if (droppedItemOverlay.getBounds().contains(e.getPoint())) {
			e.consume();
		}
	}

	private void handleMouseRelease(MouseEvent e) {
		if (droppedItemOverlay.getBounds().contains(e.getPoint())) {
			droppedItemOverlay.handleClick(e.getPoint());
			e.consume();
		}
	}

	public void overrideWarning()
	{
		overrideItemWarning = true;
		currentRoomId = client.getLocalPlayer().getWorldLocation().getRegionID();
		hideOverlay();
	}

	public boolean isWarningOverridden()
	{
		return overrideItemWarning && client.getLocalPlayer().getWorldLocation().getRegionID() == currentRoomId;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		log.debug("Game state changed to: {}", gameStateChanged.getGameState());
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			droppedItemLocations.clear();
			checkIfInToa();
			overrideItemWarning = false;
			currentRoomId = -1;
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		log.debug("Item spawned: {} at {}", itemSpawned.getItem().getId(), itemSpawned.getTile().getWorldLocation());
		if (inToa)
		{
			WorldPoint itemLocation = itemSpawned.getTile().getWorldLocation();
			droppedItemLocations.add(itemLocation);
			log.info("Item spawned in ToA: {} at {}", itemSpawned.getItem().getId(), itemLocation);
			updateOverlayVisibility();
		}
		else
		{
			log.debug("Item spawned outside ToA. inToa: {}", inToa);
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned itemDespawned)
	{
		if (inToa)
		{
			WorldPoint itemLocation = itemDespawned.getTile().getWorldLocation();
			droppedItemLocations.remove(itemLocation);
			log.info("Item despawned in ToA: {} at {}", itemDespawned.getItem().getId(), itemLocation);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject gameObject = event.getGameObject();
		if (TOA_DOOR_IDS.contains(gameObject.getId()))
		{
			checkIfInToa();
			log.debug("ToA door spawned, inToa: " + inToa);
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject gameObject = event.getGameObject();
		if (TOA_DOOR_IDS.contains(gameObject.getId()))
		{
			checkIfInToa();
			log.debug("ToA door despawned, inToa: " + inToa);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!inToa)
		{
			return;
		}

		log.debug("Menu option clicked: {} (ID: {})", event.getMenuOption(), event.getId());
		if ((event.getMenuOption().equals("Open") || event.getMenuOption().equals("Leave")) && TOA_DOOR_IDS.contains(event.getId()))
		{
			log.info("ToA door clicked. inToa: {}, Door ID: {}", inToa, event.getId());
			checkIfInToa();  // Recheck ToA status when interacting with a door

			if (!droppedItemLocations.isEmpty() && !isWarningOverridden())
			{
				event.consume(); // Cancel the door opening action
				showOverlay();
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "You cannot leave while you have dropped items!", null);
			}
			else
			{
				hideOverlay();
				overrideItemWarning = false;
				currentRoomId = -1;
			}
		}
	}
	private void updateOverlayVisibility()
	{
		if (droppedItemLocations.isEmpty())
		{
			hideOverlay();
		}
	}
	private void playInventoryFullSound()
	{
		clientThread.invoke(() -> client.playSoundEffect(2277)); // 2277 is the ID for the inventory full sound
	}

	private void showOverlay()
	{
		if (!overlayVisible)
		{
			overlayManager.add(droppedItemOverlay);
			playInventoryFullSound();
			overlayVisible = true;
		}
	}

	private void hideOverlay()
	{
		if (overlayVisible)
		{
			overlayManager.remove(droppedItemOverlay);
			overlayVisible = false;
		}
	}

	private void checkIfInToa()
	{
		if (client.getLocalPlayer() == null)
		{
			inToa = false;
			log.debug("Player is null, not in ToA");
			return;
		}

		int regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
		boolean inToaRegion = (regionId >= 13454 && regionId <= 13457) || regionId == 42;  // ToA region IDs + test region
		boolean inToaLobby = client.getVarbitValue(Varbits.TOA_LOBBY) == 1;
		boolean inToaParty = client.getVarbitValue(Varbits.TOA_PARTY) == 1;

		inToa = inToaRegion || inToaLobby || inToaParty;

		log.info("Checked if in ToA. inToa: {}, Region: {}, InToaRegion: {}, InToaLobby: {}, InToaParty: {}",
				inToa, regionId, inToaRegion, inToaLobby, inToaParty);

		if (!inToa)
		{
			droppedItemLocations.clear();
		}
	}

	private boolean isToaDoorNearby()
	{
		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();
		int z = client.getPlane();
		int centerX = client.getLocalPlayer().getLocalLocation().getSceneX();
		int centerY = client.getLocalPlayer().getLocalLocation().getSceneY();
		int radius = 5; // Adjust this value to change the search radius

		for (int x = Math.max(0, centerX - radius); x <= Math.min(Constants.SCENE_SIZE - 1, centerX + radius); x++)
		{
			for (int y = Math.max(0, centerY - radius); y <= Math.min(Constants.SCENE_SIZE - 1, centerY + radius); y++)
			{
				Tile tile = tiles[z][x][y];
				if (tile != null)
				{
					GameObject[] gameObjects = tile.getGameObjects();
					if (gameObjects != null)
					{
						for (GameObject gameObject : gameObjects)
						{
							if (gameObject != null && TOA_DOOR_IDS.contains(gameObject.getId()))
							{
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}


	public boolean isInTOA()
	{
		return inToa;
	}

	public boolean inParty()
	{
		return client.getVarbitValue(Varbits.TOA_PARTY) == 1;
	}

	public boolean hasDroppedItems()
	{
		return !droppedItemLocations.isEmpty();
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}