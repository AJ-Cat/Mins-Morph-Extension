/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph;

import com.ajcat.minsmorph.capabilities.MorphCapabilities;
import com.ajcat.minsmorph.config.MorphConfig;
import com.ajcat.minsmorph.entity.MorphEntities;
import com.ajcat.minsmorph.item.MorphItems;
import com.ajcat.minsmorph.network.MorphNetwork;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MinsMorph.MODID)
public class MinsMorph 
{
	public static final String MODID = "minsmorph";
	
	public MinsMorph(FMLJavaModLoadingContext ctx) 
	{
		IEventBus bus = ctx.getModEventBus();
		MorphItems.ITEMS.register(bus);
		MorphEntities.ENTITY_TYPES.register(bus);
		MorphNetwork.registerMessages();
		MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, MorphCapabilities::onAttachEntityCapability);
		ctx.registerConfig(Type.COMMON, MorphConfig.CONFIG_SPEC, "mins-morph.toml");
	}
}
