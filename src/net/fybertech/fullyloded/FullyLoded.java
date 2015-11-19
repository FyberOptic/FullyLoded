package net.fybertech.fullyloded;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.fybertech.meddle.Meddle;
import net.fybertech.meddleapi.ConfigFile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MainOrOffHand;
import net.minecraft.world.World;

public class FullyLoded {

	static List<IBlockState> blocks = null;
	static int maxBlocks = 100;
	static int maxHorizontal = 10;
	static int maxVertical = 10;
	
	static Block blockLitRedstoneOre = null;
	static Block blockRedstoneOre = null;
	
	public void init()
	{
		loadConfig();
		
		blockLitRedstoneOre = Block.getBlockFromName("lit_redstone_ore");
		blockRedstoneOre = Block.getBlockFromName("redstone_ore");
	}
	
	
	public static void loadConfig()
	{
		ConfigFile config = new ConfigFile(new File(Meddle.getConfigDir(), "fullyloded.cfg"));
		config.load();
		
		String defaultOres = "iron_ore, gold_ore, coal_ore, lapis_ore, diamond_ore, redstone_ore, lit_redstone_ore, emerald_ore, quartz_ore";
		
		String list = config.get(ConfigFile.key("general", "ores", defaultOres, "List of block names to mine by the vein."));
		maxBlocks = config.get(ConfigFile.key("general", "maxBlocks", 100, "Maximum number of blocks that can be mined at once."));
		maxHorizontal = config.get(ConfigFile.key("general", "maxHorizontal", 10, "Maximum horizontal distance to search the vein from the block mined."));
		maxVertical = config.get(ConfigFile.key("general", "maxVertical", 10, "Maximum vertical distance to search the vein from the block mined."));
		
		blocks = new ArrayList<>();
		String[] split = list.split(",");
		for (String id : split) {
			id = id.trim();
			
			int metaIndex = id.indexOf(":");
			int meta = 0;
			if (metaIndex != -1) {
				meta = Integer.parseInt(id.substring(metaIndex + 1));
				id = id.substring(0, metaIndex);
			}
			
			Block b = Block.getBlockFromName(id);
			IBlockState bs;
			if (b != null && b != Blocks.air) {
				if (meta != 0)
					bs = b.getStateFromMeta(meta);
				else
					bs = b.getDefaultState();
				blocks.add(bs);
				String description = Block.blockRegistry.getNameForObject(b).toString();
				if (meta != 0) description += ":" + meta;
				Meddle.LOGGER.info("[Meddle/FullyLoded] Added block: " + description);
			}
			else
				Meddle.LOGGER.warn("[Meddle/FullyLoded] Invalid block ID in config: " + id);
		}
		
		if (config.hasChanged()) config.save();
	}
	
	
	private static boolean blocksAreRelated(IBlockState block1, IBlockState block2)
	{
		if (block1 == null || block2 == null) return false;
		if (block1 == block2) return true;
		if (block1.getBlock() == blockLitRedstoneOre && block2.getBlock() == blockRedstoneOre) return true;
		if (block1.getBlock() == blockRedstoneOre && block2.getBlock() == blockLitRedstoneOre) return true;
		return false;
	}

	public static boolean addNeighbor(IBlockState block, World world, BlockPos startPos, BlockPos newPos, EntityPlayer player, HashSet<BlockPos> vein) {
		
		if (vein.contains(newPos)) return false;
		
		if (Math.abs(newPos.getX() - startPos.getX()) > maxHorizontal ||
				Math.abs(newPos.getZ() - startPos.getZ()) > maxHorizontal ||
				Math.abs(newPos.getY() - startPos.getY()) > maxVertical ||
				vein.size() >= maxBlocks) return false;
		
		IBlockState upState = world.getBlockState(newPos);
		if (upState == null) return false;				
		
		if (blocksAreRelated(upState, block))
		{
			vein.add(newPos);
			return true;
		}
		return false;
	}
	
	public static HashSet<BlockPos> findVein(IBlockState block, World world, BlockPos startPos, EntityPlayer player)
	{
		boolean addedBlocks;
		HashSet<BlockPos> vein = new HashSet<>();
		HashSet<BlockPos> currentToCheck = new HashSet<>();
		HashSet<BlockPos> nextToCheck = new HashSet<>();
		currentToCheck.add(startPos);
		BlockPos[] nextPos = new BlockPos[6];
		
		do {
			addedBlocks = false;
			for (BlockPos currentPos : currentToCheck) {
				nextPos[0] = new BlockPos(currentPos.getX() - 1, currentPos.getY(), currentPos.getZ());
				nextPos[1] = new BlockPos(currentPos.getX() + 1, currentPos.getY(), currentPos.getZ());
				nextPos[2] = new BlockPos(currentPos.getX(), currentPos.getY() - 1, currentPos.getZ());
				nextPos[3] = new BlockPos(currentPos.getX(), currentPos.getY() + 1, currentPos.getZ());
				nextPos[4] = new BlockPos(currentPos.getX(), currentPos.getY(), currentPos.getZ() - 1);
				nextPos[5] = new BlockPos(currentPos.getX(), currentPos.getY(), currentPos.getZ() + 1);
				
				for (BlockPos newPos : nextPos) {
					if (vein.contains(newPos)) continue;
					if (addNeighbor(block, world, startPos, newPos, player, vein)) {
						addedBlocks = true;
						nextToCheck.add(newPos);
					}
				}
			}
			currentToCheck.clear();
			currentToCheck.addAll(nextToCheck);
			nextToCheck.clear();
		} while (addedBlocks && vein.size() < maxBlocks);
		
		return vein;
	}
	
	public static boolean destroyBlock(IBlockState state, World world, BlockPos newPos, EntityPlayer player) {
		if (player.getHeldMainHandItem() == null || player.getHeldMainHandItem().stackSize < 1) return false;				
		
		IBlockState upState = world.getBlockState(newPos);
		if (upState == null) return false;				
		Block block = state.getBlock();
		
		if (blocksAreRelated(upState, state))
		{
			world.setBlockState(newPos, Blocks.air.getDefaultState(), 3);					
			player.getHeldMainHandItem().damageItem(1, player);
			
			if (player.getHeldMainHandItem().stackSize < 1) {							
				player.setHeldItem(MainOrOffHand.MAIN_HAND, (ItemStack)null);
			}
			
			if (!world.isRemote) block.harvestBlock(world, player, newPos, upState, null, player.getHeldMainHandItem() == null ? null : player.getHeldMainHandItem().copy());
			
			return true;
		}
		
		return false;
	}
	
	public static void destroyNeighbors(Block block, World world, BlockPos pos, EntityPlayer player)
	{		
		for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
			for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
				for (int y = -1; y <= 1; y++) {
					BlockPos newPos = new BlockPos(x, pos.getY() + y, z);
					if (destroyBlock(block.getDefaultState(), world, newPos, player))
						destroyNeighbors(block, world, newPos, player);
				}
			}
		}
	}
	
	public static boolean onBlockDestroyedHook(boolean result, ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity)
	{
		if (blocks == null) return result;		
		
		if (entity.isSneaking()) return result;		

		if (stack.getItem().canHarvestBlock(state) && blocks.contains(state) && entity instanceof EntityPlayer) {
			HashSet<BlockPos> vein = findVein(state, world, pos, (EntityPlayer)entity);
			for (BlockPos currentPos : vein) {
				if (!destroyBlock(state, world, currentPos, (EntityPlayer)entity))
					break;
			}
		}
		
		return result;
	}
}

