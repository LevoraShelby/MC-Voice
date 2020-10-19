package test;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.bukkit.Location;
import org.bukkit.World;

import main.VoiceChatServer;

public class WhispersTest {
	public static void main(String[] args) throws SocketException, UnknownHostException {
		VoiceChatServer vcServer = new VoiceChatServer(8081);
		World world = new WorldImpl();

		new Thread( () -> {
			try {
				String soundGeneratorOtp = vcServer.playerJoined(new PlayerImpl(new Location(world, 0, 0, 0))).get();

				DatagramSocket socket = new DatagramSocket();
				byte[] signInData = new byte[7];
				signInData[0] = 0x00;
				for(int i = 0; i < 6; i++) {
					signInData[i + 1] = (byte) soundGeneratorOtp.charAt(i);
				}
				DatagramPacket signInPacket =
					new DatagramPacket(signInData, signInData.length, InetAddress.getLocalHost(), 8081);
				socket.send(signInPacket);

				DatagramPacket tokenPacket = new DatagramPacket(new byte[5], 5);
				socket.receive(tokenPacket);
				byte[] token = Arrays.copyOfRange(tokenPacket.getData(), 1, 5);

				byte[] audio = new byte[2000];
				byte[] data = new byte[2005];
				data[0] = 0x01;
				for(int i = 0; i < 4; i++) {
					data[i + 1] = token[i];
				}

				DatagramPacket voipPacket =
					new DatagramPacket(audio, audio.length, InetAddress.getLocalHost(), 8081);
				File audioFile = new File("/home/trevor/Downloads/sample.wav");
				AudioInputStreamHolder streamHolder = new AudioInputStreamHolder(
					AudioSystem.getAudioInputStream(audioFile)
				);
				ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
				executor.scheduleAtFixedRate(() -> {
					try {
						if(streamHolder.get().read(audio) == -1) {
							streamHolder.get().close();
							streamHolder.set(AudioSystem.getAudioInputStream(audioFile));
							streamHolder.get().read(audio);
						}
						for(int i = 0; i < audio.length; i++) {
							data[i + 5] = audio[i];
						}
						voipPacket.setData(data);
						socket.send(voipPacket);
					} catch (Exception err) { err.printStackTrace(); }
				}, 0, 62500, TimeUnit.MICROSECONDS);
			} catch (Exception err) { err.printStackTrace(); }
		}).start();
	}



	private static class AudioInputStreamHolder {
		private static final AudioFormat format = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true
		);
		private AudioInputStream stream;
		private AudioInputStream formattedStream;

		public AudioInputStreamHolder(AudioInputStream stream) {
			this.stream = stream;
			this.formattedStream = AudioSystem.getAudioInputStream(format, stream);
		}

		public AudioInputStream get() {
			return stream;
		}

		public void set(AudioInputStream stream) {
			try {
				this.formattedStream.close();
				this.stream.close();
			} catch (IOException e) { e.printStackTrace(); }
			this.stream = stream;
			this.formattedStream = AudioSystem.getAudioInputStream(format, stream);
		}
	}
}
