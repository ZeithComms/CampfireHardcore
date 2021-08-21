package org.zeith.comms.c21campfirehardcore;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeith.comms.c21campfirehardcore.api.CampfireData;
import org.zeith.comms.c21campfirehardcore.api.CampfireListData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

@Mod(CampfireHardcore.MOD_ID)
public class CampfireHardcore
{
	public static final Logger LOGGER = LogManager.getLogger();

	public static final String MOD_ID = "campfirehardcore";

	private static final List<Block> BLOCKS = new ArrayList<>();
	private static final List<Item> ITEMS = new ArrayList<>();

	public CampfireHardcore()
	{
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		bus.addListener(CampfireData::setup);
		bus.addListener(CampfireListData::setup);
		bus.addGenericListener(Item.class, this::registerItems);
		bus.addGenericListener(Block.class, this::registerBlocks);

		MinecraftForge.EVENT_BUS.addListener(this::respawn);
		MinecraftForge.EVENT_BUS.addListener(this::interact);
		MinecraftForge.EVENT_BUS.addListener(this::breakBlock);

		init();
	}

	private void init()
	{
		BiConsumer<Item, Block> biRegistry = (i, b) ->
		{
			if(i != null) ITEMS.add(i);
			if(b != null) BLOCKS.add(b);
		};

		Campfire.register(biRegistry);
	}

	private void registerItems(RegistryEvent.Register<Item> e)
	{
		ITEMS.forEach(e.getRegistry()::register);
	}

	private void registerBlocks(RegistryEvent.Register<Block> e)
	{
		BLOCKS.forEach(e.getRegistry()::register);
	}

	public void respawn(PlayerEvent.PlayerRespawnEvent e)
	{
		Player pl = e.getPlayer();

		if(!e.isEndConquered() && pl instanceof ServerPlayer spl)
		// only when death in survival
		{
			ServerLevel level = spl.getLevel();
			BlockPos campfire = CampfireData.get(pl)
					.resolve()
					.map(cd -> cd.checkCampfire(level))
					.orElse(null);
			if(campfire == null)
			{
				if(((ServerPlayer) pl).gameMode.getGameModeForPlayer().isSurvival())
					spl.setGameMode(GameType.SPECTATOR);
			}
		}
	}

	public void interact(PlayerInteractEvent.RightClickBlock e)
	{
		Level level = e.getWorld();
		BlockPos pos = e.getPos().immutable();
		Player player = e.getPlayer();

		if(isCampfire(level, pos) && player instanceof ServerPlayer sp)
		{
			if(sp.getRespawnDimension() != level.dimension() || !pos.equals(sp.getRespawnPosition()))
			{
				sp.setRespawnPosition(level.dimension(), pos.immutable(), 0F, false, true);
				CampfireData.get(sp).ifPresent(cd ->
				{
					cd.campfirePos = pos;
					cd.campfireDimension = level.dimension();
					CampfireListData.get(sp.getLevel()).ifPresent(cld -> cld.addCampfire(pos));
				});
			} else CampfireData.get(sp).ifPresent(cd ->
			{
				cd.campfirePos = pos;
				cd.campfireDimension = level.dimension();
				CampfireListData.get(sp.getLevel()).ifPresent(cld -> cld.addCampfire(pos));
			});
		}
	}

	public void breakBlock(BlockEvent.NeighborNotifyEvent e)
	{
		LevelAccessor world = e.getWorld();
		if(world instanceof ServerLevel sl)
		{
			if(!isCampfire(sl, e.getPos()))
			{
				CampfireListData.get(sl).ifPresent(cld -> cld.removeCampfire(e.getPos()));
			}
		}
	}

	public static void registerBlockIntoBlockEntityType(BlockEntityType<?> type, Block block)
	{
		Set<Block> validBlocks = type.validBlocks;
		if(validBlocks instanceof ImmutableSet) validBlocks = new HashSet<>(validBlocks);
		try
		{
			validBlocks.add(block);
		} catch(UnsupportedOperationException e)
		{
			validBlocks = new HashSet<>(validBlocks);
			validBlocks.add(block);
		}
		type.validBlocks = validBlocks;
	}

	public static boolean isCampfire(Level lvl, BlockPos pos)
	{
		return isCampfire(lvl.getBlockState(pos));
	}

	public static boolean isCampfire(BlockState state)
	{
		if(!state.getOptionalValue(BlockStateProperties.LIT).orElse(true))
			return false;
		return state.getBlock() instanceof Campfire;
	}

	public static ToIntFunction<BlockState> litBlockEmission(int lvl)
	{
		return (state) -> state.getValue(BlockStateProperties.LIT) ? lvl : 0;
	}
}