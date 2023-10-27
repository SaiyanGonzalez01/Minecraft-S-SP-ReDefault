package net.minecraft.src;

import java.io.IOException;
import java.io.InputStream;

import net.lax1dude.eaglercraft.AssetRepository;
import net.lax1dude.eaglercraft.EaglerAdapter;
import net.lax1dude.eaglercraft.adapter.SimpleStorage;

public class TexturePackCustom extends TexturePackImplementation {
	public TexturePackCustom(String name, ITexturePack base) {
		super(name, name, base);
		try {
			AssetRepository.installTemp(SimpleStorage.get(name));
			this.loadThumbnailImage();
			this.loadDescription();
			AssetRepository.resetTemp();
		} catch (IOException ignored) {}
	}

	public boolean func_98140_c(String par1Str) {
		return EaglerAdapter.loadResource(par1Str) != null;
	}

	public boolean isCompatible() {
		return true;
	}

	protected InputStream func_98139_b(String par1Str) throws IOException {
		return EaglerAdapter.loadResource(par1Str);
	}

	@Override
	public byte[] getResourceAsBytes(String par1Str) {
		return EaglerAdapter.loadResourceBytes(par1Str);
	}
}
