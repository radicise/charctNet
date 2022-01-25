import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.LinkedList;
import java.util.List;
class UserData{
	byte[] login;
	String username;
	//TODO String nick;
	UserData(String username, byte[] login) {
		this.username = username;
		this.login = login;
	}
	static UserData[] getUsers() throws Exception {
		FileInputStream conf = null;
		try {
			conf = new FileInputStream("cN-serverUsers");
		}
		catch (Exception e) {
			(new File("cN-serverUsers")).createNewFile();
			FileOutputStream fOS = new FileOutputStream(new File("cN-serverUsers"), false);
			fOS.write(new byte[]{0, 0});
			fOS.close();
			return new UserData[0];
		}
		DataInputStream conD = new DataInputStream(conf);
		UserData[] output = new UserData[conD.readShort()];
		String ts;
		byte[] tBA;
		for (int n = 0; n < output.length; n++) {
			tBA = new byte[conD.readShort()];
			conf.read(tBA);
			ts = new String(tBA, "UTF-8");
			byte[] sBA = new byte[32];
			conf.read(sBA);
			output[n] = new UserData(ts, sBA);
		}
		conD.close();
		
		return output;
	}
	static void writeUsers(UserData[] users) throws Exception {
		FileOutputStream fOS = null;
		try {
			fOS = new FileOutputStream(new File("cN-serverUsers"), false);
		}
		catch (Exception e) {
			(new File("cN-serverUsers")).createNewFile();
			fOS = new FileOutputStream(new File("cN-serverUsers"), false);
		}
		DataOutputStream fOD = new DataOutputStream(fOS);
		fOD.writeShort(users.length);
		for (UserData u : users) {
			fOD.writeShort(u.username.getBytes("UTF-8").length);
			fOD.write(u.username.getBytes("UTF-8"));
			fOD.write(Arrays.copyOf(u.login, 32));
		}
		fOD.close();
	}
}
class Handling implements Runnable {
	public static log chatlg = new log("cN-chatLog.txt", (short) 30, "chat", 65536, true, StandardCharsets.UTF_8);
	public static log servlg = new log("cN-serverLog.txt", (short) 30, "server", 65536, true, StandardCharsets.UTF_8);
	static volatile List<String> connected = new ArrayList<String>();
	static volatile List<Socket> socks = new ArrayList<Socket>();
	public static volatile String motd = "Development server, you may experience bugs";
	//For future-proofing, please add no user names over 64 bytes when encoded in UTF-8
	static byte[] psk;
	static String[] unames = null;
	static SecureRandom sRand = new SecureRandom();
	public static final double version = 0.4;//version
	public static int port = 15227;
	public static volatile byte[] colB = new byte[]{20, 2, -127, -127, -127};
	public static volatile byte[] colF = new byte[]{20, 3, -127, -127, -127};
	Socket socket;
	String username = "";
	Handling(Socket sock) {
		socket = sock;
	}
	public static void main(String[] arg) throws Exception {
		if (arg.length > 0 && arg[0].toLowerCase().equals("adduser")) {
			UserData[] uDat = UserData.getUsers();
			for (UserData c : uDat) {
				if (c.username.equals(arg[1])) {
					System.out.println("An account with that username already exists on this server!");
					System.exit(1);
				}
			}
			uDat = Arrays.copyOf(uDat, (uDat.length + 1));
			MessageDigest dig = MessageDigest.getInstance("SHA-256");
			byte[] secBA = dig.digest((arg[1] + "/" + arg[2]).getBytes("UTF-8"));
			uDat[uDat.length - 1] = new UserData(arg[1], secBA);
			UserData.writeUsers(uDat);
			System.out.println("Added user successfully! Server restart is required to admit user added");
			System.exit(0);
		}
		if (arg.length > 0 && arg[0].toLowerCase().equals("removeaccount")) {
			UserData[] uDat = UserData.getUsers();
			int i = -1;
			for (int j = 0; j < uDat.length; j++) {
				if (uDat[j].username.equals(arg[1])) {
					i = j;
					break;
				}
			}
			if (i == -1) {
				System.out.println("There was no account with that username on this server!");
				System.exit(2);
			}
			LinkedList<UserData> UDs = new LinkedList(Arrays.asList(uDat));
			UDs.remove(i);
			UserData.writeUsers(UDs.toArray(new UserData[0]));
			System.out.println("Successfully removed the specified user account!");
			System.exit(0);
		}
		if (arg.length > 0 && arg[0].toLowerCase().equals("showaccounts")) {
			UserData[] uDat = UserData.getUsers();
			System.out.println("Number of accounts: " + uDat.length);
			System.out.println("Account listing:");
			int i = 0;
			for (UserData c : uDat) {
				System.out.println(i + " \u0009username: " + c.username);
				i++;
			}
			System.exit(0);
		}
		System.out.println("Server is starting...");
		UserData[] uDat = UserData.getUsers();
		psk = new byte[uDat.length * 32];
		unames = new String[uDat.length];
		for (int n = 0; n < uDat.length; n++) {
			unames[n] = uDat[n].username;
			uDat[n].login = Arrays.copyOf(uDat[n].login, 32);
			for (int i = 0; i < 32; i++) {
				psk[(n * 32) + i] = uDat[n].login[i];
			}
		}
		servlg.startExecutor();
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
	static void send (byte[] data) {
		List<Socket> tl = socks;
		for (Socket s : tl) {
			try {
				s.getOutputStream().write(data);
			}
			catch (Exception e) {
				servlg.append("Exception in transmitting data: " + e + "\n");
				synchronized (socks) {
        			List<Socket> sl = socks;
        			sl.remove(s);
        			socks = sl;
        		}
			}
		}
	}
	static void changeColor(int uR, int uG, int uB, boolean bg, boolean noColor) {
		send(new byte[]{20, (byte) ((bg ? 1 : 0) + (noColor ? 2 : 0)),(byte) uR, (byte) uG, (byte) uB});
		if (bg) {
			colB = new byte[]{20, (byte) ((bg ? 1 : 0) + (noColor ? 2 : 0)),(byte) uR, (byte) uG, (byte) uB};
		}
		else {
			colF = new byte[]{20, (byte) ((bg ? 1 : 0) + (noColor ? 2 : 0)),(byte) uR, (byte) uG, (byte) uB};
		}
	}
	static void transmit (String message) {
		ByteArrayOutputStream tBarOS = new ByteArrayOutputStream();
		DataOutputStream dOS = new DataOutputStream(tBarOS);
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
		send(tBarOS.toByteArray());
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
		MessageDigest dig = MessageDigest.getInstance("SHA-256");//Just in case someone hijacks SecureRandom and doesn't bother to wait for the next leap second
		byte[] salt = dig.digest(out.toByteArray());
		out.reset();
		while (inS.available() < 1) {//TODO bad use of .available()?
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
		if (inS.available() < 1) {//TODO bad use of .available()?
			return;
		}
		byte[] nBs = new byte[inD.readShort()];
		inS.read(nBs);
		String name = new String(nBs, StandardCharsets.UTF_8);
		if (!Arrays.asList(unames).contains(name) && !name.equals("guest") && !(name.length() > 5 && name.substring(0, 6).equals("guest-"))) {
			out.write(13);
			out.write("username does not exist on this server".getBytes(StandardCharsets.UTF_8));
			out.writeTo(outS);
			return;
		}
		int ind = Arrays.asList(unames).indexOf(name);
		if (!name.equals("guest") && !((name.length() > 5 && name.substring(0, 6).equals("guest-")))) {
			out.write(psk, (ind * 32), 32);
		}
		out.write(salt);
		out.write(nBs);
		byte[] exp = dig.digest(out.toByteArray());
		out.reset();
		if (!name.equals("guest") && !((name.length() > 5 && name.substring(0, 6).equals("guest-")))) {
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
		outS.write(colB);
		outS.write(colF);
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
		int io;
		int it;
		int ih;
		String us;
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
						transmit("server: Server commands: !help - View available commands, !connected - View connected client usernames, !theme fg|bg <r> <g> <b> - Change the server theme, use /help to view client-side commands\n");
						break;
					case ("!connected"):
						transmit("Server: Connected client usernames:\n");
						tsl = connected;
						for (String s : tsl) {
							transmit(s + "\n");
						}
						break;
					default:
						if (message.split(" ")[0].equals("!theme")) {
							if (message.toLowerCase().equals("!theme clear")) {
								changeColor(0, 0, 0, false, true);
								changeColor(0, 0, 0, true, true);
								transmit("server: cleared the theme\n");
								break;
							}
							if (message.split(" ").length < 3) {
								transmit("server: invalid argument count\n");
								break;
							}
							try {
								if ((!(message.split(" ")[1].equals("fg"))) && (!(message.split(" ")[1].equals("bg")))) {
									throw new Exception();
								}
								io = Integer.valueOf(message.split(" ")[2]);
								it = Integer.valueOf(message.split(" ")[3]);
								ih = Integer.valueOf(message.split(" ")[4]);;
								changeColor(io, it, ih, message.split(" ")[1].equals("bg"), false);
								if (message.split(" ")[1].equals("bg")) {
									us = "background";
								}
								else {
									us = "foreground";
								}
								transmit("server: changed the " + us + " colour to rgb " + (io % 256) + " " + (it % 256) + " " + (ih % 256) + "\n");
							}
							catch (Exception e) {
								transmit("server: could not parse arguments\n");
							}
							break;
						}
						transmit("server: Unknown command, use !help to view available commands\n");
				}
			}
		}
	}
}
