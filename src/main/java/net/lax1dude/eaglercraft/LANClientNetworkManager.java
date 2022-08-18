package net.lax1dude.eaglercraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.lax1dude.eaglercraft.sp.relay.pkt.IPacket;
import net.lax1dude.eaglercraft.sp.relay.pkt.IPacket00Handshake;
import net.lax1dude.eaglercraft.sp.relay.pkt.IPacket01ICEServers;
import net.lax1dude.eaglercraft.sp.relay.pkt.IPacket03ICECandidate;
import net.lax1dude.eaglercraft.sp.relay.pkt.IPacket04Description;
import net.lax1dude.eaglercraft.sp.relay.pkt.IPacket05ClientSuccess;
import net.lax1dude.eaglercraft.sp.relay.pkt.IPacket06ClientFailure;
import net.lax1dude.eaglercraft.sp.relay.pkt.IPacketFFErrorCode;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;

public class LANClientNetworkManager implements INetworkManager {

	private static final int PRE = 0, INIT = 1, SENT_ICE_CANDIDATE = 2, SENT_DESCRIPTION = 3;
	
	private static final String[] initStateNames = new String[] { "PRE", "INIT", "SENT_ICE_CANDIDATE", "SENT_DESCRIPTION" };
	
	public final String displayCode;
	public final String displayRelay;
	
	private NetHandler theNetHandler;
	
	private LANClientNetworkManager(String displayCode, String displayRelay) {
		this.displayCode = displayCode;
		this.displayRelay = displayRelay;
		this.theNetHandler = null;
	}
	
	public static LANClientNetworkManager connectToWorld(RelayServerSocket sock, String displayCode, String displayRelay) {
		EaglerAdapter.clearLANClientState();
		int connectState = -1;
		IPacket pkt;
		mainLoop: while(!sock.isClosed()) {
			if((pkt = sock.readPacket()) != null) {
				if(pkt instanceof IPacket00Handshake) {
					if(connectState == PRE) {
						
						// %%%%%%  Process IPacket00Handshake  %%%%%%
						
						System.out.println("Relay [" + displayRelay + "|" + displayCode + "] recieved handshake, "
								+ "client id: " + ((IPacket00Handshake)pkt).connectionCode);
						connectState = INIT;
						
					}else {
						sock.close();
						System.err.println("Relay [" + displayRelay + "|" + displayCode + "] unexpected packet: "
								+ "IPacket00Handshake in state " + initStateNames[connectState]);
						return null;
					}
				}else if(pkt instanceof IPacket01ICEServers) {
					if(connectState == INIT) {
						
						// %%%%%%  Process IPacket01ICEServers  %%%%%%
						
						IPacket01ICEServers ipkt = (IPacket01ICEServers) pkt;
						
						// print servers
						System.out.println("Relay [" + displayRelay + "|" + displayCode + "] provided ICE servers:");
						List<String> servers = new ArrayList();
						for(net.lax1dude.eaglercraft.sp.relay.pkt.ICEServerSet.RelayServer srv : ipkt.servers) {
							System.out.println("Relay [" + displayRelay + "|" + displayCode + "]     " + srv.type.name()
									+ ": " + srv.address);
							servers.add(srv.getICEString());
						}
						
						// process
						EaglerAdapter.clientLANSetICEServersAndConnect(servers.toArray(new String[servers.size()]));
						
						// await result
						long lm = System.currentTimeMillis();
						do {
							String c = EaglerAdapter.clientLANAwaitICECandidate();
							if(c != null) {
								System.out.println("Relay [" + displayRelay + "|" + displayCode + "] client sent ICE candidate");
								
								// 'this.iceCandidateHandler' was called, send result:
								sock.writePacket(new IPacket03ICECandidate("", c));
								
								connectState = SENT_ICE_CANDIDATE;
								continue mainLoop;
							}
							try {
								Thread.sleep(20l);
							} catch (InterruptedException e) {
							}
						}while(System.currentTimeMillis() - lm > 3000l);

						// no ice candidates were sent
						sock.close();
						System.err.println("Relay [" + displayRelay + "|" + displayCode + "] client provide ICE candidate timeout");
						return null;
						
					}else {
						sock.close();
						System.err.println("Relay [" + displayRelay + "|" + displayCode + "] unexpected packet: "
								+ "IPacket01ICEServers in state " + initStateNames[connectState]);
						return null;
					}
				}else if(pkt instanceof IPacket03ICECandidate) {
					if(connectState == SENT_ICE_CANDIDATE) {
						
						// %%%%%%  Process IPacket03ICECandidate  %%%%%%
						
						IPacket03ICECandidate ipkt = (IPacket03ICECandidate) pkt;

						// process
						System.out.println("Relay [" + displayRelay + "|" + displayCode + "] recieved server ICE candidate");
						EaglerAdapter.clientLANSetICECandidate(ipkt.candidate);

						// await result
						long lm = System.currentTimeMillis();
						do {
							String c = EaglerAdapter.clientLANAwaitDescription();
							if(c != null) {
								System.out.println("Relay [" + displayRelay + "|" + displayCode + "] client sent description");
								
								// 'this.descriptionHandler' was called, send result:
								sock.writePacket(new IPacket04Description("", c));
								
								connectState = SENT_DESCRIPTION;
								continue mainLoop;
							}
							try {
								Thread.sleep(20l);
							} catch (InterruptedException e) {
							}
						}while(System.currentTimeMillis() - lm > 3000l);
						
						// no description was sent
						sock.close();
						System.err.println("Relay [" + displayRelay + "|" + displayCode + "] client provide description timeout");
						return null;
						
					}else {
						sock.close();
						System.err.println("Relay [" + displayRelay + "|" + displayCode + "] unexpected packet: "
								+ "IPacket03ICECandidate in state " + initStateNames[connectState]);
						return null;
					}
				}else if(pkt instanceof IPacket04Description) {
					if(connectState == SENT_DESCRIPTION) {
						
						// %%%%%%  Process IPacket04Description  %%%%%%
						
						IPacket03ICECandidate ipkt = (IPacket03ICECandidate) pkt;
						
						// process
						System.out.println("Relay [" + displayRelay + "|" + displayCode + "] recieved server description");
						EaglerAdapter.clientLANSetDescription(ipkt.candidate);

						// await result
						long lm = System.currentTimeMillis();
						do {
							if(EaglerAdapter.clientLANAwaitChannel()) {
								System.out.println("Relay [" + displayRelay + "|" + displayCode + "] client opened data channel");
								
								// 'this.remoteDataChannelHandler' was called, success
								sock.writePacket(new IPacket05ClientSuccess());
								sock.close();
								return new LANClientNetworkManager(displayCode, displayRelay);
								
							}
							try {
								Thread.sleep(20l);
							} catch (InterruptedException e) {
							}
						}while(System.currentTimeMillis() - lm > 3000l);
						
						// no channel was opened
						sock.writePacket(new IPacket06ClientFailure());
						sock.close();
						System.err.println("Relay [" + displayRelay + "|" + displayCode + "] client open data channel timeout");
						
						return null;
						
					}else {
						sock.close();
						System.err.println("Relay [" + displayRelay + "|" + displayCode + "] unexpected packet: "
								+ "IPacket04Description in state " + initStateNames[connectState]);
						return null;
					}
				}else if(pkt instanceof IPacketFFErrorCode) {
					
					// %%%%%%  Process IPacketFFErrorCode  %%%%%%
					
					IPacketFFErrorCode ipkt = (IPacketFFErrorCode) pkt;
					System.err.println("Relay [" + displayRelay + "|" + displayCode + "] connection failed: " +
							IPacketFFErrorCode.code2string(ipkt.code) + "(" + ipkt.code + "): " + ipkt.desc);
					Throwable t;
					while((t = sock.getException()) != null) {
						t.printStackTrace();
					}
					sock.close();
					return null;
					
				}else {
					
					// %%%%%%  Unexpected Packet  %%%%%%
					
					System.err.println("Relay [" + displayRelay + "] unexpected packet: " + pkt.getClass().getSimpleName());
					sock.close();
					return null;
				}
			}
			try {
				Thread.sleep(20l);
			} catch (InterruptedException e) {
			}
		}
		return null;
	}

