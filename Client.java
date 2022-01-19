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
	public static final double version = 0.2;//Version
	public static log conversation = new log("cN-chatLogged.txt", (short) 30, "conver", 65536, false, StandardCharsets.UTF_8);
	public static void main(String[] args) throws Exception {
		System.out.println("Starting program...");
		String inputEncoding = "UTF-8";
		boolean useTerminalEscapes = true;
		int ti = 0;
		byte[] ip = new byte[4];
		int port = 0;
		try {
			String[] inputs = args[2].split(":");
			port = Integer.valueOf(inputs[1]);
			String ipS = inputs[0];
			String[] nums = ipS.split("\\.");
			for (byte n = 0; n < 4; n++) {
				ti = Integer.valueOf(nums[n]);
				if (ti > 127) {
					ti -= 256;
				}
				ip[n] = (byte) ti;
			}
			String ts;
			for (int n = 3; n < args.length; n++) {
				ts = args[n].toLowerCase();
				switch (ts) {
					case ("useterminalescapes=true"):
						useTerminalEscapes = true;
						break;
					case ("useterminalescapes=false"):
						useTerminalEscapes = false;
						break;
					case ("inputencoding='utf-8'"):
						inputEncoding = "UTF-8";
						break;
					case ("inputencoding='utf-16le'"):
						inputEncoding = "UTF-16LE";
						break;
					case ("inputencoding='utf-16be'"):
						inputEncoding = "UTF-16BE";
						break;
					case ("inputencoding='utf-16'"):
						inputEncoding = "UTF-16";
						break;
					case ("inputencoding='us-ascii'"):
						inputEncoding = "US-ASCII";
						break;
					case ("inputencoding='iso-8859-1'"):
						inputEncoding = "ISO-8859-1";
						break;
					default:
						System.out.println("invalid operation modifier, launching program anyways");
				}
			}
		}
		catch (Exception e) {
			System.out.println("could not parse arguments: " + e);
			System.exit(1);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String uname = args[0];//"defaultAccount";
		String password = args[1];//"BennyAndTheJets3301";
		System.out.println("Attempting connection...\nIf you see visible codes in your terminal, add \"useTerminalEscapes=false\" to program launch arguments");
		Socket cnct = null;
		try {
			cnct = new Socket(InetAddress.getByAddress(ip), port);
		}
		catch (Exception e) {
			System.out.println("Could not connect to server: " + e);
			System.exit(3);
		}
		OutputStream outS = cnct.getOutputStream();
		InputStream inS = cnct.getInputStream();
		DataOutputStream ouD = new DataOutputStream(out);
		DataInputStream inD = new DataInputStream(inS);
		ouD.writeByte(12);
		ouD.writeDouble(version);
		ouD.flush();
		out.writeTo(outS);
		out.reset();
		byte[] messBs;
		ti = inS.read();
		if (ti == -1) {
			System.out.println("unexpectedly disconnected from server");
			System.exit(2);
		}
		if (ti == 13) {
			messBs = new byte[inS.available()];
			inS.read(messBs);
			System.out.println("disconnected by server for reason: " + new String(messBs, StandardCharsets.UTF_8));
			System.exit(0);
		}
		if (ti != 1) {
			System.out.println("error: invalid packet");
			System.exit(0);
		}
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
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
		conversation.startExecutor();
		conversation.append("+Connected to " + cnct.getInetAddress().getHostAddress() + ":" + cnct.getPort() + "\n");
		while (inS.available() < 1) {
			Thread.sleep(50);
		}
		boolean useTeEsc = useTerminalEscapes;
		new Thread(new Runnable() {
        	public void run() {
        		try {
        			byte[] message;
        			int si;
        			String tex;
        			while (true) {
						si = inS.read();
						if (si == -1) {
							System.out.println("unexpectedly disconnected from server");
							System.exit(2);
						}
						if (si == 13) {
							message = new byte[inS.available()];
							inS.read(message);
							System.out.println("disconnected by server for reason: " + new String(message, StandardCharsets.UTF_8));
							System.exit(0);
						}
						if (si != 7) {
							System.out.println("disconnected by client for invalid packet" + si);
							System.exit(0);
						}
						message = new byte[inD.readShort()];
						inS.read(message);
						tex = new String(message, StandardCharsets.UTF_8);
						if (useTeEsc) {
							System.out.print("\u001b7\u001b[1S\u001b[1A\u001b[1G\u001b[1L" + tex + "\u001b[0m\u001b8\u001b[1B");
						}
						else {
							System.out.print(tex);
						}
						conversation.append("\"" + tex);
        			}
				}
        		catch (Exception e) {
					System.out.println("Exception in message reception Thread: " + e);
					System.exit(0);
				}
        	}
        }).start();
		BufferedReader inRead = new BufferedReader(new InputStreamReader(System.in, inputEncoding));
		String input;
		byte[] mess;
		while (true) {
			input = inRead.readLine();
			if (useTerminalEscapes) {
				System.out.print("\u001b[1T\u001b[2K\u001b[1G");
			}
			if (input.length() < 1) {
				continue;
			}
			if (input.toLowerCase().equals("/help")) {
				System.out.println("Use \"/exit\" to exit the program\nUse \"/showConfig\" to display the current program configuration");
			}
			else if (input.toLowerCase().equals("/exit")) {
				outS.write(13);
				System.out.println("Exiting...");
				conversation.flush();
				Thread.sleep(200);
				System.exit(0);
			}
			else if (input.toLowerCase().equals("/showconfig")) {
				System.out.println("useTerminalEscapes=" + useTerminalEscapes + "\ninputEncoding=" + inputEncoding);
			}
			else if (input.length() > 8000) {
				System.out.println("client: you may not send messages in excess of 8000 characters!");
			}
			else {
				out.write(7);
				mess = input.getBytes(StandardCharsets.UTF_8);
				ouD.writeShort(mess.length);
				out.write(mess);
				out.writeTo(outS);
				out.reset();
			}
		}
	}
}
