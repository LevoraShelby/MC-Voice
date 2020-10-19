package main;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ClientPacket {
	private boolean isValidPacket;
	private ClientPacketType type;
	private String otp;
	private byte[] token;
	private byte[] audio;
	private SocketAddress address;

	public ClientPacket(DatagramPacket packet) {
		this.address = packet.getSocketAddress();
		if(packet.getLength() == 0 || packet.getLength() > 2005) {
			this.type = null;
			this.invalidate();
			return;
		}
		this.type = this.getType(packet.getData()[0]);
		if(this.type == ClientPacketType.SIGN_IN) {
			if(packet.getLength() != 7) {
				this.invalidate();
				return;
			}
			this.otp = this.getOtp(Arrays.copyOfRange(packet.getData(), 1, 7));
			this.isValidPacket = this.otp != null;
			this.token = null;
			this.audio = null;
		}
		else if(this.type == ClientPacketType.VOIP) {
			if(packet.getLength() != 2005) {
				this.invalidate();
				return;
			}
			this.isValidPacket = true;
			this.token = Arrays.copyOfRange(packet.getData(), 1, 5);
			this.audio  = Arrays.copyOfRange(packet.getData(), 5, 2005);
			this.otp = null;
		}
		else if(
			this.type == ClientPacketType.TOKEN_CHANGE_ACK
			|| this.type == ClientPacketType.SIGN_OUT
		) {
			if(packet.getLength() != 5) {
				this.invalidate();
				return;
			}
			this.isValidPacket = true;
			this.token = Arrays.copyOfRange(packet.getData(), 1, 5);
			this.otp = null;
			this.audio = null;
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


	public ClientPacketType getType() {
		return this.type;
	}


	public String getOtp() {
		return this.otp;
	}


	public byte[] getToken() {
		return this.token;
	}


	public byte[] getAudio() {
		return this.audio;
	}


	private void invalidate() {
		this.isValidPacket = false;
		this.otp = null;
		this.token = null;
		this.audio = null;
	}


	private ClientPacketType getType(byte typeByte) {
		int typeIndex = typeByte & 0xFF;
		if(typeIndex > 3) return null;
		return ClientPacketType.values()[typeIndex];
	}


	private String getOtp(byte[] otpBytes) {
		String otp = new String(otpBytes);
		if(Pattern.matches("^[0-9a-kmnp-z]+$", otp)) {
			return otp;
		}
		else {
			return null;
		}
	}


	public enum ClientPacketType {
		SIGN_IN,
		VOIP,
		TOKEN_CHANGE_ACK,
		SIGN_OUT
	}
}
