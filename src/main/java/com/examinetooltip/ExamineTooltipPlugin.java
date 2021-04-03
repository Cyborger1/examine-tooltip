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

import static com.examinetooltip.ItemActionMap.ITEM_EXPECTED_CHAT_MESSAGE;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Examine Tooltip",
	description = "Shows tooltips or RS3 style hover boxes on examine",
	tags = {"examine", "tooltip", "text", "hover", "rs3"}
)
public class ExamineTooltipPlugin extends Plugin
{
	private static final Pattern PATCH_INSPECT_PATTERN = Pattern.compile("^This is an? .+\\. The (?:soil|patch) has");

	private static final Pattern PLUGIN_HUB_PATCH_PAYMENT_1_PATTERN = Pattern.compile(
		"^A farmer will watch over an? .+ patch for"
	);

	private static final Pattern PLUGIN_HUB_PATCH_PAYMENT_2_PATTERN = Pattern.compile(
		"^An? .+ patch can NOT be protected by a farmer"
	);

	private static final ImmutableSet<ChatMessageType> VALID_CHAT_MESSAGE_TYPES = ImmutableSet.of(
		ChatMessageType.ITEM_EXAMINE,
		ChatMessageType.NPC_EXAMINE,
		ChatMessageType.OBJECT_EXAMINE,
		ChatMessageType.GAMEMESSAGE
	);

	private static final int EQUIPMENT_FLOATING_GROUP_ID = 84;
	private static final int EXAMINE_TICK_GRACE_PERIOD = 1;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ExamineTooltipOverlay examineTooltipOverlay;

	@Inject
	private ExamineTooltipConfig config;

	@Inject
	private Client client;

	@Getter
	private final EvictingQueue<ExamineTextTime> examines = EvictingQueue.create(5);

	private final Queue<ExamineTextTime> pendingExamines = new ArrayDeque<>();

	private ExamineTextTime pendingPatchInspect;

