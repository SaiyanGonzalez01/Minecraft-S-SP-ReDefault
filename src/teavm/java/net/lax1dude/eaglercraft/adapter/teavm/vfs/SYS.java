package net.lax1dude.eaglercraft.adapter.teavm.vfs;

public class SYS {

	public static final VirtualFilesystem VFS;

	static {

		VirtualFilesystem.VFSHandle vh = VirtualFilesystem.openVFS("_net_lax1dude_eaglercraft_adapter_teavm_vfs_VirtualFilesystem_1_5_2_eagStorage");

		if(vh.vfs == null) {
			System.err.println("Could not init filesystem!");
			throw new RuntimeException("Could not init filesystem: VFSHandle.vfs was null");
		}

		VFS = vh.vfs;

	}
}
