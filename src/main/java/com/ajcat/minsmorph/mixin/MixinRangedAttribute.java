/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

@Mixin(RangedAttribute.class)
public class MixinRangedAttribute
{
	// Vanilla caps max health at 1024. Morphs can legitimately have a higher max health, so only lift that cap.
	@Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
	private void minsmorph$sanitizeMaxHealth(double value, CallbackInfoReturnable<Double> cir)
	{
		if((Object) this == Attributes.MAX_HEALTH)
		{
			cir.setReturnValue(Mth.clamp(value, 1.0D, (double) Float.MAX_VALUE));
		}
	}

	@Inject(method = "getMaxValue", at = @At("RETURN"), cancellable = true)
	private void minsmorph$getMaxHealthValue(CallbackInfoReturnable<Double> cir)
	{
		if((Object) this == Attributes.MAX_HEALTH)
		{
			cir.setReturnValue((double) Float.MAX_VALUE);
		}
	}
}
