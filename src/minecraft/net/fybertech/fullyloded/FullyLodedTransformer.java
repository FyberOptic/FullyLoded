package net.fybertech.fullyloded;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import cpw.mods.fml.relauncher.IClassTransformer;

public class FullyLodedTransformer implements IClassTransformer {

	
	String pickaxe = "uy"; //DynamicMappings.getClassMapping("net/minecraft/item/ItemPickaxe");
	
	
	@Override
	public byte[] transform(String arg0, byte[] arg2) 
	{
		if (arg0.equals("net.minecraft.item.ItemPickaxe") || arg0.equals(pickaxe)) return transformPickaxe(arg2);
		return arg2;
	}
	
	
	private byte[] failGracefully(String error, byte[] bytes)
	{
		System.out.println("[FullyLoded] " + error);
		return bytes;
	}
	
	
	
	private MethodNode getMethodNodeFromMapping(ClassNode cn, String mapping)
	{
		String[] split = mapping.split(" ");
		
		for (Iterator<MethodNode> it = cn.methods.iterator(); it.hasNext();) {
			MethodNode method = it.next();
			if (method.name.equals(split[1]) && method.desc.equals(split[2])) return method;
		}
		
		return null;
	}
	
	
	private byte[] transformPickaxe(byte[] bytes)
	{
		ClassReader reader = new ClassReader(bytes);
		ClassNode cn = new ClassNode();
		reader.accept(cn, 0);
		
		// MD: tw/a (Lur;Lyc;IIIILmd;)Z net/minecraft/src/ItemTool/func_77660_a (Lnet/minecraft/src/ItemStack;Lnet/minecraft/src/World;IIIILnet/minecraft/src/EntityLiving;)Z
		String mapping; //DynamicMappings.getMethodMapping("net/minecraft/item/Item onBlockDestroyed (Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/minecraft/entity/EntityLivingBase;)Z");
		if (cn.name.equals(pickaxe)) mapping = "tw a (Lur;Lyc;IIIILmd;)Z";
		else mapping = "net/minecraft/item/ItemPickaxe onBlockDestroyed (Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;IIIILnet/minecraft/entity/EntityLiving;)Z"; 
		
		//System.out.println("Using " + mapping);
		
		//if (mapping == null) return failGracefully("Couldn't find mapping for Item.onBlockDestroyed!", bytes);
		MethodNode onBlockDestroyed = getMethodNodeFromMapping(cn, mapping);
		if (onBlockDestroyed != null) return failGracefully("ItemPickaxe.onBlockDestroyed already exists!", bytes);
		
		String[] split = mapping.split(" ");
		
		onBlockDestroyed = new MethodNode(Opcodes.ACC_PUBLIC, split[1], split[2], null, null);
		
		InsnList list = onBlockDestroyed.instructions;
		
		list.add(new VarInsnNode(Opcodes.ALOAD, 0));
		list.add(new VarInsnNode(Opcodes.ALOAD, 1));
		list.add(new VarInsnNode(Opcodes.ALOAD, 2));
		list.add(new VarInsnNode(Opcodes.ILOAD, 3));
		list.add(new VarInsnNode(Opcodes.ILOAD, 4));
		list.add(new VarInsnNode(Opcodes.ILOAD, 5));		
		list.add(new VarInsnNode(Opcodes.ILOAD, 6));
		list.add(new VarInsnNode(Opcodes.ALOAD, 7));
		list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, cn.superName, split[1], split[2]));
		
		list.add(new VarInsnNode(Opcodes.ALOAD, 1));
		list.add(new VarInsnNode(Opcodes.ALOAD, 2));
		list.add(new VarInsnNode(Opcodes.ILOAD, 3));
		list.add(new VarInsnNode(Opcodes.ILOAD, 4));
		list.add(new VarInsnNode(Opcodes.ILOAD, 5));
		list.add(new VarInsnNode(Opcodes.ILOAD, 6));
		list.add(new VarInsnNode(Opcodes.ALOAD, 7));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/fybertech/fullyloded/FullyLoded", "onBlockDestroyedHook", split[2].replace("(", "(Z")));		
		
		list.add(new InsnNode(Opcodes.IRETURN));
		
		cn.methods.add(onBlockDestroyed);		
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		cn.accept(writer);
		
		System.out.println("[FullyLoded] Patched ItemPickaxe");
		
		return writer.toByteArray();
	}



}
