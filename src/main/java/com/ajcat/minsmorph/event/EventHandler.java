/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.event;

import com.ajcat.minsmorph.MinsMorph;
import com.ajcat.minsmorph.entity.MorphEntities;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MinsMorph.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EventHandler 
{   
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) 
    {
    	event.put(MorphEntities.FAKE_TARGET.get(), Mob.createMobAttributes().build());
    }
}
