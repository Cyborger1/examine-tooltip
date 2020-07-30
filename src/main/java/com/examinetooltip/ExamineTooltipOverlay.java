package com.examinetooltip;

import com.google.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import static net.runelite.api.widgets.WidgetInfo.SEED_VAULT_ITEM_CONTAINER;
import static net.runelite.api.widgets.WidgetInfo.TO_CHILD;
import static net.runelite.api.widgets.WidgetInfo.TO_GROUP;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.TooltipComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.apache.commons.text.WordUtils;

public class ExamineTooltipOverlay extends Overlay
{
	private final static int PADDING = 5;

	@Inject
	private TooltipManager tooltipManager;

	@Inject
	private ExamineTooltipConfig config;

	@Inject
	private ExamineTooltipPlugin plugin;

	@Inject
	private RuneLiteConfig runeLiteConfig;

	@Inject
	private Client client;

	private final Map<ExamineTextTime, Dimension> dimMap = new HashMap<>();

	public ExamineTooltipOverlay()
	{
		setPosition(OverlayPosition.TOOLTIP);
		setPriority(OverlayPriority.HIGHEST);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Instant now = Instant.now();
		Duration timeout = Duration.ofSeconds(config.tooltipTimeout());
		boolean shouldClearDimMap = !dimMap.isEmpty();

		for (ExamineTextTime examine : plugin.getExamines())
		{
			Duration since = Duration.between(examine.getTime(), now);
			if (since.compareTo(timeout) < 0)
			{
				if (!config.rs3Style() || examine.getType() == ExamineType.PRICE_CHECK)
				{
					renderAsTooltip(examine);
				}
				else
				{
					renderAsRS3(examine, graphics);
					shouldClearDimMap = false;
				}
			}
		}

		if (shouldClearDimMap || dimMap.size() > 10)
		{
			dimMap.clear();
		}

		return null;
	}

	private void renderAsTooltip(ExamineTextTime examine)
	{
		tooltipManager.add(new Tooltip(getWrappedText(examine.getText())));
	}

	private void renderAsRS3(ExamineTextTime examine, Graphics2D graphics)
	{
		ExamineType type = examine.getType();
		boolean foundObject = false;
		int x = 0, y = 0;
		switch (type)
		{
			case NPC:
				final NPC[] cachedNPCs = client.getCachedNPCs();
				final NPC npc = cachedNPCs[examine.getId()];
				if (npc != null)
				{
					Shape shape = Perspective.getClickbox(client,
						npc.getModel(),
						npc.getOrientation(),
						npc.getLocalLocation());
					if (shape != null)
					{
						Rectangle box = shape.getBounds();
						if (box != null)
						{
							x = box.x;
							y = box.height + box.y;
							foundObject = true;
						}
					}
				}
				break;

			case ITEM:
				int wId = examine.getWidgetId();
				int widgetGroup = TO_GROUP(wId);
				int widgetChild = TO_CHILD(wId);

				Widget widget = client.getWidget(widgetGroup, widgetChild);
				if (widget != null)
				{
					WidgetItem widgetItem = widget.getWidgetItem(examine.getActionParam());
					if (widgetItem != null)
					{
						Rectangle slotBounds = widgetItem.getCanvasBounds();
						if (slotBounds != null)
						{
							x = slotBounds.x;
							y = slotBounds.height + slotBounds.y;
							foundObject = true;
						}
					}
				}
				break;

			case ITEM_INTERFACE:
				Rectangle bounds = findWidgetBounds(examine.getWidgetId(), examine.getActionParam());
				if (bounds != null)
				{
					x = bounds.x;
					y = bounds.height + bounds.y;
					foundObject = true;
				}
				break;

			case ITEM_GROUND:
			case OBJECT:
				// Yes, for these, ActionParam and WidgetID are scene coordinates
				LocalPoint point = LocalPoint.fromScene(examine.getActionParam(), examine.getWidgetId());
				int id = examine.getId();

				Tile tile = client.getScene().getTiles()
					[client.getPlane()][point.getSceneX()][point.getSceneY()];

				if (tile != null)
				{
					Shape shape = getObjectShapeFromTile(tile, point, type, id);
					if (shape == null)
					{
						Tile bridge = tile.getBridge();
						if (bridge != null)
						{
							shape = getObjectShapeFromTile(bridge, point, type, id);
						}
					}

					if (shape != null)
					{
						Rectangle box = shape.getBounds();
						if (box != null)
						{
							x = box.x;
							y = box.height + box.y;
						}
					}
					// Fallback to tile
					else
					{
						net.runelite.api.Point p = Perspective.localToCanvas(client, point, client.getPlane(), -50);
						if (p != null)
						{
							x = p.getX();
							y = p.getY();
						}
					}
					foundObject = true;
				}

				break;

			default:
				return;
		}

		// Give up and render as tooltip if target not found
		if (!foundObject)
		{
			renderAsTooltip(examine);
			return;
		}

		final TooltipComponent tooltipComponent = new TooltipComponent();
		tooltipComponent.setText(getWrappedText(examine.getText()));
		tooltipComponent.setBackgroundColor(runeLiteConfig.overlayBackgroundColor());
		tooltipComponent.setModIcons(client.getModIcons());

		Dimension dim = dimMap.get(examine);
		if (dim != null)
		{
			int xMin, xMax, yMin, yMax;

			if (type == ExamineType.ITEM || type == ExamineType.ITEM_INTERFACE)
			{
				xMin = 0;
				xMax = client.getCanvas().getSize().width;
				yMin = 0;
				yMax = client.getCanvas().getSize().height;
			}
			else
			{
				xMin = client.getViewportXOffset();
				xMax = client.getViewportWidth() + xMin;
				yMin = client.getViewportYOffset();
				yMax = client.getViewportHeight() + yMin;
			}

			xMin += PADDING;
			xMax -= PADDING;
			yMin += PADDING;
			yMax -= PADDING;

			if (x < xMin)
			{
				x = xMin;
			}
			else if (x + dim.width > xMax)
			{
				x = xMax - dim.width;
			}

			if (y < yMin)
			{
				y = yMin;
			}
			else if (y + dim.height > yMax)
			{
				y = yMax - dim.height;
			}
		}

		tooltipComponent.setPreferredLocation(new Point(x, y));
		dimMap.put(examine, tooltipComponent.render(graphics));
	}

