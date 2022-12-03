package com.oxhp.conversion.utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ReadStream extends Thread {

	private String name;
	private InputStream is;
	private Thread thread;
	private StringBuilder sb = null;
	private boolean addNl = false;
	private Exception runException = null;

	public Exception getRunException() {
		return runException;
	}

	public void setRunException(Exception runException) {
		this.runException = runException;
	}

	public ReadStream(String name, InputStream is) {
		this.name = name;
		this.is = is;
	}

	public ReadStream(String name, InputStream is, boolean addNl) {
		this.name = name;
		this.is = is;
		this.addNl = addNl;
	}

	public void start() {
		thread = new Thread(this);
		thread.start();
	}

	public void run() {
		try {
			if (sb == null) {
				 sb = new StringBuilder();
			}
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			while (true) {
				String s = br.readLine();
				if (s == null) {
					break;
				}

				sb.append(s);
				if (addNl) {
					sb.append("\n");
				}
				// System.out.println("[" + name + "] " + s);
			}
			is.close();
		} catch (Exception ex) {
			System.out.println("Problem reading stream " + name + "... :" + ex);
			ex.printStackTrace();
			runException = ex;
		}
	}

	public String getString() {
		String retString = null;
		if (sb != null) {
			retString = sb.toString();
			sb.delete(0, sb.length());
			sb = null;
		}
		return retString;
	}
}
