package org.zeith.comms.c21campfirehardcore.api;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.zeith.comms.c21campfirehardcore.CampfireHardcore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.zeith.comms.c21campfirehardcore.CampfireHardcore.MOD_ID;

public class CampfireData
		implements INBTSerializable<CompoundTag>, ICapabilityProvider
{
	@CapabilityInject(CampfireData.class)
	public static Capability<CampfireData> CH_DATA = null;

	public BlockPos campfirePos;
	public ResourceKey<Level> campfireDimension = Level.OVERWORLD;

	final Player player;

	public CampfireData(Player player)
	{
		this.player = player;
	}

	public BlockPos checkCampfire(ServerLevel l)
	{
		if(campfirePos != null)
		{
			ChunkAccess chunk = l.getChunk(campfirePos);
			BlockState state = chunk.getBlockState(campfirePos);
			if(CampfireHardcore.isCampfire(state)) return campfirePos;
		}
		return null;
	}

	public void removeCampfire()
	{
		campfirePos = null;
		campfireDimension = Level.OVERWORLD;

		player.sendMessage(new TranslatableComponent("chat.campfirehardcore.campfire_removed").withStyle(ChatFormatting.RED), Util.NIL_UUID);
		if(player instanceof ServerPlayer)
			((ServerPlayer) player).setRespawnPosition(Level.OVERWORLD, null, 0F, false, true);
	}

	@Override
	public CompoundTag serializeNBT()
	{
		CompoundTag nbt = new CompoundTag();

		if(campfirePos != null) nbt.putLong("CampfirePos", campfirePos.asLong());
		nbt.putString("CampfireDimension", campfireDimension.getRegistryName().toString());

		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt)
	{
		if(nbt.contains("CampfirePos", Constants.NBT.TAG_LONG))
			campfirePos = BlockPos.of(nbt.getLong("CampfirePos"));
		else
			campfirePos = null;

		this.campfireDimension = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, nbt.get("CampfireDimension")).resultOrPartial(CampfireHardcore.LOGGER::error).orElse(Level.OVERWORLD);
	}

	public final LazyOptional<CampfireData> lazyThis = LazyOptional.of(() -> this);

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
	{
		return CH_DATA.orEmpty(cap, lazyThis);
	}

	public static void setup(FMLCommonSetupEvent e)
	{
		MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, CampfireData::addCapabilitiesToPlayers);
		MinecraftForge.EVENT_BUS.addListener(CampfireData::clonePlayer);
		CapabilityManager.INSTANCE.register(CampfireData.class);
	}

	private static void addCapabilitiesToPlayers(AttachCapabilitiesEvent<Entity> e)
	{
		if(e.getObject() instanceof Player pl)
		{
			e.addCapability(new ResourceLocation(MOD_ID, "data"), new CampfireData(pl));
		}
	}

	private static void clonePlayer(PlayerEvent.Clone e)
	{
		e.getOriginal().reviveCaps(); // stupid capabilities validation
		CampfireData old = get(e.getOriginal()).resolve().orElse(null);
		e.getOriginal().invalidateCaps(); // more stupid nonsense 🔫

		CampfireData _new = get(e.getPlayer()).resolve().orElse(null);

		if(_new != null && old != null)
			_new.deserializeNBT(old.serializeNBT());
	}

	public static LazyOptional<CampfireData> get(Player p)
	{
		return p.getCapability(CH_DATA);
	}
}