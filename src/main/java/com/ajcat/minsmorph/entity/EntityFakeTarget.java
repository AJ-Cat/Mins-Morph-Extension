/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.entity;

import com.ajcat.minsmorph.capabilities.IMorphCapability;
import com.ajcat.minsmorph.capabilities.MorphCapabilityImpl;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;

public class EntityFakeTarget extends AbstractOwnableMob<LivingEntity>
{
	private int lifeTicks = 40;
	private double followDistance = 2.0D;
	private int boundMorphId = Integer.MIN_VALUE;

	public EntityFakeTarget(EntityType<? extends AbstractOwnableMob<?>> p_21368_, Level p_21369_)
	{
		super(p_21368_, p_21369_);
		this.setNoAi(true);
		this.setInvulnerable(true);
		this.setSilent(true);
		this.noPhysics = true;
	}
	
	@Override
	public void tick() 
	{
		super.tick();
		if(--this.lifeTicks <= 0)
		{
			this.discard();
			return;
		}
		if(this.getOwner() == null)
		{
			this.discard();
		}
		else
		{
			LivingEntity living = this.getOwner();
			IMorphCapability cap = living.getCapability(MorphCapabilityImpl.MORPH).orElse(null);
			LivingEntity currentMorph = cap == null ? null : cap.getMorph();
			if(currentMorph == null || currentMorph.getId() != this.boundMorphId)
			{
				// The owner changed or removed its morph; do not leave an old ability target behind.
				this.discard();
				return;
			}
			Vec3 look = living.getViewVector(1.0F);
			Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
			if(horizontal.lengthSqr() < 1.0E-4D)
			{
				horizontal = new Vec3(0.0D, 0.0D, 1.0D);
			}
			horizontal = horizontal.normalize();
			Vec3 position = living.position().add(horizontal.scale(this.followDistance));
			// Keep the dummy near the owner's body; using the full eye height would put it far above
			// the navigation target for very large morphs.
			double y = living.getY() + Math.min(living.getBbHeight() * 0.5D, 1.5D);
			this.setPos(position.x, y, position.z);
		}
	}

	public void bindToOwnerMorph(LivingEntity owner)
	{
		IMorphCapability cap = owner.getCapability(MorphCapabilityImpl.MORPH).orElse(null);
		LivingEntity morph = cap == null ? null : cap.getMorph();
		this.boundMorphId = morph == null ? Integer.MIN_VALUE : morph.getId();
	}

	public void setLifeTicks(int lifeTicks)
	{
		this.lifeTicks = lifeTicks;
	}

	public void setFollowDistance(double followDistance)
	{
		this.followDistance = followDistance;
	}
	
	@Override
	public boolean isAlliedTo(Entity p_20355_)
	{
		return false;
	}
	
	@Override
	public boolean isAlliedTo(Team p_20032_)
	{
		return false;
	}
	
	@Override
	public boolean addEffect(MobEffectInstance p_147208_, Entity p_147209_)
	{
		return false;
	}
	
	@Override
	public boolean canBeAffected(MobEffectInstance p_21197_)
	{
		return false;
	}
	
	@Override
	public boolean hurt(DamageSource p_21016_, float p_21017_)
	{
		if(p_21016_.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
		{
			this.discard();
		}
		return false;
	}
	
	@Override
	public void setDeltaMovement(Vec3 p_20257_) 
	{

	}
	
	@Override
	public Vec3 getDeltaMovement() 
	{
		return Vec3.ZERO;
	}
	
	@Override
	public boolean isPickable() 
	{
		return false;
	}
	
	@Override
	public boolean displayFireAnimation() 
	{
		return false;
	}
	
	@Override
	protected void playHurtSound(DamageSource p_21493_) 
	{
		
	}
	
	@Override
	protected void playStepSound(BlockPos p_20135_, BlockState p_20136_) 
	{
		
	}
}