	@Provides
	ExamineTooltipConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExamineTooltipConfig.class);
	}

	private void resetPlugin()
	{
		examines.clear();
		pendingExamines.clear();
		pendingPatchInspect = null;
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(examineTooltipOverlay);
		resetPlugin();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(examineTooltipOverlay);
		resetPlugin();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		resetPlugin();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = Text.removeTags(event.getMenuOption());

		int id = event.getId();
		int actionParam = event.getActionParam();
		int wId = event.getWidgetId();

		ChatMessageType expected;
		ExamineType type;
		if (option.equals("Examine"))
		{
			switch (event.getMenuAction())
			{
				case EXAMINE_ITEM:
					if (!config.showItemExamines())
					{
						return;
					}
					type = ExamineType.ITEM;
					expected = ChatMessageType.ITEM_EXAMINE;
					break;
				case EXAMINE_ITEM_GROUND:
					if (!config.showGroundItemExamines())
					{
						return;
					}
					type = ExamineType.ITEM_GROUND;
					expected = ChatMessageType.ITEM_EXAMINE;
					break;
				case CC_OP_LOW_PRIORITY:
					if (!config.showItemExamines())
					{
						return;
					}
					type = ExamineType.ITEM_INTERFACE;
					expected = ChatMessageType.GAMEMESSAGE;
					break;
				case EXAMINE_OBJECT:
					if (!config.showObjectExamines())
					{
						return;
					}
					type = ExamineType.OBJECT;
					expected = ChatMessageType.OBJECT_EXAMINE;
					break;
				case EXAMINE_NPC:
					if (!config.showNPCExamines())
					{
						return;
					}
					type = ExamineType.NPC;
					expected = ChatMessageType.NPC_EXAMINE;
					break;
				default:
					return;
			}
		}
		else if (option.equals("Inspect"))
		{
			switch (event.getMenuAction())
			{
				case GAME_OBJECT_FIRST_OPTION:
				case GAME_OBJECT_SECOND_OPTION:
				case GAME_OBJECT_THIRD_OPTION:
				case GAME_OBJECT_FOURTH_OPTION:
				case GAME_OBJECT_FIFTH_OPTION:
					type = ExamineType.PATCH_INSPECT;
					expected = ChatMessageType.GAMEMESSAGE;
					break;
				default:
					return;
			}
		}
		else
		{
			ImmutableMap<String, ChatMessageType> messageMap;
			switch (event.getMenuAction())
			{
				case ITEM_FIRST_OPTION:
				case ITEM_SECOND_OPTION:
				case ITEM_THIRD_OPTION:
				case ITEM_FOURTH_OPTION:
				case ITEM_FIFTH_OPTION:
				case CC_OP:
				case CC_OP_LOW_PRIORITY:
					if (!config.showItemMessages())
					{
						return;
					}

					int actualId;
					// Snag ITEM ID when checked in equipment widget
					int wGroup = WidgetInfo.TO_GROUP(wId);
					if (wGroup == WidgetID.EQUIPMENT_GROUP_ID
						|| wGroup == EQUIPMENT_FLOATING_GROUP_ID)
					{
						Widget w = client.getWidget(wId);
						if (w == null)
						{
							return;
						}

						Widget item = w.getChild(1);
						if (item == null)
						{
							return;
						}

						actualId = item.getItemId();
					}
					else
					{
						actualId = id;
					}

					messageMap = ITEM_EXPECTED_CHAT_MESSAGE.get(actualId);
					if (event.getMenuAction() == MenuAction.CC_OP
						|| event.getMenuAction() == MenuAction.CC_OP_LOW_PRIORITY)
					{
						type = ExamineType.ITEM_INTERFACE;
					}
					else
					{
						type = ExamineType.ITEM;
					}
					break;

				default:
					return;
			}

			if (messageMap == null)
			{
				return;
			}

			expected = messageMap.get(option);
			if (expected == null)
			{
				return;
			}
		}

		ExamineTextTime examine = new ExamineTextTime();
		examine.setActionTick(client.getTickCount());
		examine.setType(type);
		examine.setExpectedMessageType(expected);
		examine.setId(id);
		examine.setWidgetId(wId);
		examine.setActionParam(actionParam);

		if (type == ExamineType.PATCH_INSPECT)
		{
			// Patch inspects require the player to move up to the patch.
			// They have to be treated separately and cannot be queued.
			pendingPatchInspect = examine;
		}
		else
		{
			pendingExamines.offer(examine);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType messageType = event.getType();
		if (!VALID_CHAT_MESSAGE_TYPES.contains(messageType))
		{
			return;
		}

		Instant now = Instant.now();
		String text = Text.removeTags(event.getMessage().replace("<br>", " "));

		if (event.getType() == ChatMessageType.ITEM_EXAMINE)
		{
			if (text.startsWith("Price of"))
			{
				if (config.showPriceCheck())
				{
					ExamineTextTime examine = new ExamineTextTime();
					examine.setType(ExamineType.PRICE_CHECK);
					examine.setText(text);
					examine.setTime(now);
					examines.add(examine);
				}
				return;
			}
			else if (checkPluginHubPatchPaymentException(text))
			{
				if (config.showPluginHubPatchPayment())
				{
					ExamineTextTime examine = new ExamineTextTime();
					examine.setType(ExamineType.PLUGIN_HUB_PATCH_PAYMENT);
					examine.setText(text);
					examine.setTime(now);
					examines.add(examine);
				}
				return;
			}
		}

		ExamineTextTime pending = null;
		if (event.getType() == ChatMessageType.GAMEMESSAGE
			&& PATCH_INSPECT_PATTERN.matcher(event.getMessage()).lookingAt())
		{
			if (pendingPatchInspect != null && config.showPatchInspects())
			{
				pending = pendingPatchInspect;
				pendingPatchInspect = null;
			}
		}
		else
		{
			pending = pendingExamines.poll();
			if (pending != null
				&& (messageType != pending.getExpectedMessageType()
					|| pending.getActionTick() < client.getTickCount() - EXAMINE_TICK_GRACE_PERIOD))
			{
				pendingExamines.clear();
				return;
			}
		}

		if (pending != null)
		{
			pending.setTime(now);
			pending.setText(text);
			examines.removeIf(x -> x.getText().equals(text));
			examines.add(pending);
		}
	}

	private boolean checkPluginHubPatchPaymentException(String text)
	{
		return PLUGIN_HUB_PATCH_PAYMENT_1_PATTERN.matcher(text).lookingAt()
			|| PLUGIN_HUB_PATCH_PAYMENT_2_PATTERN.matcher(text).lookingAt();
	}
}

/*
subscribe(MenuOptionClicked.class, ev ->
{
	log.debug(String.format("ev: %s, id: %d, act: %d, wid: %d",
		ev.getMenuOption(), ev.getId(), ev.getActionParam(), ev.getWidgetId()));
});

subscribe(ChatMessage.class, ev ->
{
	log.debug(String.format("type: %s, mess: %s",
		ev.getType(), ev.getMessage()));
});

 */