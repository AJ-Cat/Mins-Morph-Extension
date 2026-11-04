/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ajcat.minsmorph.capabilities.MorphCapabilityImpl;
import com.ajcat.minsmorph.entity.MorphEntities;
import com.ajcat.minsmorph.util.MorphUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;

public class MorphCommand 
{
	public static final SuggestionProvider<CommandSourceStack> DATAS = SuggestionProviders.register(ResourceLocation.parse("datas"), (ctx, builder) ->
	{
		if(!(ctx.getSource() instanceof CommandSourceStack))
		{
			// Client-side command tree: ask the server for suggestions instead of trying to read its players/morphs locally.
			return ctx.getSource().customSuggestion(ctx);
		}
		return SharedSuggestionProvider.suggest(getDataSuggestions(ctx), builder);
	});
	
	public static final SuggestionProvider<CommandSourceStack> GOALS = SuggestionProviders.register(ResourceLocation.parse("goals"), (ctx, builder) ->
	{
		if(!(ctx.getSource() instanceof CommandSourceStack))
		{
			return ctx.getSource().customSuggestion(ctx);
		}
		return SharedSuggestionProvider.suggest(getGoalSuggestions(ctx), builder);
	});
	
	public static final SuggestionProvider<CommandSourceStack> ANIMATIONS = SuggestionProviders.register(ResourceLocation.parse("animations"), (ctx, builder) ->
	{
		if(!(ctx.getSource() instanceof CommandSourceStack))
		{
			return ctx.getSource().customSuggestion(ctx);
		}
		return SharedSuggestionProvider.suggest(getAnimationSuggestions(ctx), builder);
	});
	
