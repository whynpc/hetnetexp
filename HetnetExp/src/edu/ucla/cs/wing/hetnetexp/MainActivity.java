package edu.ucla.cs.wing.hetnetexp;


import java.util.LinkedList;
import java.util.Queue;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	private Queue<String> msgQueue = new LinkedList<String>();
	
	private EditText msgbox;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		msgbox = (EditText) findViewById(R.id.msgbox);
		
		startService(new Intent(this, BackgroundService.class));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onClickMobileDataOn(View view) {
		MobileInfo.getInstance().setMobileDataEnabled(true);
	}
	
	public void onClickMobileDataOff(View view) {
		MobileInfo.getInstance().setMobileDataEnabled(false);
		
	}
	
	public void onClickWifiOn(View view) {
		MobileInfo.getInstance().setWifiEnabled(true);		
	}
	
	public void onClickWifiOff(View view) {
		MobileInfo.getInstance().setWifiEnabled(false);
		
	}
	
	public void onClickUdpStart(View view) {
		
	}
	
	public void onClickUdpStop(View view) {
		
	}
	
	public void onClickTcpStart(View view) {
		
	}
	
	public void onClickTcpStop(View view) {
		
		
	}
	
	public void onClickPingpongStart(View view) {
		
	} 
	
	public void onClickPingpongStop(View view) {
		
	}
	
	public void onClickDebug(View view) {
		
	}
	
	private void updateMsgbox() {
		while (msgQueue.size() > 5) {
			msgQueue.poll();
		}
		StringBuilder sb = new StringBuilder();
		for (String msg : msgQueue) {
			sb.append(msg);
			sb.append('\n');
		}
		msgbox.setText(sb.toString());		
		
	}

}
