package edu.ucla.cs.wing.hetnetexp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.os.Environment;
import android.util.Log;

public class EventLog {

	public static final String TAG = "hetnet";
	public static final String SEPARATOR = ";";

	public static enum LogType {
		DEBUG, UDP, PINGPONG, BYTES, TRACE, HANDOFF
	};
	
	private static Set<EventLog> loggers = new HashSet<EventLog>();
	
	private Set<LogType> filters;

	private PrintWriter writer;
	
	public void close() {
		if (writer != null) {
			writer.flush();
			writer.close();
			writer = null;
		}
		loggers.remove(this);
	}
	
	public EventLog() {
		
	}
	
	public void addFilter(LogType t) {
		if (filters == null) {
			filters = new HashSet<EventLog.LogType>();			
		}
		filters.add(t);		
	}
	
	public boolean filterType(LogType t) {
		return (filters == null || filters.contains(t));
	}
	
	public PrintWriter getWriter() {
		return writer;
	}
	
	public static void initEnvironment() {
		File dir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "hetnet");
		if (!dir.exists()) {
			dir.mkdirs();
		}		
	}	

	public boolean open(String fileName) {
		boolean ret = true;
		try {
			File dir = new File(Environment.getExternalStorageDirectory()
					+ File.separator + "dnsexp" + File.separator + "log");
			if (!dir.exists()) {
				dir.mkdirs();
			}			
			writer = new PrintWriter(new FileOutputStream(new File(
					dir.getAbsolutePath(), fileName)), true);
		} catch (FileNotFoundException e) {
			writer = null;
			ret = false;
		}
		loggers.add(this);
		return ret;
	}

	public static String genLogFileName(List<String> parameters) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String parameter : parameters) {
			if (first) {
				first = false;
			} else {
				sb.append("_");
			}
			String p = parameter.replace('&', '-');
			sb.append(p);
		}
		sb.append(".txt");
		
		return sb.toString();
	}	
	
	public static void write2(PrintWriter writer, String formatedData) {
		if (writer != null) {
			synchronized (writer) {
				writer.println(formatedData);
				//writer.flush();
			}			
		} else {
			
			Log.d(TAG, formatedData);			
		}
	}
	
	public static String data2FormatedData(LogType type, String data) {
		StringBuilder sb = new StringBuilder();
		sb.append(System.currentTimeMillis());
		sb.append(SEPARATOR);
		sb.append(type);
		sb.append(SEPARATOR);
		if (data != null) {
			sb.append(data);
			sb.append(SEPARATOR);
		}
		return sb.toString();
	}	
	
	public static void writePublic(LogType type, String data) {	
		String formatedData = data2FormatedData(type, data);
		if (type != LogType.TRACE)
			Log.d(TAG, formatedData);		
		for (EventLog logger : loggers) {
			if (logger.filterType(type))
				write2(logger.writer, formatedData);			
		}
	}	
		
	public void writePrivate(LogType type, String data) {
		String formatedData = data2FormatedData(type, data);
		if (type != LogType.TRACE)
			Log.d(TAG, formatedData);
		if (filterType(type))
			write2(writer, formatedData);			
	}	

}
