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
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Examine Tooltip",
	description = "Adds a tooltip under the cursor when examining things",
	tags = {"examine", "hover", "text"}
)
public class ExamineTooltipPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ExamineTooltipOverlay examineTooltipOverlay;

	@Inject
	private ExamineTooltipConfig config;

	@Getter
	private EvictingQueue<ExamineTextTime> examines = EvictingQueue.create(5);

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
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(examineTooltipOverlay);
		examines.clear();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if ((config.showItemExamines() && type == ChatMessageType.ITEM_EXAMINE)
			|| (config.showObjectExamines() && type == ChatMessageType.OBJECT_EXAMINE)
			|| (config.showNPCExamines() && type == ChatMessageType.NPC_EXAMINE))
		{
			String text = Text.removeTags(event.getMessage());

			examines.removeIf(e -> e.getText().equals(text));
			examines.add(ExamineTextTime.builder()
				.text(text)
				.time(Instant.now()).build());
		}
	}
}
