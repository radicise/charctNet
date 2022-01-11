package messageServer;
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
public class Handling implements Runnable {
	static int[] pws = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0x8cd1d8ce, 0xbdbe6b8c, 0xf858694c, 0xd7ecd800, 0x7e7a956a, 0xb4b82a54, 0xcde27bf9, 0x6c06e2eb};
	static String[] unames = {"guest", "radicise"};
	static List<String> namel = Arrays.asList(unames);
	static ArrayList<String> names = new ArrayList<String>(namel);
	static SecureRandom sRand = new SecureRandom();
	public static final double version = 0.1;
	Socket socket;
	Handling(Socket sock) {
		socket = sock;
	}
	public static void main(String[] arg) {
		System.exit(0);
		try {
			ServerSocket servsock = new ServerSocket(15227);
			accept(servsock);
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
	}
	public static void accept(ServerSocket servsock) throws Exception {
		while (true) {
			Handling pl = new Handling(servsock.accept());
	        new Thread(new Runnable() {
	        	Handling ple = pl;
	        	public void run() {
	        		try {
	        			ple.serve();
	        			ple.socket.close();
	        		}
	        		catch (Exception e) {
	        			System.out.println(e);
	        		}
	        	}
	        }).start();
	        Thread.sleep(500);
		}
	}
	public void run() {}
	public void serve() throws Exception {
		OutputStream outS = socket.getOutputStream();
		DataOutputStream outD = new DataOutputStream(outS);
		InputStream inS = socket.getInputStream();
		DataInputStream inD = new DataInputStream(inS);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream ouD = new DataOutputStream(out);
		out.write(ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array());
		//TODO Possibly fix previous line, is backing array equal length equal to allocation value in all implementations?
		outD.writeInt(sRand.nextInt());
		outD.writeInt(sRand.nextInt());
		MessageDigest dig = MessageDigest.getInstance("SHA-256");
		byte[] salt = dig.digest(out.toByteArray());
		out.reset();
		out.write(1);
		out.write(salt);
		out.writeTo(outS);//Salt packet, ha-ha-ha
		out.reset();
		while (inS.available() < 1) {
			Thread.sleep(200);
		}
		if (inS.read() == 13) {
			return;
		}
		double ver = inD.readDouble();
		if (ver < 0) {
			outD.writeDouble(version);
			return;
		}
		if (ver > version) {
			out.write(13);
			out.write("requested protocol not available".getBytes(StandardCharsets.UTF_8));
			out.writeTo(outS);
			return;
		}
		while (inS.available() < 1) {
			Thread.sleep(200);
		}
		if (inS.read() == 13) {
			return;
		}
		byte[] resp = new byte[32];
		int ti;
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
		String name = StandardCharsets.UTF_8.decode(ByteBuffer.allocate(nBs.length).put(nBs)).toString();
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
		out.write(nBs);
		out.write(salt);
		byte[] exp = dig.digest(out.toByteArray());
		out.reset();
		for (byte n = 0; n < 32; n++) {
			if (exp[n] != resp[n]) {
				out.write(13);
				out.write("inauthentic login".getBytes(StandardCharsets.UTF_8));
				out.writeTo(outS);
				return;
			}
		}
		System.out.println("[" + name + "] connected from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
	}
}
