package edu.ucla.cs.wing.hetnetexp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import edu.ucla.cs.wing.hetnetexp.EventLog.LogType;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;

public class BackgroundService extends Service {
	
	private static BackgroundService _instance;
	
	
	public static BackgroundService getInstance() {
		return _instance;
	}
	
	public static final int MONITOR_INTERVAL = 100;
	
	// comm with server
	public static final int OP_UDP_START = 1;
	public static final int OP_UDP_STOP = 2;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Msg.LOCAL_BYTES:
				sendMsg2Ui(Msg.LOCAL_BYTES, Long.valueOf(mobileBytesInc));
				break;			
			default:
				break;
				
			}
		}		
	};
	
	private MobileInfo mobileInfo;
	
	private boolean mobileDataOn;
	private long mobileBytes, mobileBytesInc;
	
	private boolean udpRunning, pingpongRunning, traceRunning;
	
	private Timer monitorTimer = new Timer();
	
	private Timer commTimer = new Timer();
	
	private Timer switchTimer = new Timer();
	
	private SharedPreferences prefs;
	
	private DatagramSocket udpSocket;	
	
	private EventLog udpLog, traceLog;
	
	private String lastTestLog;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		_instance = this;
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		EventLog.initEnvironment();
		
		udpLog = new EventLog();
		udpLog.addFilter(LogType.UDP);
		udpLog.addFilter(LogType.DEBUG);
		udpLog.addFilter(LogType.TRACE);
		udpLog.addFilter(LogType.HANDOFF);
		udpLog.addFilter(LogType.ACCOUNTING);
		
		traceLog = new EventLog();
						
		MobileInfo.init(this);
		mobileInfo = MobileInfo.getInstance();
		
		
		mobileDataOn = mobileInfo.getMobileDataEnabled();
		mobileBytes = mobileInfo.getMobileRxByte() + mobileInfo.getMobileTxByte();
		
		
		monitorTimer.schedule(new MonitorTask(), 0, MONITOR_INTERVAL);		
		
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	public long getMobileBytesInc() {
		return mobileBytesInc;
	}
	
	public void onMobileDataChange(boolean on) {		
		if (on) {
			if (!mobileDataOn) {
				mobileDataOn = true;
				mobileBytes = mobileInfo.getTotalRxByte() + mobileInfo.getTotalTxByte();				
			}			
		} else {
			if (mobileDataOn) {
				mobileDataOn = false;
				mobileBytesInc = mobileInfo.getTotalRxByte() + mobileInfo.getTotalTxByte() - mobileBytes;
				sendMsg2Ui(Msg.LOCAL_BYTES, Long.valueOf(mobileBytesInc));				
				EventLog.writePublic(LogType.DEBUG, "LocalBytes;" + String.valueOf(mobileBytesInc));				
			}
		}
	}
	
	private class MonitorTask extends TimerTask {
		
		String netTech;
		long wait = 0;
		long logInterval = 0;
		
		public MonitorTask() {
			logInterval = Long.parseLong(prefs.getString("log_interval", "100"));
			
		}

		@Override
		public void run() {
			
			String nt = mobileInfo.getNetworkTech();
			if (netTech != null && !netTech.equals(nt)) {
				EventLog.writePublic(LogType.HANDOFF, "Inter;" + netTech + ";" + nt);
				onInterHandoff(netTech, nt);				
			}
			netTech = nt;
			
			wait += MONITOR_INTERVAL;
			if (wait >= logInterval) {
				wait = 0;
				
			}
			logInterval = Long.parseLong(prefs.getString("log_interval", "100"));
			
			StringBuilder sb = new StringBuilder();
			sb.append(mobileInfo.getNetworkTech());
			sb.append(EventLog.SEPARATOR);			
			sb.append(mobileInfo.getWifiSsid());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getWifiSignal());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getNetworkType());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getCellId());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getSignalStrengthDBM());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getMobileRxByte());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getMobileTxByte());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getTotalRxByte());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getTotalTxByte());
			sb.append(EventLog.SEPARATOR);			
			sb.append(mobileInfo.getLocalIpAddress());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getGeoLat());
			sb.append(EventLog.SEPARATOR);
			sb.append(mobileInfo.getGeoLong());
			
			EventLog.writePublic(LogType.TRACE, sb.toString());
		}		
	}
	
	private void resetUdpSocket() {
		try {
			udpSocket = new DatagramSocket();
			udpSocket.setSoTimeout(100);
		} catch (Exception e) {
			EventLog.writePublic(LogType.DEBUG, e.toString());			
		}
	}
	
	private void sendUdpStart() {
		// msg format: OP (1 byte), Device ID (4 bytes), rate (4 bytes)
		byte[] buf = new byte[1024];
		buf[0] = OP_UDP_START;
		
		byte[] uid = mobileInfo.getDeviceId().getBytes();
		for (int i = 0; i < 4; i ++) {
			buf[1 + i] = uid[uid.length - 4 + i]; 
		}
		
		int rate = Integer.parseInt(prefs.getString("udp_rate", "100"));		
		buf[8] = (byte) ((rate) & 0xFF);
		buf[7] = (byte) ((rate >> 8) & 0xFF);
		buf[6] = (byte) ((rate >> 16) & 0xFF);
		buf[5] = (byte) ((rate >> 24) & 0xFF);			
		
		String serverAddr = prefs.getString("server_addr", "192.155.86.168");
		int serverPort = Integer.parseInt(prefs.getString("server_udp_port", "9999"));
		SocketAddress serverAddress = new InetSocketAddress(serverAddr, serverPort);
		try {
			DatagramPacket pkt = new DatagramPacket(buf, buf.length, serverAddress);
			udpSocket.send(pkt);
			udpLog.writePrivate(LogType.UDP, "Send;Start");
		} catch (Exception e) {
			EventLog.writePublic(LogType.DEBUG, e.toString());			
		}
		
	}
	
	private void sendUdpStop() {
		// msg format: OP (1 byte), Device ID (4 bytes)
		byte[] buf = new byte[1024];
		buf[0] = OP_UDP_STOP;
		
		byte[] uid = mobileInfo.getDeviceId().getBytes();
		for (int i = 0; i < 4; i ++) {
			buf[1 + i] = uid[uid.length - 4 + i]; 
		}
		
		String serverAddr = prefs.getString("server_addr", "192.155.86.168");
		int serverPort = Integer.parseInt(prefs.getString("server_udp_port", "9999"));
		SocketAddress serverAddress = new InetSocketAddress(serverAddr, serverPort);
		try {
			DatagramPacket pkt = new DatagramPacket(buf, buf.length, serverAddress);
			udpSocket.send(pkt);
			udpLog.writePrivate(LogType.UDP, "Send;Stop");
		} catch (Exception e) {
			EventLog.writePublic(LogType.DEBUG, e.toString());			
		}
	}
	
	private void recvUdp() {		
		byte[] buf = new byte[2000];
		DatagramPacket pkt = new DatagramPacket(buf, buf.length);
		
		while (udpRunning) {
			try {
				udpSocket.receive(pkt);
				
				int seq = 0;
				seq += ((buf[0] & 0xFF) << 24);
				seq += ((buf[1] & 0xFF) << 16);
				seq += ((buf[2] & 0xFF) << 8);
				seq += ((buf[3] & 0xFF) << 0);
				udpLog.writePrivate(LogType.UDP, "RECV;" + seq);
			} catch (IOException e) {
				
			}
		}
	}
	
	public void startUdp() {
		if (!udpRunning) {
			sendMsg2Ui(Msg.TASK_STATE, "Start UDP");
			udpRunning = true;
			
			List<String> parameters = new ArrayList<String>();
			parameters.add("udp");
			parameters.add(String.valueOf(System.currentTimeMillis()));			
			parameters.add(prefs.getString("udp_rate", "100"));			
			udpLog.open(EventLog.genLogFileName(parameters));
			udpLog.writePrivate(LogType.UDP, "Start");			
			
			
			commTimer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					resetUdpSocket();					
					sendUdpStart();
					
					// start receiving
					(new Thread() {
						@Override
						public void run() {
							recvUdp();
						}
					}).start();
					
				}
			}, 0);
		}
	}
	
	public void stopUdp() {
		sendMsg2Ui(Msg.TASK_STATE, "Stop UDP");
		udpRunning = false;
		commTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				sendUdpStop();				
			}
		}, 0);
		
		udpLog.writePrivate(LogType.UDP, "Stop");
		lastTestLog = udpLog.getFilename();
		udpLog.close();
	}
	
	public void startPingpong() {		
		if (!pingpongRunning) {
			sendMsg2Ui(Msg.TASK_STATE, "Start Pingpong");
			EventLog.writePublic(LogType.DEBUG, "pingpong start");
			pingpongRunning  = true;			
			long interval = Long.parseLong(prefs.getString("pingpong_interval", "10000"));
			long delay = Long.parseLong(prefs.getString("pingpong_delay", "5000"));
			int rounds = Integer.parseInt(prefs.getString("pingpong_rounds", "5"));
			
			for (int i = 0; i < rounds; i ++, delay += interval) {
				switchTimer.schedule(new TimerTask() {
					
					@Override
					public void run() {
						EventLog.writePublic(LogType.DEBUG, "Pingpong: " + mobileInfo.getNetworkTech());
						if (mobileInfo.getNetworkTech().equals("WIFI")) {
							mobileInfo.setWifiEnabled(false);
						} else if (!mobileInfo.getNetworkTech().equals("NULL")) {
							mobileInfo.setWifiEnabled(true);
						} else {
							
						}
					}
				}, delay);
			}
			switchTimer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					sendMsg2Ui(Msg.TASK_STATE, "Stop Pingpong");
					EventLog.writePublic(LogType.DEBUG, "pingpong stop");
					pingpongRunning = false;
					
				}
			}, delay + 1000);			
		}
	}
	
	public void stopPingpong() {		
		if (pingpongRunning) {
			sendMsg2Ui(Msg.TASK_STATE, "Stop Pingpong");
			EventLog.writePublic(LogType.DEBUG, "pingpong stop");
			pingpongRunning = false;
			switchTimer.cancel();
			switchTimer = new Timer();
		}
	}
	
	private void sendMsg2Ui(int what, Object obj) {
		try {
			Handler client = MainActivity.getHandler();
			if (client != null) {
				Message msg = new Message();
				msg.what = what;
				msg.obj = obj;
				client.sendMessage(msg);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public void onInterHandoff(String oldTech, String newTech) {
		if (udpRunning) {
			resetUdpSocket();
			
			// try to notify the server of the new address
			for (long d = 0; d < 5000; d += 200) {
				commTimer.schedule(new TimerTask() {					
					@Override
					public void run() {
						sendUdpStart();						
					}
				}, d);
			}
		}
	}
	
	public void startTrace() {
		if (!traceRunning) {
			traceRunning = true;	
			sendMsg2Ui(Msg.TASK_STATE, "Start Trace");
			List<String> parameters = new ArrayList<String>();
			parameters.add("trace");
			parameters.add(String.valueOf(System.currentTimeMillis()));
			traceLog.open(EventLog.genLogFileName(parameters));			
		}
	}
	
	public void stopTrace() {
		if (traceRunning) {
			traceRunning = false;
			sendMsg2Ui(Msg.TASK_STATE, "Stop Trace");
			traceLog.close();			
		}
	}
	
	public void addAccountingData(double opBefore, double opAfter) {
		try {
			if (lastTestLog != null) {
				udpLog.open(lastTestLog);
				long op1, op2;
				op1 = (long) (opBefore * 1024);
				op2 = (long) (opAfter * 1024);
				udpLog.writePrivate(LogType.ACCOUNTING, mobileBytesInc + ";" + op1 + ";" + op2);
				udpLog.close();
			}
		} catch (Exception e) {
			EventLog.writePublic(LogType.DEBUG, e.toString());
		}
		
	}

}
