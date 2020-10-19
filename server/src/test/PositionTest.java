package test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import main.VoiceChatServer;

public class PositionTest {
	public static void main(String[] args) throws Exception {
		VoiceChatServer vcServer = new VoiceChatServer(8081);
		World world = new WorldImpl();

		Player speaker = new PlayerImpl(new Location(world, 0.0, 0.0, 0.0));
		VoipConnection speakerConn = new VoipConnection();
		String speakerOtp = vcServer.playerJoined(speaker).get();
		speakerConn.signIn(speakerOtp);
		speakerConn.receive();

		Player walker = new PlayerImpl(new Location(world, 0.0, 0.0, 0.0));
		VoipConnection walkerConn = new VoipConnection();
		String walkerOtp = vcServer.playerJoined(walker).get();
		walkerConn.signIn(walkerOtp);
		walkerConn.receive();

		walker.teleport(walker.getLocation().add(new Vector(9.0, 0.0, 0.0)));
		byte[] audio = new byte[2000];
		audio[0] = 0x0A;
		speakerConn.voice(audio);
		System.out.println(walkerConn.receive().getAudio()[0]);
	}



	private static class ServerPacket {
		private boolean isValidPacket;
		private ServerPacketType type;
		private byte[] token;
		private byte[] audio;
		private byte[] speakerUid;
		private SocketAddress address;

		public ServerPacket(DatagramPacket packet) {
			this.address = packet.getSocketAddress();
			if(packet.getLength() == 0 || packet.getLength() > 2017) {
				this.type = null;
				this.invalidate();
				return;
			}
			this.type = this.getType(packet.getData()[0]);
			if(this.type == ServerPacketType.TOKEN_CHANGE) {
				if(packet.getLength() != 5) {
					this.invalidate();
					return;
				}
				this.isValidPacket = true;
				this.token = Arrays.copyOfRange(packet.getData(), 1, 5);
				this.audio = null;
				this.speakerUid = null;
			}
			else if(this.type == ServerPacketType.VOIP) {
				if(packet.getLength() != 2017) {
					this.invalidate();
					return;
				}
				this.isValidPacket = true;
				this.audio = Arrays.copyOfRange(packet.getData(), 17, 2017);
				this.speakerUid = Arrays.copyOfRange(packet.getData(), 1, 17);
				this.token = null;
			}
			else {
				this.invalidate();
			}
		}

		public SocketAddress getAddress() {
			return this.address;
		}

		public boolean isValid() {
			return this.isValidPacket;
		}

		public ServerPacketType getType() {
			return this.type;
		}

		public byte[] getToken() {
			return this.token;
		}

		public byte[] getAudio() {
			return this.audio;
		}

		public byte[] getSpeakerUid() {
			return this.speakerUid;
		}

		private void invalidate() {
			this.isValidPacket = false;
			this.token = null;
			this.audio = null;
			this.speakerUid = null;
		}

		private ServerPacketType getType(byte typeByte) {
			int typeIndex = typeByte & 0xFF;
			if(typeIndex > 1) return null;
			return ServerPacketType.values()[typeIndex];
		}

		public enum ServerPacketType {
			TOKEN_CHANGE,
			VOIP
		}
	}



	private static class VoipConnection {
		private DatagramSocket socket;
		private byte[] token;

		public VoipConnection() throws Exception {
			this.socket = new DatagramSocket();
			this.token = null;
		}


		public void signIn(String otp) throws Exception {
			byte[] signInData = new byte[7];
			signInData[0] = 0x00;
			for(int i = 0; i < 6; i++) {
				signInData[i + 1] = (byte) otp.charAt(i);
			}
			DatagramPacket signInPacket = 
				new DatagramPacket(signInData, signInData.length, InetAddress.getLocalHost(), 8081);
			this.socket.send(signInPacket);
		}


		public void voice(byte[] audio) throws Exception {
			byte[] voipData = new byte[2005];
			voipData[0] = 0x01;
			for(int i = 0; i < 4; i++) {
				voipData[i + 1] = this.token[i];
			}
			for(int i = 0; i < 2000; i++) {
				voipData[i + 5] = audio[i];
			}
			DatagramPacket voipPacket =
				new DatagramPacket(voipData, voipData.length, InetAddress.getLocalHost(), 8081);
			this.socket.send(voipPacket);
		}


		public ServerPacket receive() throws Exception {
			DatagramPacket packet = new DatagramPacket(new byte[2017], 2017);
			this.socket.receive(packet);
			ServerPacket serverPacket = new ServerPacket(packet);
			if(serverPacket.isValid() && serverPacket.getType() == ServerPacket.ServerPacketType.TOKEN_CHANGE) {
				this.token = serverPacket.getToken();
			}
			return serverPacket;
		}
	}
}
