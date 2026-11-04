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

import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

@Mixin(Projectile.class)
public class MixinProjectile 
{
	@Inject(at = @At(value = "RETURN"), method = "getOwner", cancellable = true)
	private void getOwner(CallbackInfoReturnable<Entity> cir)
	{
		if(cir.getReturnValue() != null)
		{
			if(MorphUtil.getMorphOwner(cir.getReturnValue()) != null)
			{
				cir.setReturnValue(MorphUtil.getMorphOwner(cir.getReturnValue()));
			}
		}
	}
}
