package main;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Arrays;

public class ServerPacket {
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
