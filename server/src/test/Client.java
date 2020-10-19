package test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.Line;
//import javax.sound.sampled.LineUnavailableException;
//import javax.sound.sampled.Mixer;

public class Client {
	public static void main(String[] args) {
//		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true);
//		SourceDataLine output = AudioSystem.getSourceDataLine(format);
//		output.open();
//		output.start();
//		Thread framePositionPrinter = new Thread( () -> {
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) { e.printStackTrace(); return; }
//			for(int i = 0; i < 16; i++) {
//				System.out.println(output.getLongFramePosition());
//				try {
//					Thread.sleep(62, 500000);
//				} catch (InterruptedException e) { e.printStackTrace(); return; }
//			}
//		});
//		framePositionPrinter.start();
//		for(int i = 0; i < 32; i++) {
//			output.write(new byte[2000], 0, 2000);
//		};
//		output.close();
		List<Byte> list1 = Collections.unmodifiableList(Arrays.asList((byte) 0x04));
		List<Byte> list2 = Arrays.asList((byte) 0x04);
		System.out.println(list1 == list2);
		System.out.println(list1.equals(list2));
		System.out.println(list2.equals(list1));
	}


//	private static void surveyMixers(Mixer.Info[] mixerInfos, Line.Info lineInfo) {
//		for(int i = 0; i < mixerInfos.length; i++) {
//			Mixer.Info mixerInfo = mixerInfos[i];
//			Mixer mixer = AudioSystem.getMixer(mixerInfo);
//			if(mixer.isLineSupported(lineInfo)) System.out.print("*");
//			System.out.println(mixerInfo.getDescription());
//		}
//	}
}
