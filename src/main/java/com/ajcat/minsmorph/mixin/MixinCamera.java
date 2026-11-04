/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.ajcat.minsmorph.capabilities.IMorphCapability;
import com.ajcat.minsmorph.capabilities.MorphCapabilityImpl;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

@Mixin(Camera.class)
public abstract class MixinCamera
{
	@ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
	private double minsmorph$largeMorphCameraDistance(double distance)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if(minecraft.options.getCameraType().isFirstPerson())
		{
			return distance;
		}
		Entity entity = ((Camera)(Object)this).getEntity();
		if(entity == null)
		{
			return distance;
		}
		IMorphCapability cap = entity.getCapability(MorphCapabilityImpl.MORPH).orElse(null);
		LivingEntity morph = cap == null ? null : cap.getMorph();
		if(morph == null)
		{
			return distance;
		}
		AABB cullingBox = morph.getBoundingBoxForCulling();
		double morphSize = Math.max(cullingBox.getXsize(), Math.max(cullingBox.getYsize(), cullingBox.getZsize()));
		if(morphSize <= 0.0D)
		{
			morphSize = Math.max(morph.getBbWidth(), morph.getBbHeight());
		}
		return Math.max(distance, morphSize * 1.5D);
	}
}
