package test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.function.Function;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class AudioServer {
	private static final int WORD_MAX_VAL = (int) Math.pow(2, 15) - 1;
	private static final int WORD_MIN_VAL = (int) -Math.pow(2, 15);
	private static final Function<Integer, Integer> adjustVolume = vol -> (int) (vol * 0.25);
	private static final int port = 50005;

	public static void main(String[] args) throws Exception {
		new AudioServer().run();
	}

	public void run() throws Exception {
		DatagramSocket serverSocket = new DatagramSocket(port);

		/**
		 * Formula for lag = (byte_size/sample_rate)*2
		 * Byte size 9728 will produce ~ 0.45 seconds of lag. Voice slightly broken.
		 * Byte size 1400 will produce ~ 0.06 seconds of lag. Voice extremely broken.
		 * Byte size 4000 will produce ~ 0.18 seconds of lag. Voice slightly more broken then 9728.
		 */

		byte[] receiveData = new byte[2000];

		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true);
		SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(format);
		sourceDataLine.open();
//		FloatControl volume = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
		sourceDataLine.start();

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		int i = 0;
		while (i != 1000) {
			serverSocket.receive(receivePacket);
			try {
				byte[] soundBytes = receivePacket.getData();
				for(int j = 0; j < soundBytes.length; j += 2) {
					operateOnWord(soundBytes, j, adjustVolume, true);
				}
//				for(int j = 0; j < soundBytes.length; j++) {
//					soundBytes[j] = adjustVolume.apply((int) soundBytes[j]).byteValue();
//				}
				sourceDataLine.write(soundBytes, 0, soundBytes.length);
			} catch (Exception e) {
				e.printStackTrace();
			}
			i++;
		}

		sourceDataLine.drain();
		sourceDataLine.close();
		serverSocket.close();
	}


	private static void operateOnWord(byte[] arr, int offset, Function<Integer, Integer> operator, boolean bigEndian) {
		int val;
		if(bigEndian) {
			val = (arr[offset] << 8) | (arr[offset+1] & 0xFF);
		}
		else {
			val = (arr[offset + 1] << 8) | (arr[offset] & 0xFF);
		}

		val = operator.apply(val);
		if(val > WORD_MAX_VAL) val = WORD_MAX_VAL;
		else if(val < WORD_MIN_VAL) val = WORD_MIN_VAL;

		if(bigEndian) {
			arr[offset] = (byte) (val >> 8);
			arr[offset + 1] = (byte) (val & 0x00FF);
		}
		else {
			arr[offset] = (byte) (val & 0x00FF);
			arr[offset + 1] = (byte) (val >> 8);
		}
	}
}