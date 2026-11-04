/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.mixin;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Maps;
import com.ajcat.minsmorph.misc.ILevelEntityGetterAdapter;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;

@Mixin(LevelEntityGetterAdapter.class)
public class MixinLevelEntityGetterAdapter implements ILevelEntityGetterAdapter
{
	@Unique
	private Map<UUID, Entity> byUuid = Maps.newHashMap();
	
	@Unique
	private Int2ObjectMap<Entity> byId = new Int2ObjectLinkedOpenHashMap<>();
	   
	@Inject(at = @At(value = "HEAD"), method = "get(I)Lnet/minecraft/world/level/entity/EntityAccess;", cancellable = true)
	private void getEntity(int id, CallbackInfoReturnable<Entity> cir)
	{
		if(this.byId.containsKey(id))
		{
			cir.setReturnValue(this.byId.get(id));
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "get(Ljava/util/UUID;)Lnet/minecraft/world/level/entity/EntityAccess;", cancellable = true)
	private void getEntity(UUID uuid, CallbackInfoReturnable<Entity> cir)
	{
		if(this.byUuid.containsKey(uuid))
		{
			cir.setReturnValue(this.byUuid.get(uuid));
		}
	}

	@Override
	public Int2ObjectMap<Entity> byId()
	{
		return this.byId;
	}

	@Override
	public Map<UUID, Entity> byUuid()
	{
		return this.byUuid;
	}
}
