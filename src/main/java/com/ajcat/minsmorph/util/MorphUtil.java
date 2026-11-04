/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.Lists;
import com.ajcat.minsmorph.capabilities.IMorphCapability;
import com.ajcat.minsmorph.capabilities.MorphCapabilityImpl;
import com.ajcat.minsmorph.config.MorphConfig;
import com.ajcat.minsmorph.entity.EntityFakeTarget;
import com.ajcat.minsmorph.entity.MorphEntities;
import com.ajcat.minsmorph.misc.ILevelEntityGetterAdapter;
import com.ajcat.minsmorph.misc.IWrappedGoal;
import com.ajcat.minsmorph.network.MorphNetwork;
import com.ajcat.minsmorph.network.SyncMorphOptionsPacket;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.util.BrainBuilder;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public class MorphUtil 
{
	public static final String MANUAL_TRIGGER_TICKS_KEY = "MorphManualTriggerTicks";
	public static final String BRAIN_ACTION_PREFIX = "brain:";
	private static final Pattern GOAL_INDEX_PATTERN = Pattern.compile("^(.*?)(\\d+)$");
	private static final List<MobActionAdapter> ACTION_ADAPTERS = new ArrayList<>();
	private static final int MAX_MANUAL_BRAIN_TICKS = 200;
	private static final Map<UUID, List<ManualBrainAction>> MANUAL_BRAIN_ACTIONS = new HashMap<>();

	public static final Map<Integer, Entity> ENTITY_MAP = new HashMap<>();
	public static final Map<Integer, Entity> ENTITY_MAP2 = new HashMap<>();
	
	public static final List<Attribute> ATTRIBUTES = Lists.newArrayList(Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.ATTACK_DAMAGE, Attributes.ATTACK_KNOCKBACK, Attributes.FLYING_SPEED, Attributes.FOLLOW_RANGE, Attributes.JUMP_STRENGTH, Attributes.KNOCKBACK_RESISTANCE, Attributes.MAX_HEALTH);
	
	public static final Method GET_ENTITY = ObfuscationReflectionHelper.findMethod(Level.class, "m_142646_");

	static
	{
		ACTION_ADAPTERS.add(new IronsSpellbooksAdapter());
		ACTION_ADAPTERS.add(new MowzieAbilityAdapter());
		ACTION_ADAPTERS.add(new AquamiraePlayAdapter());
		ACTION_ADAPTERS.add(new MutantMethodAdapter());
		ACTION_ADAPTERS.add(new GenericFieldAnimationAdapter());
	}
	
	public static void selectAnimation(LivingEntity owner, String animationName)
	{
		owner.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
		{
			t.selectAnimation(animationName);
		});
	}
	
	public static void selectGoal(LivingEntity owner, String goalName)
	{
		owner.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
		{
			t.selectGoal(goalName);
		});
	}
	
	public static void selectOption(ServerPlayer sender, UUID ownerUuid, MorphSelectionType selectionType, int selectionId, int revision)
	{
		if(sender == null || !sender.getUUID().equals(ownerUuid))
		{
			sendClientWarning(sender, "minsmorph.sync.invalid_owner");
			return;
		}
		selectOption(sender, selectionType, selectionId, revision);
	}

	public static void selectOption(LivingEntity owner, MorphSelectionType selectionType, int selectionId, int revision)
	{
		owner.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t ->
		{
			if(t.getOptionsRevision() != revision)
			{
				if(owner instanceof ServerPlayer serverPlayer)
				{
					sendClientWarning(serverPlayer, "minsmorph.sync.stale");
				}
				syncOptions(owner);
				return;
			}
			if(selectionType == MorphSelectionType.GOAL)
			{
				List<String> goals = t.getGoalOptions();
				if(selectionId >= 0 && selectionId < goals.size())
				{
					t.setSelectedGoalId(selectionId);
					t.selectGoal(goals.get(selectionId));
					syncOptions(owner);
				}
				else if(owner instanceof ServerPlayer serverPlayer)
				{
					sendClientWarning(serverPlayer, "minsmorph.sync.invalid_selection");
				}
			}
			else if(selectionType == MorphSelectionType.ANIMATION)
			{
				List<String> animations = t.getAnimationOptions();
				if(selectionId >= 0 && selectionId < animations.size())
				{
					t.setSelectedAnimationId(selectionId);
					t.selectAnimation(animations.get(selectionId));
					syncOptions(owner);
				}
				else if(owner instanceof ServerPlayer serverPlayer)
				{
					sendClientWarning(serverPlayer, "minsmorph.sync.invalid_selection");
				}
			}
		});
	}

	public static void triggerGoal(LivingEntity owner)
	{
		owner.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
		{
			String goalName = t.getSelectedGoal();
			String animationName = t.getSelectedAnimation();
			if(t.getMorph() instanceof Mob mob && (goalName == null || goalName.isBlank()) && animationName != null && !animationName.isBlank())
			{
				if(!triggerAnimationOnly(owner, mob, animationName))
				{
					if(owner instanceof ServerPlayer serverPlayer)
					{
						sendClientWarning(serverPlayer, "minsmorph.sync.unsupported_action");
					}
				}
				return;
			}
			if(t.getMorph() instanceof Mob mob && goalName != null && !goalName.isBlank())
			{
				String trimmedGoalName = goalName.trim();
				if(trimmedGoalName.startsWith(BRAIN_ACTION_PREFIX))
				{
					if(!triggerBrainAction(owner, mob, trimmedGoalName, animationName) && owner instanceof ServerPlayer serverPlayer)
					{
						sendClientWarning(serverPlayer, "minsmorph.sync.unsupported_action");
					}
					return;
				}
				LivingEntity realTarget = findRealTarget(owner);
				LivingEntity fallbackTarget = realTarget != null ? null : createEphemeralFakeTarget(owner);
				if(realTarget == null && (fallbackTarget == null || !fallbackTarget.isAlive()))
				{
					if(owner instanceof ServerPlayer serverPlayer)
					{
						sendClientWarning(serverPlayer, "minsmorph.sync.missing_target");
					}
					syncOptions(owner);
					return;
				}
				Set<WrappedGoal> set = mob.goalSelector.getAvailableGoals();
				List<WrappedGoal> list = Lists.newArrayList(set);
				String normalizedGoalName = goalName.trim();
				int expectedIndex = getGoalIndexSuffix(normalizedGoalName);
				String baseGoalName = removeGoalIndexSuffix(normalizedGoalName);
				boolean triggeredGoal = false;
				for(WrappedGoal goal : set)
				{
					int index = list.indexOf(goal);
					String name = MorphUtil.getGoalName(goal.getGoal());
					boolean indexMatched = expectedIndex >= 0 && index == expectedIndex && name.equals(baseGoalName);
					if(indexMatched || name.equals(normalizedGoalName))
					{
						mob.getPersistentData().putInt(MANUAL_TRIGGER_TICKS_KEY, 20);
						((IWrappedGoal) goal).setCanUse();
						MorphUtil.setTarget(owner, mob, goal, fallbackTarget, realTarget);
						MorphUtil.setAnimation(mob, animationName);
						try
						{
							// Prefer normal GoalSelector path so goals that rely on selector-managed state can initialize properly.
							mob.goalSelector.tick();
							if(!goal.isRunning())
							{
								goal.start();
							}
							// Some custom goals (e.g. teleport-type goals) require at least one tick after start.
							if(goal.isRunning())
							{
								goal.tick();
							}
						}
						catch(Exception e)
						{
							goal.stop();
							mob.setTarget(null);
							if(owner instanceof ServerPlayer serverPlayer)
							{
								sendClientWarning(serverPlayer, "minsmorph.sync.goal_failed");
							}
						}
						triggeredGoal = true;
						break;
					}
				}
				if(!triggeredGoal && owner instanceof ServerPlayer serverPlayer)
				{
					sendClientWarning(serverPlayer, "minsmorph.sync.unsupported_action");
				}
			}
		});
	}

	private static LivingEntity createEphemeralFakeTarget(LivingEntity owner)
	{
		if(owner.level.isClientSide)
		{
			return null;
		}
		EntityFakeTarget fakeTarget = new EntityFakeTarget(MorphEntities.FAKE_TARGET.get(), owner.level);
		fakeTarget.setOwner(owner);
		fakeTarget.bindToOwnerMorph(owner);
		fakeTarget.setLifeTicks(MAX_MANUAL_BRAIN_TICKS);
		fakeTarget.setFollowDistance(2.0D);
		owner.level.addFreshEntity(fakeTarget);
		return fakeTarget;
	}

	private static LivingEntity findRealTarget(LivingEntity owner)
	{
		HitResult hitResult = ProjectileUtil.getHitResultOnViewVector(owner, entity -> !entity.isAlliedTo(owner), 30.0F);
		if(hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living)
		{
			return living;
		}
		return null;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<BrainAction> getBrainActions(Mob mob)
	{
		List<BrainAction> actions = new ArrayList<>();
		Brain<?> brain = mob.getBrain();
		if(brain == null)
		{
			return actions;
		}
		try
		{
			BrainBuilder builder = brain.createBuilder();
			Map<Integer, Map<Activity, Set<BehaviorControl>>> byPriority = builder.getAvailableBehaviorsByPriority();
			Map<String, Integer> counts = new HashMap<>();
			for(Map.Entry<Integer, Map<Activity, Set<BehaviorControl>>> priorityEntry : byPriority.entrySet())
			{
				for(Map.Entry<Activity, Set<BehaviorControl>> activityEntry : priorityEntry.getValue().entrySet())
				{
					String activityName = sanitizeBrainName(activityEntry.getKey().getName());
					for(BehaviorControl behavior : activityEntry.getValue())
					{
						String baseName = BRAIN_ACTION_PREFIX + activityName + ":" + sanitizeBrainName(getBrainBehaviorName(behavior));
						int count = counts.merge(baseName, 1, Integer::sum);
						actions.add(new BrainAction(count == 1 ? baseName : baseName + count, behavior));
					}
				}
			}
		}
		catch(Exception e)
		{
			
		}
		return actions;
	}

	private static String getBrainBehaviorName(BehaviorControl<?> behavior)
	{
		String name = behavior.debugString();
		if(name == null || name.isBlank())
		{
			Class<?> clazz = behavior.getClass();
			name = clazz.isAnonymousClass() ? clazz.getSuperclass().getSimpleName() : clazz.getSimpleName();
		}
		return name == null || name.isBlank() ? "behavior" : name;
	}

	private static String sanitizeBrainName(String name)
	{
		return name == null || name.isBlank() ? "unknown" : name.replaceAll("[^A-Za-z0-9_.-]", "_");
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean triggerBrainAction(LivingEntity owner, Mob mob, String actionName, String animationName)
	{
		BrainAction action = null;
		for(BrainAction candidate : getBrainActions(mob))
		{
			if(candidate.name.equals(actionName))
			{
				action = candidate;
				break;
			}
		}
		if(action == null || !(mob.level instanceof ServerLevel level))
		{
			return false;
		}
		if(action.behavior.getStatus() == Behavior.Status.RUNNING)
		{
			if(animationName != null && !animationName.isBlank())
			{
				setAnimation(mob, animationName);
			}
			return true;
		}
		LivingEntity realTarget = findRealTarget(owner);
		LivingEntity target = realTarget;
		boolean fakeTarget = false;
		boolean started = tryStartBrainAction(mob, action, level);
		if(!started)
		{
			if(target == null)
			{
				target = createEphemeralFakeTarget(owner);
				fakeTarget = target != null;
			}
			if(target != null)
			{
				setBrainTarget(mob, target);
				started = tryStartBrainAction(mob, action, level);
			}
		}
		if(!started)
		{
			if(fakeTarget && target != null)
			{
				clearBrainTarget(mob, target);
			}
			return false;
		}
		if(animationName != null && !animationName.isBlank())
		{
			setAnimation(mob, animationName);
		}
		if(action.behavior.getStatus() == Behavior.Status.RUNNING)
		{
			registerManualBrainAction(owner, mob, action.behavior, target, fakeTarget);
		}
		return true;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean tryStartBrainAction(Mob mob, BrainAction action, ServerLevel level)
	{
		try
		{
			return action.behavior.tryStart(level, mob, level.getGameTime());
		}
		catch(Exception e)
		{
			return action.behavior.getStatus() == Behavior.Status.RUNNING;
		}
	}

	private static void setBrainTarget(Mob mob, LivingEntity target)
	{
		mob.setTarget(target);
		Brain<?> brain = mob.getBrain();
		if(brain != null)
		{
			brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
			brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
			brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, 1.0F, 1));
			brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, new NearestVisibleLivingEntities(mob, List.of(target)));
		}
	}

	private static void clearBrainTarget(Mob mob, LivingEntity target)
	{
		if(mob.getTarget() == target)
		{
			mob.setTarget(null);
		}
		Brain<?> brain = mob.getBrain();
		if(brain != null)
		{
			if(brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == target)
			{
				brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
			}
			brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
			brain.eraseMemory(MemoryModuleType.WALK_TARGET);
			brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerManualBrainAction(LivingEntity owner, Mob mob, BehaviorControl behavior, LivingEntity target, boolean fakeTarget)
	{
		if(!(mob.level instanceof ServerLevel))
		{
			return;
		}
		List<ManualBrainAction> list = MANUAL_BRAIN_ACTIONS.computeIfAbsent(owner.getUUID(), uuid -> new ArrayList<>());
		Iterator<ManualBrainAction> iterator = list.iterator();
		while(iterator.hasNext())
		{
			ManualBrainAction action = iterator.next();
			if(action.mob == mob)
			{
				action.stop();
				iterator.remove();
			}
		}
		list.add(new ManualBrainAction(mob, behavior, target, fakeTarget));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void tickManualBrainActions(LivingEntity owner, LivingEntity morph)
	{
		if(owner.level.isClientSide)
		{
			return;
		}
		List<ManualBrainAction> list = MANUAL_BRAIN_ACTIONS.get(owner.getUUID());
		if(list == null || list.isEmpty())
		{
			return;
		}
		if(!(morph.level instanceof ServerLevel level))
		{
			for(ManualBrainAction action : list)
			{
				action.cleanup();
			}
			MANUAL_BRAIN_ACTIONS.remove(owner.getUUID());
			return;
		}
		Iterator<ManualBrainAction> iterator = list.iterator();
		while(iterator.hasNext())
		{
			ManualBrainAction action = iterator.next();
			if(action.mob != morph || !action.mob.isAlive() || action.ticks++ >= MAX_MANUAL_BRAIN_TICKS)
			{
				action.stop();
				iterator.remove();
				continue;
			}
			try
			{
				Brain<?> brain = action.mob.getBrain();
				boolean handledByBrain = brain != null && brain.getRunningBehaviors().contains(action.behavior);
				if(!handledByBrain)
				{
					action.behavior.tickOrStop(level, action.mob, level.getGameTime());
				}
			}
			catch(Exception e)
			{
				action.stop();
				iterator.remove();
				continue;
			}
			if(action.behavior.getStatus() != Behavior.Status.RUNNING)
			{
				action.cleanup();
				iterator.remove();
			}
		}
		if(list.isEmpty())
		{
			MANUAL_BRAIN_ACTIONS.remove(owner.getUUID());
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void clearManualBrainActions(LivingEntity owner)
	{
		List<ManualBrainAction> list = MANUAL_BRAIN_ACTIONS.remove(owner.getUUID());
		if(list != null)
		{
			for(ManualBrainAction action : list)
			{
				action.stop();
			}
		}
	}

	private static boolean triggerAnimationOnly(LivingEntity owner, Mob mob, String animationName)
	{
		boolean success = setAnimation(mob, animationName);
		if(!success && owner instanceof ServerPlayer serverPlayer)
		{
			sendClientWarning(serverPlayer, "minsmorph.sync.adapter_failed");
		}
		return success;
	}

	public static void triggerGoal(ServerPlayer sender, UUID ownerUuid, int revision)
	{
		if(sender == null || !sender.getUUID().equals(ownerUuid))
		{
			sendClientWarning(sender, "minsmorph.sync.invalid_owner");
			return;
		}
		sender.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t ->
		{
			if(t.getOptionsRevision() != revision)
			{
				sendClientWarning(sender, "minsmorph.sync.stale");
				syncOptions(sender);
				return;
			}
			List<String> goals = t.getGoalOptions();
			int selectedGoalId = t.getSelectedGoalId();
			if(selectedGoalId < 0 || selectedGoalId >= goals.size())
			{
				sendClientWarning(sender, "minsmorph.sync.no_goal_selected");
				syncOptions(sender);
				return;
			}
			selectGoal(sender, goals.get(selectedGoalId));
			int selectedAnimationId = t.getSelectedAnimationId();
			List<String> animations = t.getAnimationOptions();
			if(selectedAnimationId >= 0 && selectedAnimationId < animations.size())
			{
				selectAnimation(sender, animations.get(selectedAnimationId));
			}
			triggerGoal((LivingEntity) sender);
		});
	}

	public static boolean selectGoalByName(LivingEntity owner, String goalName)
	{
		if(goalName == null || goalName.isBlank())
		{
			return false;
		}
		IMorphCapability cap = owner.getCapability(MorphCapabilityImpl.MORPH).orElse(new MorphCapabilityImpl(owner));
		int selectionId = cap.getGoalOptions().indexOf(goalName);
		if(selectionId < 0)
		{
			return false;
		}
		selectOption(owner, MorphSelectionType.GOAL, selectionId, cap.getOptionsRevision());
		return true;
	}

	public static boolean selectAnimationByName(LivingEntity owner, String animationName)
	{
		if(animationName == null || animationName.isBlank())
		{
			return false;
		}
		IMorphCapability cap = owner.getCapability(MorphCapabilityImpl.MORPH).orElse(new MorphCapabilityImpl(owner));
		int selectionId = cap.getAnimationOptions().indexOf(animationName);
		if(selectionId < 0)
		{
			return false;
		}
		selectOption(owner, MorphSelectionType.ANIMATION, selectionId, cap.getOptionsRevision());
		return true;
	}

	private static void sendClientWarning(ServerPlayer player, String translationKey)
	{
		if(player != null)
		{
			player.displayClientMessage(Component.translatable(translationKey), true);
		}
	}

	private static int getGoalIndexSuffix(String goalName)
	{
		Matcher matcher = GOAL_INDEX_PATTERN.matcher(goalName);
		if(matcher.matches())
		{
			try
			{
				return Integer.parseInt(matcher.group(2));
			}
			catch(NumberFormatException ignored)
			{
				
			}
		}
		return -1;
	}

	private static String removeGoalIndexSuffix(String goalName)
	{
		Matcher matcher = GOAL_INDEX_PATTERN.matcher(goalName);
		if(matcher.matches())
		{
			return matcher.group(1);
		}
		return goalName;
	}
	public static void setTarget(LivingEntity owner, Mob morph, LivingEntity fakeTarget)
	{
		setTarget(owner, morph, null, fakeTarget, null);
	}
	
	public static void setTarget(LivingEntity owner, Mob morph, WrappedGoal goal, LivingEntity fakeTarget)
	{
		setTarget(owner, morph, goal, fakeTarget, null);
	}

	public static void setTarget(LivingEntity owner, Mob morph, WrappedGoal goal, LivingEntity fakeTarget, LivingEntity preferredTarget)
	{
		if(goal != null && fakeTarget != null)
		{
			((IWrappedGoal) goal).setFakeTarget(fakeTarget);
		}
		if(preferredTarget != null)
		{
			if(goal != null)
			{
				((IWrappedGoal) goal).setTarget(preferredTarget);
			}
			morph.setTarget(preferredTarget);
			return;
		}
		HitResult hitResult = ProjectileUtil.getHitResultOnViewVector(owner, entity -> !entity.isAlliedTo(owner), 30.0F);
		if(hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living)
		{
			if(goal != null)
			{
				((IWrappedGoal) goal).setTarget(living);
			}
			morph.setTarget(living);
			return;
		}
		if(fakeTarget != null)
		{
			morph.setTarget(fakeTarget);
		}
	}
	
	public static void getMorph(Entity entity, Consumer<LivingEntity> consumer)
	{
		entity.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
		{
    		LivingEntity morph = t.getMorph();
    		if(morph != null)
    		{
    			consumer.accept(morph);
    		}
		});
	}
	
	public static boolean isMorph(Entity entity)
	{
		return entity.getId() < 0 && !(entity instanceof Player);
	}
	
	public static void setChangedDimension(LivingEntity living, boolean value)
	{
		IMorphCapability cap = living.getCapability(MorphCapabilityImpl.MORPH).orElse(new MorphCapabilityImpl(living));
		cap.setChangedDimension(value);
	}
	
	public static boolean isChangedDimension(LivingEntity living)
	{
		IMorphCapability cap = living.getCapability(MorphCapabilityImpl.MORPH).orElse(new MorphCapabilityImpl(living));
		return cap.isChangedDimension();
	}
	
	public static boolean isPersistent(LivingEntity living)
	{
		IMorphCapability cap = living.getCapability(MorphCapabilityImpl.MORPH).orElse(new MorphCapabilityImpl(living));
		return cap.isPersistent();
	}
	
	public static boolean hasMorph(LivingEntity living)
	{
		IMorphCapability cap = living.getCapability(MorphCapabilityImpl.MORPH).orElse(new MorphCapabilityImpl(living));
		return cap.getMorph() != null;
	}

	public static boolean canMorphFly(LivingEntity morph)
	{
		if(!(morph instanceof Mob mob))
		{
			return false;
		}
		// Explicit config overrides always win, then fall back to several heuristics used by vanilla and modded flying mobs.
		if(MorphConfig.isFlightOverride(morph.getType()))
		{
			return true;
		}
		if(mob instanceof FlyingAnimal)
		{
			return true;
		}
		MoveControl control = mob.getMoveControl();
		PathNavigation navigation = mob.getNavigation();
		if(control instanceof FlyingMoveControl || navigation instanceof FlyingPathNavigation)
		{
			return true;
		}
		if(mob.getAttribute(Attributes.FLYING_SPEED) != null && mob.getAttributeValue(Attributes.FLYING_SPEED) > 0.0D)
		{
			return true;
		}
		return hasFlightLikeName(control) || hasFlightLikeName(navigation) || hasFlightLikeName(mob);
	}

	private static boolean hasFlightLikeName(Object object)
	{
		if(object == null)
		{
			return false;
		}
		Class<?> current = object.getClass();
		while(current != null && current != Object.class)
		{
			String name = current.getSimpleName().toLowerCase();
			if(name.contains("flight") || name.contains("flying") || name.contains("fly") || name.contains("wing") || name.contains("aerial") || name.contains("avian"))
			{
				return true;
			}
			current = current.getSuperclass();
		}
		return false;
	}

	public static void syncOptions(LivingEntity owner)
	{
		if(owner.level.isClientSide)
		{
			return;
		}
		owner.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t ->
		{
			if(owner instanceof ServerPlayer serverPlayer)
			{
				MorphNetwork.sendToPlayer(new SyncMorphOptionsPacket(owner.getUUID(), t.getOptionsRevision(), t.getGoalOptions(), t.getAnimationOptions(), t.getDataOptions(), t.getSelectedGoalId(), t.getSelectedAnimationId()), serverPlayer);
			}
		});
	}
	
	public static void removeMorph(LivingEntity living)
	{
		clearManualBrainActions(living);
		living.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
		{
			if(hasMorph(living))
			{
				if(living instanceof Player player)
				{
					// noGravity is set by every morph tick, including for creative/spectator players.
					// It must always be cleared on reset, otherwise a flying morph leaves the player
					// permanently suspended after unmorphing.
					player.setNoGravity(false);
					player.getAbilities().flying = false;
					if(!player.getAbilities().instabuild)
					{
						player.getAbilities().mayfly = false;
					}
					player.onUpdateAbilities();
				}
				t.setMorph(null);
				t.setType(null);
				t.setPersistent(false);
				living.heal(living.getMaxHealth());
				living.refreshDimensions();
			}
		});
	}
    
	public static String getGoalName(Goal goal)
	{
		String name = goal.getClass().getSimpleName();
		if(goal.getClass().isAnonymousClass())
		{
			name = goal.getClass().getSuperclass().getSimpleName();
		}
		return name;
	}
	
	@SuppressWarnings("unchecked")
	public static boolean setData(Mob mob, String dataName, String dataValue)
	{
		if(dataName == null || dataValue == null)
		{
			return false;
		}
		try
		{
			Field f = findFieldInHierarchy(mob.getClass(), dataName);
			if(f == null || !EntityDataAccessor.class.isAssignableFrom(f.getType()))
			{
				return false;
			}
			f.setAccessible(true);
			Object fieldValue = f.get(Modifier.isStatic(f.getModifiers()) ? null : mob);
			if(!(fieldValue instanceof EntityDataAccessor<?> accessor))
			{
				return false;
			}
			EntityDataSerializer<?> serializer = accessor.getSerializer();
			Object parsedValue;
			if(serializer == EntityDataSerializers.BOOLEAN)
			{
				if(dataValue.equalsIgnoreCase("true") || dataValue.equals("1"))
				{
					parsedValue = Boolean.TRUE;
				}
				else if(dataValue.equalsIgnoreCase("false") || dataValue.equals("0"))
				{
					parsedValue = Boolean.FALSE;
				}
				else
				{
					return false;
				}
			}
			else if(serializer == EntityDataSerializers.INT)
			{
				parsedValue = Integer.parseInt(dataValue);
			}
			else if(serializer == EntityDataSerializers.FLOAT)
			{
				parsedValue = Float.parseFloat(dataValue);
			}
			else if(serializer == EntityDataSerializers.STRING)
			{
				parsedValue = dataValue;
			}
			else if(serializer == EntityDataSerializers.BYTE)
			{
				parsedValue = (byte) Integer.parseInt(dataValue);
			}
			else
			{
				return false;
			}
			mob.getEntityData().set((EntityDataAccessor<Object>) accessor, parsedValue);
			return true;
		}
		catch (SecurityException | IllegalAccessException | IllegalArgumentException e)
		{
			
		}
		return false;
	}

	public static List<String> getSettableDatas(Mob mob)
	{
		List<String> list = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		Class<?> current = mob.getClass();
		while(current != null && current != Object.class)
		{
			for(Field f : current.getDeclaredFields())
			{
				if(!EntityDataAccessor.class.isAssignableFrom(f.getType()) || !seen.add(f.getName()))
				{
					continue;
				}
				try
				{
					f.setAccessible(true);
					Object fieldValue = f.get(Modifier.isStatic(f.getModifiers()) ? null : mob);
					if(fieldValue instanceof EntityDataAccessor<?> accessor && isSettableSerializer(accessor.getSerializer()))
					{
						list.add(f.getName());
					}
				}
				catch (SecurityException | IllegalAccessException | IllegalArgumentException e)
				{
					
				}
			}
			current = current.getSuperclass();
		}
		return list;
	}

	private static boolean isSettableSerializer(EntityDataSerializer<?> serializer)
	{
		return serializer == EntityDataSerializers.BOOLEAN || serializer == EntityDataSerializers.INT || serializer == EntityDataSerializers.FLOAT || serializer == EntityDataSerializers.STRING || serializer == EntityDataSerializers.BYTE;
	}
	
	public static boolean setAnimation(Mob mob, String animationName)
    {
		if(animationName == null || animationName.isBlank())
		{
			return false;
		}
		for(MobActionAdapter adapter : ACTION_ADAPTERS)
		{
			if(adapter.supports(mob))
			{
				boolean success = adapter.execute(mob, animationName);
				if(success)
				{
					return true;
				}
			}
		}
		return false;
    }

	private static Field findFieldInHierarchy(Class<?> clazz, String fieldName)
	{
		Class<?> current = clazz;
		while(current != null && current != Object.class)
		{
			try
			{
				return current.getDeclaredField(fieldName);
			}
			catch(NoSuchFieldException ignored)
			{
				current = current.getSuperclass();
			}
		}
		return null;
	}

	private static Method findMethodAssignable(Class<?> clazz, String methodName, Class<?> paramType)
	{
		for(Method method : clazz.getMethods())
		{
			if(method.getName().equals(methodName) && method.getParameterCount() == 1)
			{
				Class<?> expected = method.getParameterTypes()[0];
				if(expected.isAssignableFrom(paramType))
				{
					return method;
				}
			}
		}
		return null;
	}
	
	public static List<String> getDatas(Class<?> clazz)
	{
		Set<String> list = new LinkedHashSet<>();
		Class<?> current = clazz;
		while(current != null && current != Object.class)
		{
			for(Field f : current.getDeclaredFields())
			{
				if(EntityDataAccessor.class.isAssignableFrom(f.getType()))
				{
					list.add(f.getName());
				}
			}
			current = current.getSuperclass();
		}
		return new ArrayList<>(list);
	}
    
	public static List<String> getGoals(Mob mob)
	{
		List<String> list = new ArrayList<>();
		Set<WrappedGoal> set = mob.goalSelector.getAvailableGoals();
		List<WrappedGoal> goals = Lists.newArrayList(set);
		for(WrappedGoal goal : set)
		{
			((IWrappedGoal)goal).setEntity(mob);
			String goalName = goal.getGoal().getClass().getSimpleName();
			if(goal.getGoal().getClass().isAnonymousClass())
			{
    			goalName = goal.getGoal().getClass().getSuperclass().getSimpleName();
			}
			if(list.contains(goalName))
			{
				goalName = goalName + goals.indexOf(goal);
			}
			list.add(goalName);
		}
		for(BrainAction action : getBrainActions(mob))
		{
			list.add(action.name);
		}
		return list;
	}
	
	public static List<String> getAnimations(Class<?> clazz)
	{
		List<String> list = new ArrayList<>();
		for(MobActionAdapter adapter : ACTION_ADAPTERS)
		{
			for(String action : adapter.scan(clazz))
			{
				if(!list.contains(action))
				{
					list.add(action);
				}
			}
		}
		return list;
	}
	
	public static void getMethodCalls(Class<?> clazz, Consumer<MethodInsnNode> consumer)
	{
        try
        {
    		ClassNode classNode = getClassNode(clazz);
    		for(MethodNode method : classNode.methods)
    		{
    		    for(AbstractInsnNode ain : method.instructions.toArray())
    		    {
    		        if(ain.getType() == AbstractInsnNode.METHOD_INSN) 
    		        {
    		        	MethodInsnNode min = (MethodInsnNode) ain;
    		            consumer.accept(min);
    		        }
    		    }
    		}
        }
        catch (IOException | SecurityException e) 
        {
        	
		}
	}
	
	public static void getFieldCalls(Class<?> clazz, Consumer<FieldInsnNode> consumer)
	{
        try
        {
    		ClassNode classNode = getClassNode(clazz);
    		for(MethodNode method : classNode.methods)
    		{
    		    for(AbstractInsnNode ain : method.instructions.toArray())
    		    {
    		        if(ain.getType() == AbstractInsnNode.FIELD_INSN) 
    		        {
    		        	FieldInsnNode fin = (FieldInsnNode) ain;
    		            consumer.accept(fin);
    		        }
    		    }
    		}
        }
        catch (IOException | SecurityException e) 
        {
        	
		}
	}

    public static ClassNode getClassNode(Class<?> clazz) throws IOException
    {
        String className = clazz.getName().replace('.', '/') + ".class";
        ClassLoader classLoader = clazz.getClassLoader();
        if(classLoader == null)
        {
        	classLoader = ClassLoader.getSystemClassLoader();
        }
        try(InputStream classStream = classLoader.getResourceAsStream(className))
        {
            if(classStream == null)
            {
                throw new IOException("Class not found: " + clazz.getName());
            }
            ClassReader classReader = new ClassReader(classStream);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            return classNode;
        }
    }
    
    public static void tick(LivingEntity player, LivingEntity morph)
    {
    	morph.getPersistentData().putUUID("MorphOwnerUUID", player.getUUID());
    	player.eyeHeight = morph.eyeHeight;
    	player.dimensions = morph.dimensions;
    	player.refreshDimensions();
		if(!player.isAlive())
		{
			morph.discard();
		}
		else
		{
    		if(morph instanceof Mob mob && mob.getPersistentData().contains(MANUAL_TRIGGER_TICKS_KEY))
    		{
    			int ticks = mob.getPersistentData().getInt(MANUAL_TRIGGER_TICKS_KEY);
    			if(ticks > 0)
    			{
    				mob.getPersistentData().putInt(MANUAL_TRIGGER_TICKS_KEY, ticks - 1);
    			}
    		}
	    	sync(morph, player);
			for(Entity entity : morph.getPassengers())
			{
				tickPassenger(morph, entity);
			}
			try
			{
				ILevelEntityGetterAdapter adapter = (ILevelEntityGetterAdapter) GET_ENTITY.invoke(player.level);
				adapter.byId().putIfAbsent(morph.getId(), morph);
				adapter.byId().values().removeIf(t -> !t.isAlive());
				adapter.byUuid().putIfAbsent(morph.getUUID(), morph);
				adapter.byUuid().values().removeIf(t -> !t.isAlive());
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
	    	if(morph instanceof Mob mob)
	    	{
	    		if(!player.level.isClientSide)
	    		{
	    			tickManualBrainActions(player, morph);
	        		serverAiStep(mob);
	    		}
				if(mob.getTarget() != null)
				{
					if(!mob.getTarget().isAlive())
					{
						mob.setTarget(null);
					}
				}
	    	}
		}
    }
    
    public static void tickPassenger(Entity vehicle, Entity passenger)
    {
    	if(passenger.getVehicle() == vehicle)
    	{
        	passenger.rideTick();
            for(Entity entity : passenger.getPassengers())
            {
            	tickPassenger(passenger, entity);
            }
    	}
    }
    
	//from ichun's morph mod;
    public static void sync(LivingEntity morph, LivingEntity player)
    {
        morph.tickCount = player.tickCount;

        morph.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        
        morph.xOld = player.xOld;
        morph.yOld = player.yOld;
        morph.zOld = player.zOld;

        morph.xo = player.xo;
        morph.yo = player.yo;
        morph.zo = player.zo;

        morph.yRotO = player.yRotO;
        morph.xRotO = player.xRotO;

        morph.yHeadRot = player.yHeadRot;
        morph.yHeadRotO = player.yHeadRotO;

        morph.yBodyRot = player.yBodyRot;
        morph.yBodyRotO = player.yBodyRotO;
        
        morph.walkAnimation = player.walkAnimation;

        morph.horizontalCollision = player.horizontalCollision;
        morph.verticalCollision = player.verticalCollision;
        
        morph.setOnGround(player.onGround());
        morph.setSwimming(player.isSwimming());
        morph.setSprinting(player.isSprinting());

        morph.hurtTime = player.hurtTime;
        morph.deathTime = player.deathTime;
        morph.fallDistance = player.fallDistance;

        morph.swingTime = player.swingTime;
        morph.swinging = player.swinging;
        morph.swingingArm = player.swingingArm;
        morph.attackAnim = player.attackAnim;
        morph.oAttackAnim = player.oAttackAnim;
    }
	
    public static void serverAiStep(Mob mob)
    {
        mob.setNoActionTime(mob.getNoActionTime() + 1);
        mob.level.getProfiler().push("sensing");
        mob.getSensing().tick();
        mob.level.getProfiler().pop();
        int i = mob.level.getServer().getTickCount() + mob.getId();
        if(i % 2 != 0 && mob.tickCount > 1)
        {
        	mob.level.getProfiler().push("goalSelector");
        	try
        	{
            	mob.goalSelector.tickRunningGoals(false);
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        		mob.setTarget(null);
        	}
        	mob.level.getProfiler().pop();
        }
        else 
        {
        	mob.level.getProfiler().push("goalSelector");
        	try
        	{
        		mob.goalSelector.tick();
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        	mob.level.getProfiler().pop();
        }

        mob.level.getProfiler().push("navigation");
        mob.getNavigation().tick();
        mob.level.getProfiler().pop();
        mob.level.getProfiler().push("mob tick");
        customServerAiStep(mob);
        mob.level.getProfiler().pop();
        mob.level.getProfiler().push("controls");
        mob.level.getProfiler().push("move");
        mob.getMoveControl().tick();
        mob.level.getProfiler().popPush("look");
        mob.getLookControl().tick();
        mob.level.getProfiler().popPush("jump");
        mob.getJumpControl().tick();
        mob.level.getProfiler().pop();
        mob.level.getProfiler().pop();
        DebugPackets.sendGoalSelector(mob.level, mob, mob.goalSelector);
    }
    
    public static void customServerAiStep(Mob mob)
    {
		Method m = ObfuscationReflectionHelper.findMethod(Mob.class, "m_8024_");
		try 
		{
			m.invoke(mob);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
    }
    
    public static Entity getMorphOwner(Entity entity)
    {
    	if(entity.getPersistentData().contains("MorphOwnerUUID"))
    	{
    		return getEntityByUUID(entity.level, entity.getPersistentData().getUUID("MorphOwnerUUID"));
    	}
    	return null;
    }

	public static boolean areFriendly(Entity attacker, Entity target)
	{
		if(attacker == null || target == null)
		{
			return false;
		}
		if(attacker == target)
		{
			return true;
		}
		Entity attackerOwner = MorphUtil.getMorphOwner(attacker);
		Entity targetOwner = MorphUtil.getMorphOwner(target);
		Entity attackerRoot = attackerOwner != null ? attackerOwner : attacker;
		Entity targetRoot = targetOwner != null ? targetOwner : target;
		if(attackerRoot == targetRoot)
		{
			return true;
		}
		// Do not call isAlliedTo() here: this method is used by our isAlliedTo mixin.
		if(attackerRoot.getTeam() != null && attackerRoot.getTeam() == targetRoot.getTeam())
		{
			return true;
		}
		return false;
	}

	public static boolean shouldCancelMorphTarget(Mob attacker, LivingEntity target)
	{
		if(target == null)
		{
			return false;
		}
		LivingEntity effectiveTarget = getEffectiveLivingTarget(target);
		if(areFriendly(attacker, target))
		{
			return true;
		}
		Entity attackerOwner = MorphUtil.getMorphOwner(attacker);
		Entity targetOwner = MorphUtil.getMorphOwner(effectiveTarget);
		boolean morphInvolved = attackerOwner != null || targetOwner != null;
		if(!morphInvolved)
		{
			return false;
		}
		EntityType<?> attackerType = getEffectiveType(attacker);
		EntityType<?> targetType = getEffectiveType(effectiveTarget);
		return attackerType != null && attackerType == targetType;
	}

	public static LivingEntity getEffectiveLivingTarget(LivingEntity living)
	{
		IMorphCapability cap = living.getCapability(MorphCapabilityImpl.MORPH).orElse(null);
		if(cap != null && cap.getMorph() != null)
		{
			return cap.getMorph();
		}
		return living;
	}

	private static EntityType<?> getEffectiveType(Entity entity)
	{
		if(entity instanceof LivingEntity living)
		{
			IMorphCapability cap = living.getCapability(MorphCapabilityImpl.MORPH).orElse(null);
			if(cap != null && cap.getMorph() != null)
			{
				return cap.getMorph().getType();
			}
		}
		return entity.getType();
	}
    
	@SuppressWarnings("unchecked")
	public static Entity getEntityByUUID(Level level, UUID uuid)
	{
		try 
		{
			LevelEntityGetter<Entity> entities = (LevelEntityGetter<Entity>) GET_ENTITY.invoke(level);
			return entities.get(uuid);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private static class BrainAction
	{
		private final String name;
		private final BehaviorControl behavior;

		private BrainAction(String name, BehaviorControl behavior)
		{
			this.name = name;
			this.behavior = behavior;
		}
	}

	@SuppressWarnings("rawtypes")
	private static class ManualBrainAction
	{
		private final Mob mob;
		private final BehaviorControl behavior;
		private final LivingEntity target;
		private final boolean fakeTarget;
		private int ticks;

		private ManualBrainAction(Mob mob, BehaviorControl behavior, LivingEntity target, boolean fakeTarget)
		{
			this.mob = mob;
			this.behavior = behavior;
			this.target = target;
			this.fakeTarget = fakeTarget;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private void stop()
		{
			if(this.behavior.getStatus() == Behavior.Status.RUNNING && this.mob.level instanceof ServerLevel level)
			{
				this.behavior.doStop(level, this.mob, level.getGameTime());
			}
			this.cleanup();
		}

		private void cleanup()
		{
			if(this.fakeTarget && this.target != null)
			{
				clearBrainTarget(this.mob, this.target);
			}
		}
	}

	private interface MobActionAdapter
	{
		boolean supports(Mob mob);
		boolean execute(Mob mob, String actionName);
		List<String> scan(Class<?> clazz);
	}

	private static class GenericFieldAnimationAdapter implements MobActionAdapter
	{
		@Override
		public boolean supports(Mob mob)
		{
			return true;
		}

		@Override
		public boolean execute(Mob mob, String actionName)
		{
			Field field = findFieldInHierarchy(mob.getClass(), actionName);
			if(field == null)
			{
				return false;
			}
			try
			{
				Method method = findMethodAssignable(mob.getClass(), "setAnimation", field.getType());
				if(method != null)
				{
					field.setAccessible(true);
					method.invoke(mob, field.get(Modifier.isStatic(field.getModifiers()) ? null : mob));
					return true;
				}
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				
			}
			return false;
		}

		@Override
		public List<String> scan(Class<?> clazz)
		{
			List<String> actions = new ArrayList<>();
			Class<?> current = clazz;
			while(current != null && current != Object.class)
			{
				for(Field field : current.getDeclaredFields())
				{
					String simple = field.getType().getSimpleName();
					if((simple.contains("Animation") || simple.contains("AbilityType")) && !field.getType().isArray() && !actions.contains(field.getName()))
					{
						actions.add(field.getName());
					}
				}
				current = current.getSuperclass();
			}
			return actions;
		}
	}

	private static class MowzieAbilityAdapter implements MobActionAdapter
	{
		@Override
		public boolean supports(Mob mob)
		{
			return mob.getClass().getName().toLowerCase().contains("mowz");
		}

		@Override
		public boolean execute(Mob mob, String actionName)
		{
			Field field = findFieldInHierarchy(mob.getClass(), actionName);
			if(field == null)
			{
				return false;
			}
			try
			{
				Method method = findMethodAssignable(mob.getClass(), "sendAbilityMessage", field.getType());
				if(method != null)
				{
					field.setAccessible(true);
					method.invoke(mob, field.get(Modifier.isStatic(field.getModifiers()) ? null : mob));
					return true;
				}
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				
			}
			return false;
		}

		@Override
		public List<String> scan(Class<?> clazz)
		{
			return Collections.emptyList();
		}
	}

	private static class AquamiraePlayAdapter implements MobActionAdapter
	{
		@Override
		public boolean supports(Mob mob)
		{
			return mob.getClass().getName().toLowerCase().contains("aquamirae");
		}

		@Override
		public boolean execute(Mob mob, String actionName)
		{
			Field field = findFieldInHierarchy(mob.getClass(), actionName);
			if(field == null)
			{
				return false;
			}
			try
			{
				field.setAccessible(true);
				Object animation = field.get(Modifier.isStatic(field.getModifiers()) ? null : mob);
				if(animation != null)
				{
					Method play = animation.getClass().getMethod("play", Entity.class, int.class);
					play.invoke(animation, mob, 20);
					return true;
				}
			}
			catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				
			}
			return false;
		}

		@Override
		public List<String> scan(Class<?> clazz)
		{
			return Collections.emptyList();
		}
	}

	private static class MutantMethodAdapter implements MobActionAdapter
	{
		@Override
		public boolean supports(Mob mob)
		{
			String className = mob.getClass().getName().toLowerCase();
			return className.contains("mutant") || className.contains("irons_spellbooks") || className.contains("spellbook");
		}

		@Override
		public boolean execute(Mob mob, String actionName)
		{
			String methodName = actionName.startsWith("M_") ? actionName.substring(2) : actionName;
			for(Method method : mob.getClass().getMethods())
			{
				if(method.getName().equals(methodName))
				{
					try
					{
						if(method.getParameterCount() == 0)
						{
							method.invoke(mob);
							return true;
						}
						if(method.getParameterCount() == 1 && method.getParameterTypes()[0].isAssignableFrom(Entity.class))
						{
							method.invoke(mob, mob);
							return true;
						}
					}
					catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
					{
						
					}
				}
			}
			return false;
		}

		@Override
		public List<String> scan(Class<?> clazz)
		{
			String className = clazz.getName().toLowerCase();
			if(!(className.contains("mutant") || className.contains("irons_spellbooks") || className.contains("spellbook")))
			{
				return Collections.emptyList();
			}
			List<String> methods = new ArrayList<>();
			for(Method method : clazz.getMethods())
			{
				String name = method.getName().toLowerCase();
				if(method.getParameterCount() <= 1 && (name.contains("spell") || name.contains("ability") || name.contains("animation")))
				{
					String key = "M_" + method.getName();
					if(!methods.contains(key))
					{
						methods.add(key);
					}
				}
			}
			return methods;
		}
	}

	private static class IronsSpellbooksAdapter implements MobActionAdapter
	{
		@Override
		public boolean supports(Mob mob)
		{
			String className = mob.getClass().getName().toLowerCase();
			return className.contains("irons_spellbooks") || className.contains("spellbook");
		}

		@Override
		public boolean execute(Mob mob, String actionName)
		{
			return new MutantMethodAdapter().execute(mob, actionName);
		}

		@Override
		public List<String> scan(Class<?> clazz)
		{
			return new MutantMethodAdapter().scan(clazz);
		}
	}
}
