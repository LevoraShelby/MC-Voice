package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//TODO: If closing is added, add locks for closing, remove "close" checks, or throw
//SocketExceptions like DatagramSockets do.
public class VoipConnection {
	private ExecutorService executor;
	private DatagramSocket socket;
	private SocketAddress serverAddress;
	private byte[] token;

	public VoipConnection(SocketAddress serverAddress) throws SocketException  {
		this.executor = Executors.newCachedThreadPool();
		this.socket = new DatagramSocket();
		this.serverAddress = serverAddress;
		this.token = null;
	}


	public Future<Boolean> signIn(String otp) {
		return this.executor.submit(() -> {
			if(this.socket.isClosed()) { return null; }
			else if(this.token != null) { return null; }

			byte[] signInData = new byte[7];
			signInData[0] = 0x00;
			for(int i = 0; i < 6; i++) {
				signInData[i + 1] = (byte) otp.charAt(i);
			}
			DatagramPacket signInPacket = 
				new DatagramPacket(signInData, signInData.length, serverAddress);
			this.socket.send(signInPacket);
			try {
				ServerPacket response = this.receive().get(5, TimeUnit.SECONDS);
				if(response.isValid() && response.getType() == ServerPacket.ServerPacketType.TOKEN_CHANGE) {
					this.token = response.getToken();
					return true;
				}
				else {
					return false;
				}
			} catch (TimeoutException e) { return false; }
		});
	}


	public void voice(byte[] audio) throws IOException {
		if(this.socket.isClosed()) { System.err.println("socket closed"); return; }

		byte[] voipData = new byte[2005];
		voipData[0] = 0x01;
		for(int i = 0; i < 4; i++) {
			voipData[i + 1] = this.token[i];
		}
		for(int i = 0; i < 2000; i++) {
			voipData[i + 5] = audio[i];
		}
		DatagramPacket voipPacket =
			new DatagramPacket(voipData, voipData.length, this.serverAddress);
		this.socket.send(voipPacket);
	}


	public Future<ServerPacket> receive() throws IOException {
		return this.executor.submit(() -> {
			if(this.socket.isClosed()) { return null; }

			DatagramPacket packet = new DatagramPacket(new byte[2017], 2017);
			this.socket.receive(packet);
			return new ServerPacket(packet);
		});
	}
}
