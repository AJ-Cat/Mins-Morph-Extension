/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mixin(SynchedEntityData.class)
public class MixinSynchedEntityData 
{
	@Shadow
	@Final
	private Entity entity;

	// Morph entities are not part of the normal chunk tracker, so their synced data has to be broadcast manually.
	// Inject at RETURN so the packet contains the value that was just set instead of the previous dirty entry.
	@Inject(at = @At(value = "RETURN"), method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V")
	private <T> void set(EntityDataAccessor<T> accessor, T value, CallbackInfo ci)
	{
		if(!MorphUtil.isMorph(this.entity) || this.entity.level.isClientSide)
		{
			return;
		}
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if(server == null)
		{
			return;
		}
		List<SynchedEntityData.DataValue<?>> list = this.entity.getEntityData().packDirty();
		if(list == null || list.isEmpty())
		{
			return;
		}
		for(ServerPlayer player : server.getPlayerList().getPlayers())
		{
			if(player.level == this.entity.level)
			{
				player.connection.connection.send(new ClientboundSetEntityDataPacket(this.entity.getId(), list));
			}
		}
	}
}
