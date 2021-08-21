package org.zeith.comms.c21campfirehardcore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;

public class Campfire
		extends CampfireBlock
{
	public static final Campfire BLOCK = new Campfire(true, 0, BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.PODZOL).strength(2.0F).sound(SoundType.WOOD).lightLevel(CampfireHardcore.litBlockEmission(15)).noOcclusion());
	public static final BlockItem ITEM = new BlockItem(BLOCK, new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS).rarity(Rarity.UNCOMMON));

	public static void register(BiConsumer<Item, Block> biRegistry)
	{
		biRegistry.accept(ITEM.setRegistryName("campfire"), BLOCK.setRegistryName("campfire"));
		CampfireHardcore.registerBlockIntoBlockEntityType(BlockEntityType.CAMPFIRE, BLOCK);
	}

	public Campfire(boolean particles, int fireDamage, Properties props)
	{
		super(particles, fireDamage, props);
	}

	@Override
	public InteractionResult use(BlockState p_51274_, Level p_51275_, BlockPos p_51276_, Player p_51277_, InteractionHand p_51278_, BlockHitResult p_51279_)
	{
		InteractionResult result = super.use(p_51274_, p_51275_, p_51276_, p_51277_, p_51278_, p_51279_);

		ItemStack held = p_51277_.getItemInHand(p_51278_);
		if(result == InteractionResult.PASS && (held.isEmpty() || held.getItem() instanceof BlockItem))
			return InteractionResult.SUCCESS;

		return result;
	}

	@Override
	public Optional<Vec3> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader world, BlockPos pos, float orientation, @Nullable LivingEntity entity)
	{
		if(!state.getValue(LIT)) return Optional.empty();
		return RespawnAnchorBlock.findStandUpPosition(type, world, pos);
	}
}
