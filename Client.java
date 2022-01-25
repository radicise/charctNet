import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
class Config {
	String ipCPort;
	String uname;
	byte[] pwdHash;
	String serverName;
	static String otherArgs;
	static boolean useTerminalEscapes = true;
	static String termColour = "24b";
	static String inputEncoding = Charset.defaultCharset().name();
	Config(String sn, String ip, String name, byte[] pw) {
		serverName = sn;
		ipCPort = ip;
		uname = name;
		pwdHash = pw;
	}
	static void launcher(String[] args) throws Exception {
		String[] carg = new String[args.length + 3];
		String ts;
		for (int n = 0; n < args.length; n++) {
			carg[n + 3] = args[n];
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
				case ("colour='24b'"):
					termColour = "24b";
					break;
				case ("colour='3b'"):
					termColour = "3b";
					break;
				case ("colour='nocolour'"):
					termColour = "noColour";
					break;
				default:
					System.out.println("invalid operation modifier, launching program anyways");
			}
		}
		if (useTerminalEscapes) {
			System.out.println("If you see visible codes on your terminal or terminal emulator, relaunch with \"useTerminalEscapes=false\" added to program launch arguments (!!!USING THAT ARGUMENT CAUSES VISUAL BUGS, IT IS RECOMMENDED THAT IT ONLY BE USED WHEN NEEDED!!!)");//TODO Edit message
		}
		Config[] confs = null;
		try {
			confs = fromServ();
		}
		catch (Exception e) {
			System.out.println("Exception loading config file: " + e);
			System.exit(4);
		}
		int ti = 0;
		System.out.println("Configuration listing:");
		for (Config c : confs) {
			System.out.println(ti + " \u0009"+ c.serverName + " \u0009" + c.ipCPort + " \u0009" + c.uname);
			ti++;
		}
		BufferedReader inp = new BufferedReader(new InputStreamReader(System.in, inputEncoding));
		ti = -1;
		while (ti < 0 || ti >= confs.length) {
			System.out.print("Selection: ");
			ts = inp.readLine();
			try {
				ti = Integer.valueOf(ts);
			}
			catch (Exception e) {
				ti = -1;
			}
			if (ti < 1 || ti >= confs.length) {
				if (useTerminalEscapes) {
					System.out.print("\u001b[1T\u001b[G\u001b[0J");
				}
			}
		}
		carg[0] = confs[ti].uname;
		carg[1] = "unused String";
		carg[2] = confs[ti].ipCPort;
		Client.launchpoint(carg, confs[ti].pwdHash);
	}
	static void toServ(Config[] configs) throws Exception {
		FileOutputStream fOS;
		try {
			fOS = new FileOutputStream(new File("cN-servers"), false);
		}
		catch (Exception e) {
			(new File("cN-servers")).createNewFile();
			fOS = new FileOutputStream(new File("cN-servers"), false);
		}
		DataOutputStream fOD = new DataOutputStream(fOS);
		fOD.writeInt((int) (Client.version * 100));
		fOD.writeInt(configs.length);
		for (Config confi : configs) {
			fOD.writeShort(confi.uname.getBytes("UTF-8").length);
			fOD.write(confi.uname.getBytes("UTF-8"));
			fOD.writeShort(confi.serverName.getBytes("UTF-8").length);
			fOD.write(confi.serverName.getBytes("UTF-8"));
			fOD.writeShort(confi.ipCPort.getBytes("UTF-8").length);
			fOD.write(confi.ipCPort.getBytes("UTF-8"));
			fOD.flush();
			fOS.write(confi.pwdHash);
		}
		fOD.close();
	}
	static Config[] fromServ() throws Exception {
		FileInputStream conf;
		try {
			conf = new FileInputStream("cN-servers");
		}
		catch (Exception e) {
			(new File("cN-servers")).createNewFile();
			FileOutputStream fOS = new FileOutputStream(new File("cN-servers"), false);
			fOS.write(new byte[]{(byte) ((((int) (Client.version * 100)) & 0xff000000) >> 24), (byte) ((((int) (Client.version * 100)) & 0xff0000) >> 16), (byte) ((((int) (Client.version * 100)) & 0xff00) >> 8), (byte) (((int) (Client.version * 100)) & 0xff), 0, 0, 0, 0});
			fOS.close();
			return new Config[0];
		}
		DataInputStream conD = new DataInputStream(conf);
		int ver = conD.readInt();
		conD.close();
		switch (ver) {
			case (40):
				return fromServ_4();
		}
		throw new Exception("Unsupported 'cN-servers' config file version");
	}
	static Config[] fromServ_4() throws Exception {//vCh
		FileInputStream conf;
		try {
			conf = new FileInputStream("cN-servers");
		}
		catch (Exception e) {
			(new File("cN-servers")).createNewFile();
			FileOutputStream fOS = new FileOutputStream(new File("cN-servers"), false);
			fOS.write(new byte[]{0, 0, 0, 40, 0, 0, 0, 0});//vCh
			fOS.close();
			return new Config[0];
		}
		DataInputStream conD = new DataInputStream(conf);
		if (conD.readInt() != 40) {//vCh
			conD.close();
			throw new Exception();
		}
		int amnt = conD.readInt();
		Config[] output = new Config[amnt];
		byte[] uname;
		byte[] sN;
		byte[] ip;
		byte[] pHash;
		for (int n = 0; n < amnt; n++) {
			uname = new byte[conD.readShort()];
			conf.read(uname);
			sN = new byte[conD.readShort()];
			conf.read(sN);
			ip = new byte[conD.readShort()];
			conf.read(ip);
			pHash = new byte[32];
			conf.read(pHash);
			output[n] = new Config(new String(sN, "UTF-8"), new String(ip, "UTF-8"), new String(uname, "UTF-8"), pHash);
		}
		conD.close();
		return output;
	}
}
class Client {
	public static final double version = 0.4;//Version
	public static log conversation = new log("cN-chatLogged.txt", (short) 30, "conver", 65536, false, StandardCharsets.UTF_8);
	static String fg = "[0m";
	static String bg = "[0m";
	static volatile String theme = "\u001b[0m\u001b[0m";
	static volatile String termColour = "24b";
	static volatile boolean useTerminalEscapes;
	public static String termColor(int r, int g, int b, boolean bg) {
		byte a = (byte) (bg ? 10 : 0);
		if (Math.min(Math.min(r, g), b) < 0 || Math.max(Math.max(r, g), b) > 255) {
			return "[0m";
		}
		if (termColour.equals("3b")) {
			return "[" + (a + 30 + (r >> 7) + ((g & 0x80) >> 6) + ((b & 0x80) >> 5)) + "m";
		}
		if (termColour.equals("24b")) {
			return "["+ (38 + a) + ";2;" + r + ";" + g + ";" + b + "m";
		}
		if (termColour.equals("noColour")) {
			return "[0m";
		}
		return "[0m";
	}
	public static void main(String[] args) throws Exception {
		if (args.length > 0 && args[0].toLowerCase().equals("removeconfig")) {
			Config[] uDat = Config.fromServ();
			int i = -1;
			for (int j = 0; j < uDat.length; j++) {
				if (uDat[j].serverName.equals(args[1])) {
					i = j;
					break;
				}
			}
			if (i == -1) {
				System.out.println("There was no config with the specified name!");
				System.exit(2);
			}
			LinkedList<Config> UDs = new LinkedList(Arrays.asList(uDat)); 
			UDs.remove(i);
			Config.toServ(UDs.toArray(new Config[0]));
			System.out.println("Successfully removed the specified configuration!");
			System.exit(0);
		}
		if (args.length > 0 && args[0].toLowerCase().equals("showconfigs")) {
			Config[] uDat = Config.fromServ();
			System.out.println("Number of configurations: " + uDat.length);
			System.out.println("Configuration listing:");
			int i = 0;
			for (Config c : uDat) {
				System.out.println(i + "   \u0009configName: " + c.serverName + "   username: " + c.uname + "   IPv4+Port: " + c.ipCPort);
				i++;
			}
			System.exit(0);
		}
		if (args.length > 0 && args[0].toLowerCase().equals("addconfig")) {
			Config[] uDat = Config.fromServ();
			for (Config c : uDat) {
				if (c.serverName.equals(args[1])) {
					System.out.println("A configuration with that servername already exists!");
					System.exit(1);
				}
			}
			Config[] confs = Config.fromServ();
			Config[] nC = Arrays.copyOf(confs, confs.length + 1);
			ByteArrayOutputStream tbArOS = new ByteArrayOutputStream();
			tbArOS.write((args[3] + "/" + args[4]).getBytes("UTF-8"));
			byte[] pwH = MessageDigest.getInstance("SHA-256").digest(tbArOS.toByteArray());
			nC[confs.length] = new Config(args[1], args[2], args[3], pwH);
			Config.toServ(nC);
			System.out.println("Added new connection successfully!");
		}
		else if (args.length > 0 && args[0].toLowerCase().equals("launchoptions")) {
			FileOutputStream fOS;
			try {
				fOS = new FileOutputStream(new File("cN-modifiers"), false);
			}
			catch (Exception e) {
				(new File("cN-servers")).createNewFile();
				fOS = new FileOutputStream(new File("cN-modifiers"), false);
			}
			DataOutputStream fOD = new DataOutputStream(fOS);
			fOD.writeShort(args.length - 1);
			for (int c = 1; c < args.length; c++) {
				fOD.writeShort(args[c].getBytes("UTF-8").length);
				fOD.write(args[c].getBytes("UTF-8"));
			}
			fOD.flush();
			fOD.close();
			System.out.println("Updated launch options successfully!");
		}
		else if (args.length > 0 && args[0].toLowerCase().equals("launch")) {
			FileInputStream conf = null;
			try {
				conf = new FileInputStream("cN-modifiers");
			}
			catch (Exception e) {
				System.out.println("No launch option file present. To create a launch option file, run with this argument syntax: \"launchOptions\" [<options>]");
				System.exit(5);
			}
			DataInputStream conD = new DataInputStream(conf);
			String[] options = new String[conD.readShort()];
			byte[] oB;
			for (int n = 0; n < options.length; n++) {
				oB = new byte[conD.readShort()];
				conf.read(oB);
				options[n] = new String(oB, "UTF-8");
			}
			Config.launcher(options);
		}
		else if (args.length > 0 && args[0].toLowerCase().equals("help")) {
			System.out.println("Syntax:\n\"launch\"\n\"addConfig\" <configName> <serverIP> <username> <password>\n\"removeConfig\" <configName>\n\"showConfigs\"\n\"launchOptions\" [<options>]\n\"help\"");
		}
		else {
			System.out.println("Invalid arguments! Valid syntax is:\n\"launch\"\n\"addConfig\" <configName> <serverIP> <username> <password>\n\"removeConfig\" <configName>\n\"showConfigs\"\n\"launchOptions\" [<options>]\n\"help\"");
		}
	}
	static void launchpoint(String[] arg, byte[] pwH) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
	        public void run() {
	        	if (useTerminalEscapes) {
					System.out.print("\u001b[0m");
				}
	        }
	    });
		try {
			System.out.println("Launching client...");
			mai(arg, pwH);
		}
		catch (Exception e) {
			if (useTerminalEscapes) {
				System.out.print("\u001b[0m");
			}
			System.out.println("Exception occurred: " + e);
			System.exit(0);
		}
		System.exit(0);
	}
	public static void mai(String[] args, byte[] pwH) throws Exception {
		String inputEncoding = Charset.defaultCharset().name();
		useTerminalEscapes = true;
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
					case ("colour='24b'"):
						termColour = "24b";
						break;
					case ("colour='3b'"):
						termColour = "3b";
						break;
					case ("colour='nocolour'"):
						termColour = "noColour";
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
		String uname = args[0];
		System.out.println("Attempting connection...");
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
		out.write(pwH);
		for (byte n = 0; n < 8; n++) {
			ouD.writeInt(inD.readInt());
		}
		ouD.flush();
		out.write(uname.getBytes(StandardCharsets.UTF_8));
		byte[] resp = dig.digest(out.toByteArray());
		out.reset();
		out.write(11);
		out.write(resp);
		ouD.writeShort(uname.getBytes(StandardCharsets.UTF_8).length);
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
        			int r;
        			int g;
        			int b;
        			int c;
        			while (true) {
						si = inS.read();
						if (si == -1) {
							System.out.println("disconnected");
							System.exit(2);
						}
						if (si == 13) {
							message = new byte[inS.available()];
							inS.read(message);
							System.out.println("disconnected by server for reason: " + new String(message, StandardCharsets.UTF_8));
							System.exit(0);
						}
						if (si != 7 && si != 20) {
							System.out.println("disconnected by client for invalid packet id: " + si);
							System.exit(0);
						}
						if (si == 20 && useTeEsc) {
							c = (inS.read() & 0xff);
							r = (inS.read() & 0xff);
							g = (inS.read() & 0xff);
							b = (inS.read() & 0xff);
							if ((c & 2) == 2) {
								if ((c & 1) == 0) {
									fg = "[0m";
								}
								else {
									bg = "[0m";
								}
							}
							else {
								if ((c & 1) == 0) {
									fg = termColor(r, g, b, false);
								}
								else {
									bg = termColor(r, g, b, true);
								}
							}
							if (bg.equals("[0m")) {
								theme = "\u001b" + bg + "\u001b" + fg;
							}
							else {
								theme = "\u001b" + fg + "\u001b" + bg;
							}
						}
						if (si == 20 && !useTeEsc) {
							inS.skip(4);
						}
						if (si == 7) {
							message = new byte[inD.readShort()];
							inS.read(message);
							tex = new String(message, StandardCharsets.UTF_8);
							if (useTeEsc) {
								System.out.print(theme + "\u001b7\u001b[1S\u001b[1A\u001b[1G\u001b[1L" + tex + "\u001b8\u001b[1B");
							}
							else {
								System.out.print(tex);
							}
							conversation.append("\"" + tex);
						}
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
				System.out.print(theme + "\u001b[1T\u001b[2K\u001b[1G");
			}
			if (input.length() < 1) {
				continue;
			}
			if (input.toLowerCase().equals("/help")) {
				System.out.println("Use \"/exit\" to exit the program\nUse \"/showConfig\" to display the current program configuration, use !help to show serverside commands");
			}
			else if (input.toLowerCase().equals("/exit")) {
				outS.write(13);
				if (useTerminalEscapes) {
					System.out.print("\u001b[0m");
				}
				System.out.println("Exiting...");
				conversation.flush();
				System.exit(0);
			}
			else if (input.toLowerCase().equals("/showconfig")) {
				System.out.println("useTerminalEscapes=" + useTerminalEscapes + "\ninputEncoding='" + inputEncoding + "'\ncolour='" + termColour + "'");
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
