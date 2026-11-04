/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class TriggerGoalPacket 
{
	private final UUID ownerUuid;
	private final int revision;
	
	public TriggerGoalPacket(UUID ownerUuid, int revision) 
	{
		this.ownerUuid = ownerUuid;
		this.revision = revision;
	}

	public TriggerGoalPacket(FriendlyByteBuf buf)
	{
		this.ownerUuid = buf.readUUID();
		this.revision = buf.readVarInt();
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeUUID(this.ownerUuid);
		buf.writeVarInt(this.revision);
	}
	
	public static boolean handle(TriggerGoalPacket message, Supplier<NetworkEvent.Context> ctx) 
	{
		ctx.get().enqueueWork(() ->
		{
			if(ctx.get().getDirection().getReceptionSide().isServer() && ctx.get().getSender() != null)
			{
				MorphUtil.triggerGoal(ctx.get().getSender(), message.ownerUuid, message.revision);
			}
		});
		ctx.get().setPacketHandled(true);
		return true;
	}
}
