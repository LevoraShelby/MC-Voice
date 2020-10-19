package demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import main.VoiceChatServer;
import test.PlayerImpl;

public class SignIn {
	public static void main(String[] args) {
		try {
			VoiceChatServer vcServer = new VoiceChatServer(8080);
			vcServer.playerJoined(new PlayerImpl(null));
			DatagramSocket socket = new DatagramSocket(8081);
			socket.send(new DatagramPacket(new byte[] {
				0x00,
				0x61,
				0x61,
				0x61,
				0x61,
				0x61,
				0x61
			}, 7, new InetSocketAddress(InetAddress.getLocalHost(), 8080)));
			DatagramPacket packet = new DatagramPacket(new byte[5], 5);
			socket.receive(packet);
			for(int i = 0; i < packet.getData().length; i++) {
				System.out.println(packet.getData()[i] & 0xFF);
			}
			socket.close();
		} catch (IOException e ) { e.printStackTrace(); }
	}
}
