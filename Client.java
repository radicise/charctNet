import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
class Client {
	public static final double version = 0.1;//version
	public static void main(String[] args) throws Exception {
		String uname = "radicise";
		String password = "BennyAndTheJets3301";
		Socket cnct = new Socket(InetAddress.getByAddress(new byte[]{0x7f, 0x00, 0x00, 0x01}), 15227);
		OutputStream outS = cnct.getOutputStream();
		InputStream inS = cnct.getInputStream();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream ouD = new DataOutputStream(out);
		DataInputStream inD = new DataInputStream(inS);
		int ti;
		ouD.writeByte(12);
		ouD.writeDouble(version);
		ouD.flush();
		out.writeTo(outS);
		out.reset();
		byte[] messBs;
		while (inS.available() < 1) {
			Thread.sleep(200);
		}
		ti = inS.read();
		if (ti == 13) {
			messBs = new byte[inS.available()];
			inS.read(messBs);
			System.out.println("kicked by server for reason: " + new String(messBs, StandardCharsets.UTF_8));
			System.exit(0);
		}
		if (ti != 1) {
			System.out.println("error: invalid packet");
			System.exit(0);
		}
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
		for (byte n : out.toByteArray()) {
			System.out.print(n + " ");
		}
		out.write(dig.digest(password.getBytes(StandardCharsets.UTF_8)));
		for (byte n = 0; n < 8; n++) {
			ouD.writeInt(inD.readInt());
		}
		ouD.flush();
		out.write(uname.getBytes(StandardCharsets.UTF_8));
		byte[] resp = dig.digest(out.toByteArray());
		out.reset();
		out.write(11);
		out.write(resp);
		out.write(uname.getBytes(StandardCharsets.UTF_8));
		out.writeTo(outS);
		out.reset();
		while (inS.available() < 1) {
			Thread.sleep(200);
		}
		ti = inS.read();
		if (ti == 13) {
			messBs = new byte[inS.available()];
			inS.read(messBs);
			System.out.println("kicked by server for reason: " + new String(messBs, StandardCharsets.UTF_8));
			System.exit(0);
		}
		new Thread(new Runnable() {
        	public void run() {
        		try {
        			byte[] message;
        			int si;
        			while (true) {
						while (inS.available() < 1) {
							Thread.sleep(200);
						}
						si = inS.read();
						if (si == 13) {
							message = new byte[inS.available()];
							inS.read(message);
							System.out.println("kicked by server for reason: " + new String(message, StandardCharsets.UTF_8));
							System.exit(0);
						}
						message = new byte[inS.available()];
						inS.read(message);
						System.out.println(new String(message, StandardCharsets.UTF_8));
        			}
				}
        		catch (Exception e) {
					System.out.println("Exception in message reception Thread: " + e);
					System.exit(0);
				}
        	}
        }).start();
		BufferedReader inRead = new BufferedReader(new InputStreamReader(System.in));
		String input;
		while (true) {
			input = inRead.readLine();
			if (input.equals("/help")) {
				System.out.println("Use \"/exit\" to exit the program");
			}
			else if (input.equals("/exit")) {
				outS.write(13);
				System.out.println("Exiting...");
				Thread.sleep(200);
				System.exit(0);
			}
			else {
				out.write(7);
				out.write(input.getBytes(StandardCharsets.UTF_8));
			}
		}
	}
}
