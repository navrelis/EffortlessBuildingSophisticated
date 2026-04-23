package sophisticated.building.create.foundation.utility;

import net.createmod.catnip.nbt.NBTProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class BlockHelper {
	private static final List<IntegerProperty> COUNT_STATES = List.of(
			BlockStateProperties.EGGS,
			BlockStateProperties.PICKLES,
			BlockStateProperties.CANDLES
	);

	private static final List<Block> VINELIKE_BLOCKS = List.of(
			Blocks.VINE, Blocks.GLOW_LICHEN
	);

	private static final List<BooleanProperty> VINELIKE_STATES = List.of(
			BlockStateProperties.UP,
			BlockStateProperties.NORTH,
			BlockStateProperties.EAST,
			BlockStateProperties.SOUTH,
			BlockStateProperties.WEST,
			BlockStateProperties.DOWN
	);

	public static BlockState setZeroAge(BlockState blockState) {
		if (blockState.hasProperty(BlockStateProperties.AGE_1))
			return blockState.setValue(BlockStateProperties.AGE_1, 0);
		if (blockState.hasProperty(BlockStateProperties.AGE_2))
			return blockState.setValue(BlockStateProperties.AGE_2, 0);
		if (blockState.hasProperty(BlockStateProperties.AGE_3))
			return blockState.setValue(BlockStateProperties.AGE_3, 0);
		if (blockState.hasProperty(BlockStateProperties.AGE_5))
			return blockState.setValue(BlockStateProperties.AGE_5, 0);
		if (blockState.hasProperty(BlockStateProperties.AGE_7))
			return blockState.setValue(BlockStateProperties.AGE_7, 0);
		if (blockState.hasProperty(BlockStateProperties.AGE_15))
			return blockState.setValue(BlockStateProperties.AGE_15, 0);
		if (blockState.hasProperty(BlockStateProperties.AGE_25))
			return blockState.setValue(BlockStateProperties.AGE_25, 0);
		if (blockState.hasProperty(BlockStateProperties.LEVEL_HONEY))
			return blockState.setValue(BlockStateProperties.LEVEL_HONEY, 0);
		if (blockState.hasProperty(BlockStateProperties.HATCH))
			return blockState.setValue(BlockStateProperties.HATCH, 0);
		if (blockState.hasProperty(BlockStateProperties.STAGE))
			return blockState.setValue(BlockStateProperties.STAGE, 0);
		if (blockState.is(BlockTags.CAULDRONS))
			return Blocks.CAULDRON.defaultBlockState();
		if (blockState.hasProperty(BlockStateProperties.LEVEL_COMPOSTER))
			return blockState.setValue(BlockStateProperties.LEVEL_COMPOSTER, 0);
		if (blockState.hasProperty(BlockStateProperties.EXTENDED))
			return blockState.setValue(BlockStateProperties.EXTENDED, false);
		return blockState;
	}

	public static ItemStack getRequiredItem(BlockState state) {
		ItemStack itemStack = new ItemStack(state.getBlock());
		Item item = itemStack.getItem();
		if (item == Items.FARMLAND || item == Items.DIRT_PATH)
			itemStack = new ItemStack(Items.DIRT);
		return itemStack;
	}

	public static void destroyBlock(Level world, BlockPos pos, float effectChance) {
		destroyBlock(world, pos, effectChance, stack -> Block.popResource(world, pos, stack));
	}

	public static void destroyBlock(Level world, BlockPos pos, float effectChance,
	                                Consumer<ItemStack> droppedItemCallback) {
		destroyBlockAs(world, pos, null, ItemStack.EMPTY, effectChance, droppedItemCallback);
	}

	public static boolean destroyBlockAs(Level world, BlockPos pos, @Nullable Player player, ItemStack usedTool,
	                                  float effectChance, Consumer<ItemStack> droppedItemCallback) {
		FluidState fluidState = world.getFluidState(pos);
		BlockState state = world.getBlockState(pos);

		if (world.random.nextFloat() < effectChance)
			world.levelEvent(2001, pos, Block.getId(state));
		BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;

		if (player != null) {
			usedTool.mineBlock(world, state, pos, player);
			player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
		}

		if (world instanceof ServerLevel serverLevel && world.getGameRules()
				.getBoolean(GameRules.RULE_DOBLOCKDROPS)
				&& (player == null || !player.isCreative())) {
			List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, blockEntity, player, usedTool);
			for (ItemStack itemStack : drops)
				droppedItemCallback.accept(itemStack);
			if (state.getBlock() instanceof IceBlock && false) {
				if (world.dimensionType().ultraWarm())
					return false;

				BlockState blockstate = world.getBlockState(pos.below());
				if (blockstate.blocksMotion() || blockstate.liquid())
					world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
				return false;
			}

			state.spawnAfterBreak((ServerLevel) world, pos, ItemStack.EMPTY, true);
		}

		world.setBlockAndUpdate(pos, fluidState.createLegacyBlock());
		return true;
	}

	public static boolean isSolidWall(BlockGetter reader, BlockPos fromPos, Direction toDirection) {
		return hasBlockSolidSide(reader.getBlockState(fromPos.relative(toDirection)), reader,
				fromPos.relative(toDirection), toDirection.getOpposite());
	}

	public static boolean noCollisionInSpace(BlockGetter reader, BlockPos pos) {
		return reader.getBlockState(pos)
				.getCollisionShape(reader, pos)
				.isEmpty();
	}

	private static void placeRailWithoutUpdate(Level world, BlockState state, BlockPos target) {
		LevelChunk chunk = world.getChunkAt(target);
		int idx = chunk.getSectionIndex(target.getY());
		LevelChunkSection chunksection = chunk.getSection(idx);
		if (chunksection == null) {
			chunksection = new LevelChunkSection(world.registryAccess()
					.registryOrThrow(Registries.BIOME));
			chunk.getSections()[idx] = chunksection;
		}
		BlockState old = chunksection.setBlockState(SectionPos.sectionRelative(target.getX()),
				SectionPos.sectionRelative(target.getY()), SectionPos.sectionRelative(target.getZ()), state);
		chunk.setUnsaved(true);

		world.setBlock(target, state, 82);
		world.neighborChanged(target, world.getBlockState(target.below())
				.getBlock(), target.below());
	}

	public static CompoundTag prepareBlockEntityData(BlockState blockState, BlockEntity blockEntity) {
		CompoundTag data = null;
		if (blockEntity == null)
			return null;
		RegistryAccess access = blockEntity.getLevel().registryAccess();
		if (blockEntity instanceof IPartialSafeNBT safeNbtBE) {
			data = new CompoundTag();
			safeNbtBE.writeSafe(data, access);
			data = NBTProcessors.process(blockState, blockEntity, data, true);
		}

		return data;
	}

	public static void placeSchematicBlock(Level world, BlockState state, BlockPos target, ItemStack stack,
	                                       @Nullable CompoundTag data) {
		BlockEntity existingBlockEntity = world.getBlockEntity(target);

		if (state.hasProperty(BlockStateProperties.EXTENDED))
			state = state.setValue(BlockStateProperties.EXTENDED, Boolean.FALSE);
		if (state.hasProperty(BlockStateProperties.WATERLOGGED))
			state = state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE);

		if (state.getBlock() == Blocks.COMPOSTER)
			state = Blocks.COMPOSTER.defaultBlockState();
		else if (state.is(BlockTags.CAULDRONS))
			state = Blocks.CAULDRON.defaultBlockState();

		if (world.dimensionType().ultraWarm() && state.getFluidState().is(FluidTags.WATER)) {
			int i = target.getX();
			int j = target.getY();
			int k = target.getZ();
			world.playSound(null, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
					2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

			for (int l = 0; l < 8; ++l) {
				world.addParticle(ParticleTypes.LARGE_SMOKE, i + Math.random(), j + Math.random(), k + Math.random(),
						0.0D, 0.0D, 0.0D);
			}
			Block.dropResources(state, world, target);
			return;
		}

		if (state.getBlock() instanceof BaseRailBlock) {
			placeRailWithoutUpdate(world, state, target);
		} else {
			world.setBlock(target, state, 18);
		}

		if (data != null) {
			BlockEntity blockEntity = world.getBlockEntity(target);
			if (blockEntity != null) {
				data.putInt("x", target.getX());
				data.putInt("y", target.getY());
				data.putInt("z", target.getZ());
				blockEntity.loadWithComponents(data, world.registryAccess());
			}
		}

		try {
			state.getBlock().setPlacedBy(world, target, state, null, stack);
		} catch (Exception e) {
		}
	}

	public static double getBounceMultiplier(Block block) {
		if (block instanceof SlimeBlock)
			return 0.8D;
		if (block instanceof BedBlock)
			return 0.66 * 0.8D;
		return 0;
	}

	public static boolean hasBlockSolidSide(BlockState p_220056_0_, BlockGetter p_220056_1_, BlockPos p_220056_2_,
	                                        Direction p_220056_3_) {
		return !p_220056_0_.is(BlockTags.LEAVES)
				&& Block.isFaceFull(p_220056_0_.getCollisionShape(p_220056_1_, p_220056_2_), p_220056_3_);
	}

	public static BlockState copyProperties(BlockState fromState, BlockState toState) {
		for (Property<?> property : fromState.getProperties()) {
			toState = copyProperty(property, fromState, toState);
		}
		return toState;
	}

	public static <T extends Comparable<T>> BlockState copyProperty(Property<T> property, BlockState fromState,
	                                                                BlockState toState) {
		if (fromState.hasProperty(property) && toState.hasProperty(property)) {
			return toState.setValue(property, fromState.getValue(property));
		}
		return toState;
	}
}