	private String getWrappedText(String text)
	{
		if (config.wrapTooltip())
		{
			return WordUtils.wrap(text, config.wrapTooltipColumns(), "</br>", false);
		}
		else
		{
			return text;
		}
	}

	// Modified from findItemFromWidget in ExaminePlugin.java in Runelite's Repo
	private Rectangle findWidgetBounds(int widgetId, int actionParam)
	{
		int widgetGroup = TO_GROUP(widgetId);
		int widgetChild = TO_CHILD(widgetId);
		Widget widget = client.getWidget(widgetGroup, widgetChild);

		if (widget == null)
		{
			return null;
		}

		Widget widgetItem = null;
		if (WidgetInfo.EQUIPMENT.getGroupId() == widgetGroup
			|| (WidgetInfo.BANK_EQUIPMENT_CONTAINER.getGroupId() == widgetGroup
				&& widgetChild >= 75 && widgetChild <= 85)
			|| widgetGroup == 84)
		{
			widgetItem = widget.getChild(1);
		}
		else if (WidgetInfo.SMITHING_INVENTORY_ITEMS_CONTAINER.getGroupId() == widgetGroup)
		{
			widgetItem = widget.getChild(2);
		}
		else if (WidgetInfo.BANK_ITEM_CONTAINER.getGroupId() == widgetGroup
			|| WidgetInfo.CLUE_SCROLL_REWARD_ITEM_CONTAINER.getGroupId() == widgetGroup
			|| WidgetInfo.LOOTING_BAG_CONTAINER.getGroupId() == widgetGroup
			|| WidgetID.SEED_VAULT_INVENTORY_GROUP_ID == widgetGroup
			|| WidgetID.SEED_BOX_GROUP_ID == widgetGroup
			|| WidgetID.PLAYER_TRADE_SCREEN_GROUP_ID == widgetGroup
			|| WidgetID.PLAYER_TRADE_INVENTORY_GROUP_ID == widgetGroup
			|| WidgetID.SHOP_INVENTORY_GROUP_ID == widgetGroup
			|| WidgetID.GUIDE_PRICE_GROUP_ID == widgetGroup
			|| WidgetID.EQUIPMENT_INVENTORY_GROUP_ID == widgetGroup
			|| WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getGroupId() == widgetGroup
			|| WidgetInfo.RUNE_POUCH_ITEM_CONTAINER.getGroupId() == widgetGroup
			|| WidgetInfo.SHOP_ITEMS_CONTAINER.getGroupId() == widgetGroup)
		{
			widgetItem = widget.getChild(actionParam);
		}
		else if (WidgetID.SEED_VAULT_GROUP_ID == widgetGroup)
		{
			Widget seedWidget = client.getWidget(SEED_VAULT_ITEM_CONTAINER);
			if (seedWidget != null)
			{
				widgetItem = seedWidget.getChild(actionParam);
			}
		}

		if (widgetItem != null)
		{
			return widgetItem.getBounds();
		}
		else
		{
			return null;
		}
	}

	private Shape getObjectShapeFromTile(Tile tile, LocalPoint point, ExamineType type, int id)
	{
		Shape shape = null;
		if (type == ExamineType.ITEM_GROUND)
		{
			List<TileItem> groundItemsList = tile.getGroundItems();
			if (groundItemsList != null)
			{
				for (TileItem item : groundItemsList)
				{
					if (item != null && item.getId() == id)
					{
						shape = Perspective.getClickbox(client,
							item.getModel(), 0, point);
						if (shape != null)
						{
							break;
						}
					}
				}
			}
		}
		else
		{
			GameObject[] gameObjects = tile.getGameObjects();
			if (gameObjects != null)
			{
				for (GameObject object : gameObjects)
				{
					if (object != null)
					{
						int objId = object.getId();
						ObjectComposition comp = client.getObjectDefinition(objId);
						if (comp != null)
						{
							try
							{
								ObjectComposition impostor = comp.getImpostor();
								if (impostor != null)
								{
									objId = impostor.getId();
								}
							}
							catch (Exception e)
							{
								// Ignore
							}
						}
						if (objId == id)
						{
							shape = object.getConvexHull();
							if (shape != null)
							{
								break;
							}
						}
					}
				}
			}

			if (shape == null)
			{
				DecorativeObject object = tile.getDecorativeObject();
				if (object != null && object.getId() == id)
				{
					shape = object.getConvexHull();
				}
			}

			if (shape == null)
			{
				WallObject object = tile.getWallObject();
				if (object != null && object.getId() == id)
				{
					shape = object.getConvexHull();
				}
			}
		}

		return shape;
	}
}
