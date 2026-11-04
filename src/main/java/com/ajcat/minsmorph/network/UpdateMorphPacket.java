/*
 * Min's Morph Extension
 * A GPLv3 derivative of Min's Morph by min01 (https://github.com/min2222/Mins-Morph).
 * Modifications Copyright (C) 2026 AJ-Cat.
 * Licensed under GPL-3.0-only. See LICENSE and NOTICE.
 */
package com.ajcat.minsmorph.network;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.ajcat.minsmorph.capabilities.MorphCapabilityImpl;
import com.ajcat.minsmorph.util.MorphUtil;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class UpdateMorphPacket 
{
	private final UUID entityUUID;
	private final EntityType<?> type;
	private final int entityId;
	private final boolean reset;
	@Nullable
	private final List<SynchedEntityData.DataValue<?>> dataValues;
	
	public UpdateMorphPacket(LivingEntity entity, EntityType<?> type, int entityId, boolean reset) 
	{
		this(entity, type, entityId, reset, null);
	}

	public UpdateMorphPacket(LivingEntity entity, EntityType<?> type, int entityId, boolean reset, @Nullable List<SynchedEntityData.DataValue<?>> dataValues)
	{
		this.entityUUID = entity.getUUID();
		this.type = type;
		this.entityId = entityId;
		this.reset = reset;
		this.dataValues = dataValues;
	}

	public UpdateMorphPacket(FriendlyByteBuf buf)
	{
		this.entityUUID = buf.readUUID();
		this.type = ForgeRegistries.ENTITY_TYPES.getValue(buf.readResourceLocation());
		this.entityId = buf.readInt();
		this.reset = buf.readBoolean();
		this.dataValues = buf.readBoolean() ? buf.readList(buffer ->
		{
			int id = buffer.readUnsignedByte();
			return SynchedEntityData.DataValue.read(buffer, id);
		}) : null;
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeUUID(this.entityUUID);
		buf.writeResourceLocation(ForgeRegistries.ENTITY_TYPES.getKey(this.type));
		buf.writeInt(this.entityId);
		buf.writeBoolean(this.reset);
		boolean hasData = this.dataValues != null && !this.dataValues.isEmpty();
		buf.writeBoolean(hasData);
		if(hasData)
		{
			buf.writeCollection(this.dataValues, (buffer, value) -> value.write(buffer));
		}
	}
	
	public static boolean handle(UpdateMorphPacket message, Supplier<NetworkEvent.Context> ctx) 
	{
		ctx.get().enqueueWork(() ->
		{
			if(ctx.get().getDirection().getReceptionSide().isClient())
			{
				LogicalSidedProvider.CLIENTWORLD.get(ctx.get().getDirection().getReceptionSide()).filter(ClientLevel.class::isInstance).ifPresent(level -> 
				{
					LivingEntity entity = (LivingEntity) MorphUtil.getEntityByUUID(level, message.entityUUID);
					if(entity != null)
					{
						entity.getCapability(MorphCapabilityImpl.MORPH).ifPresent(t -> 
						{
							if(!message.reset)
							{
								if(message.type != null)
								{
									LivingEntity living = t.getMorph();
									// Reusing the client-side morph keeps renderer/model state (for example phase-dependent models)
									// intact when an unrelated morph update is received.
									boolean sameMorph = living != null && living.getType() == message.type && living.getId() == message.entityId;
									if(!sameMorph)
									{
										if(message.type.create(level) instanceof LivingEntity created)
										{
											created.setId(message.entityId);
											t.setMorph(created);
											living = created;
										}
										else
										{
											MorphUtil.removeMorph(entity);
											return;
										}
									}
									if(living != null && message.dataValues != null)
									{
										living.getEntityData().assignValues(message.dataValues);
									}
								}
								else
								{
									MorphUtil.removeMorph(entity);
								}
							}
							else
							{
								com.ajcat.minsmorph.util.MorphClientState.removeSnapshot(message.entityUUID);
								MorphUtil.removeMorph(entity);
							}
						});
					}
				});
			}
		});
		ctx.get().setPacketHandled(true);
		return true;
	}
}
