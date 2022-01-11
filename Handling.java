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
	static Object lSync = new Object();
	static volatile List<Socket> socks = new ArrayList<Socket>();
	//For future-proofing, please add no user names over 64 bytes when encoded in UTF-8
	static int[] pws = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0x8cd1d8ce, 0xbdbe6b8c, 0xf858694c, 0xd7ecd800, 0x7e7a956a, 0xb4b82a54, 0xcde27bf9, 0x6c06e2eb};
	static String[] unames = {"guest", "defaultAccount"};
	static List<String> namel = Arrays.asList(unames);
	static ArrayList<String> names = new ArrayList<String>(namel);
	static SecureRandom sRand = new SecureRandom();
	public static final double version = 0.1;//version
	Socket socket;
	String username = "";
	Handling(Socket sock) {
		socket = sock;
	}
	public static void main(String[] arg) {
		System.out.println("Server is starting...");
		try {
			ServerSocket servsock = new ServerSocket(15227);
			accept(servsock);
		} catch (Exception e) {
			System.out.println("Exception in server: " + e);
		}
	}
	static void accept(ServerSocket servsock) throws Exception {
		System.out.println("Server has started...");
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
	        		synchronized (lSync) {
	        			List<Socket> tl = socks;
	        			tl.remove(ple.socket);
	        			socks = tl;
	        		}
	        		if (!ple.username.equals("")) {
	        			transmit(ple.username + " has left the chat");
	        		}
	        	}
	        }).start();
	        Thread.sleep(500);
		}
	}
	static void transmit (String message) {
		List<Socket> tl = socks;
		byte[] mBs = message.getBytes(StandardCharsets.UTF_8);
		byte[] data = new byte[mBs.length + 1];
		System.arraycopy(mBs, 0, data, 1, mBs.length);
		data[0] = 7;
		for (Socket s : tl) {
			try {
				s.getOutputStream().write(data);
			}
			catch (Exception e) {
				System.out.println("Exception in transmitting message: " + e);
			}
		}
		System.out.println("\"" + message);
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
		ouD.writeInt(sRand.nextInt());
		ouD.writeInt(sRand.nextInt());
		ouD.flush();
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
		byte[] salt = dig.digest(out.toByteArray());
		out.reset();
		while (inS.available() < 1) {
			Thread.sleep(200);
		}
		if (inS.read() == 13) {
			return;
		}
		double ver = inD.readDouble();
		if (ver < 0) {
			ouD.writeDouble(version);
			ouD.flush();
			out.writeTo(outS);
			return;
		}
		if (ver > version) {
			out.write(13);
			out.write("requested protocol not available".getBytes(StandardCharsets.UTF_8));
			out.writeTo(outS);
			return;
		}
		out.reset();
		out.write(1);
		out.write(salt);
		out.writeTo(outS);//Salt packet, ha-ha-ha
		out.reset();
		while (inS.available() < 1) {
			Thread.sleep(200);
		}
		int ti = inS.read();
		if (ti == 13) {
			return;
		}
		if (ti != 11) {
			System.out.println("invalid packet");
			out.write(13);
			out.write("invalid packet".getBytes(StandardCharsets.UTF_8));
			out.writeTo(outS);
			return;
		}
		byte[] resp = new byte[32];
		for (byte n = 0; n < 32; n++) {
			ti = inS.read();
			if (ti == -1) {
				throw new Exception();
			}
			if (ti > 127) {
				ti -= 256;
			}
			resp[n] = (byte) ti;
		}
		byte[] nBs = new byte[inS.available()];
		//TODO .available() is an, "estimate", is it exact for all implementations of socket-based 'InputStream's?
		inS.read(nBs);
		String name = new String(nBs, StandardCharsets.UTF_8);
		if (!names.contains(name)) {
			out.write(13);
			out.write("invalid username".getBytes(StandardCharsets.UTF_8));
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
		out.write(("Welcome back, " + name).getBytes(StandardCharsets.UTF_8));
		out.writeTo(outS);
		out.reset();
		System.out.println("+{" + name + "} connected from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
		byte[] mData;
		String message;
		synchronized (lSync) {
			List<Socket> tl = socks;
			tl.add(socket);
			socks = tl;
		}
		Thread.sleep(200);
		transmit(name + " has entered the chat");
		while (true) {
			while (inS.available() < 1) {
				Thread.sleep(200);
			}
			ti = inS.read();
			if (ti == 13) {
				return;
			}
			if (ti != 7) {
				System.out.println("invalid packet");
				out.write(13);
				out.write("invalid packet".getBytes(StandardCharsets.UTF_8));
				out.writeTo(outS);
				return;
			}
			mData = new byte[inS.available()];
			inS.read(mData);
			message = new String(mData, StandardCharsets.UTF_8);
			transmit("{" + name + "} " + message);
		}
	}
}
