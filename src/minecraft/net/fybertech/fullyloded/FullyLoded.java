package net.fybertech.fullyloded;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.event.EventBus;

@Mod(modid = "fullyloded", name = "FullyLoded", version = "1.0.2")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class FullyLoded extends DummyModContainer {

	public static class BlockPos
	{
		public int x;
		public int y;
		public int z;
		
		public BlockPos(int x, int y, int z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public int getX() { return x; }
		public int getY() { return y; }
		public int getZ() { return z; }
		
		
		@Override
		public boolean equals(Object object)
		{
			if (!(object instanceof BlockPos)) return false;
			
			BlockPos pos = (BlockPos)object;
			return pos.x == x && pos.y == y && pos.z == z;
		}
		
		
		@Override
		public String toString()
		{
			return "X: " + x + " Y: " + y + " Z: " + z;
		}
		
		
		@Override
	    public int hashCode() {
	        int hash = 1;
	        hash = hash * 17 + x;
	        hash = hash * 31 + y;
	        hash = hash * 13 + z;
	        return hash;
	    }
	}
	
	public static class IBlockState
	{
		public int blockID;
		public int blockMeta;
		
		public IBlockState() {};
		
		public IBlockState(Block block, int meta)
		{
			this.blockID = block.blockID;
			this.blockMeta = meta;
		}
		
		public IBlockState(int blockID, int meta)
		{
			this.blockID = blockID;
			this.blockMeta = meta;
		}
		
		public Block getBlock()
		{
			return Block.blocksList[blockID];
		}
		
		@Override
		public boolean equals(Object state)
		{			
			return state instanceof IBlockState && ((IBlockState)state).blockID == blockID && ((IBlockState)state).blockMeta == blockMeta;
		}
		
		@Override
		public String toString()
		{
			return "ID: " + blockID + " Meta: " + blockMeta;
		}
	}
	
	
	static List<IBlockState> blocks = null;
	static int maxBlocks = 100;
	static int maxHorizontal = 10;
	static int maxVertical = 10;
	
	static Block blockLitRedstoneOre = null;
	static Block blockRedstoneOre = null;
	
	
	public static void loadConfig(File file)
	{
		Configuration cfg = new Configuration(file);
		
		cfg.load();
		
		String defaultOres = Block.oreIron.blockID + ", " + Block.oreGold.blockID + ", " + Block.oreCoal.blockID + ", " + Block.oreLapis.blockID 
				+ ", " + Block.oreDiamond.blockID + ", " + Block.oreRedstone.blockID + ", " + Block.oreRedstoneGlowing.blockID 
				+ ", " + Block.oreEmerald.blockID;
		
		String list = cfg.get("general", "ores", defaultOres, "List of block IDs to mine by the vein.\n\nMetadata can be provided by using a colon followed by the value.\ne.g. 2001:1, 2001:2, 2001:3, etc").value;
		maxBlocks = cfg.get("general", "maxBlocks", 100, "Maximum number of blocks that can be mined at once.").getInt(100);
		maxHorizontal = cfg.get("general", "maxHorizontal", 10, "Maximum horizontal distance to search the vein from the block mined.").getInt(10);
		maxVertical = cfg.get("general", "maxVertical", 10, "Maximum vertical distance to search the vein from the block mined.").getInt(10);
		
		blocks = new ArrayList<IBlockState>();
		String[] split = list.split(",");
		for (String id : split) {
			id = id.trim();
			
			int metaIndex = id.indexOf(":");
			int meta = 0;
			if (metaIndex != -1) {
				meta = Integer.parseInt(id.substring(metaIndex + 1));
				id = id.substring(0, metaIndex);
			}
			
			int blockID = Integer.parseInt(id);
			
			if (blockID > 0) {				
				blocks.add(new IBlockState(blockID, meta));
				String description = blockID + ""; //Block.blockRegistry.getNameForObject(b).toString();
				if (meta != 0) description += ":" + meta;
				System.out.println("[FullyLoded] Added block ID: " + description);
			}
			else
				System.out.println("[FullyLoded] Invalid block ID in config: " + id);
		}
		
		cfg.save();
	}
	
	
	
	@PreInit
	public void preInit(FMLPreInitializationEvent event) 
	{
		blockLitRedstoneOre = Block.oreRedstoneGlowing;
		blockRedstoneOre = Block.oreRedstone;
		
		loadConfig(event.getSuggestedConfigurationFile());
	}

	@Init
	public void init(FMLInitializationEvent event) {
	}

	@PostInit
	public static void postInit(FMLPostInitializationEvent event) {
		
	}
	
	
	
	
	
	
	
	
	
	
	
	private static boolean blocksAreRelated(IBlockState block1, IBlockState block2)
	{
		if (block1 == null || block2 == null) return false;
		if (block1.equals(block2)) return true;
		if (block1.getBlock() == blockLitRedstoneOre && block2.getBlock() == blockRedstoneOre) return true;
		if (block1.getBlock() == blockRedstoneOre && block2.getBlock() == blockLitRedstoneOre) return true;
		return false;
	}

	
	public static IBlockState getBlockState(World world, BlockPos pos)
	{
		IBlockState state = new IBlockState();
		state.blockID = world.getBlockId(pos.x, pos.y, pos.z);
		state.blockMeta = world.getBlockMetadata(pos.x, pos.y, pos.z);
		
		return state;
	}
	
	
	public static boolean addNeighbor(IBlockState block, World world, BlockPos startPos, BlockPos newPos, EntityPlayer player, HashSet<BlockPos> vein) {
		
		if (vein.contains(newPos)) return false;
		
		if (Math.abs(newPos.getX() - startPos.getX()) > maxHorizontal ||
				Math.abs(newPos.getZ() - startPos.getZ()) > maxHorizontal ||
				Math.abs(newPos.getY() - startPos.getY()) > maxVertical ||
				vein.size() >= maxBlocks) return false;
		
		IBlockState upState = getBlockState(world, newPos);
		if (upState == null || upState.blockID == 0) return false;				
		
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
		HashSet<BlockPos> vein = new HashSet<BlockPos>();
		HashSet<BlockPos> currentToCheck = new HashSet<BlockPos>();
		HashSet<BlockPos> nextToCheck = new HashSet<BlockPos>();
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
		if (player.getHeldItem() == null || player.getHeldItem().stackSize < 1) return false;
		
		IBlockState upState = getBlockState(world, newPos);		
		if (upState == null) return false;	
		
		Block block = state.getBlock();
		
		if (blocksAreRelated(upState, state))
		{
			player.getHeldItem().damageItem(1, player);
			
			if (player.getHeldItem().stackSize < 1) {							
				player.destroyCurrentEquippedItem();
			}			
			
			if (!world.isRemote) {				
				world.setBlockWithNotify(newPos.x, newPos.y, newPos.z, 0);
				block.harvestBlock(world, player, newPos.x, newPos.y, newPos.z, upState.blockMeta);
			}
			
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
					if (destroyBlock(new IBlockState(block, 0), world, newPos, player))
						destroyNeighbors(block, world, newPos, player);
				}
			}
		}
	}
	
	
	public static boolean onBlockDestroyedHook(boolean result, ItemStack stack, World world, int blockID, int x, int y, int z, EntityLiving entity)
	{
		if (blocks == null) return result;		
		
		if (entity.isSneaking()) return result;
		
		IBlockState state = new IBlockState();
		state.blockID = blockID;
		state.blockMeta = world.getBlockMetadata(x,  y,  z);

		BlockPos startPos = new BlockPos(x, y, z);
		
		if (stack.getItem().canHarvestBlock(state.getBlock()) && blocks.contains(state) && entity instanceof EntityPlayer) {
			HashSet<BlockPos> vein = findVein(state, world, startPos, (EntityPlayer)entity);
			for (BlockPos currentPos : vein) {			
				if (currentPos.equals(startPos)) continue;
				if (!destroyBlock(state, world, currentPos, (EntityPlayer)entity))
					break;
			}
		}
		
		return result;
	}
}

