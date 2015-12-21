package net.fybertech.fullyloded;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class FullyLodedFML implements IFMLLoadingPlugin {

	@Override
	public String[] getLibraryRequestClass() {
		return null;
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { FullyLodedTransformer.class.getName() };
	}

	@Override
	public String getModContainerClass() {
		//return FullyLoded.class.getName();
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {		
	}

}
