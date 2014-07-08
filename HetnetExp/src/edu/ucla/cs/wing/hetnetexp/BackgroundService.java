package edu.ucla.cs.wing.hetnetexp;

import java.math.RoundingMode;
import java.util.Timer;
import java.util.TimerTask;

import edu.ucla.cs.wing.hetnetexp.EventLog.LogType;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class BackgroundService extends Service {
	
	private static BackgroundService _instance;
	
	public static BackgroundService getInstance() {
		return _instance;
		
	}
	
	private MobileInfo mobileInfo;
	
	private boolean mobileDataOn;
	private long mobileBytes;
	
	private boolean udpRunning, tcpRunning, pingpongRunning;
	
	private Timer monitorTimer = new Timer();
	
	private Timer commTimer = new Timer();
	
	private Timer switchTimer = new Timer();
	
	private SharedPreferences prefs;
	

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
		MobileInfo.init(this);
		mobileInfo = MobileInfo.getInstance();
		
		
		
		// mobileDataOn
		
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
				long deltaMobileBytes = mobileInfo.getTotalRxByte() + mobileInfo.getTotalTxByte() - mobileBytes;
				// TODO: report deltamobilebytes				
			}
			
		}
	}
	
	public void onMobileConnectivityChange(boolean connected) {
		
	}
	
	public void startUdp() {
		if (!udpRunning) {
			udpRunning = true;
			
		}
		
	}
	
	public void stopUdp() {
		if (udpRunning) {
			udpRunning = false;
			
			
		}
		
	}
	
	
	public void startPingpong() {
		if (!pingpongRunning) {
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
					pingpongRunning = false;
					
				}
			}, delay + 1000);
			
		}
	}
	
	public void stopPingpong() {
		if (pingpongRunning) {
			pingpongRunning = false;
			switchTimer.cancel();
			switchTimer = new Timer();
		}
		
	}
	
	 

}
