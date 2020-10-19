package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bukkit.entity.Player;

import main.ClientPacket.ClientPacketType;

//TODO: Refresh tokens.
public class VoiceChatServer {
	private static final char[] otpChars = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
		'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'p',
		'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
	};

	private final SecureRandom secureRng;
	private final DatagramSocket socket;
	//TODO: Make player state class maybe?
	private final Map<List<Byte>, Player> playerTokens;
	private final Map<String, Player> playerOtps;
	private final Map<Player, SocketAddress> playerConnections;
	private final Map<Player, byte[]> playerUids;
	private final ReadWriteLock stateLock;
	private final ExecutorService executor;

	public VoiceChatServer(int port) throws SocketException, UnknownHostException {
		this.socket = new DatagramSocket(port);
		this.secureRng = new SecureRandom();
		this.playerTokens = new HashMap<List<Byte>, Player>();
		this.playerOtps = new HashMap<String, Player>();
		this.playerConnections = new HashMap<Player, SocketAddress>();
		this.playerUids = new HashMap<Player, byte[]>();
		this.stateLock = new ReentrantReadWriteLock(true);
		this.executor = Executors.newCachedThreadPool();

		Executors.newSingleThreadExecutor().submit(() -> {
			while(true) {
				DatagramPacket packet = new DatagramPacket(new byte[2005], 2005);
				try {
					this.socket.receive(packet);
					ClientPacket clientPacket = new ClientPacket(packet);
					if(!clientPacket.isValid()) {
						return;
					}
					executor.submit(this.usePacket(clientPacket));
				}
				catch (IOException e) { e.printStackTrace(); }	
			}
		});
	}


	private void createSound(
		byte[] audio, Player speakingPlayer,
		Map<Integer, List<SocketAddress>> audibility
	) {
		audibility.forEach( (distance, connections) -> {
			byte[] header = new byte[] {0x01};
			header = VoiceChatServer.concatenate(header, this.playerUids.get(speakingPlayer));
			byte[] adjustedAudio = adjustVolumeByDistance(audio.clone(), distance);
			byte[] data = VoiceChatServer.concatenate(header, adjustedAudio);
			DatagramPacket outgoingPacket = new DatagramPacket(data, data.length);
			for(SocketAddress connection : connections) {
				outgoingPacket.setSocketAddress(connection);
				try {
					this.socket.send(outgoingPacket);
				} catch (IOException e) { e.printStackTrace(); }
			}
		});
	}


	//state lock should be locked by the thread that calls this.
	private List<Byte> generateToken() {
		List<Byte> token;
		do {
			byte[] tokenArr = new byte[4];
			this.secureRng.nextBytes(tokenArr);
			token = VoiceChatServer.asList(tokenArr);
		} while( this.playerTokens.containsKey(token) );
		return token;
	}


	//state lock should be locked by the thread that calls this.
	private String generateOtp() {
		String otp;
		do {
			otp = "";
			for(int i = 0; i < 6; i++) {
				otp += VoiceChatServer.otpChars[(int) (this.secureRng.nextDouble() * 34)];
			}
		} while(this.playerOtps.keySet().contains(otp));
		return otp;
	}


	//This method is not synchronized. External synchronization is required to call it.
	private Map<Integer, List<SocketAddress>> getAudibility(Player speakingPlayer) {
		Map<Integer, List<SocketAddress>> audibility =
				new HashMap<Integer, List<SocketAddress>>();
		this.playerConnections.forEach( (player, connection) -> {
			if(player != speakingPlayer) {
				if(player.getWorld() != speakingPlayer.getWorld()) {
					return;
				}

				int distance = (int) player.getLocation().distance(speakingPlayer.getLocation());
				if(distance < 16) {
					if(audibility.containsKey(distance)) {
						audibility.get(distance).add(this.playerConnections.get(player));
					}
					else {
						List<SocketAddress> connections = new ArrayList<>();
						connections.add(this.playerConnections.get(player));
						audibility.put(distance, connections);
					}
				}
			}
		});
		return audibility;
	}


	//Returns a task that processes that latest packet.
	private Runnable usePacket(ClientPacket packet) {
		return () -> {
			ClientPacketType packetType = packet.getType();
			if(packetType == ClientPacketType.SIGN_IN) {
				this.stateLock.writeLock().lock();
				List<Byte> token = null;
				try {
					if(this.playerOtps.containsKey(packet.getOtp())) {
						Player connectingPlayer = this.playerOtps.remove(packet.getOtp());
						this.playerConnections.put(connectingPlayer, packet.getAddress());
						token = this.generateToken();
						this.playerTokens.put(token, connectingPlayer);
						//TODO: Add a system for players who aren't acknowledging tokens.
						//TODO: Add a system that refreshes a player's token every 10 minutes.
					}
					else {
					//TODO: Add ip address to some list if an invalid otp is given. count offenses
					//and refuse to acknowledge packets from ip after a certain amount is reached.
					//The ip should also be logged. it should also be realized that the server is
					//under a distributed attack if too many wrong guesses from too many different
					//IPs are being made.
					//TODO: Maybe I should limit the client connections the server acknowledges to
					//only those under an IP that's already connected to the Minecraft server. That
					//way, DDoS should be harder.
					//TODO: Since there will only be a limited number of failures per OTP, it is
					//reasonable and, thus, necessary to respond back to the client and inform them
					//that they gave an invalid OTP.
					}
				} finally {
					this.stateLock.writeLock().unlock();
				}
				//if the packet is a legitimate log-in, the server sends the logged-in client a
				//token to use for authentication.
				if(token != null) {
					byte[] data = new byte[5];
					data[0] = 0x00;
					for(int i = 0; i < 4; i++) {
						data[i + 1] = token.get(i);
					}
					DatagramPacket tokenChangePacket = new DatagramPacket(data, 5, packet.getAddress());
					try {
						this.socket.send(tokenChangePacket);
					} catch (IOException e) { e.printStackTrace(); }
				}
			}

			//TODO: Rate-limit VoiP packets to one per sixteenth of a second (62,500 microseconds).
			//TODO: What if someone keeps exceeding the VoiP packet rate-limit? Soft-ban?
			else if(packetType == ClientPacketType.VOIP) {
				Map<Integer, List<SocketAddress>> audibility = null;
				Player speakingPlayer;
				this.stateLock.readLock().lock();
				try {
					List<Byte> token = VoiceChatServer.asList(packet.getToken());
					speakingPlayer = this.playerTokens.get(token);
					if(speakingPlayer != null) {
						audibility = this.getAudibility(speakingPlayer);
					}
					else {
					//TODO: Add ip address to some list if an invalid token is given. count offenses
					//and refuse to acknowledge packets from ip after a certain amount is reached.
					//The ip should also be logged. it should also be realized that the server is
					//under a distributed attack if too many wrong guesses from too many different
					//IPs are being made.
					}
				} finally {
					this.stateLock.readLock().unlock();
				}
				if(audibility != null) {
					this.createSound(packet.getAudio(), speakingPlayer, audibility);
				}
			}

			else if(packetType == ClientPacketType.TOKEN_CHANGE_ACK) {
				//TODO
			}

			else if(packetType == ClientPacketType.SIGN_OUT) {
				this.stateLock.writeLock().lock();
				try {
					List<Byte> token = VoiceChatServer.asList(packet.getToken());
					if(this.playerTokens.containsKey(token)) {
						Player disconnectingPlayer = this.playerTokens.remove(token);
						this.playerConnections.remove(disconnectingPlayer);
					}
					else {
						//TODO
					}
				} finally {
					this.stateLock.writeLock().lock();
				}
			}
		};
	}


	public Future<String> playerJoined(Player player) {
		return this.executor.submit( () -> {
			this.stateLock.writeLock().lock();
			try {
				if(!this.playerConnections.containsKey(player)) {
					ByteBuffer bb = ByteBuffer.allocate(16);
					bb.putLong(player.getUniqueId().getMostSignificantBits());
					bb.putLong(player.getUniqueId().getLeastSignificantBits());
					byte[] uid = bb.array();
					this.playerUids.put(player, uid);

					String otp = this.generateOtp();
					this.playerOtps.put(otp, player);
					return otp;
				}
				return null;
			} finally {
				this.stateLock.writeLock().unlock();
			}
		});
	}


	//TODO: Test
	public Future<?> playerQuit(Player player) {
		return this.executor.submit( () -> {
			this.stateLock.writeLock().lock();
			try {
				this.playerUids.remove(player);
				//If the player is connected to voicechat, this removes them from the listed
				//connections and gets rid of their token.
				if(this.playerConnections.containsKey(player)) {
					this.playerConnections.remove(player);
					for(List<Byte> token : this.playerTokens.keySet()) {
						if(this.playerTokens.get(token) == player) {
							this.playerTokens.remove(token);
							break;
						}
					}
				}
				//If the player still hasn't connected to voicechat by now, this removes their
				//token.
				else if(this.playerOtps.containsValue(player)) {
					for(String otp : this.playerOtps.keySet()) {
						if(this.playerOtps.get(otp) == player) {
							this.playerOtps.remove(otp);
							break;
						}
					}
				}
			} finally {
				this.stateLock.writeLock().unlock();
			}
		});
	}


	private static List<Byte> asList(byte[] arr) {
		List<Byte> list = new ArrayList<>((int) (arr.length * 4/3));
		for(int i = 0; i < arr.length; i++) {
			list.add(arr[i]);
		}
		return list;
	}


	private static byte[] concatenate(byte[] arr1, byte[] arr2) {
		byte[] res = new byte[arr1.length + arr2.length];
		for(int i = 0; i < arr1.length; i++) {
			res[i] = arr1[i];
		}
		for(int i = arr1.length; i < res.length; i++) {
			res[i] = arr2[i - arr1.length];
		}
		return res;
	}


	private static byte[] adjustVolumeByDistance(byte[] audio, int distance) {
		if(distance <= 6) {
			return audio;
		}
		else {
			float volume = (16.0f - (float) distance)/10.0f;
			for(int i = 0; i < audio.length; i += 2) {
				int val = (audio[i] << 8) | (audio[i+1] & 0xFF);
				val = (int) (val * volume);
				if(val > Short.MAX_VALUE) val = Short.MAX_VALUE;
				else if(val < Short.MIN_VALUE) val = Short.MIN_VALUE;	
				audio[i] = (byte) (val >> 8);
				audio[i + 1] = (byte) (val & 0x00FF);
			}
			return audio;
		}
	}
}
