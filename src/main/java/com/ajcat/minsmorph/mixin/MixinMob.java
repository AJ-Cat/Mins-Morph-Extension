/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.ajcat.minsmorph.entity.EntityFakeTarget;
import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

@Mixin(Mob.class)
public class MixinMob 
{
	@Inject(at = @At(value = "HEAD"), method = "serverAiStep", cancellable = true)
	private final void serverAiStep(CallbackInfo ci)
	{
		if(MorphUtil.isMorph(Mob.class.cast(this)))
		{
			ci.cancel();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "getItemBySlot", cancellable = true)
	private void getItemBySlot(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir)
	{
		if(MorphUtil.getMorphOwner(Mob.class.cast(this)) != null)
		{
			cir.setReturnValue(((LivingEntity) MorphUtil.getMorphOwner(Mob.class.cast(this))).getItemBySlot(slot));
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "setTarget", cancellable = true)
	private void setTarget(@Nullable LivingEntity target, CallbackInfo ci)
	{
		Mob attacker = Mob.class.cast(this);
		if(target != null)
		{
			if(MorphUtil.shouldCancelMorphTarget(attacker, target))
			{
				ci.cancel();
				return;
			}
			if(MorphUtil.getMorphOwner(target) != null)
			{
				ci.cancel();
				attacker.setTarget((LivingEntity) MorphUtil.getMorphOwner(target));
				return;
			}
			if(MorphUtil.getMorphOwner(attacker) == null && target instanceof EntityFakeTarget)
			{
				ci.cancel();
				return;
			}
		}
	}
}
