package main;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PositionalVoiceChat extends JavaPlugin {
	@Override
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		try {
			VoiceChatServer vcServer = new VoiceChatServer(8081);
			pm.registerEvents(new VoiceChatConnector(vcServer), this);
		} catch (SocketException | UnknownHostException e) { e.printStackTrace(); }
	}
}
