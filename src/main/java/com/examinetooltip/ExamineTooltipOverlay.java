package com.examinetooltip;

import com.examinetooltip.components.AlphaTooltipComponent;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ItemLayer;
import net.runelite.api.NPC;
import net.runelite.api.Node;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.apache.commons.text.WordUtils;

public class ExamineTooltipOverlay extends Overlay
{
	private final static int SCREEN_PADDING = 5;
	private final static int EXAMINE_PADDING = 10;

	private final static Set<ExamineType> TOOLTIP_ONLY_EXAMINE_TYPES =
		ImmutableSet.of(ExamineType.PRICE_CHECK, ExamineType.PLUGIN_HUB_PATCH_PAYMENT);

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
	private final Map<ExamineTextTime, Rectangle> rectMap = new HashMap<>();

	public ExamineTooltipOverlay()
	{
		setPosition(OverlayPosition.TOOLTIP);
		setPriority(Overlay.PRIORITY_HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Instant now = Instant.now();
		Duration defaultTimeout = Duration.ofSeconds(config.tooltipTimeout());
		Duration patchTimeout = Duration.ofSeconds(defaultTimeout.getSeconds() + config.patchInspectExtraTime());
		boolean shouldClearDimMap = !dimMap.isEmpty();
		boolean shouldClearRectMap = !rectMap.isEmpty();

		for (ExamineTextTime examine : plugin.getExamines())
		{
			Duration since = Duration.between(examine.getTime(), now);
			Duration timeout;

			if (examine.getType() == ExamineType.PATCH_INSPECT)
			{
				timeout = patchTimeout;
			}
			else
			{
				timeout = defaultTimeout;
			}

			if (since.compareTo(timeout) < 0)
			{
				long timeLeft = (timeout.minus(since)).toMillis();
				int fadeout = config.tooltipFadeout();
				double alpha;
				if (timeLeft < fadeout && fadeout > 0)
				{
					alpha = Math.min(1.0, timeLeft / (double) fadeout);
				}
				else
				{
					alpha = 1.0;
				}

				if (!config.rs3Style() || TOOLTIP_ONLY_EXAMINE_TYPES.contains(examine.getType()))
				{
					renderAsTooltip(examine, alpha);
				}
				else
				{
					renderAsRS3(examine, graphics, alpha);
					shouldClearDimMap = false;
					shouldClearRectMap = false;
				}
			}
		}

		if (shouldClearDimMap || dimMap.size() > 10)
		{
			dimMap.clear();
		}

		if (shouldClearRectMap || rectMap.size() > 10)
		{
			rectMap.clear();
		}

		return null;
	}

	private LayoutableRenderableEntity getRenderableEntity(ExamineTextTime examine, double alphaModifier)
	{
		final AlphaTooltipComponent tooltipComponent = new AlphaTooltipComponent();
		tooltipComponent.setText(getWrappedText(examine.getText()));
		tooltipComponent.setModIcons(client.getModIcons());
		tooltipComponent.setAlphaModifier(alphaModifier);

		if (config.customBackgroundColor() != null)
		{
			tooltipComponent.setBackgroundColor(config.customBackgroundColor());
		}
		else
		{
			tooltipComponent.setBackgroundColor(runeLiteConfig.overlayBackgroundColor());
		}

		return tooltipComponent;
	}

	private void renderAsTooltip(ExamineTextTime examine, double alphaModifier)
	{
		tooltipManager.add(new Tooltip(getRenderableEntity(examine, alphaModifier)));
	}

	private void renderAsRS3(ExamineTextTime examine, Graphics2D graphics, double alphaModifier)
	{
		ExamineType type = examine.getType();
		Rectangle bounds = null;
		WorldView wv = client.getTopLevelWorldView();
		switch (type)
		{
			case NPC:
				final NPC npc = wv.npcs().byIndex(examine.getId());
				if (npc != null)
				{
					Shape shape = npc.getConvexHull();
					if (shape != null)
					{
						bounds = shape.getBounds();
					}
				}
				break;

			case ITEM_INTERFACE:
				bounds = findWidgetBounds(examine.getWidgetId(), examine.getActionParam());
				break;

			case PATCH_INSPECT:
			case ITEM_GROUND:
			case OBJECT:
				// Yes, for these, ActionParam and WidgetID are scene coordinates
				LocalPoint point = LocalPoint.fromScene(examine.getActionParam(), examine.getWidgetId(), wv);
				int id = examine.getId();

				Tile tile = wv.getScene().getTiles()
					[wv.getPlane()][point.getSceneX()][point.getSceneY()];

				if (tile != null)
				{
					Shape shape = getObjectShapeFromTile(tile, type, id);
					if (shape == null)
					{
						Tile bridge = tile.getBridge();
						if (bridge != null)
						{
							shape = getObjectShapeFromTile(bridge, type, id);
						}
					}

					if (shape == null)
					{
						// Fallback to tile
						shape = Perspective.getCanvasTilePoly(client, point);
					}

					if (shape != null)
					{
						bounds = shape.getBounds();
					}
				}

				break;

			default:
				return;
		}

		// Try previously known location
		if (bounds == null && config.previousBoundsFallback())
		{
			bounds = rectMap.get(examine);
		}

		// Give up and render as tooltip if target not found
		if (bounds == null)
		{
			if (config.tooltipFallback())
			{
				renderAsTooltip(examine, alphaModifier);
			}
			return;
		}

		boolean isInterfaceExamine = type == ExamineType.ITEM_INTERFACE;

		int x = bounds.x;
		int y = bounds.height + bounds.y;

		if (!isInterfaceExamine)
		{
			x -= EXAMINE_PADDING;
			y += EXAMINE_PADDING;
		}

		final LayoutableRenderableEntity tooltipComponent = getRenderableEntity(examine, alphaModifier);

		if (isInterfaceExamine || config.clampRS3())
		{
			Dimension dim = dimMap.get(examine);
			if (dim != null)
			{
				int xMin, xMax, yMin, yMax;

				if (isInterfaceExamine)
				{
					xMin = 0;
					xMax = client.getCanvasWidth();
					yMin = 0;
					yMax = client.getCanvasHeight();
				}
				else
				{
					xMin = client.getViewportXOffset();
					xMax = client.getViewportWidth() + xMin;
					yMin = client.getViewportYOffset();
					yMax = client.getViewportHeight() + yMin;
				}

				xMin += SCREEN_PADDING;
				xMax -= SCREEN_PADDING;
				yMin += SCREEN_PADDING;
				yMax -= SCREEN_PADDING;

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
		}

		tooltipComponent.setPreferredLocation(new Point(x, y));
		dimMap.put(examine, tooltipComponent.render(graphics));
		rectMap.put(examine, bounds);
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

	private Rectangle findWidgetBounds(int widgetId, int actionParam)
	{
		Widget widget = client.getWidget(WidgetUtil.componentToInterface(widgetId), WidgetUtil.componentToId(widgetId));

		if (widget == null)
		{
			return null;
		}

		if (actionParam < 0)
		{
			return widget.getBounds();
		}

		try
		{
			Widget widgetItem = widget.getChild(actionParam);
			if (widgetItem != null)
			{
				return widgetItem.getBounds();
			}
		}
		catch (Exception e)
		{
			// Ignore
		}

		return null;
	}

	private Shape getObjectShapeFromTile(Tile tile, ExamineType type, int id)
	{
		if (type == ExamineType.ITEM_GROUND)
		{
			ItemLayer itemLayer = tile.getItemLayer();
			if (itemLayer != null)
			{
				Node current = itemLayer.getTop();
				while (current instanceof TileItem)
				{
					if (((TileItem) current).getId() == id)
					{
						return itemLayer.getCanvasTilePoly();
					}
					current = current.getNext();
				}
			}
		}
		else
		{
			for (GameObject object : tile.getGameObjects())
			{
				if (objectIdEquals(object, id))
				{
					Shape shape = object.getConvexHull();
					if (shape != null)
					{
						return shape;
					}
				}
			}

			GroundObject gObj = tile.getGroundObject();
			if (objectIdEquals(gObj, id))
			{
				Shape shape = gObj.getConvexHull();
				if (shape != null)
				{
					return shape;
				}
			}

			DecorativeObject dObj = tile.getDecorativeObject();
			if (objectIdEquals(dObj, id))
			{
				Shape shape = dObj.getConvexHull();
				if (shape != null)
				{
					return shape;
				}
			}

			WallObject wObj = tile.getWallObject();
			if (objectIdEquals(wObj, id))
			{
				return wObj.getConvexHull();
			}
		}

		return null;
	}

	// From ObjectIndicators plugin
	private boolean objectIdEquals(TileObject tileObject, int id)
	{
		if (tileObject == null)
		{
			return false;
		}

		if (tileObject.getId() == id)
		{
			return true;
		}

		// Menu action EXAMINE_OBJECT sends the transformed object id, not the base id, unlike
		// all of the GAME_OBJECT_OPTION actions, so check the id against the impostor ids
		final ObjectComposition comp = client.getObjectDefinition(tileObject.getId());

		if (comp.getImpostorIds() != null)
		{
			return Arrays.stream(comp.getImpostorIds()).anyMatch(imp -> imp == id);
		}

		return false;
	}
}
