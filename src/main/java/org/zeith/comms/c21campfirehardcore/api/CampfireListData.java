package org.zeith.comms.c21campfirehardcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.zeith.comms.c21campfirehardcore.CampfireHardcore.MOD_ID;

public class CampfireListData
		implements INBTSerializable<CompoundTag>, ICapabilityProvider
{
	@CapabilityInject(CampfireListData.class)
	public static Capability<CampfireListData> CH_DATA = null;

	public final List<BlockPos> respawnCampfires = new ArrayList<>();

	public final LazyOptional<CampfireListData> lazyThis = LazyOptional.of(() -> this);

	final ServerLevel level;

	public CampfireListData(ServerLevel level)
	{
		this.level = level;
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
	{
		return CH_DATA.orEmpty(cap, lazyThis);
	}

	public static void setup(FMLCommonSetupEvent e)
	{
		MinecraftForge.EVENT_BUS.addGenericListener(Level.class, CampfireListData::addCapabilitiesToLevel);
		CapabilityManager.INSTANCE.register(CampfireListData.class);
	}

	private static void addCapabilitiesToLevel(AttachCapabilitiesEvent<Level> e)
	{
		if(e.getObject() instanceof ServerLevel sl)
			e.addCapability(new ResourceLocation(MOD_ID, "data"), new CampfireListData(sl));
	}

	@Override
	public CompoundTag serializeNBT()
	{
		CompoundTag nbt = new CompoundTag();

		nbt.put("RespawnCampfires", new LongArrayTag(this.respawnCampfires.stream().mapToLong(BlockPos::asLong).toArray()));

		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt)
	{
		this.respawnCampfires.clear();
		this.respawnCampfires.addAll(Arrays.stream(nbt.getLongArray("RespawnCampfires")).mapToObj(BlockPos::of).collect(Collectors.toList()));
	}

	public static LazyOptional<CampfireListData> get(ServerLevel lvl)
	{
		return lvl.getCapability(CH_DATA);
	}

	public void addCampfire(BlockPos pos)
	{
		pos = pos.immutable();
		if(!this.respawnCampfires.contains(pos))
		{
			this.respawnCampfires.add(pos);
		}
	}

	public void removeCampfire(BlockPos pos)
	{
		pos = pos.immutable();
		final BlockPos ipos = pos;
		if(this.respawnCampfires.contains(pos))
		{
			this.respawnCampfires.remove(pos);
			for(ServerPlayer player : level.getServer().getPlayerList().getPlayers())
				CampfireData.get(player)
						.filter(cd -> ipos.equals(cd.campfirePos) && cd.campfireDimension.equals(level.dimension()))
						.ifPresent(CampfireData::removeCampfire);
		}
	}
}
