/*
 * Copyright (c) 2020, Cyborger1
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.examinetooltip;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(ExamineTooltipConfig.CONFIG_GROUP)
public interface ExamineTooltipConfig extends Config
{
	String CONFIG_GROUP = "examinetooltip";
	String ITEM_EXAMINES_KEY_NAME = "showItemExamines";

	@ConfigItem(
		keyName = "rs3Style",
		name = "RS3 style examine box",
		description = "Show examines as a hovering box under the examined items, else show as cursor tooltip",
		position = 1
	)

	default boolean rs3Style()
	{
		return true;
	}

	@ConfigItem(
		keyName = ITEM_EXAMINES_KEY_NAME,
		name = "Show item examines",
		description = "Show text from examining items",
		position = 2
	)

	default boolean showItemExamines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPriceCheck",
		name = "Show price check",
		description = "Show the price check text from the Examine Plugin (\"Price of ...\"), always shown as cursor tooltip",
		position = 3
	)
	default boolean showPriceCheck()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showObjectExamines",
		name = "Show object examines",
		description = "Show text from examining objects (e.g. scenery)",
		position = 4
	)
	default boolean showObjectExamines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNPCExamines",
		name = "Show NPC examines",
		description = "Show text from examining NPCs",
		position = 5
	)
	default boolean showNPCExamines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tooltipTimeout",
		name = "Tooltip timeout",
		description = "How long to show the examine tooltip",
		position = 6
	)
	@Units(Units.SECONDS)
	@Range(min = 1, max = 10)
	default int tooltipTimeout()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "wrapTooltip",
		name = "Wrap tooltip",
		description = "Wraps the text in the tooltip if it gets too long",
		position = 7
	)
	default boolean wrapTooltip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wrapTooltipColumns",
		name = "Wrap columns",
		description = "How many text columns (or characters) before wrapping the text",
		position = 8
	)
	@Range(
		min = 20
	)
	default int wrapTooltipColumns()
	{
		return 30;
	}
}
