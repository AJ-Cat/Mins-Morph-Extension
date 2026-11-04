/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.capabilities;

import java.util.List;

import com.ajcat.minsmorph.MinsMorph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

@AutoRegisterCapability
public interface IMorphCapability extends ICapabilitySerializable<CompoundTag>
{
	ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MinsMorph.MODID, "morph");

	void tick();
	
	void setPersistent(boolean isPersistent);
	
	boolean isPersistent();
	
	void setMorph(LivingEntity morph);
	
	LivingEntity getMorph();
	
	void setType(EntityType<?> type);
	
	EntityType<?> getType();
	
	LivingEntity getFakeTarget();
	
	void syncMorph();
	
	void setData(String dataName, String dataValue);
	
	void setChangedDimension(boolean isChangedDimension);
	
	boolean isChangedDimension();
	
	void selectGoal(String selectedGoal);
	
	String getSelectedGoal();
	
	void selectAnimation(String selectedAnimation);
	
	String getSelectedAnimation();

	void setSelectedGoalId(int selectedGoalId);

	int getSelectedGoalId();

	void setSelectedAnimationId(int selectedAnimationId);

	int getSelectedAnimationId();

	void setGoalOptions(List<String> options);

	List<String> getGoalOptions();

	void setAnimationOptions(List<String> options);

	List<String> getAnimationOptions();

	void setDataOptions(List<String> options);

	List<String> getDataOptions();

	void setOptionsRevision(int revision);

	int getOptionsRevision();
}
