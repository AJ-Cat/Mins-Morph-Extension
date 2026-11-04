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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ajcat.minsmorph.misc.ITrackedEntity;
import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

@Mixin(ChunkMap.class)
public class MixinChunkMap
{
	@Shadow
	@Final
	private ServerLevel level;

	@Shadow
	int viewDistance;
	
	@Inject(at = @At(value = "HEAD"), method = "broadcastAndSend", cancellable = true)
	private void broadcastAndSend(Entity entity, Packet<?> packet, CallbackInfo ci)
	{
		if(MorphUtil.isMorph(entity))
		{
			EntityType<?> type = entity.getType();
			int i = type.clientTrackingRange() * 16;
			if(i != 0)
			{
	            int j = type.updateInterval();
	            ChunkMap.TrackedEntity chunkmap$trackedentity = ChunkMap.class.cast(this).new TrackedEntity(entity, i, j, type.trackDeltas());
	            ((ITrackedEntity)chunkmap$trackedentity).updatePlayersCustom(this.level.players());
	            ((ITrackedEntity)chunkmap$trackedentity).setViewDist(this.viewDistance);
	            chunkmap$trackedentity.broadcastAndSend(packet);
			}
		}
	}
}
