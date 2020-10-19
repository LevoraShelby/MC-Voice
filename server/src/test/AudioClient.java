package test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;

public class AudioClient {
	public static void main(String[] args) throws Exception {
		new AudioClient().run();
	}

	public void run() throws Exception {
		//frameSize is (sampleSizeInBits / 8) * channels
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true);

		TargetDataLine line = AudioSystem.getTargetDataLine(format);
		line.open();
		line.start();

		byte[] data = new byte[2000];
		InetAddress addr = InetAddress.getByName("127.0.0.1");
		DatagramSocket socket = new DatagramSocket();
		int i = 1000;
		while (i != 0) {
			//read the next chunk of data from the TargetDataLine.
			line.read(data, 0, data.length);
			//send this chunk of data.
			DatagramPacket dgp = new DatagramPacket(data, data.length, addr, 50005);

			socket.send(dgp);
			i--;
		}
		socket.close();
	}
}