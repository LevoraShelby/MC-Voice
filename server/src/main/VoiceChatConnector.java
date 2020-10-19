package main;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VoiceChatConnector implements Listener {
	private final VoiceChatServer vcServer;

	public VoiceChatConnector(VoiceChatServer vcServer) {
		this.vcServer = vcServer;
	}


	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Future<String> incomingOtp = this.vcServer.playerJoined(e.getPlayer());
		new Thread( () -> {
			try {
				String otp = incomingOtp.get();
				//TODO: The Minecraft server NEEDS to communicate the VoiP server's address, lest some malicious
				//third party tell their own address to the client and act as a proxy.
				e.getPlayer().sendMessage(otp);
			} catch (InterruptedException | ExecutionException err) { err.printStackTrace(); }
		}).start();
	}


	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		this.vcServer.playerQuit(e.getPlayer());
	}
}