	@Override
	public void setNetHandler(NetHandler var1) {
		theNetHandler = var1;
	}

	@Override
	public void addToSendQueue(Packet var1) {
		try {
			ByteArrayOutputStream bao = new ByteArrayOutputStream(var1.getPacketSize() + 1);
			Packet.writePacket(var1, new DataOutputStream(bao));
			EaglerAdapter.clientLANSendPacket(bao.toByteArray());
		}catch(IOException e) {
			System.err.println("Failed to serialize minecraft packet '" + var1.getClass().getSimpleName() + "' to remote LAN world");
			e.printStackTrace();
		}
	}

	@Override
	public void wakeThreads() {
		// no
	}

	@Override
	public void processReadPackets() {
		if(this.theNetHandler != null) {
			byte[] data;
			while((data = EaglerAdapter.clientLANReadPacket()) != null) {
				try {
					ByteArrayInputStream bai = new ByteArrayInputStream(data);
					int pktId = bai.read();
					
					if(pktId == -1) {
						System.err.println("Recieved invalid '-1' packet");
						continue;
					}
					
					Packet pkt = Packet.getNewPacket(pktId);
					
					if(pkt == null) {
						System.err.println("Recieved invalid '" + pktId + "' packet");
						continue;
					}
					
					pkt.readPacketData(new DataInputStream(bai));
					
					try {
						pkt.processPacket(theNetHandler);
					}catch(Throwable t) {
						System.err.println("Could not process minecraft packet 0x" + Integer.toHexString(pkt.getPacketId()) + " class '" + pkt.getClass().getSimpleName() + "' from remote LAN world");
						t.printStackTrace();
					}
					
				}catch(IOException ex) {
					System.err.println("Could not deserialize a " + data.length + " byte long minecraft packet of type '" + (data.length <= 0 ? -1 : (int)(data[0] & 0xFF)) + "' from remote LAN world");
				}
			}
		}
	}

	@Override
	public void serverShutdown() {
		if(!EaglerAdapter.clientLANClosed()) {
			EaglerAdapter.clientLANCloseConnection();
		}
	}

	@Override
	public int packetSize() {
		return 0;
	}

	@Override
	public void networkShutdown(String var1, Object... var2) {
		if(!EaglerAdapter.clientLANClosed()) {
			EaglerAdapter.clientLANCloseConnection();
		}
	}

	@Override
	public void closeConnections() {
		if(!EaglerAdapter.clientLANClosed()) {
			EaglerAdapter.clientLANCloseConnection();
		}
	}

	@Override
	public String getServerURI() {
		return "[lan:" + displayRelay + ":" + displayCode + "]";
	}

}