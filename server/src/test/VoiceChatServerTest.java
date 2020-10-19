package test;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import main.VoiceChatServer;

public class VoiceChatServerTest {
	public static void main(String[] args) throws Exception {
		VoiceChatServer vcServer = new VoiceChatServer(8081);
		World world = new WorldImpl();

		Player player1 = new PlayerImpl(new Location(world, 1.0, 0.0, 0.0));
		Player player2 = new PlayerImpl(new Location(world, 2.0, 0.0, 0.0));
		System.out.println("player1 otp: " + vcServer.playerJoined(player1).get());
		System.out.println("player2 otp: " + vcServer.playerJoined(player2).get());

//		new Thread(() -> {
//			boolean teleportCloser = false;
//			while(true) {
//				if(teleportCloser) {
//					player2.teleport(new Location(world, 2.0, 0.0, 0.0));
//				}
//				else {
//					player2.teleport(new Location(world, 10.0, 0.0, 0.0));	
//				}
//				System.out.println(teleportCloser ? "teleported closer" : "teleported away");
//				teleportCloser = !teleportCloser;
//				try {
//					Thread.sleep(5000);
//				} catch (InterruptedException e) { e.printStackTrace(); }	
//			}
//		}).start();
	}
}

//192.168.137.120:8081