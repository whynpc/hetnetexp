package edu.ucla.cs.wing.hetnetexp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BackgroundService extends Service {
	
	private static BackgroundService _instance;
	
	public static BackgroundService getInstance() {
		return _instance;
		
	}
	
	private MobileInfo mobileInfo;
	
	private boolean mobileDataOn;
	private long mobileBytes;
	
	private boolean udpRunning, tcpRunning, pingpongRunning;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		_instance = this;
		
		
		
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
	 

}
