/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.misc;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;

public interface ITrackedEntity
{
	public void updatePlayersCustom(List<ServerPlayer> players);
	
	public void setViewDist(int dist);
}
