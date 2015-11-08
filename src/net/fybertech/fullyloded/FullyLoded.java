package net.fybertech.fullyloded;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.fybertech.meddle.Meddle;
import net.fybertech.meddleapi.ConfigFile;
import net.minecraft.block.Block;
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

	static List<Block> blocks = null;
	
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
		
		blocks = new ArrayList<>();
		String[] split = list.split(",");
		for (String id : split) {
			id = id.trim();
			Block b = Block.getBlockFromName(id);
			if (b != null && b != Blocks.air) blocks.add(b);
			else Meddle.LOGGER.warn("[Meddle/FullyLoded] Invalid block ID in config: " + id);
		}
		
		if (config.hasChanged()) config.save();
	}
	
	
	private static boolean blocksAreRelated(Block block1, Block block2)
	{
		if (block1 == null || block2 == null) return false;
		if (block1 == block2) return true;
		if (block1 == blockLitRedstoneOre && block2 == blockRedstoneOre) return true;
		if (block1 == blockRedstoneOre && block2 == blockLitRedstoneOre) return true;
		return false;
	}
	
	
	public static void destroyNeighbors(Block block, World world, BlockPos pos, EntityPlayer player)
	{		
		for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
			for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
				for (int y = -1; y <= 1; y++) {
					if (player.getHeldMainHandItem() == null || player.getHeldMainHandItem().stackSize < 1) return;				
					
					BlockPos newPos = new BlockPos(x, pos.getY() + y, z);
					IBlockState upState = world.getBlockState(newPos);
					if (upState == null) continue;				
					
					if (blocksAreRelated(upState.getBlock(), block))
					{
						world.setBlockState(newPos, Blocks.air.getDefaultState(), 3);					
						player.getHeldMainHandItem().damageItem(1, player);
						
						if (player.getHeldMainHandItem().stackSize < 1) {							
							player.setHeldItem(MainOrOffHand.MAIN_HAND, (ItemStack)null);
						}					
						
						if (!world.isRemote) block.harvestBlock(world, player, newPos, upState, null, player.getHeldMainHandItem() == null ? null : player.getHeldMainHandItem().copy());
						
						destroyNeighbors(block, world, newPos, player);
					}
				}				
			}
		}
	}
	
	
	public static boolean onBlockDestroyedHook (boolean result, ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity)
	{
		if (blocks == null) return result;		
		
		if (entity.isSneaking()) return result;
		
		Block b = state.getBlock();
		if (stack.getItem().canHarvestBlock(b) && blocks.contains(b) && entity instanceof EntityPlayer) {
			destroyNeighbors(b, world, pos, (EntityPlayer)entity);			
		}		
		
		return result;
	}
}

