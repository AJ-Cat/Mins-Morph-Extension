/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.event;

import java.util.List;

import com.ajcat.minsmorph.MinsMorph;
import com.ajcat.minsmorph.capabilities.MorphCapabilityImpl;
import com.ajcat.minsmorph.command.MorphCommand;
import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent.ImpactResult;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = MinsMorph.MODID)
public class EventHandlerForge
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
    	MorphCommand.register(event.getDispatcher(), event.getBuildContext());
    }
    
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event)
    {
    	MorphUtil.getMorph(event.getEntity(), t -> 
    	{
    		if(t.getAttribute(Attributes.ATTACK_DAMAGE) != null)
    		{
        		t.doHurtTarget(event.getTarget());
    		}
    	});
    }
    
    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event)
    {
    	Entity entity = event.getEntity();
		if(MorphUtil.isMorph(entity))
		{
			Entity owner = MorphUtil.getMorphOwner(entity);
			if(owner != null)
			{
				MorphUtil.setChangedDimension((LivingEntity) owner, true);
			}
			event.setCanceled(true);
			entity.remove(RemovalReason.CHANGED_DIMENSION);
		}
    }
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
    	Player player = event.getEntity();
    	if(MorphUtil.isChangedDimension(player))
    	{
			player.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
			{
				if(t.getType() != null && t.getType().create(player.level) instanceof LivingEntity morph)
				{
					t.setMorph(morph);
				}
				else
				{
					MorphUtil.removeMorph(player);
				}
			});
    		MorphUtil.setChangedDimension(player, false);
    	}
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event)
    {
    	if(event.getLevel().isClientSide())
    	{
    		return;
    	}
    	if(event.getEntity() instanceof LivingEntity living && !(living instanceof Player) && MorphUtil.isChangedDimension(living))
    	{
    		living.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t ->
    		{
    			if(t.getMorph() == null && t.getType() != null && t.getType().create(living.level) instanceof LivingEntity morph)
    			{
    				t.setMorph(morph);
    			}
    			MorphUtil.setChangedDimension(living, false);
    		});
    	}
    }
    
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event)
    {
    	LivingEntity target = event.getEntity();
    	Entity attacker = event.getSource().getEntity();
    	if(attacker instanceof LivingEntity livingAttacker && MorphUtil.areFriendly(livingAttacker, target))
    	{
    		event.setCanceled(true);
    		return;
    	}
    	LivingEntity effectiveTarget = MorphUtil.getEffectiveLivingTarget(target);
    	if(attacker instanceof Mob mobAttacker)
    	{
    		if(effectiveTarget != target && !mobAttacker.canAttack(effectiveTarget))
    		{
    			event.setCanceled(true);
    		}
    	}
    }
    
    @SubscribeEvent
    public static void onLivingConversion(LivingConversionEvent.Pre event)
    {
    	if(MorphUtil.isMorph(event.getEntity()))
    	{
    		event.setConversionTimer(0);
    		event.setCanceled(true);
    	}
    }
    
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event)
    {
    	if(event.getRayTraceResult() instanceof EntityHitResult entityHit)
    	{
    		if(MorphUtil.isMorph(entityHit.getEntity()))
    		{
            	event.setImpactResult(ImpactResult.SKIP_ENTITY);
    		}
    	}
    }
    
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event)
    {
    	Explosion explosion = event.getExplosion();
    	if(explosion.getIndirectSourceEntity() != null)
    	{
    		LivingEntity source = explosion.getIndirectSourceEntity();
    		if(MorphUtil.getMorphOwner(source) != null)
    		{
    			Entity owner = MorphUtil.getMorphOwner(source);
    			List<Entity> list = event.getAffectedEntities();
    			if(list.contains(owner) && !list.isEmpty())
    			{
    				list.removeIf(t -> t == owner);
    			}
    		}
    	}
    }
    
    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event)
    {
   		if(event.getTarget() instanceof LivingEntity living)
		{
   			living.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t ->
        	{
        		if(t.getMorph() != null)
        		{
        			t.syncMorph();
        		}
        	});
		}
    }
    
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
    	if(event.isWasDeath())
    	{
        	Player original = event.getOriginal();
        	original.revive();
        	Player player = event.getEntity();
        	original.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t ->
        	{
        		if(t.getMorph() != null && t.isPersistent())
        		{
        			player.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t2 ->
                	{
                		t2.setType(t.getMorph().getType());
                		t2.setPersistent(true);
                	});
        		}
        	});
    	}
    }
}
