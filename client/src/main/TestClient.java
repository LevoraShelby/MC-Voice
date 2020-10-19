package main;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TestClient {
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
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}

					//TODO: Make constants related to audio format variable.

					//Sends client's voice to server.
					new Thread(() -> {
						try {
							byte[] audio = Files.readAllBytes(Paths.get("/home/trevor/Projects/MC Voice/test voice binaries/bin1"));
							while(true) {
								for(int i = 0; i < audio.length; i += 2000) {
									conn.voice(Arrays.copyOfRange(audio, i, i + 2000));
									Thread.sleep(62);
								}
							}
						} catch (Exception exc) {}
					}).start();
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
}
