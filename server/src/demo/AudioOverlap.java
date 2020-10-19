package demo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioOverlap {
	public static void main(String[] args) throws LineUnavailableException, UnsupportedAudioFileException, IOException, InterruptedException {
//		listen("/home/trevor/Projects/MC Voice/test voice binaries/bin4");
		noise(new String[] {
			"/home/trevor/Projects/MC Voice/test voice binaries/bin4"
		});
	}


	private static void noise(String[] filenames) throws LineUnavailableException, IOException {
		byte[][] voices = new byte[filenames.length][];
		for(int i = 0; i < filenames.length; i++) {
			FileInputStream voiceStream = new FileInputStream(filenames[i]);
			byte[] voice = new byte[voiceStream.available()];
			voiceStream.read(voice);
			voiceStream.close();
			voices[i] = voice;
		}

		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true);
		SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(format);
		sourceDataLine.open();
		sourceDataLine.start();

		for(int index = 0; index < voices[0].length; index += 2000) {
			byte[] chunk = new byte[2000];
			//TODO: Change mixing method. Averaging makes speaker A quieter is speaker B is present,
			//even if speaker B isn't making much noise.
			for(int subindex = 0; subindex < chunk.length; subindex += 2) {
				int sumVal = 0;
				for(int voiceIndex = 0; voiceIndex < voices.length; voiceIndex++) {
					byte[] voice = voices[voiceIndex];
					sumVal += (voice[index+subindex] << 8) | (voice[index+subindex+1] & 0xFF);
				}
				int avgVal = sumVal/voices.length;
				chunk[subindex] = (byte) (avgVal >> 8);
				chunk[subindex+1] = (byte) (avgVal & 0x00FF);
			}
			sourceDataLine.write(chunk, 0, 2000);
		}

		sourceDataLine.close();
	}


	private static void listen(String filename) throws LineUnavailableException, IOException {
		//frameSize is (sampleSizeInBits / 8) * channels
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true);

		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		if (!AudioSystem.isLineSupported(info)) {
			System.out.println("Line matching " + info + " not supported.");
			return;
		}

		TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(format);
		line.start();

		FileOutputStream out = new FileOutputStream(filename);
		byte[] data = new byte[2000];
		int i = 240;
		while (i != 0) {
			//read the next chunk of data from the TargetDataLine.
			line.read(data, 0, data.length);
			out.write(data);
			i--;
		}
		System.out.println("!");
		out.close();
	}
}
