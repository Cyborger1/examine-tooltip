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

import com.google.common.collect.EvictingQueue;
import com.google.inject.Provides;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Examine Tooltip",
	description = "Adds tooltips on screen when examining things",
	tags = {"examine", "tooltip", "text"}
)
public class ExamineTooltipPlugin extends Plugin
{
	private final static String PRICE_CHECK_START = "Price of";
	private final static Pattern PRICE_CHECK_PATTERN =
		Pattern.compile("^Price of (?:([\\d,.]+) x )?(.+):(?: GE average (?:([\\d,\\.]+(?: \\([\\d,\\.]+ea\\))?)))?(?: HA value (?:([\\d,\\.]+(?: \\([\\d,\\.]+ea\\))?)))?$");

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ExamineTooltipOverlay examineTooltipOverlay;

	@Getter
	private final EvictingQueue<ExamineTextTime> examines = EvictingQueue.create(5);

	private final Queue<ExamineTextTime> pendingExamines = new ArrayDeque<>();

	@Provides
	ExamineTooltipConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExamineTooltipConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(examineTooltipOverlay);
		examines.clear();
		pendingExamines.clear();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(examineTooltipOverlay);
		examines.clear();
		pendingExamines.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		examines.clear();
		pendingExamines.clear();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!Text.removeTags(event.getMenuOption()).equals("Examine"))
		{
			return;
		}

		ExamineType type;
		int id = event.getId();
		int actionParam = event.getActionParam();
		int wId = event.getWidgetId();
		switch (event.getMenuAction())
		{
			case EXAMINE_ITEM:
				type = ExamineType.ITEM;
				break;
			case EXAMINE_ITEM_GROUND:
				type = ExamineType.ITEM_GROUND;
				break;
			case CC_OP_LOW_PRIORITY:
				type = ExamineType.ITEM_INTERFACE;
				break;
			case EXAMINE_OBJECT:
				type = ExamineType.OBJECT;
				break;
			case EXAMINE_NPC:
				type = ExamineType.NPC;
				break;
			default:
				return;
		}

		ExamineTextTime examine = new ExamineTextTime();
		examine.setType(type);
		examine.setId(id);
		examine.setWidgetId(wId);
		examine.setActionParam(actionParam);
		examine.setObjectName(Text.removeTags(event.getMenuTarget()));
		pendingExamines.offer(examine);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ExamineType type;
		switch (event.getType())
		{
			case ITEM_EXAMINE:
				if (Text.removeTags(event.getMessage()).startsWith(PRICE_CHECK_START))
				{
					type = ExamineType.PRICE_CHECK;
				}
				else
				{
					type = ExamineType.ITEM;
				}
				break;
			case OBJECT_EXAMINE:
				type = ExamineType.OBJECT;
				break;
			case NPC_EXAMINE:
				type = ExamineType.NPC;
				break;
			case GAMEMESSAGE:
				type = ExamineType.ITEM_INTERFACE;
				break;
			default:
				return;
		}

		Instant now = Instant.now();
		String text = Text.removeTags(event.getMessage());

		if (type == ExamineType.PRICE_CHECK)
		{
			Matcher matcher = PRICE_CHECK_PATTERN.matcher(text);
			if (matcher.matches())
			{
				String itemName = matcher.group(2);
				ExamineTextTime matchingExamine;
				for (ExamineTextTime examine : examines)
				{
					ExamineType t = examine.getType();
					if ((t == ExamineType.ITEM || t == ExamineType.ITEM_GROUND || t == ExamineType.ITEM_INTERFACE)
						&& examine.getObjectName().equals(itemName))
					{
						examine.setQuantity(matcher.group(1));
						examine.setGeValue(matcher.group(3));
						examine.setHaValue(matcher.group(4));
						examine.setContainsPriceCheckInfo(true);
						break;
					}
				}
			}
			return;
		}

		if (pendingExamines.isEmpty())
		{
			return;
		}

		ExamineTextTime pending = pendingExamines.poll();

		if (pending.getType() == type || (type == ExamineType.ITEM && pending.getType() == ExamineType.ITEM_GROUND))
		{
			pending.setTime(now);
			pending.setText(text);
			pending.setContainsPriceCheckInfo(false);
			examines.removeIf(x -> x.getText().equals(text));
			examines.add(pending);
		}
		else
		{
			pendingExamines.clear();
		}
	}
}
