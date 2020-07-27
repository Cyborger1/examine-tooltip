package com.examinetooltip;

import com.google.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import org.apache.commons.text.WordUtils;

public class ExamineTooltipOverlay extends Overlay
{
	@Inject
	private TooltipManager tooltipManager;

	@Inject
	private ExamineTooltipConfig config;

	@Inject
	private ExamineTooltipPlugin plugin;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Instant now = Instant.now();
		Duration timeout = Duration.ofSeconds(config.tooltipTimeout());

		for (ExamineTextTime examine : plugin.getExamines())
		{
			Duration since = Duration.between(examine.getTime(), now);
			if (since.compareTo(timeout) < 0)
			{
				String text = examine.getText();
				if (config.wrapTooltip())
				{
					text = WordUtils.wrap(text, config.wrapTooltipColumns(), "</br>", false);
				}
				tooltipManager.add(new Tooltip(text));
			}
		}

		return null;
	}
}
