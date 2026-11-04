/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.capabilities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public class MorphCapabilities
{
	public static void onAttachEntityCapability(AttachCapabilitiesEvent<Entity> event)
	{
    	Entity entity = event.getObject();
    	if(entity instanceof LivingEntity living)
    	{
    		event.addCapability(MorphCapabilityImpl.ID, new MorphCapabilityImpl(living));
    	}
	}
}
