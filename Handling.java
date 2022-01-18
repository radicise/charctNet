import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
class Handling implements Runnable {
	public static log chatlg = new log("cN-chatLog.txt", (short) 30, "chat", 65536, true, StandardCharsets.UTF_8);
	public static log servlg = new log("cN-serverLog.txt", (short) 30, "server", 65536, true, StandardCharsets.UTF_8);
	static volatile List<String> connected = new ArrayList<String>();
	static volatile List<Socket> socks = new ArrayList<Socket>();
	public static volatile String motd = "Development server, you may experence bugs";
	//For future-proofing, please add no user names over 64 bytes when encoded in UTF-8
	static int[] pws = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0x8cd1d8ce, 0xbdbe6b8c, 0xf858694c, 0xd7ecd800, 0x7e7a956a, 0xb4b82a54, 0xcde27bf9, 0x6c06e2eb};
	static String[] unames = {"guest", "defaultAccount"};
	static List<String> namel = Arrays.asList(unames);
	static ArrayList<String> names = new ArrayList<String>(namel);
	static SecureRandom sRand = new SecureRandom();
	public static final double version = 0.2;//version
	public static int port = 15227;
	Socket socket;
	String username = "";
	Handling(Socket sock) {
		socket = sock;
	}
	public static void main(String[] arg) {
		System.out.println("Server is starting...");
		try {
			ServerSocket servsock = new ServerSocket(port);
			accept(servsock);
		} catch (Exception e) {
			servlg.append("Exception in server: " + e + "\n");
		}
	}
	static void close() {
		servlg.append("Server closing...\n");
		chatlg.flush();
		servlg.flush();
		byte[] mBs = "server closing".getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[mBs.length + 1];
		System.arraycopy(mBs, 0, data, 1, mBs.length);
		data[0] = 13;
		List<Socket> tl = socks;
		for (Socket s : tl) {
			try {
				s.getOutputStream().write(data);
			}
			catch (Exception e) {
				servlg.append("Exception in transmitting disconnection: " + e + "\n");
			}
		}
		System.exit(0);
	}
	static void accept(ServerSocket servsock) throws Exception {
		servlg.startExecutor();
		chatlg.startExecutor();
		servlg.append("Server has started on port " + port + "\n");
		while (true) {
			Handling pl = new Handling(servsock.accept());
	        new Thread(new Runnable() {
	        	Handling ple = pl;
	        	public void run() {
	        		try {
	        			ple.serve();
	        			Thread.sleep(200);
	        			ple.socket.close();
	        		}
	        		catch (Exception e) {
	        			System.out.println(e);
	        		}
	        		synchronized (socks) {
	        			List<Socket> tl = socks;
	        			tl.remove(ple.socket);
	        			socks = tl;
	        		}
	        		synchronized (connected) {
	        			List<String> tn = connected;
	        			tn.remove(ple.username);
	        			connected = tn;
	        		}
	        		if (!ple.username.equals("")) {
	        			transmit(ple.username + " has left the chat\n");
	        		}
	        	}
	        }).start();
	        Thread.sleep(500);
		}
	}
	static void transmit (String message) {
		ByteArrayOutputStream tBarOS = new ByteArrayOutputStream();
		DataOutputStream dOS = new DataOutputStream(tBarOS);
		List<Socket> tl = socks;
		byte[] mBs = message.getBytes(StandardCharsets.UTF_8);
		tBarOS.write(7);
		try {
			dOS.writeShort(mBs.length);
			dOS.write(mBs);
			dOS.flush();
		}
		catch (Exception e) {
			servlg.append("exception in forming message packet: " + e);
			return;
		}
		byte[] data = tBarOS.toByteArray();
		for (Socket s : tl) {
			try {
				s.getOutputStream().write(data);
			}
			catch (Exception e) {
				servlg.append("Exception in transmitting message: " + e + "\n");
				synchronized (socks) {
        			List<Socket> sl = socks;
        			sl.remove(s);
        			socks = sl;
        		}
			}
		}
		chatlg.append(message);
	}
	public void run() {}
	void serve() throws Exception {
		OutputStream outS = socket.getOutputStream();
		InputStream inS = socket.getInputStream();
		DataInputStream inD = new DataInputStream(inS);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream ouD = new DataOutputStream(out);
		out.write(ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array());
		//TODO Possibly fix previous line, is backing array equal length equal to allocation value in all implementations?
		int si;
		ouD.writeInt(sRand.nextInt());
		ouD.writeInt(sRand.nextInt());
		ouD.flush();
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
		byte[] salt = dig.digest(out.toByteArray());
		out.reset();
		while (inS.available() < 1) {
			Thread.sleep(200);
		}
		si = inS.read();
		if (si == 13 || si == -1) {
			return;
		}
		double ver = inD.readDouble();
		if (ver < 0) {
			ouD.writeDouble(version);
			ouD.flush();
			out.writeTo(outS);
			return;
		}
		if (ver != version) {
			out.write(13);
			out.write("requested protocol not available".getBytes(StandardCharsets.UTF_8));
			out.writeTo(outS);
			return;
		}
		out.reset();
		out.write(1);
		out.write(salt);
		out.writeTo(outS);
		out.reset();
		int ti = inS.read();
		if (ti == 13 || ti == -1) {
			return;
		}
		if (ti != 11) {
			servlg.append("invalid packet\n");
			out.write(13);
			out.write("invalid packet".getBytes(StandardCharsets.UTF_8));
			out.writeTo(outS);
			return;
		}
		byte[] resp = new byte[32];
		for (byte n = 0; n < 32; n++) {
			ti = inS.read();
			if (ti == -1) {
				return;
			}
			if (ti > 127) {
				ti -= 256;
			}
			resp[n] = (byte) ti;
		}
		if (inS.available() < 1) {
			return;
		}
		byte[] nBs = new byte[inS.available()];
		//TODO It is never correct to use the return value of InputStream.available() to allocate a buffer intended to hold all data in the stream
		inS.read(nBs);
		String name = new String(nBs, StandardCharsets.UTF_8);
		if (!names.contains(name)) {
			out.write(13);
			out.write("username does not exist on this server".getBytes(StandardCharsets.UTF_8));
			out.writeTo(outS);
			return;
		}
		int ind = names.indexOf(name);
		for (byte n = 0; n < 8; n++) {
			ouD.writeInt(pws[(8 * ind) + n]);
		}
		ouD.flush();
		out.write(salt);
		out.write(nBs);
		byte[] exp = dig.digest(out.toByteArray());
		out.reset();
		if (!name.equals("guest")) {
			for (byte n = 0; n < 32; n++) {
				if (exp[n] != resp[n]) {
					out.write(13);
					out.write("inauthentic login".getBytes(StandardCharsets.UTF_8));
					out.writeTo(outS);
					return;
				}
			}
		}
		username = name;
		out.write(7);
		byte[] greeting = ("Welcome back, " + name + "\n").getBytes(StandardCharsets.UTF_8);
		ouD.writeShort(greeting.length);
		ouD.flush();
		out.write(greeting);
		out.writeTo(outS);
		out.reset();
		greeting = (motd + "\n").getBytes(StandardCharsets.UTF_8);
		out.write(7);
		ouD.writeShort(greeting.length);
		ouD.flush();
		out.write(greeting);
		out.writeTo(outS);
		out.reset();
		servlg.append("+{" + name + "} connected from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + "\n");
		byte[] mData;
		String message;
		synchronized (socks) {
			List<Socket> tl = socks;
			tl.add(socket);
			socks = tl;
		}
		synchronized (connected) {
			List<String> tn = connected;
			tn.add(name);
			connected = tn;
		}
		Thread.sleep(200);
		transmit(name + " has entered the chat\n");
		List<String> tsl;
		while (true) {
			ti = inS.read();
			if (ti == 13) {
				return;
			}
			if (ti == -1) {
				servlg.append("client socket was unexpectedly disconnected\n");
				return;
			}
			if (ti != 7) {
				servlg.append("invalid packet\n");
				out.write(13);
				out.write("invalid packet".getBytes(StandardCharsets.UTF_8));
				out.writeTo(outS);
				return;
			}
			mData = new byte[inD.readShort()];
			inS.read(mData);
			message = new String(mData, StandardCharsets.UTF_8);
			transmit("{" + name + "} " + message + "\n");
			if (message.length() > 0 && message.charAt(0) == '!') {
				switch (message) {
					case ("!help"):
						transmit("server: Server commands: !help - View available commands, !connected - View connected client usernames\n");
						break;
					case ("!connected"):
						transmit("Server: Connected client usernames:\n");
						tsl = connected;
						for (String s : tsl) {
							transmit(s + "\n");
						}
						break;
					default:
						transmit("server: Unknown command, use !help to view available commands\n");
				}
			}
		}
	}
}
