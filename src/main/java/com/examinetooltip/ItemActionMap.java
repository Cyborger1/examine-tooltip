/*
 * Copyright (c) 2021, Cyborger1
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

import com.google.common.collect.ImmutableMap;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemID;

public class ItemActionMap
{
	private static final ImmutableMap<String, ChatMessageType> BONE_CRUSHER_MAP = ImmutableMap.of(
		"Activity", ChatMessageType.GAMEMESSAGE,
		"Check", ChatMessageType.GAMEMESSAGE);

	private static final ImmutableMap<String, ChatMessageType> SLAYER_RING_MAP = ImmutableMap.of("Check", ChatMessageType.GAMEMESSAGE);

	private static final ImmutableMap<String, ChatMessageType> GEM_BAG_MAP = ImmutableMap.of(
		"Check", ChatMessageType.GAMEMESSAGE,
		"Open", ChatMessageType.GAMEMESSAGE,
		"Close", ChatMessageType.GAMEMESSAGE);

	public static final ImmutableMap<Integer, ImmutableMap<String, ChatMessageType>> ITEM_EXPECTED_CHAT_MESSAGE =
		ImmutableMap.<Integer, ImmutableMap<String, ChatMessageType>>builder()

			.put(ItemID.BONECRUSHER, BONE_CRUSHER_MAP)
			.put(ItemID.BONECRUSHER_NECKLACE, BONE_CRUSHER_MAP)

			.put(ItemID.SLAYER_RING_1, SLAYER_RING_MAP)
			.put(ItemID.SLAYER_RING_2, SLAYER_RING_MAP)
			.put(ItemID.SLAYER_RING_3, SLAYER_RING_MAP)
			.put(ItemID.SLAYER_RING_4, SLAYER_RING_MAP)
			.put(ItemID.SLAYER_RING_5, SLAYER_RING_MAP)
			.put(ItemID.SLAYER_RING_6, SLAYER_RING_MAP)
			.put(ItemID.SLAYER_RING_7, SLAYER_RING_MAP)
			.put(ItemID.SLAYER_RING_8, SLAYER_RING_MAP)
			.put(ItemID.SLAYER_RING_ETERNAL, SLAYER_RING_MAP)

			.put(ItemID.GEM_BAG_12020, GEM_BAG_MAP)
			.put(ItemID.OPEN_GEM_BAG, GEM_BAG_MAP)

			.build();
}
