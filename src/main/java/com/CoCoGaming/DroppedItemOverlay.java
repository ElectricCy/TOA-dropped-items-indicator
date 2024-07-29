package com.CoCoGaming;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.*;

import javax.inject.Inject;
import java.awt.*;

class DroppedItemOverlay extends OverlayPanel
{
    private final Client client;
    private final ExamplePlugin plugin;
    private final ExampleConfig config;
    private Rectangle buttonBounds;

    @Inject
    private DroppedItemOverlay(ExamplePlugin plugin, ExampleConfig config, Client client)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.BOTTOM_LEFT); // Changed from TOP_CENTER to ABOVE_CHATBOX_RIGHT
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.hasDroppedItems() || !plugin.isInTOA() || plugin.isWarningOverridden())
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setBackgroundColor(new Color(255, 0, 0, 220));
        panelComponent.setPreferredSize(new Dimension(200, 0)); // Changed from 300 to 200 to fit better over the chatbox

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("WARNING!")
                .color(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Dropped items in room!")
                .leftColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Pick up before leaving.")
                .leftColor(Color.YELLOW)
                .build());

        // Add the clickable button
        panelComponent.getChildren().add(LineComponent.builder()
                .right("I have what I need")
                .rightColor(Color.CYAN)
                .build());

        Dimension dimension = super.render(graphics);

        if (dimension != null)
        {
            Rectangle bounds = getBounds();
            buttonBounds = new Rectangle(
                    bounds.x + bounds.width - 120,
                    bounds.y + bounds.height - 20,
                    120,
                    20
            );
        }

        return dimension;
    }

    public void handleClick(Point point)
    {
        if (buttonBounds != null && buttonBounds.contains(point))
        {
            plugin.overrideWarning();
        }
    }
}