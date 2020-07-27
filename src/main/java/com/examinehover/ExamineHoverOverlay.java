package com.examinehover;

import com.google.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class ExamineHoverOverlay extends Overlay
{
	@Inject
	private TooltipManager tooltipManager;

	@Inject
	private ExamineHoverConfig config;

	@Inject
	private ExamineHoverPlugin plugin;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Instant now = Instant.now();
		Duration timeout = Duration.ofSeconds(config.hoverTime());

		for (ExamineTextTime examine : plugin.getExamines())
		{
			Duration since = Duration.between(examine.getTime(), now);
			if (since.compareTo(timeout) < 0)
			{
				tooltipManager.add(new Tooltip(examine.getText()));
			}
		}

		return null;
	}
}