	public static final SuggestionProvider<CommandSourceStack> ENTITIES = SuggestionProviders.register(ResourceLocation.parse("entities"), (p_258164_, p_258165_) -> 
	{
		return SharedSuggestionProvider.suggestResource(ForgeRegistries.ENTITY_TYPES.getValues().stream().filter((p_247987_) -> 
		{
			return p_247987_.isEnabled(p_258164_.getSource().enabledFeatures()) && p_247987_ != MorphEntities.FAKE_TARGET.get();
		}), p_258165_, EntityType::getKey, (p_212436_) -> 
		{
			return Component.translatable(Util.makeDescriptionId("entity", EntityType.getKey(p_212436_)));
		});
	});
	   
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx)
	{
		dispatcher.register(Commands.literal("morph").requires((sourceStack) -> 
		{
			return sourceStack.hasPermission(2);
		}).then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("set").then(Commands.argument("morphTarget", ResourceArgument.resource(ctx, Registries.ENTITY_TYPE)).suggests(ENTITIES).then(Commands.argument("persistent", BoolArgumentType.bool()).executes((commandCtx) -> 
		{
			return morph(commandCtx.getSource(), EntityArgument.getPlayers(commandCtx, "targets"), ResourceArgument.getEntityType(commandCtx, "morphTarget"), BoolArgumentType.getBool(commandCtx, "persistent"));
		}))))).then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("set").then(Commands.argument("morphTarget", ResourceArgument.resource(ctx, Registries.ENTITY_TYPE)).suggests(ENTITIES).executes((commandCtx) -> 
		{
			return morph(commandCtx.getSource(), EntityArgument.getPlayers(commandCtx, "targets"), ResourceArgument.getEntityType(commandCtx, "morphTarget"), false);
		})))).then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("trigger").then(Commands.argument("goalName", StringArgumentType.string()).suggests(GOALS).executes(commandCtx -> 
		{
			return triggerGoal(commandCtx.getSource(), EntityArgument.getPlayers(commandCtx, "targets"), StringArgumentType.getString(commandCtx, "goalName"));
		})))).then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("trigger").then(Commands.argument("goalName", StringArgumentType.string()).suggests(GOALS).then(Commands.argument("animationName", StringArgumentType.string()).suggests(ANIMATIONS).executes(commandCtx -> 
		{
			return triggerGoalWithAnimation(commandCtx.getSource(), EntityArgument.getPlayers(commandCtx, "targets"), StringArgumentType.getString(commandCtx, "goalName"), StringArgumentType.getString(commandCtx, "animationName"));
		}))))).then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("data").then(Commands.argument("dataName", StringArgumentType.string()).suggests(DATAS).then(Commands.argument("dataValue", StringArgumentType.string()).executes(commandCtx -> 
		{
			return setData(commandCtx.getSource(), EntityArgument.getPlayers(commandCtx, "targets"), StringArgumentType.getString(commandCtx, "dataName"), StringArgumentType.getString(commandCtx, "dataValue"));
		}))))).then(Commands.argument("targets", EntityArgument.players()).then(Commands.literal("reset").executes((commandCtx) -> 
		{
			return unmorph(commandCtx.getSource(), EntityArgument.getPlayers(commandCtx, "targets"));
		}))));
	}

	private static LivingEntity getSuggestionOwner(CommandSourceStack source)
	{
		if(source.getEntity() instanceof LivingEntity living)
		{
			return living;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static List<String> getDataSuggestions(CommandContext<SharedSuggestionProvider> ctx)
	{
		Set<String> suggestions = new LinkedHashSet<>();
		CommandContext<CommandSourceStack> sourceCtx = (CommandContext<CommandSourceStack>) (CommandContext<?>) ctx;
		try
		{
			for(ServerPlayer target : EntityArgument.getPlayers(sourceCtx, "targets"))
			{
				addDataSuggestions(target, suggestions);
			}
		}
		catch(CommandSyntaxException ignored)
		{
		}
		if(suggestions.isEmpty())
		{
			LivingEntity owner = getSuggestionOwner(sourceCtx.getSource());
			if(owner != null)
			{
				addDataSuggestions(owner, suggestions);
			}
		}
		return new ArrayList<>(suggestions);
	}

	private static void addDataSuggestions(LivingEntity living, Set<String> suggestions)
	{
		living.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t ->
		{
			if(t.getMorph() instanceof Mob mob)
			{
				suggestions.addAll(MorphUtil.getSettableDatas(mob));
			}
			else
			{
				suggestions.addAll(t.getDataOptions());
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static List<String> getGoalSuggestions(CommandContext<SharedSuggestionProvider> ctx)
	{
		Set<String> suggestions = new LinkedHashSet<>();
		CommandContext<CommandSourceStack> sourceCtx = (CommandContext<CommandSourceStack>) (CommandContext<?>) ctx;
		try
		{
			for(ServerPlayer target : EntityArgument.getPlayers(sourceCtx, "targets"))
			{
				addGoalSuggestions(target, suggestions);
			}
		}
		catch(CommandSyntaxException ignored)
		{
		}
		if(suggestions.isEmpty())
		{
			LivingEntity owner = getSuggestionOwner(sourceCtx.getSource());
			if(owner != null)
			{
				addGoalSuggestions(owner, suggestions);
			}
		}
		return new ArrayList<>(suggestions);
	}

	private static void addGoalSuggestions(LivingEntity living, Set<String> suggestions)
	{
		living.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> suggestions.addAll(t.getGoalOptions()));
	}

	@SuppressWarnings("unchecked")
	private static List<String> getAnimationSuggestions(CommandContext<SharedSuggestionProvider> ctx)
	{
		Set<String> suggestions = new LinkedHashSet<>();
		CommandContext<CommandSourceStack> sourceCtx = (CommandContext<CommandSourceStack>) (CommandContext<?>) ctx;
		try
		{
			for(ServerPlayer target : EntityArgument.getPlayers(sourceCtx, "targets"))
			{
				addAnimationSuggestions(target, suggestions);
			}
		}
		catch(CommandSyntaxException ignored)
		{
		}
		if(suggestions.isEmpty())
		{
			LivingEntity owner = getSuggestionOwner(sourceCtx.getSource());
			if(owner != null)
			{
				addAnimationSuggestions(owner, suggestions);
			}
		}
		return new ArrayList<>(suggestions);
	}

	private static void addAnimationSuggestions(LivingEntity living, Set<String> suggestions)
	{
		living.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> suggestions.addAll(t.getAnimationOptions()));
	}
	
	private static int triggerGoal(CommandSourceStack sourceStack, Collection<ServerPlayer> targets, String goalName)
	{
		for(ServerPlayer target : targets)
		{
			target.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
			{
				if(t.getMorph() != null)
				{
					if(MorphUtil.selectGoalByName(target, goalName))
					{
						MorphUtil.triggerGoal(target);
						sourceStack.sendSuccess(() -> Component.literal("Triggered goal " + goalName), true);
					}
					else
					{
						sourceStack.sendFailure(Component.literal("Unknown goal: " + goalName));
					}
				}
				else
				{
					sourceStack.sendFailure(Component.literal("Target is not morphed"));
				}
			});
		}
		return targets.size();
	}
	
	private static int triggerGoalWithAnimation(CommandSourceStack sourceStack, Collection<ServerPlayer> targets, String goalName, String animationName)
	{
		for(ServerPlayer target : targets)
		{
			target.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
			{
				if(t.getMorph() != null)
				{
					if(!MorphUtil.selectGoalByName(target, goalName))
					{
						sourceStack.sendFailure(Component.literal("Unknown goal: " + goalName));
						return;
					}
					if(!MorphUtil.selectAnimationByName(target, animationName))
					{
						sourceStack.sendFailure(Component.literal("Unknown animation: " + animationName));
						return;
					}
					MorphUtil.triggerGoal(target);
					sourceStack.sendSuccess(() -> Component.literal("Triggered goal " + goalName + " with animation " + animationName), true);
				}
				else
				{
					sourceStack.sendFailure(Component.literal("Target is not morphed"));
				}
			});
		}
		return targets.size();
	}
	
	private static int setData(CommandSourceStack sourceStack, Collection<ServerPlayer> targets, String dataName, String dataValue)
	{
		for(ServerPlayer target : targets)
		{
			target.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
			{
				if(t.getMorph() instanceof Mob mob)
				{
					if(MorphUtil.setData(mob, dataName, dataValue))
					{
						t.setData(dataName, dataValue);
						sourceStack.sendSuccess(() -> Component.literal("Changed entity data " + dataName + " to " + dataValue), true);
					}
					else
					{
						sourceStack.sendFailure(Component.literal("Unknown or unsupported data: " + dataName));
					}
				}
				else
				{
					sourceStack.sendFailure(Component.literal("Target does not have a mob morph"));
				}
			});
		}
		return targets.size();
	}
	
	private static int morph(CommandSourceStack sourceStack, Collection<ServerPlayer> targets, Reference<EntityType<?>> entityType, boolean isPersistent)
	{
		for(ServerPlayer target : targets)
		{
			target.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
			{
				if(t.getMorph() != null)
				{
					MorphUtil.removeMorph(target);
				}
				Entity entity = entityType.get().create(target.level);
				if(entityType.get() == MorphEntities.FAKE_TARGET.get())
				{
					sourceStack.sendFailure(Component.literal("Can't morph to internal fake target"));
				}
				else if(entity instanceof LivingEntity livingMorph)
				{
					t.setPersistent(isPersistent);
					t.setMorph(livingMorph);
					target.setHealth(Math.min(target.getMaxHealth(), livingMorph.getMaxHealth()));
					sourceStack.sendSuccess(() -> Component.literal("Changed morph entity of " + target.getDisplayName().getString() + " to " + livingMorph.getDisplayName().getString()), true);
				}
				else
				{
					sourceStack.sendFailure(Component.literal("Can't morph to non-living entity"));
				}
			});
		}
		return targets.size();
	}
	
	private static int unmorph(CommandSourceStack sourceStack, Collection<ServerPlayer> targets)
	{
		for(ServerPlayer target : targets)
		{
			if(MorphUtil.hasMorph(target))
			{
				MorphUtil.removeMorph(target);
				sourceStack.sendSuccess(() -> Component.literal("Removed morph of " + target.getDisplayName().getString()), true);
			}
			else
			{
				sourceStack.sendFailure(Component.literal("Target is not morphed"));
			}
		}
		return targets.size();
	}
}
