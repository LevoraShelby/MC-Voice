package main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import main.ServerPacket.ServerPacketType;

//int framesPerVoice = (int) audioOutput.getFormat().getFrameRate()/16;
//int offset = framesPerVoice/audioOutput.getFormat().getFrameSize();

public class Client {
	private static Text addressInput;
	private static Text passcodeInput;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("MC Voice");
		
		Composite addressPage = new Composite(shell, SWT.NONE);
		addressPage.setLocation(0, 0);
		addressPage.setSize(444, 271);
		
		Button muteCheck = new Button(addressPage, SWT.CHECK);
		muteCheck.setLocation(10, 142);
		muteCheck.setSize(117, 19);
		muteCheck.setText("mute");
		
		addressInput = new Text(addressPage, SWT.BORDER);
		addressInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.CR) {
					passcodeInput.setFocus();	
				}
			}
		});
		addressInput.setLocation(10, 30);
		addressInput.setSize(190, 29);
		
		Label addressLabel = new Label(addressPage, SWT.NONE);
		addressLabel.setLocation(10, 10);
		addressLabel.setSize(102, 17);
		addressLabel.setText("Server Address");
		
		Label passcodeLabel = new Label(addressPage, SWT.NONE);
		passcodeLabel.setBounds(10, 72, 64, 17);
		passcodeLabel.setText("Passcode");
				
		passcodeInput = new Text(addressPage, SWT.BORDER);
		passcodeInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == SWT.CR) {
					//parses ip and port.
					String[] ipAndPort = addressInput.getText().split(":", 2);
					String ipAddress = ipAndPort[0];
					int port;
					try {
						port = Integer.parseInt(ipAndPort[1]);	
					} catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) { return; }

					//signs into VoiP server.
					VoipConnection conn;
					try {
						//TODO: Figure out how to set up non-local sockets on this machine and
						//uncomment the address parsing and use the parsed address for the socket,
						//instead of localhost at 8081.
						conn = new VoipConnection(new InetSocketAddress(ipAddress, port));
						String otp = passcodeInput.getText();
						if(otp.length() != 6 || !otp.matches("[a-kmnp-z0-9]*")) {
							return;
						}
						//TODO: Stop this statement from blocking process (can't close window).
						if(!conn.signIn(otp).get()) {
							return;
						}
						//TODO: Remove
						else {
							addressLabel.setText("connected");
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}

					//TODO: Make constants related to audio format variable.

					//Sends client's voice to server.
					if(!muteCheck.getSelection()) {
						new Thread(() -> {
							TargetDataLine mic;
							try {
								mic = Client.getMic();
							} catch (LineUnavailableException ex) { ex.printStackTrace(); return; }

							byte[] audio = new byte[2000];
							while(true) {
								mic.read(audio, 0, audio.length);
								try {
									conn.voice(audio);
								} catch (IOException ex) { ex.printStackTrace(); }
							}
						}).start();
					}

					//TODO: Make code prettier.
					//TODO: Get rid of mute feature (for now) and have it always play sounds.
					//Receives and plays voices from server if you're muted.
					new Thread(() -> {
						Map<List<Byte>, SourceDataLine> playerVoiceOutputs = new HashMap<>();
						while(true) {
							ServerPacket packet;
							try {
								packet = conn.receive().get();
							} catch (InterruptedException | ExecutionException | IOException ex) { continue; }

							if(packet.isValid() && packet.getType() == ServerPacketType.VOIP) {
								List<Byte> speakerUid = new ArrayList<>(packet.getSpeakerUid().length);
								for(int i = 0; i < packet.getSpeakerUid().length; i++) {
									speakerUid.add(packet.getSpeakerUid()[i]);
								}
								if(!playerVoiceOutputs.containsKey(speakerUid)) {
									SourceDataLine playerVoiceOutput;
									try {
										playerVoiceOutput = Client.getAudioOutput();
									} catch (LineUnavailableException ex) { return; }
									playerVoiceOutputs.put(Collections.unmodifiableList(speakerUid), playerVoiceOutput);
								}
								SourceDataLine playerVoiceOutput = playerVoiceOutputs.get(speakerUid);
								playerVoiceOutput.write(packet.getAudio(), 0, 2000);
							}
						}
					}).start();

					//Plays voices.
//					new Thread(() -> {
//						if(!isMute) return;

//						List<Voice> processedVoices = new ArrayList<Voice>();
//						while(true) {
//							//TODO: Refactor such that the loop goes byte-by-byte then voice-by-
//							//voice. This is to allow for better operations (e.g. averaging).
//							if(voices.size() == 0) {
//								try {
//									Thread.sleep(62, 500000);
//								} catch (InterruptedException ex) { ex.printStackTrace(); return; }
//								continue;
//							}
//
//							byte[] audio = new byte[2000];
//							long bufferStartFrame = audioOutput.getLongFramePosition();
//							long bufferEndFrame = bufferStartFrame + 999;
//							for(Voice voice : voices) {
//								long voiceEndFrame = voice.startFrame + 999;
//								if(voice.startFrame > bufferEndFrame) {
//									continue;
//								}
//								else if(voiceEndFrame < bufferStartFrame) {
//									processedVoices.add(voice);
//									continue;
//								}
//
//								if(voice.startFrame >= bufferStartFrame) {
//									for(long frameIndex = voice.startFrame; frameIndex <= bufferEndFrame; frameIndex++) {
//										int byteIndex = (int) (frameIndex - bufferStartFrame) * 2;
//										Client.addAudio(voice, audio, byteIndex);
//									}
//								}
//								else if(voice.startFrame < bufferStartFrame) {
//									for(long frameIndex = bufferStartFrame; frameIndex <= voiceEndFrame; frameIndex++) {
//										int byteIndex = (int) (frameIndex - bufferStartFrame) * 2;
//										Client.addAudio(voice, audio, byteIndex);
//									}
//								}
//							}
//							voices.removeAll(processedVoices);
//							processedVoices.clear();
//							audioOutput.write(audio, 0, 2000);
//						}
//					}).start();
				}
			}
		});
		passcodeInput.setBounds(10, 95, 81, 29);

		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}


	private static TargetDataLine getMic() throws LineUnavailableException {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true);
		TargetDataLine line = AudioSystem.getTargetDataLine(format);
		line.open(); line.start();
		return line;
	}


	private static SourceDataLine getAudioOutput() throws LineUnavailableException {
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000f, 16, 1, 2, 16000f, true);
		SourceDataLine line = AudioSystem.getSourceDataLine(format);
		line.open(); line.start();
		return line;
	}


//	private static void addAudio(Voice voice, byte[] audio, int byteIndex) {
//		audio[byteIndex] = voice.block[byteIndex];
//		audio[byteIndex + 1] = voice.block[byteIndex + 1];
//
////		int frame = (audio[byteIndex] << 8) | (audio[byteIndex+1] & 0xFF);
////		frame += (voice.block[byteIndex] << 8) | (voice.block[byteIndex+1] & 0xFF);
////		if(frame < -32768) frame = -32768;
////		else if(frame > 32767) frame = 32767;
////		audio[byteIndex] = (byte) (frame >> 8);
////		audio[byteIndex+1] = (byte) (frame & 0x00FF);
//	}
}
