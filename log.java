import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class log implements Runnable {
	static SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd'_'HH:mm:ss");
	CharBuffer chab;
	String fn;
	short fq;
	String name;
	File logf;
	FileOutputStream outs;
	OutputStreamWriter dats;
	Charset asccst;
	CharsetEncoder ascenc;
	BufferedWriter bufw;
	char[] tca;
	int oldpos;
	int bufsize;
	boolean annc;
	Object used = new Object();
	public log(String filename, short waittime, String logname, int buffersize, boolean announce, Charset charset) {
		fn = filename;
		if (waittime < 20) {
			fq = 20;
		}
		else {
			fq = waittime;
		}
		fq = 5;
		name = logname;
		tca = new char[0];
		oldpos = 0;
		bufsize = buffersize;
		annc = announce;
		asccst = charset;
	}
	public synchronized void flush() {
		synchronized (used) {
			try {
				String toap;
				toap = clear();
				if (!toap.equals("")) {
					bufw.write(toap);
					bufw.flush();
					dats.flush();
					outs.flush();
				}
			}
			catch (Exception exc) {
				System.out.println("log error: " + exc);
			}
		}
	}
	public Runnable logrun = new Runnable() {
		public void run(){
			synchronized (used) {
				try {
					String toap;
					toap = clear();
					if (!toap.equals("")) {
						bufw.write(toap);
						bufw.flush();
						dats.flush();
						outs.flush();
					}
				}
				catch (Exception exc) {
					System.out.println("log error: " + exc);
				}
			}
		}
	};
	public void startExecutor() throws IOException {
		chab = CharBuffer.allocate(bufsize);
		logf = new File(fn);
	    if (!logf.exists()) {
	    	logf.createNewFile();
	    }
		outs = new FileOutputStream(fn, true);
		ascenc = asccst.newEncoder();
		dats = new OutputStreamWriter(outs, ascenc);
		bufw = new BufferedWriter(dats);
		ScheduledExecutorService logexec = Executors.newScheduledThreadPool(1);
		logexec.scheduleAtFixedRate(logrun, fq, fq, TimeUnit.SECONDS);
	}
	public synchronized String clear() {
		if (chab.position() == 0) {
			return "";
		}
		tca = new char[chab.position()];
		chab.clear();
		chab.get(tca);
		String data = new String(tca);
		chab.clear();
		return data;
	}
	public synchronized void append(String data) {
		long tiL = System.currentTimeMillis();
		if (tiL < 0) {
			System.out.println("dates before 1970 are not supported");
			System.exit(0);
		}
		String time = String.valueOf(tiL);
		String ext = "00000000000000000000".substring(time.length());
		chab.put(ext + time + data + "\r\n");
		if (annc) {
			new Thread(new Runnable() {
	        	public void run() {
	        		System.out.println(name + ": " + data);
	        	}
	        }).start();
		}
	}
	public synchronized void append(String data, boolean show) {
		chab.put(format.format(new Date()) + " " + data + "\r\n");
		if (show) {
			new Thread(new Runnable() {
	        	public void run() {
	        		System.out.println(name + ": " + data);
	        	}
	        }).start();
		}
	}
	public void run() {
	}
}