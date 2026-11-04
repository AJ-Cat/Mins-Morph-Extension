/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.misc;

import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Entity;

public interface ILevelEntityGetterAdapter 
{
	public Int2ObjectMap<Entity> byId();
	
	public Map<UUID, Entity> byUuid();
}
