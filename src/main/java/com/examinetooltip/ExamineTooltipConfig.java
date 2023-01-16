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

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(ExamineTooltipConfig.CONFIG_GROUP)
public interface ExamineTooltipConfig extends Config
{
	String CONFIG_GROUP = "examinetooltip";
	String ITEM_EXAMINES_KEY_NAME = "showItemExamines";
	String OVERLAY_LAYER_KEY_NAME = "overlayLayer";

	@ConfigSection(
		name = "RS3 examine settings",
		description = "Settings relating to using RS3 style examine boxes",
		position = 0
	)
	String rs3ExamineSettings = "rs3ExamineSettings";

	@ConfigSection(
		name = "Examine types settings",
		description = "Settings to select which examine types to process",
		position = 1
	)
	String examineTypesSettings = "examineTypesSettings";

	@ConfigSection(
		name = "Examine box display settings",
		description = "Settings relating to how the examine box is displayed visually",
		position = 2
	)
	String displaySettings = "boxDisplaySettings";

	@ConfigSection(
		name = "Plugin hub compatibility",
		description = "Settings to enable compatibility with other plugin hub plugins",
		position = 3,
		closedByDefault = true
	)
	String pluginHubCompatibility = "pluginHubCompatibility";

	@ConfigItem(
		keyName = "rs3Style",
		name = "RS3 style examine box",
		description = "Show examines as a hovering box under the examined items, else show as a cursor tooltip",
		position = 10,
		section = rs3ExamineSettings
	)
	default boolean rs3Style()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clampRS3",
		name = "Clamp RS3 examine boxes",
		description = "Prevent the RS3 examine boxes from going offscreen",
		position = 11,
		section = rs3ExamineSettings
	)
	default boolean clampRS3()
	{
		return true;
	}

	@ConfigItem(
		keyName = "previousBoundsFallback",
		name = "Enable screen location fallback",
		description = "Show examines in the last known screen location when the examined object cannot be found",
		position = 12,
		section = rs3ExamineSettings
	)
	default boolean previousBoundsFallback()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tooltipFallback",
		name = "Enable tooltip fallback",
		description = "Show examines as tooltips when the examined object cannot be found",
		position = 13,
		section = rs3ExamineSettings
	)
	default boolean tooltipFallback()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPriceCheck",
		name = "Show price check",
		description = "Show the price check text from the Examine Plugin (\"Price of ...\"), always shown as cursor tooltip",
		position = 14,
		section = examineTypesSettings
	)
	default boolean showPriceCheck()
	{
		return false;
	}

	@ConfigItem(
		keyName = ITEM_EXAMINES_KEY_NAME,
		name = "Show interface item examines",
		description = "Show text from examining items in interfaces (e.g. inventory, bank, etc.)",
		position = 15,
		section = examineTypesSettings
	)
	default boolean showItemExamines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGroundItemExamines",
		name = "Show ground item examines",
		description = "Show text from examining items on the ground",
		position = 16,
		section = examineTypesSettings
	)
	default boolean showGroundItemExamines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showObjectExamines",
		name = "Show object examines",
		description = "Show text from examining objects (e.g. scenery)",
		position = 17,
		section = examineTypesSettings
	)
	default boolean showObjectExamines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNPCExamines",
		name = "Show NPC examines",
		description = "Show text from examining NPCs",
		position = 18,
		section = examineTypesSettings
	)
	default boolean showNPCExamines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPatchInspects",
		name = "Show farming patch inspects",
		description = "Show text from inspecting farming patches",
		position = 19,
		section = examineTypesSettings
	)
	default boolean showPatchInspects()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tooltipTimeout",
		name = "Tooltip timeout",
		description = "How long to show the examine tooltip",
		position = 20,
		section = displaySettings
	)
	@Units(Units.SECONDS)
	@Range(min = 1, max = 10)
	default int tooltipTimeout()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "tooltipFadeout",
		name = "Tooltip fadeout",
		description = "Start fading out the tooltip X milliseconds before it disappears, 0 means no fadeout",
		position = 21,
		section = displaySettings
	)
	@Units(Units.MILLISECONDS)
	@Range(min = 0, max = 3000)
	default int tooltipFadeout()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = "patchInspectExtraTime",
		name = "Patch extra time",
		description = "Add extra time to show the tooltip when inspecting farming patches (the text is typically quite long)",
		position = 22,
		section = displaySettings
	)
	@Units(Units.SECONDS)
	@Range(max = 10)
	default int patchInspectExtraTime()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "wrapTooltip",
		name = "Wrap tooltip",
		description = "Wrap the text in the tooltip if it gets too long",
		position = 23,
		section = displaySettings
	)
	default boolean wrapTooltip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wrapTooltipColumns",
		name = "Wrap columns",
		description = "How many text columns (or characters) before wrapping the text",
		position = 24,
		section = displaySettings
	)
	@Range(
		min = 20
	)
	default int wrapTooltipColumns()
	{
		return 30;
	}

	@Alpha
	@ConfigItem(
		keyName = "customBackgroundColor",
		name = "Custom background color",
		description = "Use a custom background color instead of the globally configured overlay background default",
		position = 25,
		section = displaySettings
	)
	Color customBackgroundColor();

	@ConfigItem(
		keyName = OVERLAY_LAYER_KEY_NAME,
		name = "Overlay Layer",
		description = "Select on what level the tooltips are shown. Choose 'Above Widgets' to have the tooltips appear below the right-click menu, but might have unintended interaction with other interfaces in the game.",
		position = 26,
		section = displaySettings
	)
	default TooltipOverlayLayer overlayLayer()
	{
		return TooltipOverlayLayer.ALWAYS_ON_TOP;
	}

	@ConfigItem(
		keyName = "showPluginHubPatchPayment",
		name = "PolishToaster's Patch Payment",
		description = "Show text from PolishToaster's Patch Payment plugin, always shown as cursor tooltip",
		position = 50,
		section = pluginHubCompatibility
	)
	default boolean showPluginHubPatchPayment()
	{
		return true;
	}
}
