package net.lax1dude.eaglercraft.adapter.teavm.vfs;

public interface VFSIterator {
	
	public static class BreakLoop extends RuntimeException {
		public BreakLoop() {
			super("iterator loop break request");
		}
	}
	
	public default void end() {
		throw new BreakLoop();
	}
	
	public void next(VIteratorFile entry);
	
}
