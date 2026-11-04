/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

@Mixin(TargetGoal.class)
public class MixinTargetGoal
{
	@Shadow
	@Final
	protected Mob mob;

	@Inject(at = @At("HEAD"), method = "canAttack", cancellable = true)
	private void canAttack(LivingEntity target, TargetingConditions conditions, CallbackInfoReturnable<Boolean> cir)
	{
		if(this.mob != null && target != null && MorphUtil.shouldCancelMorphTarget(this.mob, target))
		{
			cir.setReturnValue(false);
		}
	}

	@Inject(at = @At("RETURN"), method = "canAttack", cancellable = true)
	private void canAttackWithEffectiveMorphTarget(LivingEntity target, TargetingConditions conditions, CallbackInfoReturnable<Boolean> cir)
	{
		if(this.mob == null || target == null)
		{
			return;
		}
		LivingEntity effectiveTarget = MorphUtil.getEffectiveLivingTarget(target);
		if(effectiveTarget == target)
		{
			return;
		}
		if(MorphUtil.shouldCancelMorphTarget(this.mob, effectiveTarget))
		{
			cir.setReturnValue(false);
			return;
		}
		cir.setReturnValue(conditions.test(this.mob, effectiveTarget));
	}
}
